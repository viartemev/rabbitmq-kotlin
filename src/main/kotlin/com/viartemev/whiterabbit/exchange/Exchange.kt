package com.viartemev.whiterabbit.exchange

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import kotlinx.coroutines.future.await

class Exchange {

    //TODO move channel from method and use internal pool for it
    suspend fun declareExchange(channel: Channel, exchangeSpecification: ExchangeSpecification): AMQP.Exchange.DeclareOk {
        val declaration = AMQP.Exchange.Declare.Builder()
                .exchange(exchangeSpecification.name)
                .type(exchangeSpecification.type.asString)
                .durable(exchangeSpecification.durable)
                .autoDelete(exchangeSpecification.autoDelete)
                .internal(exchangeSpecification.internal)
                .arguments(exchangeSpecification.arguments)
                .build()

        //FIXME IOException can be thrown here
        return channel.asyncCompletableRpc(declaration).await().method as AMQP.Exchange.DeclareOk
    }
}