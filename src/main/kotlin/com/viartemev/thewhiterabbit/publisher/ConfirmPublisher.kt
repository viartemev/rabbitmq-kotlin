package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.Channel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resumeWithException

class ConfirmPublisher internal constructor(private val channel: Channel) {
    private val logger = KotlinLogging.logger {}
    internal val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()

    init {
        channel.addConfirmListener(AckListener(continuations))
    }

    /**
     * Publish a message and expect confirmations.
     *
     * @return acknowledgement - confirmation of the message delivery.
     * @see <a href="https://www.rabbitmq.com/confirms.html#publisher-confirms">Publisher Confirms</a>
     */
    suspend fun publishWithConfirm(message: OutboundMessage): Boolean {
        val messageSequenceNumber = channel.nextPublishSeqNo
        logger.debug { "The message Sequence Number: $messageSequenceNumber" }
        return suspendCancellableCoroutine { continuation ->
            continuations[messageSequenceNumber] = continuation
            continuation.invokeOnCancellation { continuations.remove(messageSequenceNumber) }
            try {
                channel.basicPublish(message.exchange, message.routingKey, message.properties, message.msg)
            } catch (e: Throwable) {
                logger.error(e) { "Can't publish them message" }
                continuation.resumeWithException(e)
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
    @Deprecated(message = "TODO: Link to WIKI")
    suspend fun publishWithConfirmAsync(
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        messages: List<OutboundMessage>
    ): List<Deferred<Boolean>> = coroutineScope {
        messages.map { async(coroutineContext) { publishWithConfirm(it) } }
    }
}
