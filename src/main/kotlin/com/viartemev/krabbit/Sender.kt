package com.viartemev.krabbit

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConfirmListener
import com.rabbitmq.client.MessageProperties
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.channels.Channel as KChannel

class Sender(private val queueName: String, private val channel: Channel) {
    private val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()
    private val callback = object : ConfirmListener {
        override fun handleAck(deliveryTag: Long, multiple: Boolean) {
            handle(deliveryTag, multiple, true)
        }

        override fun handleNack(deliveryTag: Long, multiple: Boolean) {
            handle(deliveryTag, multiple, false)
        }

        fun handle(deliveryTag: Long, multiple: Boolean, ack: Boolean) {
            println("deliveryTag = [$deliveryTag], multiple = [$multiple], positive = [$ack]")
            if (multiple) {
                (1..deliveryTag)
                        .forEach {
                            continuations[it]?.resume(ack)
                            continuations.remove(it)
                        }

            } else {
                continuations[deliveryTag]?.resume(ack)
                continuations.remove(deliveryTag)
            }
        }
    }

    init {
        channel.addConfirmListener(callback)
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
