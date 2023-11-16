package io.github.viartemev.rabbitmq.example

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import io.github.viartemev.rabbitmq.channel.channel
import io.github.viartemev.rabbitmq.channel.consume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

const val CONSUMER_QUEUE_NAME = "test_queue"
const val CONSUME_TIMES = 5
val ioIntensiveFunction: suspend (Delivery) -> Unit = {
    delay(2000)
    println(message = "Message: ${String(it.body)}")
}
val handler: suspend (Delivery) -> Unit = { delivery: Delivery -> withContext(Dispatchers.IO) { ioIntensiveFunction(delivery) } }

fun main() {
    val connectionFactory = ConnectionFactory().apply { useNio() }
    val connection = connectionFactory.newConnection()
    runBlocking {
        connection.channel {
            try {
                consume(CONSUMER_QUEUE_NAME, 1) {
                    coroutineScope {
                        (1..CONSUME_TIMES).map { async { consumeMessageWithConfirm(handler) } }.awaitAll()
                    }
                }
            } catch (e: RuntimeException) {
                println("Error is here...let's rollback handler actions")
            }
        }
    }
    connection.close()
}
