package com.github.viartemev.rabbitmq.channel

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection


/**
 * Creates a confirm channel on the given connection.
 *
 * @return The confirm channel that has been created.
 *
 * @see com.rabbitmq.client.Channel.confirmSelect
 */
fun Connection.createConfirmChannel(): ConfirmChannel = ConfirmChannel(this.createChannel())

/**
 * Suspends the current coroutine until a confirm channel is available or created, and then executes the specified block of code on the confirm channel.
 *
 * @param block The block of code to be executed on the confirm channel.
 *
 * @return The confirm channel on which the block of code is executed.
 */
suspend fun Connection.confirmChannel(block: suspend ConfirmChannel.() -> Unit): ConfirmChannel {
    var channel = Channels
        .localConfirmChannels[Thread.currentThread()]
    if (channel == null || !channel.isOpen) {
        channel = Channels.UncloseableConfirmChannel(createConfirmChannel())
        Channels.localConfirmChannels[Thread.currentThread()] = channel
    }
    return channel
        .also { block(it) }
}

/**
 * Creates a channel object for the current connection and executes the specified block of code on it.
 *
 * @param block the code block to be executed on the channel.
 * @return the channel object.
 */
suspend fun Connection.channel(block: suspend Channel.() -> Unit): Channel {
    var channel = Channels
        .localChannels[Thread.currentThread()]
    if (channel == null || !channel.isOpen) {
        channel = Channels.UncloseableChannel(createChannel())
        Channels.localChannels[Thread.currentThread()] = channel
    }
    return channel
        .also { block(it) }
}

fun Connection.createTxChannel(): TxChannel = TxChannel(this.createChannel())

suspend fun Connection.txChannel(block: suspend TxChannel.() -> Unit): TxChannel {
    var channel = Channels.localTxChannels[Thread.currentThread()]
    if (channel == null || !channel.isOpen) {
        channel = Channels.UnclosableTxChannel(createTxChannel())
        Channels.localTxChannels.put(Thread.currentThread(), channel)
    }
    return channel
        .also {
            block(it)
        }
}
