package io.github.viartemev.rabbitmq.publisher

import com.rabbitmq.client.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

private val logger = KotlinLogging.logger {}

/**
 * Publisher for confirmations in RabbitMQ.
 * Use ConfirmChannel.publisher() to obtain an instance.
 */
class ConfirmPublisher private constructor(
    private val channel: Channel,
    maxInFlightMessages: Int = 1000
) {
    internal val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()
    private val inFlightSemaphore = Semaphore(maxInFlightMessages)
    private val ackListener: AckListener = AckListener(continuations, inFlightSemaphore)
    @Volatile
    private var isClosed = false

    init {
        synchronized(channel) {
            val listeners = channel.javaClass.getDeclaredField("_confirmListeners").apply { isAccessible = true }.get(channel) as? MutableList<*>
            if (listeners != null && listeners.any { it is AckListener }) {
                throw IllegalStateException("AckListener already registered on this channel!")
            }
            channel.addConfirmListener(ackListener)
        }
    }

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
        // Only ConfirmChannel can create instance
        internal fun create(channel: Channel, maxInFlightMessages: Int = 1000): ConfirmPublisher = ConfirmPublisher(channel, maxInFlightMessages)
    }
}
