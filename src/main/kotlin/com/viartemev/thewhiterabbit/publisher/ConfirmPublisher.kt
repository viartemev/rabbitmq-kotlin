package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.exception.PublishException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

private val logger = KotlinLogging.logger {}

class ConfirmPublisher internal constructor(private val channel: Channel) {
    private val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()

    init {
        channel.addConfirmListener(AckListener(continuations))
    }

    /**
     * Publish a message with the waiting of confirmation.
     *
     * @see com.viartemev.thewhiterabbit.publisher.OutboundMessage
     * @return acknowledgement - represent messages handled successfully or lost by the broker.
     * @throws com.viartemev.thewhiterabbit.exception.PublishException if can't publish the message
     */
    suspend fun publishWithConfirm(message: OutboundMessage): Boolean {
        val messageSequenceNumber = channel.nextPublishSeqNo
        logger.debug { "The message Sequence Number: $messageSequenceNumber" }
        try {
            return suspendCancellableCoroutine { continuation ->
                continuations[messageSequenceNumber] = continuation
                message.run { channel.basicPublish(exchange, routingKey, properties, msg.toByteArray()) }
            }
        } catch (e: Exception) {
            continuations[messageSequenceNumber]?.resumeWithException(e)
        }
        throw PublishException("Can't publish message: $message")
    }

    /**
     * Asynchronously publish a message with the waiting of confirmation.
     *
     * @see com.viartemev.thewhiterabbit.publisher.OutboundMessage
     * @return acknowledgement - represent messages handled successfully or lost by the broker.
     * @throws java.io.IOException if an error is encountered
     */
    suspend fun asyncPublishWithConfirm(message: OutboundMessage): Deferred<Boolean> = coroutineScope {
        async { publishWithConfirm(message) }
    }

    /**
     * Publish a list of messages with the waiting of confirmation.
     *
     * @see com.viartemev.thewhiterabbit.publisher.OutboundMessage
     * @return list of acknowledgements - represent messages handled successfully or lost by the broker.
     * @throws java.io.IOException if an error is encountered
     */
    suspend fun publishWithConfirm(messages: List<OutboundMessage>): List<Boolean> {
        return messages.map { publishWithConfirm(it) }
    }

    /**
     * Asynchronously publish a list of messages with the waiting of confirmation.
     *
     * @see com.viartemev.thewhiterabbit.publisher.OutboundMessage
     * @return list of acknowledgements - represent messages handled successfully or lost by the broker.
     * @throws java.io.IOException if an error is encountered
     */
    suspend fun asyncPublishWithConfirm(messages: List<OutboundMessage>): List<Deferred<Boolean>> = coroutineScope {
        messages.map { async { publishWithConfirm(it) } }
    }
}
