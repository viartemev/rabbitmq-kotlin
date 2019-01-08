package com.viartemev.whiterabbit.sender

import com.rabbitmq.client.ConfirmListener
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class AckListener(private val continuations: ConcurrentHashMap<Long, Continuation<Boolean>>) : ConfirmListener {

    override fun handleAck(deliveryTag: Long, multiple: Boolean) {
        handle(deliveryTag, multiple, true)
    }

    override fun handleNack(deliveryTag: Long, multiple: Boolean) {
        handle(deliveryTag, multiple, false)
    }

    private fun handle(deliveryTag: Long, multiple: Boolean, ack: Boolean) {
        println("deliveryTag = [$deliveryTag], multiple = [$multiple], positive = [$ack]")
        if (multiple) {
            (1..deliveryTag)
                    .forEach { tag ->
                        continuations[tag]?.resume(ack)
                        continuations.remove(tag)
                    }

        } else {
            continuations[deliveryTag]?.resume(ack)
            continuations.remove(deliveryTag)
        }
    }

}