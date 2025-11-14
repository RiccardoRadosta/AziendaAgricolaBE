package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Mantiene la configurazione CORS esistente
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Gestisce le risorse statiche e il routing per la Single Page Application (SPA)
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResolver());
    }

    /**
     * Un resolver custom che risolve due problemi:
     * 1. Impedisce che le chiamate API vengano intercettate dal gestore di risorse statiche.
     * 2. Reindirizza tutte le rotte non-API e non-file a index.html, permettendo al router del frontend (es. React Router) di gestirle.
     */
    public static class SpaResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            // Se la richiesta inizia con "api/", non la gestiamo qui.
            // Restituendo null, passiamo il controllo ad altri handler (come i @RestController di Spring).
            if (resourcePath.startsWith("api/")) {
                return null;
            }

            // Cerca la risorsa richiesta nella cartella statica (es. "main.js", "style.css", "logo.png")
            Resource requestedResource = location.createRelative(resourcePath);

            // Se la risorsa esiste ed è leggibile, la restituisce.
            // Altrimenti, restituisce index.html per permettere al router del frontend di gestire la rotta.
            return requestedResource.exists() && requestedResource.isReadable() ? requestedResource
                    : new ClassPathResource("/static/index.html");
        }
    }
}
