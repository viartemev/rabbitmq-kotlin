package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class TxPublisher internal constructor(private val channel: Channel) {

    suspend fun publishInTx(message: OutboundMessage) {

        logger.debug { "publish in tx msg: $message" }

        // todo Docs says -- "Invocations of Channel#basicPublish will eventually block if a resource-driven alarm  is in effect."
        // todo shall we consider this case?
        withContext(Dispatchers.IO) {

            // todo handle errors
            message.apply {
                channel.basicPublish(exchange, routingKey, properties, msg.toByteArray())
            }
        }
    }

    fun commit() {
        // todo handle result
        logger.debug("tx commit")
        channel.txCommit();
    }

    fun rollback() {
        // todo handle result
        logger.debug("tx rollback")
        channel.txRollback();
    }

}
