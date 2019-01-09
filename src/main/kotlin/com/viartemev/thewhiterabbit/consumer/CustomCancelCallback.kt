package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.CancelCallback
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class CustomCancelCallback : CancelCallback {
    override fun handle(consumerTag: String?) {
        logger.debug { "Cancel callback -> tag: $consumerTag" }
    }
}