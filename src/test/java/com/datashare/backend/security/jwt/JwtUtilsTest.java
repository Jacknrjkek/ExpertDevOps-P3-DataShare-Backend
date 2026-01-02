package com.datashare.backend.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests Unitaires pour JwtUtils.
 * Vérifie la génération, l'extraction de données et la validation des JWT.
 */
@ExtendWith(MockitoExtension.class)
public class JwtUtilsTest {

    @InjectMocks
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    private final String jwtSecret = com.datashare.backend.TestConstants.TEST_JWT_SECRET;
    private final int jwtExpirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        // Injection des propriétés via ReflectionTestUtils
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", jwtExpirationMs);
    }

    /**
     * Vérifie la génération d'un token valide.
     */
    @Test
    void generateJwtToken_Success() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");

        String token = jwtUtils.generateJwtToken(authentication);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    /**
     * Vérifie l'extraction correcte du nom d'utilisateur depuis le token.
     */
    @Test
    void getUserNameFromJwtToken_Success() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");

        String token = jwtUtils.generateJwtToken(authentication);
        String username = jwtUtils.getUserNameFromJwtToken(token);

        assertEquals("testuser", username);
    }

    /**
     * Valide un token correct.
     */
    @Test
    void validateJwtToken_Valid() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");

        String token = jwtUtils.generateJwtToken(authentication);

        assertTrue(jwtUtils.validateJwtToken(token));
    }

    @Test
    void validateJwtToken_Invalid() {
        assertFalse(jwtUtils.validateJwtToken("invalid-token"));
    }

    @Test
    void validateJwtToken_Empty() {
        assertFalse(jwtUtils.validateJwtToken(""));
    }

    /**
     * Vérifie qu'un token expiré est bien rejeté.
     */
    @Test
    void validateJwtToken_Expired() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 1); // 1ms expiration

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");

        String token = jwtUtils.generateJwtToken(authentication);

        Thread.sleep(10); // Wait for expiration

        assertFalse(jwtUtils.validateJwtToken(token));
    }
}
