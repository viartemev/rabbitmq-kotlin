package com.viartemev.thewhiterabbit

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // this: CoroutineScope
    launch {
        println("First launch")
        // launch new coroutine in the scope of runBlocking
        delay(1000L)
        println("World!")
    }
    launch {
        println("Second launch")
        // launch new coroutine in the scope of runBlocking
        delay(1000L)
        println("World!")
    }

    println("Hello,")
}
