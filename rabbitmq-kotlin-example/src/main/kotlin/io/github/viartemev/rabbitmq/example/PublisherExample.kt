package io.github.viartemev.rabbitmq.example

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import io.github.viartemev.rabbitmq.channel.confirmChannel
import io.github.viartemev.rabbitmq.channel.publish
import io.github.viartemev.rabbitmq.publisher.OutboundMessage
import io.github.viartemev.rabbitmq.queue.QueueSpecification
import io.github.viartemev.rabbitmq.queue.declareQueue
import kotlinx.coroutines.*

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
