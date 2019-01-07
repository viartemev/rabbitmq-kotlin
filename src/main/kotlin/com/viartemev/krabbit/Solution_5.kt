package com.viartemev.krabbit

import com.rabbitmq.client.ConfirmListener
import com.rabbitmq.client.ConnectionFactory

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    val connection = factory.newConnection()
    val channel = connection.createChannel()
    channel.confirmSelect()
    channel.queueDeclare("hello_4", false, false, false, null)
    println("next publish tag: ${channel.nextPublishSeqNo}")
    channel.basicPublish("", "hello_4", null, "hello1".toByteArray(charset("UTF-8")))
    println("next publish tag: ${channel.nextPublishSeqNo}")
    channel.basicPublish("", "hello_4", null, "hello2".toByteArray(charset("UTF-8")))
    println("next publish tag: ${channel.nextPublishSeqNo}")
    channel.basicPublish("", "hello_4", null, "hello3".toByteArray(charset("UTF-8")))
    println("next publish tag: ${channel.nextPublishSeqNo}")
    channel.basicPublish("", "hello_4", null, "hello3".toByteArray(charset("UTF-8")))
    println("next publish tag: ${channel.nextPublishSeqNo}")
    channel.basicPublish("", "hello_4", null, "hello3".toByteArray(charset("UTF-8")))

    channel.addConfirmListener(object : ConfirmListener {
        override fun handleAck(deliveryTag: Long, multiple: Boolean) {
            println("Ack -> Tag: $deliveryTag, multiple: $multiple")
        }

        override fun handleNack(deliveryTag: Long, multiple: Boolean) {
            println("Nack -> Tag: $deliveryTag, multiple: $multiple")
        }
    })
}
