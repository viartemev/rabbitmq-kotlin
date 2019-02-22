package com.viartemev.thewhiterabbit.utils

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull

fun getQueue(queueName: String): QueuesHttpResponse? {
    val (_, _, response) = Fuel.get("http://${AbstractTestContainersTest.rabbitmq.testHostIpAddress}:${AbstractTestContainersTest.rabbitmq.managementPort()}/api/queues").authenticate(
        "guest",
        "guest"
    ).responseObject<List<QueuesHttpResponse>>()
    val queues = response.get()
    assertNotNull(queues)
    return queues.find { it.name == queueName }
}

fun checkQueue(queueName: String) {
    val (_, _, response) = Fuel.get("http://${AbstractTestContainersTest.rabbitmq.testHostIpAddress}:${AbstractTestContainersTest.rabbitmq.managementPort()}/api/queues").authenticate(
        "guest",
        "guest"
    ).responseObject<List<QueuesHttpResponse>>()
    val queues = response.get()
    assertNotNull(queues)
    Assertions.assertTrue { queues.isNotEmpty() }
    Assertions.assertTrue { queues.find { it.name == queueName } != null }
}

data class QueuesHttpResponse(val name: String, val messages: Long)
