package com.viartemev.thewhiterabbit.utils

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.queue.QueuesHttpResponse
import org.junit.jupiter.api.Assertions

fun getQueue(queueName: String): QueuesHttpResponse? {
    val (_, _, response) = Fuel.get("http://localhost:${AbstractTestContainersTest.rabbitmq.managementPort()}/api/queues").authenticate(
        "guest",
        "guest"
    ).responseObject<List<QueuesHttpResponse>>()
    val queues = response.get()
    Assertions.assertNotNull(queues)
    return queues.find { it.name == queueName }
}
