package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.CancellableContinuation
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class CustomCancelCallback(val cont: CancellableContinuation<Delivery?>) : CancelCallback {
    override fun handle(consumerTag: String?) {
        logger.debug { "Cancel callback -> tag: $consumerTag" }
    }
}