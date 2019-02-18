package com.viartemev.the_white_rabbit_vertx_example

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.consume
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch


class App : CoroutineVerticle() {

    private val factory = ConnectionFactory()
    private var connection = factory.newConnection()

    override suspend fun start() {

        // Build Vert.x Web router
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        router.get("/pull").coroutineHandler { ctx -> pull(ctx) }
        router.post("/push").coroutineHandler { ctx -> push(ctx) }

        // Start the server
        vertx.createHttpServer()
                .requestHandler(router)
                .listenAwait(config.getInteger("http.port", 8081))
    }

    suspend fun pull(ctx: RoutingContext) {
        connection.channel {
            consume("test_queue", 1) {
                consumeWithConfirm({ ctx.response().setStatusCode(200).end(json { obj("message" to String(it.body)).encode() }) })
            }
        }
    }

    suspend fun push(ctx: RoutingContext) {
        val json = ctx.bodyAsJson
        connection.confirmChannel {
            publish {
                publishWithConfirm(OutboundMessage("", "test_queue", MessageProperties.PERSISTENT_BASIC, json.getString("message")))
            }
        }
        ctx.response().setStatusCode(201).end("Done")
    }

    /**
     * An extension method for simplifying coroutines usage with Vert.x Web routers
     */
    fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
        handler { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    fn(ctx)
                } catch (e: Exception) {
                    ctx.fail(e)
                }
            }
        }
    }
}
