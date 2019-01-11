package com.viartemev.thewhiterabbit.queue

data class PurgeQueueSpecification(
        val queue: String,
        val noWait: Boolean = false
)