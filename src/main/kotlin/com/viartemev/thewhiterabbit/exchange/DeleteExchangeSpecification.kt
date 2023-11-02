package com.viartemev.thewhiterabbit.exchange

/**
 * Represents a specification for deleting an exchange.
 *
 * @param exchange The name of the exchange to delete.
 * @param ifUnused Specifies whether the exchange should only be deleted if it is currently not in use.
 * @param noWait Specifies whether the server should wait for the deletion of the exchange to complete before responding to the request.
 */
data class DeleteExchangeSpecification(
    val exchange: String,
    val ifUnused: Boolean = false,
    val noWait: Boolean = false
)
