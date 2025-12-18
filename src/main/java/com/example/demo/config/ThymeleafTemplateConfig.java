package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
public class ThymeleafTemplateConfig {

    @Bean
    public TemplateEngine emailTemplateEngine() {
        final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        // Ora il motore dei template riceve un BEAN gestito da Spring, non un oggetto privato.
        templateEngine.addTemplateResolver(htmlTemplateResolver());
        return templateEngine;
    }

    // === FIX: Dichiarato come @Bean pubblico ===
    // In questo modo, Spring gestisce il ciclo di vita di questo oggetto
    // e lo rende disponibile per l'injection in altri bean, come l'emailTemplateEngine.
    @Bean
    public ITemplateResolver htmlTemplateResolver() {
        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(1); // L'ordine è importante se ci sono più resolver
        templateResolver.setPrefix("/templates/"); // Cerca i template nella cartella /resources/templates/
        templateResolver.setSuffix(".html"); // Aggiunge .html alla fine dei nomi dei template
        templateResolver.setTemplateMode(TemplateMode.HTML); // Imposta la modalità di parsing a HTML5
        templateResolver.setCharacterEncoding("UTF-8"); // Imposta la codifica
        templateResolver.setCacheable(false); // Disabilita la cache per lo sviluppo
        return templateResolver;
    }
}
