package com.example.order_manager.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class AppConfig {

    @Bean
    fun limsWebClient(@Value("\${lims.base-url:http://localhost:8081}") baseUrl: String): WebClient =
        WebClient.builder().baseUrl(baseUrl).build()

}
