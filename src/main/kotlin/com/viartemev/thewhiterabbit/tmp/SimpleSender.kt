package com.viartemev.thewhiterabbit.tmp

import com.rabbitmq.client.ConnectionFactory


private const val QUEUE_NAME = "hello"

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            channel.queueDeclare(QUEUE_NAME, false, false, false, null)
            for (i in 1..100) {
                val message = "Hello World $i"
                channel.basicPublish("", QUEUE_NAME, null, message.toByteArray(charset("UTF-8")))
                println(" [x] Sent '$message'")
            }
        }
    }
    println("Done")
}