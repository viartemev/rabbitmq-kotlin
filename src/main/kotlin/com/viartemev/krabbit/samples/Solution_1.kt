package com.viartemev.krabbit.samples

import com.rabbitmq.client.ConnectionFactory

/**
 * Problem -> sendSuspended 100 messages to the queue
 */
fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            val sender = DefaultSender("hello_1", channel)
            // Declare the queue
            channel.queueDeclare("hello_1", false, false, false, null)

            // Send 100 messages
            for (i in 1..100) {
                val message = "Hello World $i"
                sender.send(message)
                println(" [x] Sent '$message'")
            }
        }
    }
    println("Done")
}