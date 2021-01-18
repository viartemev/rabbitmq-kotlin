package com.viartemev.thewhiterabbit.exchange

/**
 * An exchange deletion specification
 */
data class DeleteExchangeSpecification(
    /**
     * The name of the exchange
     */
    val name: String,
    /**
     * True to indicate that the exchange is only to be deleted if it is unused
     */
    val ifUnused: Boolean = false,
    /**
     * True to don't wait for a response from the server
     */
    val noWait: Boolean = false
)
