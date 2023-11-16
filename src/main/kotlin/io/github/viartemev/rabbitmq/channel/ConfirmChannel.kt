package io.github.viartemev.rabbitmq.channel

import com.rabbitmq.client.Channel
import io.github.viartemev.rabbitmq.publisher.ConfirmPublisher

/**
 * A helper class for confirming messages sent through a channel.
 *
 * This class implements the [Channel] interface and adds support for message confirmation.
 * It internally delegates all channel operations to the provided channel instance.
 *
 * To use the ConfirmChannel, call the [confirmSelect] method on the underlying channel instance
 * to enable message confirmation. After enabling confirmation, you can use the [publisher] method
 * to create a [ConfirmPublisher] instance for publishing messages with confirmation support.
 *
 * @param channel The underlying channel instance to delegate channel operations to.
 */
open class ConfirmChannel internal constructor(private val channel: Channel) : Channel by channel {
    init {
        channel.confirmSelect()
    }

    fun publisher() = ConfirmPublisher(this)
}

/**
 * Suspend function to publish a message to a confirm channel.
 *
 * @param block A suspend lambda function that will be invoked with an instance of [ConfirmPublisher].
 *              This lambda function should contain the logic to publish the message.
 *              The [ConfirmPublisher] instance can be used to perform publish operations within the lambda.
 */
suspend fun ConfirmChannel.publish(block: suspend ConfirmPublisher.() -> Unit) = block(this.publisher())
