package io.github.viartemev.rabbitmq.queue

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

/**
 * Asynchronously declares a queue.
 *
 * @param queueSpecification the queue specification containing the name, durability, exclusivity, auto-deletion, and arguments
 * @return a declaration-confirm method to indicate the queue was successfully declared
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.declareQueue(queueSpecification: QueueSpecification): AMQP.Queue.DeclareOk {
    val channel = this
    val queueDeclaration = AMQP.Queue.Declare.Builder()
        .queue(queueSpecification.name)
        .durable(queueSpecification.durable)
        .exclusive(queueSpecification.exclusive)
        .autoDelete(queueSpecification.autoDelete)
        .arguments(queueSpecification.arguments)
        .build()

    return withContext(Dispatchers.IO) {
        channel.asyncCompletableRpc(queueDeclaration).await().method as AMQP.Queue.DeclareOk
    }
}

/**
 * Asynchronously delete a queue.
 *
 * @param specification the specifications for deleting the queue
 * @return a deletion-confirm method to indicate the queue was successfully deleted
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.deleteQueue(specification: DeleteQueueSpecification): AMQP.Queue.DeleteOk {
    val channel = this
    val deleteDeclaration = AMQP.Queue.Delete.Builder()
        .queue(specification.queue)
        .ifUnused(specification.ifUnused)
        .ifEmpty(specification.ifEmpty)
        .nowait(specification.noWait)
        .build()

    return withContext(Dispatchers.IO) {
        channel.asyncCompletableRpc(deleteDeclaration).await().method as AMQP.Queue.DeleteOk
    }
}

/**
 * Asynchronously purges the contents of the given queue.
 *
 * @param specification the PurgeQueueSpecification containing the configuration for purging the queue
 * @return a purge-confirm method if the purge was executed successfully
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.purgeQueue(specification: PurgeQueueSpecification): AMQP.Queue.PurgeOk {
    val channel = this
    val deleteDeclaration = AMQP.Queue.Purge.Builder()
        .queue(specification.queue)
        .nowait(specification.noWait)
        .build()

    return withContext(Dispatchers.IO) {
        channel.asyncCompletableRpc(deleteDeclaration).await().method as AMQP.Queue.PurgeOk
    }
}

/**
 * Asynchronously binds a queue to an exchange.
 *
 * @param specification the bind queue specification
 * @return a binding-confirm method if the binding was successfully created
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.bindQueue(specification: BindQueueSpecification): AMQP.Queue.BindOk {
    val channel = this
    val bindDeclaration = AMQP.Queue.Bind.Builder()
        .queue(specification.queue)
        .routingKey(specification.routingKey)
        .exchange(specification.exchange)
        .nowait(specification.noWait)
        .arguments(specification.arguments)
        .build()

    return withContext(Dispatchers.IO) {
        channel.asyncCompletableRpc(bindDeclaration).await().method as AMQP.Queue.BindOk
    }
}

/**
 * Asynchronously unbinds a queue from an exchange, with no extra arguments.
 *
 * @param specification the specification for unbinding the queue from the exchange
 * @return an unbinding-confirm method if the binding was successfully deleted
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.unbindQueue(specification: UnbindQueueSpecification): AMQP.Queue.UnbindOk {
    val channel = this
    val unbindDeclaration = AMQP.Queue.Unbind.Builder()
        .queue(specification.queue)
        .routingKey(specification.routingKey)
        .exchange(specification.exchange)
        .arguments(specification.arguments)
        .build()

    return withContext(Dispatchers.IO) {
        channel.asyncCompletableRpc(unbindDeclaration).await().method as AMQP.Queue.UnbindOk
    }
}
