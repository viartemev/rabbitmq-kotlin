package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection


/**
 * Create a channel with enabled publisher acknowledgements on this channel.
 *
 * @see com.rabbitmq.client.Channel.confirmSelect()
 */
fun Connection.createConfirmChannel(): ConfirmChannel = ConfirmChannel(this.createChannel())

/**
 * DSL for creating a channel with enabled publisher acknowledgements on this channel.
 * Channel instances must not be shared between threads.
 * One channel per a thread approach is used.
 *
 * @see <a href="RabbitMQ Concurrency">https://www.rabbitmq.com/api-guide.html#concurrency</a>
 */
suspend fun Connection.confirmChannel(block: suspend ConfirmChannel.() -> Unit): ConfirmChannel {
    var channel = Channels.localConfirmChannels[Thread.currentThread()]
    if (channel == null || !channel.isOpen) {
        channel = Channels.UncloseableConfirmChannel(createConfirmChannel())
        Channels.localConfirmChannels[Thread.currentThread()] = channel
    }
    return channel.also { block(it) }
}

/**
 * DSL for creating a channel.
 * Channel instances must not be shared between threads.
 * One channel per a thread approach is used.
 *
 * @see <a href="RabbitMQ Concurrency">https://www.rabbitmq.com/api-guide.html#concurrency</a>
 */
suspend fun Connection.channel(block: suspend Channel.() -> Unit): Channel {
    var channel = Channels
        .localChannels[Thread.currentThread()]
    if (channel == null || !channel.isOpen) {
        channel = Channels.UncloseableChannel(createChannel())
        Channels.localChannels[Thread.currentThread()] = channel
    }
    return channel.also { block(it) }
}

/**
 * Create a channel with enabled TX mode on this channel.
 *
 * @see com.rabbitmq.client.Channel.txSelect()
 */fun Connection.createTxChannel(): TxChannel = TxChannel(this.createChannel())

/**
 * DSL for creating a channel with enabled TX mode on this channel.
 * Channel instances must not be shared between threads.
 * One channel per a thread approach is used.
 *
 * @see <a href="RabbitMQ Concurrency">https://www.rabbitmq.com/api-guide.html#concurrency</a>
 */
suspend fun Connection.txChannel(block: suspend TxChannel.() -> Unit): TxChannel {
    var channel = Channels.localTxChannels[Thread.currentThread()]
    if (channel == null || !channel.isOpen) {
        channel = Channels.UnclosableTxChannel(createTxChannel())
        Channels.localTxChannels.put(Thread.currentThread(), channel)
    }
    return channel.also { block(it) }
}
