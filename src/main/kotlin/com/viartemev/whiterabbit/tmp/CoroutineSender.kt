package com.viartemev.whiterabbit.tmp

import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val QUEUE_NAME = "hello"

fun main(args: Array<String>) = runBlocking {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            channel.queueDeclare(QUEUE_NAME, false, false, false, null)
            GlobalScope.launch {
                repeat(100) { index ->
                    launch {
                        val message = "Hello World $index"
                        if (index == 33) {
                            println("Die...")
                            throw RuntimeException()
                        }
                        channel.basicPublish("", QUEUE_NAME, null, message.toByteArray(charset("UTF-8")))
                        println(" [x] Sent '$message'")
                    }
                }
                channel.waitForConfirmsOrDie()
            }
        }
    }
    println("Done")
}