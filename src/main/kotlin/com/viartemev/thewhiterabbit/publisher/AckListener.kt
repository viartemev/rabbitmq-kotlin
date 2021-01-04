package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.ConfirmListener
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

internal class AckListener(
    private val continuations: ConcurrentHashMap<Long, Continuation<Boolean>>
) : ConfirmListener {

    private val logger = KotlinLogging.logger {}
    private val lowerBoundOfMultiple = AtomicLong(1)

    override fun handleAck(deliveryTag: Long, multiple: Boolean) {
        handle(deliveryTag, multiple, true)
    }

    override fun handleNack(deliveryTag: Long, multiple: Boolean) {
        handle(deliveryTag, multiple, false)
    }

    private fun handle(deliveryTag: Long, multiple: Boolean, ack: Boolean) {
        logger.debug { "deliveryTag = [$deliveryTag], multiple = [$multiple], positive = [$ack]" }
        val lowerBound = lowerBoundOfMultiple.get()
        if (multiple) {
            for (tag in lowerBound..deliveryTag) {
                continuations.remove(tag)?.resume(ack)
            }
            lowerBoundOfMultiple.compareAndSet(lowerBound, deliveryTag)
        } else {
            continuations.remove(deliveryTag)?.resume(ack)
            if (deliveryTag == lowerBound + 1) {
                lowerBoundOfMultiple.compareAndSet(lowerBound, deliveryTag)
            }
        }
    }
}
