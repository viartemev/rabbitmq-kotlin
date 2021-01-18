package com.viartemev.thewhiterabbit.queue

/**
 * A queue purge specification
 */
data class PurgeQueueSpecification(
    /**
     * The name of the queue
     */
    val queue: String,
    /**
     * True to don't wait for a response from the server
     */
    val noWait: Boolean = false
)
