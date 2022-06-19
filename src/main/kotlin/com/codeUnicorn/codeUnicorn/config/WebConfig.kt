package com.codeUnicorn.codeUnicorn.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// CORS 설정
@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        // Allow all origins
        registry.addMapping("/**")
            .allowedOrigins("https://codeunicorn.kr")
            .allowedMethods("GET", "POST", "PATCH")
            .allowCredentials(true)
            .exposedHeaders("*")
    }
}
