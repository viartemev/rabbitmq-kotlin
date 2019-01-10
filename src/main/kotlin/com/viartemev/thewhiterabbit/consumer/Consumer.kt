package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class Consumer(private val AMQPChannel: Channel, AMQPQueue: String, private val handlerDispatcher: CoroutineDispatcher) {
    //FIXME the queue size is Integer.MAX_VALUE
    private val continuations = LinkedBlockingQueue<Continuation<Delivery>>()

    init {
        AMQPChannel.basicConsume(AMQPQueue, false,
                DeliverCallback { consumerTag, message -> continuations.take().resume(message) },
                CancelCallback { logger.info { "Cancelled" } }
        )
    }

    suspend fun consume(handler: suspend (Delivery) -> Unit) = coroutineScope {
        val delivery = suspendCancellableCoroutine<Delivery> { continuations.offer(it) }
        withContext(handlerDispatcher) { handler(delivery) }
        AMQPChannel.basicAck(delivery.envelope.deliveryTag, false)
    }
}
