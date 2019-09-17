package com.example

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.consume
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.coRouter

@SpringBootApplication
class CoroutinesApplication {

    @Bean
    fun connectionFactory() = ConnectionFactory()

    @Bean
    fun connection(connectionFactory: ConnectionFactory) = connectionFactory.newConnection()!!

    @Bean
    fun routes(handlers: Handlers) = coRouter {
        GET("/pull", handlers::pull)
        POST("/push", handlers::push)
    }
}

data class Message(val message: String)

@Component
class Handlers(private val connection: Connection) {

    suspend fun pull(request: ServerRequest): ServerResponse {
        var message = "default_value"
        connection.channel {
            consume("test_queue", 1) {
                consumeMessageWithConfirm {
                    println("Got a message!")
                    message = String(it.body)
                }
            }
        }
        return ServerResponse.ok().bodyValue(Message(message)).awaitSingle()
    }

    suspend fun push(request: ServerRequest): ServerResponse {
        val message = request.awaitBody<Message>() ?: throw RuntimeException("A message can't be empty")
        var ack = false
        connection.confirmChannel {
            publish {
                ack = publishWithConfirm(
                    OutboundMessage(
                        "",
                        "test_queue",
                        MessageProperties.PERSISTENT_BASIC,
                        message.message
                    )
                )
            }
        }
        return if (ack) ServerResponse.ok().bodyValue("Done").awaitSingle() else ServerResponse.status(500).build().awaitSingle()
    }
}

fun main(args: Array<String>) {
    runApplication<CoroutinesApplication>(*args)
}
