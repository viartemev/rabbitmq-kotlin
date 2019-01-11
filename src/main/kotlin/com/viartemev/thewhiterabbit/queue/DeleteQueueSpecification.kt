package com.viartemev.thewhiterabbit.queue

data class DeleteQueueSpecification(
        val queue: String,
        val ifUnused: Boolean = false,
        val ifEmpty: Boolean = false,
        val noWait: Boolean = false
)