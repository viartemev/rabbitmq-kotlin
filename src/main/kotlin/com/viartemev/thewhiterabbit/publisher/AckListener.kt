package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.ConfirmListener
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private val logger = KotlinLogging.logger {}

class AckListener(private val continuations: ConcurrentHashMap<Long, Continuation<Boolean>>) : ConfirmListener {

    override fun handleAck(deliveryTag: Long, multiple: Boolean) {
        handle(deliveryTag, multiple, true)
    }

    override fun handleNack(deliveryTag: Long, multiple: Boolean) {
        handle(deliveryTag, multiple, false)
    }

    private fun handle(deliveryTag: Long, multiple: Boolean, ack: Boolean) {
        logger.debug { "deliveryTag = [$deliveryTag], multiple = [$multiple], positive = [$ack]" }
        if (multiple) {
            (1..deliveryTag).forEach { tag -> continuations.remove(tag)?.resume(ack) }
        } else {
            continuations.remove(deliveryTag)?.resume(ack)
        }
    }

}