package com.viartemev.thewhiterabbit.exchange

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.io.IOException


/**
 * Asynchronously Declare an exchange passively; that is, check if the named exchange exists.
 * @see com.viartemev.thewhiterabbit.exchange.ExchangeSpecification
 * @throws IOException the server will raise a 404 channel exception if the named exchange does not exist.
 */
suspend fun Channel.declareExchange(exchangeSpecification: ExchangeSpecification): AMQP.Exchange.DeclareOk {
    val channel = this
    val declaration = AMQP.Exchange.Declare.Builder()
        .exchange(exchangeSpecification.name)
        .type(exchangeSpecification.type.asString)
        .durable(exchangeSpecification.durable)
        .autoDelete(exchangeSpecification.autoDelete)
        .internal(exchangeSpecification.internal)
        .arguments(exchangeSpecification.arguments)
        .build()

    return withContext(Dispatchers.IO) {
        channel.asyncCompletableRpc(declaration).await().method as AMQP.Exchange.DeclareOk
    }
}

/**
 * Asynchronously delete an exchange.
 * @see com.viartemev.thewhiterabbit.exchange.DeleteExchangeSpecification
 * @return a deletion-confirm method to indicate the exchange was successfully deleted
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.deleteExchange(specification: DeleteExchangeSpecification): AMQP.Exchange.DeleteOk {
    val channel = this
    val deleteDeclaration = AMQP.Exchange.Delete.Builder()
        .exchange(specification.exchange)
        .ifUnused(specification.ifUnused)
        .nowait(specification.noWait)
        .build()

    return withContext(Dispatchers.IO) {
        channel.asyncCompletableRpc(deleteDeclaration).await().method as AMQP.Exchange.DeleteOk
    }
}

/**
 * Asynchronously bind an exchange to an exchange.
 * @see com.viartemev.thewhiterabbit.exchange.BindExchangeSpecification
 * @return a binding-confirm method if the binding was successfully created
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.bindExchange(specification: BindExchangeSpecification): AMQP.Exchange.BindOk {
    val channel = this
    val bindDeclaration = AMQP.Exchange.Bind.Builder()
        .source(specification.source)
        .destination(specification.destination)
        .nowait(specification.noWait)
        .routingKey(specification.routingKey)
        .arguments(specification.arguments)
        .build()

    return withContext(Dispatchers.IO) {
        channel.asyncCompletableRpc(bindDeclaration).await().method as AMQP.Exchange.BindOk
    }
}

/**
 * Asynchronously unbind an exchange from an exchange.
 * @see com.viartemev.thewhiterabbit.exchange.UnbindExchangeSpecification
 * @return a binding-confirm method if the binding was successfully created
 * @throws java.io.IOException if an error is encountered
 */
suspend fun Channel.unbindExchange(specification: UnbindExchangeSpecification): AMQP.Exchange.UnbindOk {
    val channel = this
    val unbindDeclaration = AMQP.Exchange.Unbind.Builder()
        .source(specification.source)
        .destination(specification.destination)
        .nowait(specification.noWait)
        .routingKey(specification.routingKey)
        .arguments(specification.arguments)
        .build()

    return withContext(Dispatchers.IO) {
        channel.asyncCompletableRpc(unbindDeclaration).await().method as AMQP.Exchange.UnbindOk
    }
}
