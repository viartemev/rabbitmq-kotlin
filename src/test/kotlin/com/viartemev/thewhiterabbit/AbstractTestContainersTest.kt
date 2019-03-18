package com.viartemev.thewhiterabbit

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.http.client.Client
import com.viartemev.thewhiterabbit.utils.RabbitMQContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractTestContainersTest {
    companion object {
        @Container
        @JvmStatic
        val rabbitmq = RabbitMQContainer()
    }

    lateinit var factory: ConnectionFactory
    lateinit var httpRabbitMQClient: Client
    val DEFAULT_VHOST = "/"

    @BeforeAll
    fun setUp() {
        factory = ConnectionFactory()
        factory.host = rabbitmq.containerIpAddress.toString()
        factory.port = rabbitmq.connectionPort()
        httpRabbitMQClient = Client("http://${rabbitmq.containerIpAddress}:${rabbitmq.managementPort()}/api/", "guest", "guest")
    }
}
