package com.viartemev.krabbit.samples

import com.rabbitmq.client.Channel

class DefaultSender(private val queueName: String, private val channel: Channel) {

    fun send(message: String) {
        channel.basicPublish("", queueName, null, message.toByteArray(charset("UTF-8")))
    }
}