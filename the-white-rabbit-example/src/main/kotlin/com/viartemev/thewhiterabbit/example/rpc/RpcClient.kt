package com.viartemev.thewhiterabbit.example.rpc

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.channel.rpc
import com.viartemev.thewhiterabbit.common.RabbitMqMessage
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
                val job = coroutineScope {
                    val result = async {
                        rpc { call(requestQueueName = "rpc_request", message = message) }
                    }
                    launch {
                        println("Another important job is in process...")
                        delay(1000)
                        println("Job is done")
                    }
                    return@coroutineScope result
                }
                println("Result: ${String(job.await().body)}")
            }
        }
    }
}

