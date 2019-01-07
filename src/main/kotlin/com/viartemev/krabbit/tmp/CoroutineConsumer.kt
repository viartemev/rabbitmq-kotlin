package com.viartemev.krabbit.tmp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private val QUEUE_NAME = "hello"

fun main(args: Array<String>) = runBlocking {
    val connectionFactory = ConnectionFactory()
    connectionFactory.host = "localhost"

    val connection = connectionFactory.newConnection()
    val channel = connection.createChannel()

    channel.exchangeDeclare(QUEUE_NAME, "fanout")
    channel.queueDeclare(QUEUE_NAME, false, false, false, null)
    channel.queueBind(QUEUE_NAME, QUEUE_NAME, "")
    println(" [*] Waiting for messages. To exit press CTRL+C")

    val message = consumeMessage(channel)
    println("Handling something...")
    delay(500)
    ack(channel, message.first)
    println("Done. Result is: ")
    println(message)
}

suspend fun ack(channel: Channel, deliveryTag: Long): Boolean {
    return suspendCancellableCoroutine { continuation ->
        channel.basicAck(deliveryTag, false)
        continuation.resume(true)
    }
}

suspend fun consumeMessage(channel: Channel): Pair<Long, String> {
    return suspendCancellableCoroutine { continuation ->
        channel.basicConsume(QUEUE_NAME, false, object : DefaultConsumer(channel) {
            override fun handleDelivery(consumerTag: String?, envelope: Envelope?, properties: AMQP.BasicProperties?, body: ByteArray?) {
                val message = String(body!!)
                val deliveryTag = envelope?.deliveryTag!!

                println(String.format("Processed %s %s", Thread.currentThread().name, message))
                continuation.resume(deliveryTag to message)
            }
        })
    }
}