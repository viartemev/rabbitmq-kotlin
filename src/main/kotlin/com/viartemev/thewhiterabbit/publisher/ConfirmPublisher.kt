package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.Channel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation

private val logger = KotlinLogging.logger {}

class ConfirmPublisher internal constructor(private val channel: Channel) {
    private val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()

    init {
        channel.addConfirmListener(AckListener(continuations))
    }

    suspend fun publish(messages: List<OutboundMessage>): List<Boolean> = coroutineScope {
        messages.map { async { publish(it) } }.awaitAll()
    }

    suspend fun publish(message: OutboundMessage): Boolean {
        val messageSequenceNumber = channel.nextPublishSeqNo
        logger.debug { "The message Sequence Number: $messageSequenceNumber" }

        return suspendCancellableCoroutine { continuation ->
            continuations[messageSequenceNumber] = continuation
            message.run {
                channel.basicPublish(exchange, routingKey, properties, body)
            }
        }
    }
}