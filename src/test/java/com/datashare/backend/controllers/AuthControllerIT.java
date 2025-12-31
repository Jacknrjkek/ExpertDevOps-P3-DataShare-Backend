package com.datashare.backend.controllers;

import com.datashare.backend.model.AppUser;
import com.datashare.backend.payload.request.LoginRequest;
import com.datashare.backend.payload.request.SignupRequest;
import com.datashare.backend.repository.AppUserRepository;
import com.datashare.backend.security.jwt.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration pour AuthController.
 * Utilise MockMvc pour simuler des appels HTTP et vérifier les réponses.
 */
@ExtendWith(MockitoExtension.class)
public class AuthControllerIT {

    private MockMvc mockMvc;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    AppUserRepository userRepository;

    @Mock
    PasswordEncoder encoder;

    @Mock
    JwtUtils jwtUtils;

    @InjectMocks
    AuthController authController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    public void setup() {
        // Initialisation de MockMvc avec le contrôleur injecté
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    /**
     * Teste le scénario nominal d'inscription réussie.
     * Vérifie le code de retour 201 (Created) et le message de succès.
     */
    @Test
    public void testRegisterUser_Success() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("newuser@test.com");
        signupRequest.setPassword(TEST_PASSWORD);

        // Simulation des dépendances
        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        when(encoder.encode(TEST_PASSWORD)).thenReturn("encodedPassword");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Exécution de la requête POST et vérification
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }

    /**
     * Teste le scénario nominal de connexion réussie.
     * Vérifie le code 200 (OK) et la présence du Token JWT.
     */
    @Test
    public void testLoginUser_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword(TEST_PASSWORD);

        // Mock Authentication
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@test.com");

        AppUser mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("fake-jwt-token");

        // Exécution de la requête POST et vérification
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("fake-jwt-token"))
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }
}
