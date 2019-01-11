package com.viartemev.thewhiterabbit.queue

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.common.resourceManagementDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

suspend fun Channel.declareQueue(queueSpecification: QueueSpecification): AMQP.Queue.DeclareOk {
    val channel = this
    val queueDeclaration = AMQP.Queue.Declare.Builder()
            .queue(queueSpecification.name)
            .durable(queueSpecification.durable)
            .exclusive(queueSpecification.exclusive)
            .autoDelete(queueSpecification.autoDelete)
            .arguments(queueSpecification.arguments)
            .build()

    return withContext(resourceManagementDispatcher) {
        channel.asyncCompletableRpc(queueDeclaration).await().method as AMQP.Queue.DeclareOk
    }
}

suspend fun Channel.deleteQueue(specification: DeleteQueueSpecification): AMQP.Queue.DeleteOk {
    val channel = this
    val deleteDeclaration = AMQP.Queue.Delete.Builder()
            .queue(specification.queue)
            .ifUnused(specification.ifUnused)
            .ifEmpty(specification.ifEmpty)
            .nowait(specification.noWait)
            .build()

    return withContext(resourceManagementDispatcher) {
        channel.asyncCompletableRpc(deleteDeclaration).await().method as AMQP.Queue.DeleteOk
    }
}

suspend fun Channel.purgeQueue(specification: PurgeQueueSpecification): AMQP.Queue.PurgeOk {
    val channel = this
    val deleteDeclaration = AMQP.Queue.Purge.Builder()
            .queue(specification.queue)
            .nowait(specification.noWait)
            .build()

    return withContext(resourceManagementDispatcher) {
        channel.asyncCompletableRpc(deleteDeclaration).await().method as AMQP.Queue.PurgeOk
    }
}

suspend fun Channel.bindQueue(specification: BindQueueSpecification): AMQP.Queue.BindOk {
    val channel = this
    val deleteDeclaration = AMQP.Queue.Bind.Builder()
            .queue(specification.queue)
            .routingKey(specification.routingKey)
            .exchange(specification.exchange)
            .nowait(specification.noWait)
            .arguments(specification.arguments)
            .build()

    return withContext(resourceManagementDispatcher) {
        channel.asyncCompletableRpc(deleteDeclaration).await().method as AMQP.Queue.BindOk
    }
}

suspend fun Channel.unbindQueue(specification: UnbindQueueSpecification): AMQP.Queue.UnbindOk {
    val channel = this
    val unbindDeclaration = AMQP.Queue.Unbind.Builder()
            .queue(specification.queue)
            .routingKey(specification.routingKey)
            .exchange(specification.exchange)
            .arguments(specification.arguments)
            .build()

    return withContext(resourceManagementDispatcher) {
        channel.asyncCompletableRpc(unbindDeclaration).await().method as AMQP.Queue.UnbindOk
    }
}