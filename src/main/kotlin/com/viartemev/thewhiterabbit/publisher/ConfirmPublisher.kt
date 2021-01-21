package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation

class ConfirmPublisher internal constructor(private val channel: Channel) {
    private val logger = KotlinLogging.logger {}
    internal val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()

    init {
        channel.addConfirmListener(AckListener(continuations))
    }

    /**
     * Publish a message and suspend until confirmation.
     *
     * @return acknowledgement - confirmation of the message delivery.
     * @see <a href="https://www.rabbitmq.com/confirms.html#publisher-confirms">Publisher Confirms</a>
     */
    suspend fun publishWithConfirm(message: OutboundMessage): Boolean {
        val messageSequenceNumber = channel.nextPublishSeqNo
        logger.debug { "A message Sequence Number: $messageSequenceNumber" }
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { continuations.remove(messageSequenceNumber) }
            try {
                continuations[messageSequenceNumber] = continuation
                channel.basicPublish(message.exchange, message.routingKey, message.properties, message.body)
            } catch (e: Throwable) {
                logger.error(e) { "An exception was thrown during the publishing" }
                continuations.remove(messageSequenceNumber)
                throw e
            }
        }
    }
}
