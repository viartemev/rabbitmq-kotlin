package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.Channel as KChannel

class Consumer(private val channel: Channel) {

    suspend fun consumeAutoAckAsync(queue: String, output: KChannel<String>) {
        println("Start consuming...")
        while (true) {
            val delivery = consume(queue)
            println("Got message: $delivery")
            delivery?.let {
                val body = String(it.body)
                output.send(body)
            }
        }
    }

    private suspend fun consume(queue: String): Delivery? {
        return suspendCancellableCoroutine { cont ->
            channel.basicConsume(queue, true,
                    DeliverCallback { consumerTag, message ->
                        //FIXME close continuation
                        if (!cont.isCompleted) {
                            cont.resume(message)
                        }
                    },
                    CancelCallback { println("Cancel") })
        }
    }
}