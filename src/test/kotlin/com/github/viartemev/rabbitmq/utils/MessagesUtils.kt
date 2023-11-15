package com.github.viartemev.rabbitmq.utils

import com.github.viartemev.rabbitmq.publisher.OutboundMessage
import com.rabbitmq.client.MessageProperties

fun createMessage(exchange: String = "", queue: String = "test_queue", body: String) =
    OutboundMessage(exchange, queue, MessageProperties.PERSISTENT_BASIC, body)
