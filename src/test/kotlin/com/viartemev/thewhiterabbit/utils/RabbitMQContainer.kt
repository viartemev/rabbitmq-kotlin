package com.viartemev.thewhiterabbit.utils

import org.testcontainers.containers.GenericContainer

private const val managementPort = 15672
private const val connectionPort = 5672

class RabbitMQContainer : GenericContainer<RabbitMQContainer>("rabbitmq:3-management") {

    init {
        withExposedPorts(managementPort, connectionPort)
    }

    fun managementPort(): Int = getMappedPort(managementPort)

    fun connectionPort(): Int = getMappedPort(connectionPort)

}
