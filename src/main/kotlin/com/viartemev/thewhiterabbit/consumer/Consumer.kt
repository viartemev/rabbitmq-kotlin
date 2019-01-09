package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class Consumer(private val AMQPChannel: Channel, private val AMQPQueue: String) {

    suspend fun consumeWithConfirm(function: suspend (Delivery) -> Unit) = coroutineScope {
        val deliveries = fetch()
        val handledDelivery = handle(deliveries, function)
        return@coroutineScope ackDelivery(handledDelivery)
    }

    private suspend fun handle(input: ReceiveChannel<Delivery>, function: suspend (Delivery) -> Unit) = coroutineScope {
        produce {
            for (delivery in input) {
                function(delivery)
                send(delivery)
            }
        }
    }

    suspend fun ackDelivery(input: ReceiveChannel<Delivery>) = coroutineScope {
        produce {
            for (delivery in input) {
                logger.debug { "Ack for delivery: $delivery" }
                AMQPChannel.basicAck(delivery.envelope.deliveryTag, false)
                send(delivery)
            }
        }
    }

    suspend fun fetch() = coroutineScope {
        produce {
            AMQPChannel.basicConsume(AMQPQueue, false,
                    DeliverCallback { consumerTag: String, message: Delivery ->
                        logger.debug { "Got message: $message" }
                        launch { send(message) }
                    },
                    CancelCallback { println("Cancelled") })
        }
    }

}