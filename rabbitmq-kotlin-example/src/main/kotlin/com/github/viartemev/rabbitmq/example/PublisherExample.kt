package com.github.viartemev.rabbitmq.example

import com.github.viartemev.rabbitmq.channel.confirmChannel
import com.github.viartemev.rabbitmq.channel.publish
import com.github.viartemev.rabbitmq.publisher.OutboundMessage
import com.github.viartemev.rabbitmq.queue.QueueSpecification
import com.github.viartemev.rabbitmq.queue.declareQueue
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

const val PUBLISHER_EXCHANGE_NAME = ""
const val PUBLISHER_QUEUE_NAME = "test_queue"
const val TIMES = 1_000

fun main() = runBlocking {
    val connectionFactory = ConnectionFactory().apply { useNio() }
    val connection = connectionFactory.newConnection()
    connection.confirmChannel {
        declareQueue(QueueSpecification(PUBLISHER_QUEUE_NAME)).queue
        publish {
            coroutineScope {
                val messages = (1..TIMES).map { createMessage("") }.map {  publishWithConfirm(it) }
            }
        }
    }
    connection.close()
}

private fun createMessage(body: String) =
    OutboundMessage(PUBLISHER_EXCHANGE_NAME, PUBLISHER_QUEUE_NAME, MessageProperties.PERSISTENT_BASIC, body)
