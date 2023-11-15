package com.github.viartemev.rabbitmq.example.rpc

import com.github.viartemev.rabbitmq.channel.channel
import com.github.viartemev.rabbitmq.channel.rpc
import com.github.viartemev.rabbitmq.publisher.OutboundMessage
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(): Unit = runBlocking {
    val connection = ConnectionFactory().apply { useNio() }.newConnection()
    val message = OutboundMessage("", "rpc_request", MessageProperties.PERSISTENT_BASIC, "Slava")
    connection.use { conn ->
        conn.channel {
            logger.info { "Asking for greeting request..." }
            val response = withTimeoutOrNull(1000) {
                async(Dispatchers.IO) {
                    rpc {
                        val result = call(message)
                        logger.info { "Got a message: ${String(result.body)}" }
                        result
                    }
                }.await()
            }
            if (response == null) {
                logger.info { "Timeout is exeeded" }
            } else {
                logger.info { "Result: ${String(response.body)}" }
            }
        }
    }
}

