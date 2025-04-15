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
 * A class that represents a publisher for confirmations in RabbitMQ.
 *
 * @property channel The channel used for communication with RabbitMQ.
 * @property continuations A thread-safe map of message sequence numbers to continuations.
 * @constructor Creates a ConfirmPublisher with the specified channel.
 */
class ConfirmPublisher internal constructor(
    private val channel: Channel,
    maxInFlightMessages: Int = 1000
) {
    internal val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()
    private val inFlightSemaphore = Semaphore(maxInFlightMessages)

    @Volatile
    private var isClosed = false

    init {
        channel.addConfirmListener(AckListener(continuations, inFlightSemaphore))
    }

    /**
     * Publishes a message with confirmation to the specified exchange and routing key.
     *
     * @param message The {@link OutboundMessage} to publish.
     * @param timeoutMillis Optional timeout in milliseconds for ожидания подтверждения. Если null — ждать бесконечно.
     * @return True if the message was published успешно, false otherwise.
     */
    suspend fun publishWithConfirm(message: OutboundMessage, timeoutMillis: Long? = null): Boolean {
        if (isClosed) throw IllegalStateException("Publisher is closed")
        val messageSequenceNumber = channel.nextPublishSeqNo
        logger.debug { "Generated message Sequence Number: $messageSequenceNumber" }
        inFlightSemaphore.acquire()
        val block: suspend () -> Boolean = {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    continuations.remove(messageSequenceNumber)
                    inFlightSemaphore.release()
                }
                continuations[messageSequenceNumber] = continuation
                try {
                    message.apply { channel.basicPublish(exchange, routingKey, properties, msg) }
                    logger.debug { "Message successfully published" }
                } catch (e: Exception) {
                    continuations.remove(messageSequenceNumber)
                    inFlightSemaphore.release()
                    continuation.resumeWithException(e)
                }
            }
        }
        return if (timeoutMillis != null) {
            withTimeout(timeoutMillis) { block() }
        } else {
            block()
        }
    }
}
