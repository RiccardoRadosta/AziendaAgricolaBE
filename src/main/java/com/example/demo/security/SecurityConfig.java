package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Regole specifiche per i prodotti
                .requestMatchers(HttpMethod.GET, "/api/products").permitAll() // Chiunque può vedere i prodotti
                .requestMatchers(HttpMethod.POST, "/api/products").authenticated() // Solo admin può creare
                .requestMatchers(HttpMethod.PUT, "/api/products/**").authenticated() // Solo admin può aggiornare
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").authenticated() // Solo admin può eliminare

                // Altre regole esistenti
                .requestMatchers("/api/admin/login").permitAll()
                .requestMatchers("/api/orders/**", "/api/newsletter/**").permitAll()
                .requestMatchers("/api/admin/**").authenticated()
                
                // Catch-all per tutte le altre richieste (se necessario, ma meglio essere espliciti)
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
