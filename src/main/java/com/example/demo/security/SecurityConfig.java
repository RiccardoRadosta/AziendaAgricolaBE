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
                // Endpoints Pubblici (accessibili a tutti)
                .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers("/api/orders/charge").permitAll()
                .requestMatchers("/api/orders/create").permitAll()
                .requestMatchers("/api/newsletter/subscribe").permitAll()
                .requestMatchers("/api/admin/login").permitAll()
                .requestMatchers("/api/settings/public").permitAll() // <-- REGOlA RIPRISTINATA
                .requestMatchers("/api/coupons/**").permitAll() // <-- REGOlA RIPRISTINATA

                // Endpoints Protetti (richiedono token admin)
                .requestMatchers(HttpMethod.POST, "/api/products").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").authenticated()
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/shipments/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").authenticated()

                // Nega tutto il resto per sicurezza (se non matchato sopra)
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
