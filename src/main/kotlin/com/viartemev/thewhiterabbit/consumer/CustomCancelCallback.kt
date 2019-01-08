package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.CancelCallback
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class CustomCancelCallback(val channel: Channel<String>) : CancelCallback {
    override fun handle(consumerTag: String?) {
        logger.debug { "Cancel callback -> tag: $consumerTag" }
        channel.close()
    }
}