package io.github.viartemev.rabbitmq.example

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import io.github.viartemev.rabbitmq.channel.channel
import io.github.viartemev.rabbitmq.channel.consume
import kotlinx.coroutines.*

const val CONSUMER_QUEUE_NAME = "test_queue"
const val CONSUME_TIMES = 5
val ioIntensiveFunction: suspend (Delivery) -> Unit = {
    delay(2000)
    println(message = "Message: ${String(it.body)}")
}
val handler: suspend (Delivery) -> Unit =
    { delivery: Delivery -> withContext(Dispatchers.IO) { ioIntensiveFunction(delivery) } }

fun main(): Unit = runBlocking {
    val connectionFactory = ConnectionFactory().apply { useNio() }
    connectionFactory.newConnection().use { connction ->
        connction.channel {
            try {
                consume(CONSUMER_QUEUE_NAME, 1) {
                    (1..CONSUME_TIMES).map { async(Dispatchers.IO) { consumeMessageWithConfirm(handler) } }.awaitAll()
                }
            } catch (e: RuntimeException) {
                println("Error is here...let's rollback handler actions")
            }
        }
    }
}
