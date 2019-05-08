package com.viartemev.thewhiterabbit.common

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import java.io.IOException

fun <T> cancelOnIOException(cancellableContinuation: CancellableContinuation<T>, block: () -> Unit) {
    try {
        block()
    } catch (e: IOException) {
        val cancelled = cancellableContinuation.cancel(e)
        if (!cancelled) throw CancellationException(e.message)
    }
}
