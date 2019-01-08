package com.viartemev.whiterabbit.common

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

val techDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()