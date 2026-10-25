package com.acikarttirma.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Frontend (ör. React, localhost:5173) farklı porttan REST'e istek atar, tarayıcı bunu CORS
 * kuralıyla denetler. Geliştirme için localhost'un tüm portlarına izin verilir.
 * (WebSocket tarafı WebSocketConfig'de zaten ayarlı.)
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}