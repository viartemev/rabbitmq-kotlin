package com.viartemev.thewhiterabbit.queue

/**
 * A queue deletion specification
 */
data class DeleteQueueSpecification(
    /**
     * The name of the queue
     */
    val queue: String,
    /**
     * True if the queue should be deleted only if not in use
     */
    val ifUnused: Boolean = false,
    /**
     * True if the queue should be deleted only if empty
     */
    val ifEmpty: Boolean = false,
    /**
     * True to don't wait for a response from the server
     */
    val noWait: Boolean = false
)
