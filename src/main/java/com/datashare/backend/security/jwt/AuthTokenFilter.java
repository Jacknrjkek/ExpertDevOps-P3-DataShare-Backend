package com.datashare.backend.security.jwt;

// === Imports Application ===
import com.datashare.backend.security.services.UserDetailsServiceImpl;

// === Imports Servlet API ===
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// === Imports Spring Security ===
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

// === Imports Utils ===
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * Filtre de sécurité exécuté à chaque requête (OncePerRequestFilter).
 *
 * Rôle :
 * 1. Intercepter la requête HTTP
 * 2. Extraire le token JWT du header "Authorization"
 * 3. Valider le token via JwtUtils
 * 4. Charger l'utilisateur associé et mettre à jour le SecurityContext
 */
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Logique principale du filtre.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            logger.info("DEBUG: Incoming Request URI: {}", request.getRequestURI());
            // Lecture du token depuis la requête
            String jwt = parseJwt(request);

            // Validation du token
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                // Extraction du username (email)
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                // Chargement des détails utilisateur (rôles, pwd...)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Création de l'objet Authentication
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Injection de l'identité dans le contexte de sécurité Spring
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        // Passe la main au filtre suivant
        filterChain.doFilter(request, response);
    }

    /**
     * Extrait le token JWT du header "Authorization".
     * Format attendu: "Bearer <token>"
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
