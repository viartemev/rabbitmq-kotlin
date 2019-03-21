package com.viartemev.thewhiterabbit.example.rpc

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.channel.rpc
import com.viartemev.thewhiterabbit.rpc.RabbitMqMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    val connectionFactory = ConnectionFactory().apply { useNio() }
    connectionFactory.newConnection().use { connection ->
        runBlocking {
            connection.channel {
                val message = RabbitMqMessage(MessageProperties.PERSISTENT_BASIC, "Slava".toByteArray())
                println("Asking for greeting request...")
                coroutineScope {
                    val result = async {
                        rpc { call(requestQueueName = "rpc_request", message = message) }
                    }
                    launch {
                        delay(5000)
                        println("Done job")
                    }
                    println("Result: ${String(result.await().body)}")
                }
            }
        }
    }
}

