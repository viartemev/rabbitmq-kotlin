package com.viartemev.thewhiterabbit.example

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

val EXCHANGE_NAME = ""
val QUEUE_NAME = "test_queue"
val times = 100_000

fun main() {
    val connectionFactory = ConnectionFactory().apply { useNio() }
    val connection = connectionFactory.newConnection()
    runBlocking {
        connection.confirmChannel {
            publish {
                coroutineScope {
                    val messages = (1..times).map { createMessage("") }
                    publishWithConfirmAsync(this.coroutineContext, messages).awaitAll()
                }
            }
        }
    }
    connection.close()
}

private fun createMessage(body: String) =
    OutboundMessage(EXCHANGE_NAME, QUEUE_NAME, MessageProperties.PERSISTENT_BASIC, body)
