package com.example.order_manager.config

import org.springframework.amqp.core.*
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val QUEUE_ORDERS = "orders.queue"
        const val EXCHANGE_ORDERS = "orders.exchange"
        const val ROUTING_KEY_ORDERS = "orders.routing.key.#"

        const val QUEUE_LIMS_SEND = "lims.send.queue"
        const val EXCHANGE_LIMS_SEND = "lims.send.exchange"
        const val ROUTING_KEY_LIMS_SEND = "lims.send.#"
    }

    @Bean
    fun queue(): Queue {
        // durable = true -> messages survive after restart fo the broker
        return Queue(QUEUE_ORDERS, true)
    }

    @Bean
    fun exchange(): TopicExchange {
        return TopicExchange(EXCHANGE_ORDERS)
    }

    @Bean
    fun binding(queue: Queue, exchange: TopicExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_ORDERS)
    }

    @Bean
    fun limsQueue(): Queue = Queue(QUEUE_LIMS_SEND, true)

    @Bean
    fun limsExchange(): TopicExchange = TopicExchange(EXCHANGE_LIMS_SEND)

    @Bean
    fun limsBinding(limsQueue: Queue, limsExchange: TopicExchange): Binding =
        BindingBuilder.bind(limsQueue).to(limsExchange).with(ROUTING_KEY_LIMS_SEND)

    // Translate Kotlin objects in json
    @Bean
    fun messageConverter(): MessageConverter {
        return JacksonJsonMessageConverter()
    }
}