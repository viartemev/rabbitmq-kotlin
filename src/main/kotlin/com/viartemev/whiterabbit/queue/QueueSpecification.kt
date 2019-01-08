package com.viartemev.whiterabbit.queue

data class QueueSpecification(
        val name: String,
        val durable: Boolean = false,
        val exclusive: Boolean = false,
        val autoDelete: Boolean = false,
        val arguments: Map<String, Any> = emptyMap()
)