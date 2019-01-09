package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.CancellableContinuation
import mu.KotlinLogging
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.resume

private val logger = KotlinLogging.logger {}

class CustomDeliveryCallback(private val continuations: ArrayBlockingQueue<CancellableContinuation<Delivery?>>) : DeliverCallback {
    override fun handle(consumerTag: String?, message: Delivery?) {
        logger.debug { "Handle -> tag: $consumerTag, message: ${message?.body?.let { String(it) }}" }
        continuations.take().resume(message)
    }
}