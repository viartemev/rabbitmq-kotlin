package com.viartemev.krabbit

import com.rabbitmq.client.ConfirmListener
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            val sender = CoroutineSender("test", channel)
            channel.confirmSelect()
            val queue = "test_with"
            channel.queueDeclare(queue, false, false, false, null)

            channel.addConfirmListener(object : ConfirmListener {
                override fun handleAck(deliveryTag: Long, multiple: Boolean) {
                    println("Ack -> tag: $deliveryTag, mult: $multiple")
                }

                override fun handleNack(deliveryTag: Long, multiple: Boolean) {
                    println("Nack -> tag: $deliveryTag, mult: $multiple")
                }
            })

            runBlocking {
                repeat(100) {
                    launch {
                        //val message = Fuel.get("http://localhost:8081/message").awaitString() + " $it"
                        val message = "Hello from coroutine"
                        //val ack = sender.sendWithAck(message)
                        println("seqNo: ${channel.nextPublishSeqNo}")
                        channel.basicPublish("", queue, MessageProperties.PERSISTENT_BASIC, message.toByteArray(charset("UTF-8")))

                        //println(" [x] Sent '$message' ack: $ack")
                    }
                }
                delay(5000)
            }
        }
    }
    println("Done")
}

