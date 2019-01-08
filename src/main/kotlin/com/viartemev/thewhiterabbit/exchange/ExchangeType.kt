package com.viartemev.thewhiterabbit.exchange

enum class ExchangeType(val asString: String) {
    DIRECT("direct"),
    FANOUT("fanout"),
    TOPIC("topic"),
    HEADERS("headers");
}