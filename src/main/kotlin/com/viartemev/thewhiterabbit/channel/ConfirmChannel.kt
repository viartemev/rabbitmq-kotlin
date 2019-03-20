package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.publisher.ConfirmPublisher

open class ConfirmChannel internal constructor(private val channel: Channel) : Channel by channel {
    init {
        channel.confirmSelect()
    }

    fun publisher() = ConfirmPublisher(this)
}

suspend fun ConfirmChannel.publish(block: suspend ConfirmPublisher.() -> Unit) = block(this.publisher())
