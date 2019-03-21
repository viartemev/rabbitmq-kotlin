package com.viartemev.thewhiterabbit.rpc

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.common.cancelOnIOException
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.util.*
import kotlin.coroutines.resume


private val logger = KotlinLogging.logger {}

//TODO add timeout
//TODO channel.basicPublish can throw an exception
class RpcClient(val channel: Channel) {

    suspend fun call(message: RpcOutboundMessage): RpcInboundMessage {
        val corrId = UUID.randomUUID().toString()

        val props = AMQP.BasicProperties.Builder()
            .correlationId(corrId)
            .replyTo(message.replyQueueName)
            .build()

        channel.basicPublish(message.exchangeName, message.requestQueueName, props, message.body)

        return suspendCancellableCoroutine { continuation ->
            cancelOnIOException(continuation) {
                channel.basicConsume(message.replyQueueName, true, { consumerTag, delivery ->
                    if (corrId == delivery.properties.correlationId) {
                        cancelOnIOException(continuation) {
                            channel.basicCancel(consumerTag)
                            continuation.resume(RpcInboundMessage(delivery.properties, delivery.body))
                        }
                    }
                }, { consumerTag ->
                    logger.debug { "Consumer $consumerTag has been cancelled for reasons other than by a call to Channel#basicCancel" }
                })
            }
        }
    }
}
