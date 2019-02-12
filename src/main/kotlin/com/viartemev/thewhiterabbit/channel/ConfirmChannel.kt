package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.viartemev.thewhiterabbit.publisher.ConfirmPublisher

class ConfirmChannel internal constructor(private val channel: Channel) : Channel by channel {
    init {
        channel.confirmSelect()
    }

    fun publisher() = ConfirmPublisher(this)
}

/**
 * Create a channel with enabled publisher acknowledgements on it.
 * @see com.rabbitmq.client.Channel.confirmSelect()
 */
fun Connection.createConfirmChannel(): ConfirmChannel = ConfirmChannel(this.createChannel())

suspend fun Connection.confirmChannel(block: suspend ConfirmChannel.() -> Unit): ConfirmChannel {
    val confirmChannel = this.createConfirmChannel()
    confirmChannel.use { block(it) }
    return confirmChannel
}

suspend fun ConfirmChannel.publish(block: suspend ConfirmPublisher.() -> Unit) {
    val publisher = this.publisher()
    block(publisher)
}
