package com.viartemev.whiterabbit.samples

import com.rabbitmq.client.ConfirmListener
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            channel.confirmSelect()
            val queue = "test_without"
            channel.queueDeclare(queue, false, true, true, null)
            for (i in 1..10) {
                println("next publish tag: ${channel.nextPublishSeqNo}")
                channel.basicPublish("", queue, MessageProperties.PERSISTENT_BASIC, "hello1".toByteArray(charset("UTF-8")))
            }

            channel.addConfirmListener(object : ConfirmListener {
                override fun handleAck(deliveryTag: Long, multiple: Boolean) {
                    println("Ack -> tag: $deliveryTag, mult: $multiple")
                }

                override fun handleNack(deliveryTag: Long, multiple: Boolean) {
                    println("Nack -> tag: $deliveryTag, mult: $multiple")
                }
            })
            Thread.sleep(5000)
        }
    }
}
