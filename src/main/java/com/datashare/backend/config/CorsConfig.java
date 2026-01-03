package com.datashare.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Configuration CORS (Cross-Origin Resource Sharing).
 * Autorise le frontend (localhost:4200) à communiquer avec cette API.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Origines autorisées (Frontend Angular)
        config.setAllowedOrigins(Arrays.asList("http://localhost:4200", "http://127.0.0.1:4200"));

        // Headers autorisés (Auth, Content-Type...)
        config.setAllowedHeaders(Arrays.asList("*"));

        // Méthodes HTTP autorisées
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Autoriser l'envoi de cookies/credentials si nécessaire
        config.setAllowCredentials(true);

        // Appliquer la config à toutes les routes
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
