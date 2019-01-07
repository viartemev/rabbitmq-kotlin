package com.viartemev.krabbit

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConfirmListener
import com.rabbitmq.client.MessageProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.channels.Channel as KChannel

typealias Ack = Pair<Long, Boolean>

class CoroutineSender(private val queueName: String, private val channel: Channel) {
    private val channels = mutableListOf<KChannel<Ack>>()

    suspend fun send(message: String) = withContext(Dispatchers.IO) {
        channel.basicPublish("", queueName, null, message.toByteArray(charset("UTF-8")))
    }

    suspend fun sendWithAck(message: String): Boolean {
        val ch = KChannel<Ack>()
        channels.add(ch)

        val seqNo = channel.nextPublishSeqNo
        println("${Thread.currentThread().name} -> seqNo: $seqNo")


        channel.basicPublish("", queueName, MessageProperties.PERSISTENT_BASIC, message.toByteArray(charset("UTF-8")))
        channel.addConfirmListener(object : ConfirmListener {
            override fun handleAck(deliveryTag: Long, multiple: Boolean) {
                println("${Thread.currentThread().name} -> Ack -> tag: $deliveryTag, mult: $multiple")
                GlobalScope.launch {
                    channels.toList().forEach { c -> c.send(Ack(deliveryTag, multiple)) }
                }
            }

            override fun handleNack(deliveryTag: Long, multiple: Boolean) {
                println("Nack -> tag: $deliveryTag, mult: $multiple")
            }
        })
        return suspendCoroutine { continuation ->
            GlobalScope.launch {
                for (ackChannel in channels) {
                    for (ack in ackChannel) {
                        println("From: ${seqNo} ack: ${ack}")
                        val eq = ack.first == seqNo
                        val inInt = seqNo <= ack.first && ack.second
                        if (eq || inInt) {
                            continuation.resume(true)
                        }
                    }
                }
            }
        }
    }
}