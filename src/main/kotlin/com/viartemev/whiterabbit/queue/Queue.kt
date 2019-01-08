package com.viartemev.whiterabbit.queue

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import kotlinx.coroutines.future.await

class Queue {

    //TODO move channel from method and use internal pool for it
    suspend fun declareQueue(channel: Channel, queueSpecification: QueueSpecification): AMQP.Queue.DeclareOk {
        val declaration = AMQP.Queue.Declare.Builder()
                .queue(queueSpecification.name)
                .durable(queueSpecification.durable)
                .exclusive(queueSpecification.exclusive)
                .autoDelete(queueSpecification.autoDelete)
                .arguments(queueSpecification.arguments)
                .build()
        //FIXME IOException can be thrown here
        return channel.asyncCompletableRpc(declaration).await().method as AMQP.Queue.DeclareOk
    }
}