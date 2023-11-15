package com.github.viartemev.rabbitmq.queue

/**
 * Represents the specification of a queue in a message broker.
 *
 * @param name The name of the queue.
 * @param durable Specifies whether the queue should survive a broker restart.
 *                Defaults to `false`.
 * @param exclusive Specifies whether the queue should be used by only one connection,
 *                  and will be deleted when that connection closes. Defaults to `false`.
 * @param autoDelete Specifies whether the queue, which has had at least one consumer,
 *                   should be deleted when the last consumer unsubscribes.
 *                   Defaults to `false`.
 * @param arguments Additional key-value pairs specifying custom arguments for the queue.
 *                  Defaults to an empty map.
 */
open class QueueSpecification(
    val name: String,
    val durable: Boolean = false,
    val exclusive: Boolean = false,
    val autoDelete: Boolean = false,
    val arguments: Map<String, Any> = emptyMap()
)
