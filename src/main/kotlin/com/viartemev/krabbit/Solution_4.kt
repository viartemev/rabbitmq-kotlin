package com.viartemev.krabbit

import awaitString
import com.github.kittinunf.fuel.Fuel
import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val time = measureTimeMillis {
        val factory = ConnectionFactory()
        factory.host = "localhost"
        val connection = factory.newConnection()
        val channel = connection.createChannel()
        val sender = CoroutineSender("hello_4", channel)
        // Declare the queue
        channel.confirmSelect()
        channel.queueDeclare("hello_4", false, false, false, null)
        runBlocking {
            repeat(4) {
                launch {
                    val message = Fuel.get("http://localhost:8081/message").awaitString() + " $it"
                    val ack = sender.sendWithAck(message)
                    println(" [x] Sent '$message' ack: $ack")
                }
            }
        }
    }
    println("Done")
    println("Time is: $time")
}

