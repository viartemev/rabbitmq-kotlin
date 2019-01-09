package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import java.util.concurrent.ArrayBlockingQueue

private val logger = KotlinLogging.logger {}

class Consumer(private val AMQPChannel: Channel, queue: String) {
    private val continuations = ArrayBlockingQueue<CancellableContinuation<Delivery?>>(10)

    init {
        AMQPChannel.basicConsume(queue, true, CustomDeliveryCallback(continuations), CustomCancelCallback())
    }

    suspend fun consume(function: suspend (Delivery) -> Unit) = coroutineScope {
        produce<Delivery> {
            val deliveries = fetch()
            val handledDelivery = handle(deliveries, function)
            ackDelivery(handledDelivery)
        }
    }

    private suspend fun handle(input: ReceiveChannel<Delivery>, function: suspend (Delivery) -> Unit) = coroutineScope {
        produce<Delivery> {
            input.map { delivery ->
                function(delivery)
                delivery
            }
        }
    }

    suspend fun ackDelivery(input: ReceiveChannel<Delivery>) = coroutineScope {
        produce<Delivery> {
            for (delivery in input) {
                logger.debug { "Ack for delivery: $delivery" }
                AMQPChannel.basicAck(delivery.envelope.deliveryTag, false)
            }
        }
    }

    suspend fun fetch() = coroutineScope {
        produce<Delivery> {
            //there is fetching from RabbitMQ
        }
    }

}