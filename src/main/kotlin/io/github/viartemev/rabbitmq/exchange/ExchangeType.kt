package io.github.viartemev.rabbitmq.exchange

/**
 * Represents the type of exchange in a messaging system.
 *
 * @param asString the string representation of the exchange type
 */
enum class ExchangeType(val asString: String) {
    DIRECT("direct"),
    FANOUT("fanout"),
    TOPIC("topic"),
    HEADERS("headers");
}
