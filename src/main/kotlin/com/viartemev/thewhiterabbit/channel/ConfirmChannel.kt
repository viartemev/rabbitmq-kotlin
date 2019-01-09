package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.viartemev.thewhiterabbit.consumer.Consumer
import com.viartemev.thewhiterabbit.publisher.ConfirmPublisher

class ConfirmChannel internal constructor(val channel: Channel) : Channel by channel {
    init {
        channel.confirmSelect()
    }

    fun publisher() = ConfirmPublisher(this)
    fun consumer(queue: String) = Consumer(this, queue)
}

fun Connection.createConfirmChannel(): ConfirmChannel = ConfirmChannel(this.createChannel())