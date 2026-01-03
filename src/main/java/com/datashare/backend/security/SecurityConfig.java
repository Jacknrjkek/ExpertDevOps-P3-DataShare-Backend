package com.datashare.backend.security;

// === Imports Application ===
import com.datashare.backend.security.jwt.AuthEntryPointJwt;
import com.datashare.backend.security.jwt.AuthTokenFilter;
import com.datashare.backend.security.services.UserDetailsServiceImpl;

// === Imports Spring Security ===
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration centrale de la sécurité de l'application (Spring Security).
 *
 * - Désactive CSRF (inutile pour API REST Stateless, Safe to disable)
 * - Configure la gestion de session en mode STATELESS (JWT obligatoire)
 * - Définit les règles d'accès aux URLs (PermitAll vs Authenticated)
 * - Intègre le filtre JWT avant l'authentification standard
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final AuthTokenFilter authTokenFilter;

    // Injection par constructeur (Bonne pratique)
    public SecurityConfig(UserDetailsServiceImpl userDetailsService, AuthEntryPointJwt unauthorizedHandler,
            AuthTokenFilter authTokenFilter) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.authTokenFilter = authTokenFilter;
    }

    /**
     * Provider d'authentification responsable de vérifier l'identité.
     * Utilise UserDetailsService pour charger l'utilisateur et PasswordEncoder pour
     * vérifier le MDP.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Interface principale pour initier l'authentification.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Encodeur de mot de passe utilisant l'algorithme BCrypt (Robuste).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Chaîne de sécurité spécifique pour les accés PUBLICS (Téléchargement de
     * fichiers partagés).
     * Aucune authentification requise.
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/share/**", "/api/download/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    /**
     * Chaîne de sécurité par défaut pour l'API Protégée.
     * Tout accès nécessite un JWT valide.
     */
    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Autoriser Login et Register sans Token
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/files/upload/anonymous")
                        .permitAll()
                        // Autoriser les requêtes OPTIONS (Pre-flight CORS)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // Tout le reste nécessite Authentification
                        .anyRequest().authenticated());

        // Configure le provider d'auth
        http.authenticationProvider(authenticationProvider());

        // Ajoute le filtre JWT avant le filtre d'autorisation de Spring Security
        // (Pour s'assurer qu'il est exécuté même si formLogin est désactivé)
        http.addFilterBefore(authTokenFilter, AuthorizationFilter.class);

        return http.build();
    }

}
