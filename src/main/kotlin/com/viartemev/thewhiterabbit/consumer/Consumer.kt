package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class Consumer(channel: Channel, queue: String) {
    private val continuations = ArrayBlockingQueue<CancellableContinuation<Delivery?>>(10)
    private val counter = AtomicLong()

    init {
        channel.basicConsume(queue, true, CustomDeliveryCallback(continuations), CustomCancelCallback())
    }

    suspend fun consumeWithoutAck(): Delivery? {
        return suspendCancellableCoroutine { cont ->
            logger.debug { "Starting consuming coroutine ${counter.incrementAndGet()}" }
            continuations.put(cont)
        }
    }
}