package com.viartemev.whiterabbit.sender

import com.rabbitmq.client.Channel
import com.rabbitmq.client.MessageProperties
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.channels.Channel as KChannel

class Sender(private val queueName: String, private val channel: Channel) {
    private val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()

    init {
        channel.addConfirmListener(AckListener(continuations))
    }

    suspend fun sendWithAck(message: String): Boolean {
        val seqNo = channel.nextPublishSeqNo
        println("seqNo: $seqNo")

        return suspendCoroutine {
            continuations[seqNo] = it
            channel.basicPublish("", queueName, MessageProperties.PERSISTENT_BASIC, message.toByteArray(charset("UTF-8")))
        }
    }
}
