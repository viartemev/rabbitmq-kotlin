package com.viartemev.whiterabbit.queue

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.viartemev.whiterabbit.common.techDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

object Queue {

    suspend fun declareQueue(channel: Channel, queueSpecification: QueueSpecification): AMQP.Queue.DeclareOk {
        val declaration = AMQP.Queue.Declare.Builder()
                .queue(queueSpecification.name)
                .durable(queueSpecification.durable)
                .exclusive(queueSpecification.exclusive)
                .autoDelete(queueSpecification.autoDelete)
                .arguments(queueSpecification.arguments)
                .build()

        return withContext(techDispatcher) {
            channel.asyncCompletableRpc(declaration).await().method as AMQP.Queue.DeclareOk
        }
    }
}