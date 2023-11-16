package io.github.viartemev.rabbitmq

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.http.client.Client
import io.github.viartemev.rabbitmq.utils.RabbitMQContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URL

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
    lateinit var connection: Connection
    val DEFAULT_VHOST = "/"

    @BeforeAll
    fun setUp() {
        factory = ConnectionFactory()
        factory.host = rabbitmq.host.toString()
        factory.port = rabbitmq.connectionPort()
        httpRabbitMQClient =
            Client(URL("http://${rabbitmq.host}:${rabbitmq.managementPort()}/api/"), "guest", "guest")
        connection = factory.newConnection()
    }
}
