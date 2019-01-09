package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class Consumer(private val channel: Channel) {

    suspend fun consumeAutoAckAsync(queue: String, output: KChannel<String>, numberOfConsumers: Int) = coroutineScope {
        for (i in 1..numberOfConsumers) {
            launch { consumeAutoAckAsync(queue, output) }
        }
    }

    suspend fun consumeAutoAckAsync(queue: String, output: KChannel<String>) {
        logger.debug { "Start consuming..." }
        while (true) {
            val delivery = consume(queue)
            logger.debug { "Got message: $delivery" }
            delivery?.let {
                val body = String(it.body)
                output.send(body)
            }
        }
    }

    private suspend fun consume(queue: String): Delivery? {
        return suspendCancellableCoroutine { cont ->
            channel.basicConsume(queue, true,
                    DeliverCallback { consumerTag, message ->
                        if (!cont.isCompleted) {
                            //TODO handle IOException
                            channel.basicCancel(consumerTag)
                            cont.resume(message)
                        }
                    },
                    CancelCallback { logger.debug { "Cancel" } })
        }
    }
}