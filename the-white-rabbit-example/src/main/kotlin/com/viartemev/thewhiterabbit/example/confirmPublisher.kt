package com.viartemev.thewhiterabbit.example

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties.PERSISTENT_BASIC
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

fun main() {
    val logger = KotlinLogging.logger {}
    val connectionFactory = ConnectionFactory().apply { useNio() }
    connectionFactory.newConnection().use { connection ->
        runBlocking {
            connection.confirmChannel {
                publish {
                    val message = OutboundMessage("", "test_queue", PERSISTENT_BASIC, "hello world")
                    val ack: Boolean = publishWithConfirm(message)
                    logger.info { "Ack: $ack" }
                }
            }
        }
    }
}
