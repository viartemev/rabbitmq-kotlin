package com.viartemev.krabbit

import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            val queue = "test"
            val sender = CoroutineSender(queue, channel)
            channel.confirmSelect()
            channel.queueDeclare(queue, false, false, false, null)

            runBlocking {
                repeat(4) {
                    launch {
                        val message = "Hello from coroutine ${it + 1}"
                        val ack = sender.sendWithAck(message)
                        println(" [x] Sent '$message' ack: $ack")
                    }
                }
            }
        }
    }
    println("Done")
}

