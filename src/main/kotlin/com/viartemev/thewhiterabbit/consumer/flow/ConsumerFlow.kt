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
    private val amqpChannel: Channel, private val amqpQueue: String
) {

    /**
     * The consumerAutoAckFlow function establishes a cold Flow for consuming messages from an AMQP queue with automatic acknowledgment enabled.
     * This flow is designed to emit messages to the downstream consumers as they arrive from the queue.
     */
    suspend fun consumerAutoAckFlow(prefetchSize: Int = 0): Flow<Delivery> = callbackFlow {
        if (prefetchSize != 0) {
            amqpChannel.basicQos(prefetchSize, false)
        }
        val deliverCallback: (consumerTag: String, message: Delivery) -> Unit = { _, message ->
            logger.debug { "Trying to send a message from the flow consumer to the flow" }
            trySendBlocking(message)
            logger.debug { "The message was successfully sent to the flow" }
        }
        val cancelCallback: (consumerTag: String) -> Unit = { _ -> channel.close() }
        val tag = amqpChannel.basicConsume(amqpQueue, true, deliverCallback, cancelCallback)
        awaitClose {
            try {
                logger.debug { "Cancelling consumer#$tag" }
                amqpChannel.basicCancel(tag)
            } catch (e: Exception) {
                logger.error(e) { "Can't cancel consumer#$tag" }
                channel.close()
            }
        }
    }

    /**
     * The consumerConfirmAckFlow function creates a cold Flow that consumes messages from an AMQP queue using a provided amqpChannel.
     * The messages are not automatically acknowledged after being received.
     * Instead, acknowledgments are manually sent to the AMQP server after the messages are successfully emitted to the downstream flow collector.
     *
     */
    suspend fun consumerConfirmAckFlow(prefetchSize: Int = 0): Flow<Delivery> = callbackFlow {
        if (prefetchSize != 0) {
            amqpChannel.basicQos(prefetchSize, false)
        }
        val deliverCallback: (consumerTag: String, message: Delivery) -> Unit = { _, message ->
            try {
                logger.debug { "Trying to send a message from the flow consumer to the flow" }
                trySendBlocking(message)
                logger.debug { "The message was successfully sent to the flow" }
                amqpChannel.basicAck(message.envelope.deliveryTag, false)
                logger.debug { "The message was successfully acknowledged" }
            } catch (e: Exception) {
                logger.error(e) { "Caught exception while delivering the message" }
                close(e)
            }
        }
        val cancelCallback: (consumerTag: String) -> Unit = { _ -> channel.close() }
        val tag = amqpChannel.basicConsume(amqpQueue, false, deliverCallback, cancelCallback)
        awaitClose {
            try {
                logger.debug { "Cancelling consumer#$tag" }
                amqpChannel.basicCancel(tag)
            } catch (e: Exception) {
                logger.error(e) { "Can't cancel consumer#$tag" }
                channel.close()
            }
        }
    }

}
