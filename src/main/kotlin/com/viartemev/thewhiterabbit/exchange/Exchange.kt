package com.viartemev.thewhiterabbit.exchange

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.common.resourceManagementDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext


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

    return withContext(resourceManagementDispatcher) {
        channel.asyncCompletableRpc(declaration).await().method as AMQP.Exchange.DeclareOk
    }
}

suspend fun Channel.deleteExchange(specification: DeleteExchangeSpecification): AMQP.Exchange.DeleteOk {
    val channel = this
    val deleteDeclaration = AMQP.Exchange.Delete.Builder()
        .exchange(specification.exchange)
        .ifUnused(specification.ifUnused)
        .nowait(specification.noWait)
        .build()

    return withContext(resourceManagementDispatcher) {
        channel.asyncCompletableRpc(deleteDeclaration).await().method as AMQP.Exchange.DeleteOk
    }
}

suspend fun Channel.bindExchange(specification: BindExchangeSpecification): AMQP.Exchange.BindOk {
    val channel = this
    val bindDeclaration = AMQP.Exchange.Bind.Builder()
        .source(specification.source)
        .destination(specification.destination)
        .nowait(specification.noWait)
        .routingKey(specification.routingKey)
        .arguments(specification.arguments)
        .build()

    return withContext(resourceManagementDispatcher) {
        channel.asyncCompletableRpc(bindDeclaration).await().method as AMQP.Exchange.BindOk
    }
}

suspend fun Channel.unbindExchange(specification: UnbindExchangeSpecification): AMQP.Exchange.UnbindOk {
    val channel = this
    val unbindDeclaration = AMQP.Exchange.Unbind.Builder()
        .source(specification.source)
        .destination(specification.destination)
        .nowait(specification.noWait)
        .routingKey(specification.routingKey)
        .arguments(specification.arguments)
        .build()

    return withContext(resourceManagementDispatcher) {
        channel.asyncCompletableRpc(unbindDeclaration).await().method as AMQP.Exchange.UnbindOk
    }
}
