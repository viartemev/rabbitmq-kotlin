package com.viartemev.thewhiterabbit.queue

/**
 * Represents a specification for purging a queue.
 *
 * @property queue The name of the queue to be purged.
 * @property noWait Indicates whether to wait until the purge operation is complete. Defaults to `false`.
 */
data class PurgeQueueSpecification(
        val queue: String,
        val noWait: Boolean = false
)
