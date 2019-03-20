package com.viartemev.thewhiterabbit.rpc

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume


class RpcClient(val channel: Channel) {

    private val requestQueueName = "rpc_queue"

    suspend fun call(message: String): String {
        val corrId = UUID.randomUUID().toString()

        val replyQueueName = channel.declareQueue(QueueSpecification("")).queue
        val props = AMQP.BasicProperties.Builder()
            .correlationId(corrId)
            .replyTo(replyQueueName)
            .build()
        channel.basicPublish("", requestQueueName, props, message.toByteArray())

        return suspendCancellableCoroutine { continuation ->
            channel.basicConsume(requestQueueName, true, { consumerTag, delivery ->
                if (corrId == delivery.properties.correlationId) {
                    channel.basicCancel(consumerTag)
                    continuation.resume(String(delivery.body))
                }
            }, { consumerTag ->
                //FIXME
            })
        }
    }
}
