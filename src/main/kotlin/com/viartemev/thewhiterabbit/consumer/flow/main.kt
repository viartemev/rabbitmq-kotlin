package com.viartemev.thewhiterabbit.consumer.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException

fun main() = runBlocking {
    flow {
        for (i in 1..5) {
            if (i == 5) {
                println("Flow is cancelling")
                throw CancellationException("Cancelled by choice")
            }
            emit(i)
            delay(100)
        }
    }.cancellable()
        .collect { value -> println(value) }
    println("Flow is completed")
}
