package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class Consumer(private val channel: Channel) {

    suspend fun consumeManualAck(queue: String): Delivery? {
        return suspendCancellableCoroutine { cont ->
            channel.basicConsume(queue, false, CustomDeliveryCallback(cont), CustomCancelCallback(cont))
        }
    }
}