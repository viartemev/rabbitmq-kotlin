package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class CustomDeliveryCallback(val channel: Channel<String>) : DeliverCallback {
    override fun handle(consumerTag: String?, message: Delivery?) {
        logger.debug { "Handle -> tag: $consumerTag, message: ${message?.body?.let { String(it) }}" }
        message?.body?.let {
            GlobalScope.launch { channel.send(String(it)) }
        }
    }
}