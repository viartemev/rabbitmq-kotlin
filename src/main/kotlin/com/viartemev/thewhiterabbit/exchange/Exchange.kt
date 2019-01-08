package com.viartemev.thewhiterabbit.exchange

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.common.resourceManagementDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

object Exchange {

    suspend fun declareExchange(channel: Channel, exchangeSpecification: ExchangeSpecification): AMQP.Exchange.DeclareOk {
        val declaration = AMQP.Exchange.Declare.Builder()
                .exchange(exchangeSpecification.name)
                .type(exchangeSpecification.type.asString)
                .durable(exchangeSpecification.durable)
                .autoDelete(exchangeSpecification.autoDelete)
                .internal(exchangeSpecification.internal)
                .arguments(exchangeSpecification.arguments)
                .build()

        return withContext(resourceManagementDispatcher) {
            channel.asyncCompletableRpc(declaration).await().method as AMQP.Exchange.DeclareOk
        }
    }
}