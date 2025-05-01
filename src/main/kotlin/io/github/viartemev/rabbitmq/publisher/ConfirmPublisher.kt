package io.github.viartemev.rabbitmq.publisher

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ShutdownListener
import com.rabbitmq.client.ShutdownSignalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

private val logger = KotlinLogging.logger {}

/**
 * Provides a coroutine-friendly way to publish messages to RabbitMQ and wait for publisher confirmations (ACK/NACK).
 * This publisher manages the correlation between published messages and their confirmations using coroutine continuations.
 * It limits the number of in-flight messages (messages published but not yet confirmed) using a semaphore.
 *
 * This class is thread-safe. It's recommended to have only one `ConfirmPublisher` instance per `Channel`.
 * Use `ConfirmChannel.publisher()` (extension function, assumed) to obtain an instance.
 *
 * @param channel The underlying RabbitMQ `Channel` configured for publisher confirms (`Channel.confirmSelect()`).
 *              The publisher adds its own `ConfirmListener` and `ShutdownListener` to this channel.
 * @param maxInFlightMessages The maximum number of messages that can be published without receiving a confirmation.
 *                            Helps to prevent overwhelming the broker or running out of memory. Defaults to 1000.
 *                            Values above 10,000 might lead to resource issues. Must be >= 1.
 */
class ConfirmPublisher private constructor(
    private val channel: Channel,
    maxInFlightMessages: Int = 1000
) {
    init {
        if (maxInFlightMessages > 10_000) {
            logger.warn { "maxInFlightMessages=$maxInFlightMessages is very high and may cause OOM or resource exhaustion!" }
        } else if (maxInFlightMessages < 1) {
            throw IllegalArgumentException("maxInFlightMessages must be >= 1")
        }
        // Автоматическое завершение всех continuations при закрытии канала
        channel.addShutdownListener(object : ShutdownListener {
            override fun shutdownCompleted(cause: ShutdownSignalException?) {
                logger.warn { "Channel is shutting down, cancelling all pending confirmations." }
                val ex = cause ?: IllegalStateException("Channel was closed while waiting for confirmation.")
                continuations.forEach { (_, continuation) ->
                    try {
                        continuation.resumeWithException(ex)
                        inFlightSemaphore.release()
                    } catch (_: Exception) {}
                }
                continuations.clear()
            }
        })
    }
    internal val continuations = ConcurrentSkipListMap<Long, Continuation<Boolean>>()
    private val inFlightSemaphore = Semaphore(maxInFlightMessages)
    private val ackListener: AckListener = AckListener(continuations, inFlightSemaphore)
    @Volatile
    private var isClosed = false

    init {
        synchronized(channel) {
            channel.addConfirmListener(ackListener)
        }
    }

    /**
     * Publishes a message and suspends the coroutine until a confirmation (ACK/NACK) is received from the broker
     * or a timeout occurs.
     *
     * Handles acquiring a permit from the in-flight message semaphore before publishing and releasing it
     * upon confirmation, cancellation, or timeout.
     *
     * @param message The [OutboundMessage] to publish.
     * @param timeoutMillis Optional timeout in milliseconds. If provided, the call will fail with [kotlinx.coroutines.TimeoutCancellationException]
     *                      if no confirmation is received within this period. If null (default), waits indefinitely.
     * @return `true` if the message was ACKed by the broker, `false` if it was NACKed.
     * @throws IllegalStateException if the publisher is already closed.
     * @throws kotlinx.coroutines.TimeoutCancellationException if a `timeoutMillis` was specified and the timeout was reached.
     * @throws kotlinx.coroutines.CancellationException if the calling coroutine is cancelled.
     * @throws java.io.IOException or other exceptions from the underlying `channel.basicPublish` if the publication fails immediately.
     * @throws Exception for other unexpected errors during the process.
     */
    suspend fun publishWithConfirm(message: OutboundMessage, timeoutMillis: Long? = null): Boolean {
        if (isClosed) throw IllegalStateException("Publisher is closed")
        val messageSequenceNumber = channel.nextPublishSeqNo
        logger.debug { "Generated message Sequence Number: $messageSequenceNumber" }
        inFlightSemaphore.acquire()
        val block: suspend () -> Boolean = {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    logger.warn { "Continuation cancelled for sequence number $messageSequenceNumber" }
                    continuations.remove(messageSequenceNumber)
                    inFlightSemaphore.release()
                }
                continuations[messageSequenceNumber] = continuation
                try {
                    message.apply { channel.basicPublish(exchange, routingKey, properties, msg) }
                    logger.debug { "Message with sequence number $messageSequenceNumber successfully published, waiting for confirm..." }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to publish message with sequence number $messageSequenceNumber" }
                    continuations.remove(messageSequenceNumber)
                    inFlightSemaphore.release()
                    try {
                        continuation.resumeWithException(e)
                    } catch (ex: Exception) {
                        logger.error(ex) { "Failed to resume continuation with exception for seqNo $messageSequenceNumber" }
                    }
                }
            }
        }
        return try {
            if (timeoutMillis != null) {
                withTimeout(timeoutMillis) { block() }
            } else {
                block()
            }
        } catch(e: Exception) {
            logger.error(e) { "Error in publishWithConfirm for sequence number $messageSequenceNumber" }
            throw e
        }
    }

    /**
     * Closes the publisher, preventing new publications and cancelling any pending confirmations.
     *
     * Removes the internal `ConfirmListener` from the channel.
     * Any coroutines currently suspended in `publishWithConfirm` will be resumed with an [IllegalStateException].
     * This method is idempotent; calling it multiple times has no effect after the first call.
     * Note: This does *not* close the underlying `Channel`.
     */
    fun close() {
        synchronized(channel) {
            if (isClosed) return
            isClosed = true
            try {
                channel.removeConfirmListener(ackListener)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to remove confirm listener. Channel might be closed already." }
            }
            val ex = IllegalStateException("Publisher closed while waiting for confirmation.")
            continuations.forEach { (_, continuation) ->
                try {
                    continuation.resumeWithException(ex)
                    inFlightSemaphore.release()
                } catch (_: Exception) {}
            }
            continuations.clear()
        }
    }

    companion object {
        /**
         * Factory method to create a [ConfirmPublisher]. Intended for internal use (e.g., by channel extensions).
         * Note: It's crucial to ensure that only one ConfirmPublisher is created per channel if using this directly.
         *
         * @param channel The channel to use, must have publisher confirms enabled.
         * @param maxInFlightMessages The maximum number of unconfirmed messages allowed.
         * @return A new instance of [ConfirmPublisher].
         */
        internal fun create(channel: Channel, maxInFlightMessages: Int = 1000): ConfirmPublisher = ConfirmPublisher(channel, maxInFlightMessages)
    }
}
