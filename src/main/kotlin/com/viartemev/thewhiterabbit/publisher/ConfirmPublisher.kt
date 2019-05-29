package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.common.cancelOnIOException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger {}

class ConfirmPublisher internal constructor(private val channel: Channel) {
    internal val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()

    init {
        channel.addConfirmListener(AckListener(continuations))
    }

    /**
     * Publish a message with the waiting of confirmation.
     *
     * @see com.viartemev.thewhiterabbit.publisher.OutboundMessage
     * @return acknowledgement - represent messages handled successfully or lost by the broker.
     * @throws java.util.concurrent.CancellationException if can't publish the message
     */
    suspend fun publishWithConfirm(message: OutboundMessage): Boolean {
        val messageSequenceNumber = channel.nextPublishSeqNo
        logger.debug { "The message Sequence Number: $messageSequenceNumber" }
        return suspendCancellableCoroutine { continuation ->
            continuations[messageSequenceNumber] = continuation
            continuation.invokeOnCancellation { continuations.remove(messageSequenceNumber) }
            cancelOnIOException(continuation) {
                message.run { channel.basicPublish(exchange, routingKey, properties, msg.toByteArray()) }
            }
        }
    }

    /**
     * Asynchronously publish a list of messages with the waiting of confirmation.
     *
     * @see com.viartemev.thewhiterabbit.publisher.OutboundMessage
     * @param coroutineContext strictly one-thread context to execute coroutines
     * @return list of acknowledgements - represent messages handled successfully or lost by the broker.
     * @throws java.util.concurrent.CancellationException if can't publish one of the messages
     */
    suspend fun publishWithConfirmAsync(
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        messages: List<OutboundMessage>
    ): List<Deferred<Boolean>> = coroutineScope {
        messages.map { async(coroutineContext) { publishWithConfirm(it) } }
    }
}
