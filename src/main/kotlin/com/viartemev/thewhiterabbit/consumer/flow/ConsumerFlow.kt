package com.viartemev.thewhiterabbit.consumer.flow

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ConsumerFlow(
    private val amqpChannel: Channel, private val amqpQueue: String, private val prefetchSize: Int
) {

    //TODO exception handling
    suspend fun consumerAutoAckFlow(): Flow<Delivery> = callbackFlow {
        amqpChannel.basicQos(prefetchSize, false)
        val tag = amqpChannel.basicConsume(amqpQueue, true, { consumerTag, message ->
            trySendBlocking(message)
        }, { consumerTag ->
            channel.close()
        })
        awaitClose {
            amqpChannel.basicCancel(tag)
            amqpChannel.close()
        }
    }

    //TODO exception handling
    suspend fun consumerConfirmAckFlow(): Flow<Delivery> = callbackFlow {
        amqpChannel.basicQos(prefetchSize, false)
        val tag = amqpChannel.basicConsume(amqpQueue, false, { consumerTag, message ->
            trySendBlocking(message)
            amqpChannel.basicAck(message.envelope.deliveryTag, false)
        }, { consumerTag ->
            channel.close()
        })
        awaitClose {
            amqpChannel.basicCancel(tag)
            amqpChannel.close()
        }
    }

}
