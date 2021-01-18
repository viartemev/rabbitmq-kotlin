package com.viartemev.thewhiterabbit.queue

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.common.RabbitMqDispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Declare a queue following the specification on the context or SingleThreadDispatcher by default.
 * @see com.viartemev.thewhiterabbit.common.RabbitMqDispatchers.SingleThreadDispatcher
 * @see com.viartemev.thewhiterabbit.queue.QueueSpecification
 * @return a declaration-confirm method to indicate the queue was successfully declared
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.declareQueue(
    queueSpecification: QueueSpecification,
    context: CoroutineContext = RabbitMqDispatchers.SingleThreadDispatcher
): AMQP.Queue.DeclareOk {
    val channel = this
    val queueDeclaration = AMQP.Queue.Declare.Builder()
        .queue(queueSpecification.name)
        .durable(queueSpecification.durable)
        .exclusive(queueSpecification.exclusive)
        .autoDelete(queueSpecification.autoDelete)
        .arguments(queueSpecification.arguments)
        .build()

    return withContext(context) {
        channel.asyncCompletableRpc(queueDeclaration).await().method as AMQP.Queue.DeclareOk
    }
}

/**
 * Delete a queue following the specification on the context or SingleThreadDispatcher by default.
 * @see com.viartemev.thewhiterabbit.common.RabbitMqDispatchers.SingleThreadDispatcher
 * @see com.viartemev.thewhiterabbit.queue.DeleteQueueSpecification
 * @return a deletion-confirm method to indicate the queue was successfully deleted
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.deleteQueue(
    specification: DeleteQueueSpecification,
    context: CoroutineContext = RabbitMqDispatchers.SingleThreadDispatcher
): AMQP.Queue.DeleteOk {
    val channel = this
    val deleteDeclaration = AMQP.Queue.Delete.Builder()
        .queue(specification.queue)
        .ifUnused(specification.ifUnused)
        .ifEmpty(specification.ifEmpty)
        .nowait(specification.noWait)
        .build()

    return withContext(context) {
        channel.asyncCompletableRpc(deleteDeclaration).await().method as AMQP.Queue.DeleteOk
    }
}

/**
 * Purge a queue following the specification on the context or SingleThreadDispatcher by default.
 * @see com.viartemev.thewhiterabbit.common.RabbitMqDispatchers.SingleThreadDispatcher
 * @see com.viartemev.thewhiterabbit.queue.PurgeQueueSpecification
 * @return a purge-confirm method if the purge was executed successfully
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.purgeQueue(
    specification: PurgeQueueSpecification,
    context: CoroutineContext = RabbitMqDispatchers.SingleThreadDispatcher
): AMQP.Queue.PurgeOk {
    val channel = this
    val deleteDeclaration = AMQP.Queue.Purge.Builder()
        .queue(specification.queue)
        .nowait(specification.noWait)
        .build()

    return withContext(context) {
        channel.asyncCompletableRpc(deleteDeclaration).await().method as AMQP.Queue.PurgeOk
    }
}

/**
 * Bind a queue following the specification on the context or SingleThreadDispatcher by default.
 * @see com.viartemev.thewhiterabbit.common.RabbitMqDispatchers.SingleThreadDispatcher
 * @see com.viartemev.thewhiterabbit.queue.BindQueueSpecification
 * @return a binding-confirm method if the binding was successfully created
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.bindQueue(
    specification: BindQueueSpecification,
    context: CoroutineContext = RabbitMqDispatchers.SingleThreadDispatcher
): AMQP.Queue.BindOk {
    val channel = this
    val bindDeclaration = AMQP.Queue.Bind.Builder()
        .queue(specification.queue)
        .routingKey(specification.routingKey)
        .exchange(specification.exchange)
        .nowait(specification.noWait)
        .arguments(specification.arguments)
        .build()

    return withContext(context) {
        channel.asyncCompletableRpc(bindDeclaration).await().method as AMQP.Queue.BindOk
    }
}

/**
 * Unbind a queue following the specification on the context or SingleThreadDispatcher by default.
 * @see com.viartemev.thewhiterabbit.common.RabbitMqDispatchers.SingleThreadDispatcher
 * @see com.viartemev.thewhiterabbit.queue.UnbindQueueSpecification
 * @return an unbinding-confirm method if the binding was successfully deleted
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.unbindQueue(
    specification: UnbindQueueSpecification,
    context: CoroutineContext = RabbitMqDispatchers.SingleThreadDispatcher
): AMQP.Queue.UnbindOk {
    val channel = this
    val unbindDeclaration = AMQP.Queue.Unbind.Builder()
        .queue(specification.queue)
        .routingKey(specification.routingKey)
        .exchange(specification.exchange)
        .arguments(specification.arguments)
        .build()

    return withContext(context) {
        channel.asyncCompletableRpc(unbindDeclaration).await().method as AMQP.Queue.UnbindOk
    }
}
