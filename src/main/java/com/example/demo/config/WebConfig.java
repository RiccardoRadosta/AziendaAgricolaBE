package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Abilita CORS per tutti gli endpoint che iniziano con /api
                registry.addMapping("/api/**")
                        // Permette le richieste da qualsiasi origine. Per lo sviluppo locale è perfetto.
                        // In produzione, dovresti sostituire "*" con il dominio reale del tuo frontend
                        // (es. "https://www.il-tuo-sito.com").
                        .allowedOrigins("*")
                        // Specifica i metodi HTTP permessi.
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        // Permette tutti gli header nelle richieste.
                        .allowedHeaders("*")
                        .allowCredentials(false);
            }
        };
    }
}
