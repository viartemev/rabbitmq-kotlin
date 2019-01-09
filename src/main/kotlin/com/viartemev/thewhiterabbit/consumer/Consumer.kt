package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlinx.coroutines.channels.Channel as KChannel


private val logger = KotlinLogging.logger {}

class Consumer(private val AMQPChannel: Channel, private val AMQPQueue: String) {

    suspend fun consume(function: (Delivery) -> Unit): KChannel<Delivery> {
        val kChannel = KChannel<Delivery>()
        AMQPChannel.basicConsume(AMQPQueue, false,
                DeliverCallback { consumerTag: String, message: Delivery ->
                    logger.debug { "Got message: $message" }
                    //FIXME globalscope???
                    GlobalScope.launch {
                        println("Sending to channel...")
                        function(message)
                        AMQPChannel.basicAck(message.envelope.deliveryTag, false)
                        kChannel.send(message)
                    }
                },
                CancelCallback { println("Cancelled") })
        return kChannel
    }
}