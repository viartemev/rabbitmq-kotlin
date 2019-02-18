package com.viartemev.the_white_rabbit_vertx_example

import io.vertx.core.Vertx
import io.vertx.kotlin.core.deployVerticleAwait

suspend fun main() {
    val vertx = Vertx.vertx()
    try {
        vertx.deployVerticleAwait("com.viartemev.the_white_rabbit_vertx_example.App")
        println("Application started")
    } catch (exception: Throwable) {
        println("Could not start application")
        exception.printStackTrace()
    }
}

