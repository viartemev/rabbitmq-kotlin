package com.viartemev.whiterabbit.publisher

import com.rabbitmq.client.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation

class Publisher(private val channel: Channel) {
    private val continuations = ConcurrentHashMap<Long, Continuation<Boolean>>()

    init {
        channel.addConfirmListener(AckListener(continuations))
    }

    //FIXME coroutine context???
    suspend fun publish(message: OutboundMessage) = coroutineScope {
        message.run {
            channel.basicPublish(exchange, routingKey, properties, body)
        }
    }

    //FIXME coroutine context???
    suspend fun publish(messages: List<OutboundMessage>) = coroutineScope {
        for (message in messages) {
            //FIXME launch???
            launch { publish(message) }
        }
    }

    //FIXME coroutine context???
    suspend fun publishWithConfirm(message: OutboundMessage): Boolean {
        val seqNo = channel.nextPublishSeqNo
        //TODO move to logger
        println("seqNo: $seqNo")

        return suspendCancellableCoroutine {
            continuations[seqNo] = it

            //FIXME move to publish??
            message.run {
                channel.basicPublish(exchange, routingKey, properties, body)
            }
        }
    }
}
