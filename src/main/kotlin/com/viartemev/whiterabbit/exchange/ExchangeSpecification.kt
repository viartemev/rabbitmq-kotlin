package com.viartemev.whiterabbit.exchange

data class ExchangeSpecification(
        val name: String,
        val type: ExchangeType = ExchangeType.DIRECT,
        val durable: Boolean = false,
        val autoDelete: Boolean = false,
        val internal: Boolean = false,
        val arguments: Map<String, Any> = emptyMap()
)