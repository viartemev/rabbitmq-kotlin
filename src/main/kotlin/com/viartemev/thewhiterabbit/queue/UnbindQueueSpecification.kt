package com.viartemev.thewhiterabbit.queue

data class UnbindQueueSpecification(
        val queue: String,
        val exchange: String,
        val routingKey: String,
        val arguments: Map<String, Any> = emptyMap()
)