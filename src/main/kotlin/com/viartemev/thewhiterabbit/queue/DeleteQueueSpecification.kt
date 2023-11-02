package com.viartemev.thewhiterabbit.queue

/**
 * Represents the specifications for deleting a queue.
 *
 * @property queue The name of the queue to be deleted.
 * @property ifUnused Whether the queue should be deleted if it is not in use.
 * @property ifEmpty Whether the queue should be deleted if it is empty.
 * @property noWait Whether to wait for the operation to complete or not.
 */
data class DeleteQueueSpecification(
        val queue: String,
        val ifUnused: Boolean = false,
        val ifEmpty: Boolean = false,
        val noWait: Boolean = false
)
