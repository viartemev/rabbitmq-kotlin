package com.viartemev.thewhiterabbit.utils

import org.testcontainers.containers.GenericContainer

class RabbitMQContainer : GenericContainer<RabbitMQContainer>("rabbitmq:3-management") {
    val managementPort = 15672
    val connectionPort = 5672

    init {
        withExposedPorts(managementPort, connectionPort)
    }

}
