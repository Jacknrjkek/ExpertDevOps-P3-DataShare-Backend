package com.datashare.backend.controllers;

import com.datashare.backend.model.AppUser;
import com.datashare.backend.payload.request.LoginRequest;
import com.datashare.backend.payload.request.SignupRequest;
import com.datashare.backend.payload.response.JwtResponse;
import com.datashare.backend.payload.response.MessageResponse;
import com.datashare.backend.repository.AppUserRepository;
import com.datashare.backend.security.jwt.JwtUtils;
import com.datashare.backend.TestConstants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests Unitaires pour AuthController.
 * Vérifie la logique métier isolée (Validation, Appels Repository).
 */
@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthController authController;

    /**
     * Vérifie que l'inscription échoue si l'email existe déjà.
     */
    @Test
    public void testRegisterUser_EmailExists() {
        SignupRequest request = new SignupRequest();
        request.setEmail("existing@test.com");
        request.setPassword(TestConstants.TEST_USER_PASSWORD);

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        MessageResponse body = (MessageResponse) response.getBody();
        assertEquals("Erreur : Cet email est déjà utilisé !", body.getMessage());
    }

    /**
     * Vérifie le succès d'une inscription standard.
     */
    @Test
    public void testRegisterUser_Success() {
        SignupRequest request = new SignupRequest();
        request.setEmail("new@test.com");
        request.setPassword(TestConstants.TEST_USER_PASSWORD);

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(encoder.encode(TestConstants.TEST_USER_PASSWORD)).thenReturn("hashedPassword");
        // save returns valid entity
        when(userRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        MessageResponse body = (MessageResponse) response.getBody();
        assertEquals("User registered successfully!", body.getMessage());
    }

    /**
     * Vérifie le succès d'une authentification standard et la génération du JWT.
     */
    @Test
    public void testAuthenticateUser_Success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("password");

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@test.com");

        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JwtResponse body = (JwtResponse) response.getBody();
        assertEquals("jwt-token", body.getAccessToken());
        assertEquals("test@test.com", body.getEmail());
    }
}
