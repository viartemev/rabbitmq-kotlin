package com.viartemev.krabbit.samples

import com.github.kittinunf.fuel.Fuel
import com.rabbitmq.client.ConnectionFactory
import kotlin.system.measureTimeMillis

/**
 * Problem -> do a request for a message and after sendSuspended the messages to the queue
 * This approach doesn't work well
 */
fun main(args: Array<String>) {
    val time = measureTimeMillis {
        val factory = ConnectionFactory()
        factory.host = "localhost"
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                val sender = DefaultSender("hello_2", channel)
                // Declare the queue
                channel.queueDeclare("hello_2", false, false, false, null)

                // Send 100 messages
                for (i in 1..100) {
                    val (_, _, result) = Fuel.get("http://localhost:8081/message").response()
                    val message = String(result.get()) + " $i"
                    sender.send(message)
                    println(" [x] Sent '$message'")
                }
            }
        }
        println("Done")
    }
    println("Time is: $time")
}