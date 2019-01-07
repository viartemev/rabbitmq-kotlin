package com.viartemev.krabbit

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConfirmListener
import com.rabbitmq.client.MessageProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.channels.Channel as KChannel


class CoroutineSender(private val queueName: String, private val channel: Channel) {

    suspend fun send(message: String) = withContext(Dispatchers.IO) {
        channel.basicPublish("", queueName, null, message.toByteArray(charset("UTF-8")))
    }

    suspend fun sendWithAck(message: String): Boolean {
        val seqNo = channel.nextPublishSeqNo
        println("seqNo: $seqNo")

        channel.basicPublish("", queueName, MessageProperties.PERSISTENT_BASIC, message.toByteArray(charset("UTF-8")))
        channel.addConfirmListener(object : ConfirmListener {
            override fun handleAck(deliveryTag: Long, multiple: Boolean) {
                println("Ack -> tag: $deliveryTag, mult: $multiple")
            }

            override fun handleNack(deliveryTag: Long, multiple: Boolean) {
                println("Nack -> tag: $deliveryTag, mult: $multiple")
            }
        })
        return suspendCoroutine { continuation ->
            continuation.resume(true)
        }
    }
}