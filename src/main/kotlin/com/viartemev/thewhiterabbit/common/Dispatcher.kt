package com.viartemev.thewhiterabbit.common

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Resource management dispatcher.
 * Used as a dispatcher only for managing exchanges and queues.
 */
val resourceManagementDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
