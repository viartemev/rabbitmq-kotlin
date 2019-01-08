package com.viartemev.whiterabbit.common

import kotlinx.coroutines.newSingleThreadContext

val resourceManagementDispatcher = newSingleThreadContext("ResourceManagementDispatcher")