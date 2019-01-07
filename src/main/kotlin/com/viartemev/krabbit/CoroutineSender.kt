package com.viartemev.krabbit

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConfirmListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class CoroutineSender(private val queueName: String, private val channel: Channel) {
    private val acks = kotlinx.coroutines.channels.Channel<Long>()
    suspend fun send(message: String) = withContext(Dispatchers.IO) {
        channel.basicPublish("", queueName, null, message.toByteArray(charset("UTF-8")))
    }

    suspend fun sendWithAck(message: String): Boolean {

        channel.basicPublish("", queueName, null, message.toByteArray(charset("UTF-8")))
        return suspendCancellableCoroutine { continuation ->
            GlobalScope.launch {
                for (y in acks) println("Print from channel: $y")
            }
            channel.addConfirmListener(object : ConfirmListener {
                override fun handleAck(deliveryTag: Long, multiple: Boolean) {
                    GlobalScope.launch { acks.send(deliveryTag) }
                    println("Delivery tag: $deliveryTag, multiple: $multiple")
                    continuation.resume(true)
                }

                override fun handleNack(deliveryTag: Long, multiple: Boolean) {
                    continuation.resume(false)
                }
            })
        }
    }
}