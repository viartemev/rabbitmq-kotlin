package com.viartemev.thewhiterabbit.queue

open class QueueSpecification(
    val name: String,
    val durable: Boolean = false, //the queue will survive a broker restart
    val exclusive: Boolean = false, //used by only one connection and the queue will be deleted when that connection closes
    val autoDelete: Boolean = false, //queue that has had at least one consumer is deleted when last consumer unsubscribes
    val arguments: Map<String, Any> = emptyMap()
)
