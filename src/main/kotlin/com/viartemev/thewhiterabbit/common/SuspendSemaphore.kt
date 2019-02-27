package com.viartemev.thewhiterabbit.common

import kotlinx.coroutines.channels.Channel

class SuspendSemaphore(permits: Int = 0, capacity: Int = Channel.UNLIMITED) {
    private val channel = Channel<Unit>(capacity)

    init {
        repeat(permits) { channel.offer(Unit) }
    }

    suspend fun acquire() {
        channel.receive()
    }

    fun release() {
        channel.offer(Unit)
    }
}
