package com.viartemev.thewhiterabbit.utils

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import com.viartemev.thewhiterabbit.AbstractTestContainersTest

fun getExchange(exchangeName: String): ExchangesHttpResponse? {
    val (_, _, response) = Fuel.get("http://${AbstractTestContainersTest.rabbitmq.containerIpAddress}:${AbstractTestContainersTest.rabbitmq.managementPort()}/api/exchanges").authenticate(
        "guest",
        "guest"
    ).responseObject<List<ExchangesHttpResponse>>()
    return response.get().find { it.name == exchangeName }
}

data class ExchangesHttpResponse(val name: String)
