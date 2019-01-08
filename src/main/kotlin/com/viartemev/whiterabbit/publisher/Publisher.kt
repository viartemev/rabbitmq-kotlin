package com.viartemev.whiterabbit.publisher

import com.rabbitmq.client.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class Publisher(private val channel: Channel) {
    private val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()

    init {
        channel.addConfirmListener(AckListener(continuations))
    }

    //FIXME do we really need coroutine scope and launch there?
    suspend fun publish(message: OutboundMessage) = coroutineScope {
        launch {
            message.run { channel.basicPublish(exchange, routingKey, properties, body) }
        }
    }

    suspend fun publish(messages: List<OutboundMessage>) = coroutineScope {
        launch { for (message in messages) publish(message) }
    }

    //FIXME coroutine scope???
    suspend fun publishWithConfirm(message: OutboundMessage): Boolean {
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