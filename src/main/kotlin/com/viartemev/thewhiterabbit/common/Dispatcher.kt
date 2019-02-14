package com.viartemev.thewhiterabbit.common

import kotlinx.coroutines.newSingleThreadContext

/**
 * Resource management dispatcher.
 * Used as dispatcher only for managing exchanges and queues.
 */
val resourceManagementDispatcher = newSingleThreadContext("ResourceManagementDispatcher")
