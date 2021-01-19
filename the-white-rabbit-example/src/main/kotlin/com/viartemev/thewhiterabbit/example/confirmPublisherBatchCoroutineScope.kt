package com.viartemev.thewhiterabbit.example

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.common.RabbitMqDispatchers
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import mu.KotlinLogging


fun main() {
    val logger = KotlinLogging.logger {}
    val times = 50
    val message = OutboundMessage("", "test_queue", MessageProperties.PERSISTENT_BASIC, "hello world")
    val connectionFactory = ConnectionFactory().apply { useNio() }
    try {
        connectionFactory.newConnection().use { connection ->
            runBlocking {
                connection.confirmChannel {
                    publish {
                        try {
                            supervisorScope {
                                val messages: List<Boolean> = (1..times)
                                    .map { message }
                                    .map { async(RabbitMqDispatchers.SingleThreadDispatcher) { publishWithConfirm(it) } }
                                    .map {
                                        try {
                                            it.await()
                                        } catch (e: Throwable) {
                                            logger.error(e) { "Failed to send the message" }
                                            false
                                        }
                                    }
                                val partitions: Pair<List<Boolean>, List<Boolean>> = messages.partition { it }
                                logger.info { "$times messages were sent, delivered: ${partitions.first.size}, not delivered: ${partitions.second.size}" }
                            }
                        } catch (e: Throwable) {
                            logger.error(e) { "Here an error: $e" }
                        }
                    }
                }
            }
        }
    } finally {
        RabbitMqDispatchers.SingleThreadDispatcher.close()
    }
}
