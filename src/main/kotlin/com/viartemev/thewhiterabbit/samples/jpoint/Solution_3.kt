package com.viartemev.thewhiterabbit.samples.jpoint

import com.github.kittinunf.fuel.Fuel
import com.rabbitmq.client.ConnectionFactory
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val time = measureTimeMillis {
        val factory = ConnectionFactory()
        factory.host = "localhost"
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                val sender = DefaultSender("hello_3", channel)
                // Declare the queue
                channel.queueDeclare("hello_3", false, false, false, null)
                val listOf = mutableListOf<CompletableFuture<Unit>>()
                // Send 100 messages
                IntRange(1, 100)
                        .mapTo(listOf) { i ->
                            CompletableFuture
                                    .supplyAsync {
                                        val (_, _, result) = Fuel.get("http://localhost:8081/message").response()
                                        String(result.get()) + " $i"
                                    }
                                    .thenApplyAsync { message ->
                                        sender.send(message)
                                        message
                                    }
                                    .thenApplyAsync { message -> println(" [x] Sent '$message'") }
                        }
                listOf.forEach { it.join() }
            }
        }
        println("Done")
    }
    println("Time is: $time")
}