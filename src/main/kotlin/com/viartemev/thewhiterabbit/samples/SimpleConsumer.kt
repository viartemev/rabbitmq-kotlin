package com.viartemev.thewhiterabbit.samples

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import com.viartemev.thewhiterabbit.channel.createConfirmChannel
import com.viartemev.thewhiterabbit.queue.Queue
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.channels.Channel as KChannel

fun main(args: Array<String>) {
    val queue = "test_queue"

    val factory = ConnectionFactory()
    factory.useNio()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createConfirmChannel().use { channel ->
            runBlocking {
                Queue.declareQueue(channel, QueueSpecification(queue))
                val consumer = channel.consumer(queue)
                consumer.consumeWithConfim { delivery: Delivery -> println("Delivery: $delivery") }
            }
        }
    }
    println("Done")
}