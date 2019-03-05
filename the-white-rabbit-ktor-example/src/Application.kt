package com.viartemev

import com.fasterxml.jackson.databind.SerializationFeature
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.consume
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val factory = ConnectionFactory()
    val connection = factory.newConnection()

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {
        get("/pull") {
            connection.channel {
                consume("test_queue") {
                    consumeMessageWithConfirm({ call.respond(String(it.body)) })
                }
            }
        }

        post("/push") {
            val body = call.receiveText()
            connection.confirmChannel {
                publish {
                    publishWithConfirm(OutboundMessage("", "test_queue", MessageProperties.PERSISTENT_BASIC, body))
                }
            }
            call.respond("Done")
        }
    }
}

