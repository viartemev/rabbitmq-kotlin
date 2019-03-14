package com.viartemev.thewhiterabbit.example

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

const val PUBLISHER_EXCHANGE_NAME = ""
const val PUBLISHER_QUEUE_NAME = "test_queue"
const val TIMES = 100_000

fun main() {
    val connectionFactory = ConnectionFactory().apply { useNio() }
    val connection = connectionFactory.newConnection()
    runBlocking {
        connection.confirmChannel {
            publish {
                coroutineScope {
                    val messages = (1..TIMES).map { createMessage("") }
                    publishWithConfirmAsync(this.coroutineContext, messages).awaitAll()
                }
            }
        }
    }
    connection.close()
}

private fun createMessage(body: String) =
    OutboundMessage(PUBLISHER_EXCHANGE_NAME, PUBLISHER_QUEUE_NAME, MessageProperties.PERSISTENT_BASIC, body)
