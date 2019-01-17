package com.viartemev.thewhiterabbit.queue

data class BindQueueSpecification(
    val queue: String,
    val exchange: String,
    val routingKey: String = "",
    val noWait: Boolean = false,
    val arguments: Map<String, Any> = emptyMap()
)
