package com.github.viartemev.rabbitmq.publisher

import com.rabbitmq.client.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation

private val logger = KotlinLogging.logger {}

/**
 * A class that represents a publisher for confirmations in RabbitMQ.
 *
 * @property channel The channel used for communication with RabbitMQ.
 * @property continuations A thread-safe map of message sequence numbers to continuations.
 * @constructor Creates a ConfirmPublisher with the specified channel.
 */
class ConfirmPublisher internal constructor(private val channel: Channel) {
    internal val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()

    init {
        channel.addConfirmListener(AckListener(continuations))
    }

    /**
     * Publishes a message with confirmation to the specified exchange and routing key.
     *
     * @param message The {@link OutboundMessage} to publish.
     * @return True if the message was published successfully, false otherwise.
     */
    suspend fun publishWithConfirm(message: OutboundMessage): Boolean {
        val messageSequenceNumber = channel.nextPublishSeqNo
        logger.debug { "Generated message Sequence Number: $messageSequenceNumber" }
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { continuations.remove(messageSequenceNumber) }
            continuations[messageSequenceNumber] = continuation
            message.apply { channel.basicPublish(exchange, routingKey, properties, msg) }
            logger.debug { "Message successfully published" }
        }
    }
}
