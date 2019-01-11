package com.viartemev.thewhiterabbit.exchange

data class DeleteExchangeSpecification(
    val exchange: String,
    val ifUnused: Boolean = false,
    val noWait: Boolean = false
)
