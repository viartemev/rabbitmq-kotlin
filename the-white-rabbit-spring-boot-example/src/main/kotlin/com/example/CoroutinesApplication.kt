package com.example

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.consume
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
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

    //FIXME add fetching messages
    suspend fun pull(request: ServerRequest): ServerResponse {
        val serviceResponse: ServerResponse.BodyBuilder = ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
        connection.channel {
            consume("test_queue", 1) {
                consumeWithConfirm({
                    serviceResponse.body(BodyInserters.fromObject(Message(String(it.body)))) })
            }
        }
        return serviceResponse.buildAndAwait()
    }

    suspend fun push(request: ServerRequest): ServerResponse {
        val message = request.awaitBody<Message>() ?: throw RuntimeException("A message can't be empty")
        connection.confirmChannel {
            publish {
                publishWithConfirm(OutboundMessage("", "test_queue", MessageProperties.PERSISTENT_BASIC, message.message))
            }
        }
        return ServerResponse.ok().bodyAndAwait("Done")
    }
}

fun main(args: Array<String>) {
    runApplication<CoroutinesApplication>(*args)
}
