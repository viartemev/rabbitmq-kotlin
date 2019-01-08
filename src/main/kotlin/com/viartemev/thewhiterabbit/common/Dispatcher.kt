package com.viartemev.thewhiterabbit.common

import kotlinx.coroutines.newSingleThreadContext

val resourceManagementDispatcher = newSingleThreadContext("ResourceManagementDispatcher")