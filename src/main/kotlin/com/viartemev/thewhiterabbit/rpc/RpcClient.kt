package com.viartemev.thewhiterabbit.rpc

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.*
import kotlin.coroutines.resume


private val logger = KotlinLogging.logger {}

class RpcClient(val channel: Channel) {

    suspend fun call(message: RpcOutboundMessage): String {
        val corrId = UUID.randomUUID().toString()

        val props = AMQP.BasicProperties.Builder()
            .correlationId(corrId)
            .replyTo(message.replyQueueName)
            .build()
        channel.basicPublish(message.exchangeName, message.requestQueueName, props, message.message.toByteArray())

        return suspendCancellableCoroutine { continuation ->
            channel.basicConsume(message.replyQueueName, true, { consumerTag, delivery ->
                if (corrId == delivery.properties.correlationId) {
                    channel.basicCancel(consumerTag)
                    continuation.resume(String(delivery.body))
                }
            }, { consumerTag ->
                logger.debug { "Consumer $consumerTag has been cancelled for reasons other than by a call to Channel#basicCancel" }
            })
        }
    }
}
