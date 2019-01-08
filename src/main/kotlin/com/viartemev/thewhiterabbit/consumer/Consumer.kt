package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel as KChannel

class Consumer(private val channel: Channel) {

    fun consumeAutoAck(queue: String): KChannel<String> {
        val ch = KChannel<String>()
        channel.basicConsume(queue, true,
                DeliverCallback { consumerTag, message ->
                    GlobalScope.launch { message?.let { ch.send(String(it.body)) } }
                },
                CustomCancelCallback(ch)
        )
        return ch
    }
}