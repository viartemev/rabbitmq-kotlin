package com.viartemev.thewhiterabbit.queue

/**
 * A queue declaration specification
 */
open class QueueSpecification(
    /**
     * The name of the queue
     */
    val name: String,
    /**
     * True if we are declaring a durable queue (the queue will survive a server restart)
     */
    val durable: Boolean = false,
    /**
     * True if we are declaring an exclusive queue (restricted to this connection)
     */
    val exclusive: Boolean = false,
    /**
     * True if we are declaring an autodelete queue (server will delete it when no longer in use)
     */
    val autoDelete: Boolean = false,
    /**
     * Other properties (construction arguments) for the queue
     */
    val arguments: Map<String, Any> = emptyMap()
)
