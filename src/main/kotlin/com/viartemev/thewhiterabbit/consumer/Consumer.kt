package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class Consumer(private val AMQPChannel: Channel, private val AMQPQueue: String) {
    private val continuations = LinkedBlockingQueue<Continuation<Delivery>>()

    init {
        AMQPChannel.basicConsume(AMQPQueue, false,
                DeliverCallback { consumerTag, message -> continuations.take().resume(message) },
                CancelCallback { logger.info { "Cancelled" } }
        )
    }

    suspend fun consume(function: suspend (Delivery) -> Unit) = coroutineScope {
        val delivery = suspendCancellableCoroutine<Delivery> { continuations.offer(it) }
        function(delivery)
        AMQPChannel.basicAck(delivery.envelope.deliveryTag, false)
    }
}
