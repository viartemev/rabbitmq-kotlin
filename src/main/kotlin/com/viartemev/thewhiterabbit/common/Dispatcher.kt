package com.viartemev.thewhiterabbit.common

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

object RabbitMqDispatchers {

    /**
     * Single-threaded coroutine dispatcher
     */
    @JvmStatic
    val SingleThreadDispatcher: ExecutorCoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}
