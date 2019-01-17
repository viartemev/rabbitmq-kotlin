package com.viartemev.thewhiterabbit.utils

import org.testcontainers.containers.GenericContainer

class RabbitMQContainer : GenericContainer<RabbitMQContainer>("rabbitmq:3-management") {
    private val managementPort = 15672
    private val connectionPort = 5672

    init {
        withExposedPorts(managementPort, connectionPort)
    }

    fun managementPort(): Int = getMappedPort(managementPort)

    fun connectionPort(): Int = getMappedPort(connectionPort)

}
