package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class Consumer(private val AMQPChannel: Channel, private val AMQPQueue: String) {

    suspend fun fetch() = coroutineScope {
        val kChannel = KChannel<Delivery>()
        AMQPChannel.basicConsume(AMQPQueue, true,
                DeliverCallback { consumerTag: String, message: Delivery ->
                    logger.debug { "Got message: $message" }
                    logger.debug { "Channel is empty: ${kChannel.isEmpty}" }
                    logger.debug { "Sending is full: ${kChannel.isFull}" }
                    logger.debug { "Sending is closedForSend: ${kChannel.isClosedForSend}" }
                    launch {
                        logger.debug { "sending..." }
                        kChannel.send(message)
                    }
                },
                CancelCallback { println("Cancelled") })
        kChannel
    }

}