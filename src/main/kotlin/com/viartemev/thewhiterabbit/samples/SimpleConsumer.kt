package com.viartemev.thewhiterabbit.samples

import com.rabbitmq.client.ConnectionFactory
import com.viartemev.thewhiterabbit.channel.createConfirmChannel
import com.viartemev.thewhiterabbit.queue.Queue
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import kotlinx.coroutines.cancelChildren
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
                val consumeWithConfirm = consumer.fetch() //{ delivery: Delivery -> println("Delivery: $delivery") }
                for (i in 1..10) consumeWithConfirm.receive()
                coroutineContext.cancelChildren()
            }
        }
    }
    println("Done")
}