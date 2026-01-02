package com.datashare.backend.security.jwt;

// === Imports JWT ===
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

// === Imports Spring & Utils ===
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

/**
 * Composant utilitaire pour la gestion des JSON Web Tokens (JWT).
 *
 * - Génération de tokens signés (HMAC SHA)
 * - Extraction du username depuis un token
 * - Validation de l'intégrité et de l'expiration du token
 */
@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    // Clé secrète injectée depuis application.properties
    @Value("${datashare.app.jwtSecret:SecretKeyToGenJWTsDefaultValueNeedsTo beVeryLongSoItIsSecureEnoughForHS512Algorithm}")
    private String jwtSecret;

    // Durée de validité du token (ex: 24h)
    @Value("${datashare.app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs;

    /**
     * Génère un nouveau token JWT pour un utilisateur authentifié.
     *
     * @param authentication l'objet Authentication contenant le Principal
     *                       (UserDetails)
     * @return la chaîne JWT signée
     */
    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername())) // Sujet = Email
                .setIssuedAt(new Date()) // Date de création
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Date d'expiration
                .signWith(key(), SignatureAlgorithm.HS256) // Signature HMAC
                .compact();
    }

    /**
     * Retourne la clé cryptographique pour signer/vérifier les tokens.
     */
    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Extrait le nom d'utilisateur (email) contenu dans le token.
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * Valide un token JWT.
     * Vérifie la signature, l'expiration et le format.
     *
     * @param authToken la chaîne JWT à vérifier
     * @return true si valide, false sinon (avec log de l'erreur)
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }
}
