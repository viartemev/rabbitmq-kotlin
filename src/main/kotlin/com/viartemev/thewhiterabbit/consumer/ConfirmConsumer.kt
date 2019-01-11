package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class ConfirmConsumer internal constructor(private val AMQPChannel: Channel, AMQPQueue: String) {
    private val continuations = KChannel<Delivery>()

    init {
        AMQPChannel.basicConsume(AMQPQueue, false,
                DeliverCallback { consumerTag, message -> continuations.sendBlocking(message) },
                CancelCallback { logger.info { "Cancelled" } }
        )
    }

    suspend fun consumeWithConfirm(handler: suspend (Delivery) -> Unit, handlerDispatcher: CoroutineDispatcher = Dispatchers.Default) = coroutineScope {
        val delivery = continuations.receive()
        withContext(handlerDispatcher) { handler(delivery) }
        AMQPChannel.basicAck(delivery.envelope.deliveryTag, false)
    }
}
