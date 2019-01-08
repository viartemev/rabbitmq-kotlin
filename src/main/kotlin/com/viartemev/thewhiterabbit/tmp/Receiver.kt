package com.viartemev.thewhiterabbit.tmp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import java.util.concurrent.Executors


private val QUEUE_NAME = "hello"

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    val es = Executors.newFixedThreadPool(20)
    val connection = factory.newConnection(es)
    val channel = connection.createChannel()

    channel.queueDeclare(QUEUE_NAME, false, false, false, null)
    println(" [*] Waiting for messages. To exit press CTRL+C")

    channel.basicConsume(QUEUE_NAME, false,
            object : DefaultConsumer(channel) {
                override fun handleDelivery(consumerTag: String?,
                                            envelope: Envelope?,
                                            properties: AMQP.BasicProperties?,
                                            body: ByteArray?) {
                    val deliveryTag = envelope?.deliveryTag
                    val message = String(body!!, Charsets.UTF_8)
                    println(" [x] Started... '$message'")
                    Thread.sleep(4000)
                    println(" [x] Finished... '$message'")

                    channel.basicAck(deliveryTag ?: 1, false)
                }
            })
}
