package com.viartemev.whiterabbit.samples.queue

import com.rabbitmq.client.ConnectionFactory
import com.viartemev.whiterabbit.queue.Queue
import com.viartemev.whiterabbit.queue.QueueSpecification
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            runBlocking {
                Queue.declareQueue(channel, QueueSpecification("name"))
            }
        }
    }
    println("Done")
}