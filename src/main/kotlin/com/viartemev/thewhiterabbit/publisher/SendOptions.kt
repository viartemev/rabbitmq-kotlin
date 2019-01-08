package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.Channel

data class SendOptions(
        val channel: Channel
)
