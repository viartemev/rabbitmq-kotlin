package com.viartemev.thewhiterabbit.utils

import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.publisher.OutboundMessage

fun createMessage(exchange: String = "", queue: String = "test_queue", body: String) =
    OutboundMessage(exchange, queue, MessageProperties.PERSISTENT_BASIC, body)
