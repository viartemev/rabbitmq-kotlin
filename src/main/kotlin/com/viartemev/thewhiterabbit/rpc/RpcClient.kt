package com.viartemev.thewhiterabbit.rpc

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import mu.KotlinLogging
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


private val logger = KotlinLogging.logger {}

//TODO add timeout
//TODO channel.basicConsume can throw an exception
//TODO channel.basicCancel can throw an exception
//TODO channel.basicPublish can throw an exception
class RpcClient(val channel: Channel) {

    suspend fun call(message: RpcOutboundMessage): String {
        val corrId = UUID.randomUUID().toString()

        val props = AMQP.BasicProperties.Builder()
            .correlationId(corrId)
            .replyTo(message.replyQueueName)
            .build()
        channel.basicPublish(message.exchangeName, message.requestQueueName, props, message.message.toByteArray())

        return suspendCoroutine { continuation ->
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
