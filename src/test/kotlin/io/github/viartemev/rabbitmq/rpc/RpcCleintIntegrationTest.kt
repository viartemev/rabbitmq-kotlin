package io.github.viartemev.rabbitmq.rpc

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import com.rabbitmq.client.MessageProperties
import io.github.viartemev.rabbitmq.AbstractTestContainersTest
import io.github.viartemev.rabbitmq.common.OutboundMessage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils
import kotlin.concurrent.thread

class RpcCleintIntegrationTest : AbstractTestContainersTest() {

    fun serverChannel() = factory.newConnection().createChannel()

    fun randomRpcQueueName() = "rpc_request_" + RandomStringUtils.randomNumeric(5)

    fun rpcServer(channel: Channel, rpcQueueName: String) {
        channel.queueDeclare(rpcQueueName, true, false, false, null)
        val rpcServer = object : com.rabbitmq.client.RpcServer(channel, rpcQueueName) {
            override fun handleCall(request: Delivery?, replyProperties: AMQP.BasicProperties?): ByteArray {
                return request?.body?.let {
                    ("Hello, " + String(it)).toByteArray()
                } ?: "Body is empty".toByteArray()
            }
        }
        rpcServer.mainloop()
    }

    @Test
    fun rpcCall() {
        val rpcQueueName = randomRpcQueueName()
        thread(isDaemon = true) { rpcServer(serverChannel(), rpcQueueName) }
        factory.newConnection().use { connection ->
            val channel = connection.createChannel()
            val rpcClient = RpcClient(channel)
            runBlocking {
                val rpcResponse = rpcClient.call(
                    OutboundMessage("", rpcQueueName, MessageProperties.PERSISTENT_BASIC, "Slava".toByteArray())
                )
                val rpcResponseAsString = String(rpcResponse.body)
                assertNotNull(rpcResponseAsString)
                assertEquals("Hello, Slava", rpcResponseAsString)
            }
        }
    }
}
