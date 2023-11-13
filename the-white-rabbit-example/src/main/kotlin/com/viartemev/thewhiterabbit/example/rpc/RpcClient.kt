package com.viartemev.thewhiterabbit.example.rpc

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.channel.rpc
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
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
                async {
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

