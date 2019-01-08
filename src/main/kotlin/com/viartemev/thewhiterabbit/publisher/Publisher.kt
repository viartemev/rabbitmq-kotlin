package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.Channel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class Publisher(private val channel: Channel) {
    private val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()

    init {
        channel.confirmSelect()
        channel.addConfirmListener(AckListener(continuations))
    }

    suspend fun publishWithConfirm(messages: List<OutboundMessage>): List<Boolean> = coroutineScope {
        messages.map { async { publishWithConfirm(it) } }.awaitAll()
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