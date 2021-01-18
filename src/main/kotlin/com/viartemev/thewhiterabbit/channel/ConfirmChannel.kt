package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.publisher.ConfirmPublisher

/**
 * A channel with enabled publisher acknowledgements
 */
open class ConfirmChannel internal constructor(private val channel: Channel) : Channel by channel {
    init {
        channel.confirmSelect()
    }

    /**
     * Build confirm publisher using the channel
     * @see com.viartemev.thewhiterabbit.publisher.ConfirmPublisher
     */
    fun publisher() = ConfirmPublisher(this)
}

/**
 * Publish a message in with enabled publisher acknowledgements
 * @see com.viartemev.thewhiterabbit.publisher.ConfirmPublisher
 */
suspend fun ConfirmChannel.publish(block: suspend ConfirmPublisher.() -> Unit) = block(this.publisher())
