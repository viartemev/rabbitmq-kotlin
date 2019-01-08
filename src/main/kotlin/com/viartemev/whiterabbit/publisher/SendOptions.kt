package com.viartemev.whiterabbit.publisher

import com.rabbitmq.client.Channel

data class SendOptions(
        val channel: Channel
)
