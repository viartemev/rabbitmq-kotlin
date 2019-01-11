package com.viartemev.thewhiterabbit.exchange

data class BindExchangeSpecification(
    val source: String,
    val destination: String,
    val noWait: Boolean = false,
    val routingKey: String,
    val arguments: Map<String, Any> = emptyMap()
)
