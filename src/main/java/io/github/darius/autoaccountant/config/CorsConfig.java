package io.github.darius.autoaccountant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // application.properties (local):
    //   app.cors.allowed-origins=http://localhost:8080,http://127.0.0.1:8080
    //   app.cors.allowed-origins=link del despliegue de Render
    // Render (variable de entorno, Spring la mapea automáticamente):
    //   APP_CORS_ALLOWED_ORIGINS=link del despliegue en Render
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}