package com.viartemev.krabbit.tmp

import com.rabbitmq.client.Channel


class CoroutineSender(private val channel: Channel) {

    suspend fun send(message: String) {
        channel.basicPublish("", "hello", null, message.toByteArray(charset("UTF-8")))
    }
}