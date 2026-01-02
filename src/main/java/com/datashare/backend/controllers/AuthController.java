package com.datashare.backend.controllers;

// === Imports métier ===
import com.datashare.backend.model.AppUser;
import com.datashare.backend.payload.request.LoginRequest;
import com.datashare.backend.payload.request.SignupRequest;
import com.datashare.backend.payload.response.JwtResponse;
import com.datashare.backend.payload.response.MessageResponse;
import com.datashare.backend.repository.AppUserRepository;
import com.datashare.backend.security.jwt.JwtUtils;

// === Imports Spring ===
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

// === Validation & HTTP ===
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

// === Logging ===
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller responsable de l'authentification et de l'inscription des
 * utilisateurs.
 *
 * - Expose des endpoints REST
 * - Gère la connexion (login) avec génération de JWT
 * - Gère l'inscription (register) avec hash du mot de passe
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

        /**
         * AuthenticationManager :
         * composant Spring Security chargé de vérifier les identifiants
         * (email + mot de passe).
         */
        @Autowired
        AuthenticationManager authenticationManager;

        /**
         * Repository JPA pour accéder aux utilisateurs stockés en base de données.
         */
        @Autowired
        AppUserRepository userRepository;

        /**
         * PasswordEncoder :
         * permet de hasher les mots de passe avant stockage
         * (ex : BCrypt).
         */
        @Autowired
        PasswordEncoder encoder;

        /**
         * Utilitaire JWT :
         * génère et valide les tokens JWT.
         */
        @Autowired
        JwtUtils jwtUtils;

        /**
         * Endpoint de connexion utilisateur.
         *
         * @param loginRequest contient l'email et le mot de passe
         * @return JWT + informations utilisateur si authentification réussie
         */
        @PostMapping("/login")
        public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {

                // Authentifie l'utilisateur via Spring Security
                logger.info("DEBUG: Login attempt for: {}", loginRequest.getEmail());
                Authentication authentication;
                try {
                        authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        loginRequest.getEmail(),
                                                        loginRequest.getPassword()));
                } catch (Exception e) {
                        logger.error("DEBUG: Authentication failed for {}: {}", loginRequest.getEmail(),
                                        e.getMessage());
                        throw e;
                }

                // Stocke l'authentification dans le contexte de sécurité
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Génère un token JWT à partir de l'authentification
                String jwt = jwtUtils.generateJwtToken(authentication);

                // Récupère les détails de l'utilisateur authentifié
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                // Récupère l'utilisateur complet depuis la base
                AppUser user = userRepository
                                .findByEmail(userDetails.getUsername())
                                .orElseThrow();

                // Retourne le token JWT + infos utiles côté front
                return ResponseEntity.ok(
                                new JwtResponse(
                                                jwt,
                                                user.getId(),
                                                user.getEmail()));
        }

        /**
         * Endpoint d'inscription utilisateur.
         *
         * @param signUpRequest contient email et mot de passe
         * @return message de confirmation ou erreur
         */
        @PostMapping("/register")
        public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {

                // Vérifie si l'email existe déjà en base
                if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                        return ResponseEntity
                                        .badRequest()
                                        .body(new MessageResponse("Erreur : Cet email est déjà utilisé !"));
                }

                // Création du nouvel utilisateur
                AppUser user = new AppUser();
                user.setEmail(signUpRequest.getEmail());

                // Hash du mot de passe avant stockage (sécurité)
                user.setPasswordHash(
                                encoder.encode(signUpRequest.getPassword()));

                // Sauvegarde en base de données
                userRepository.save(user);

                // Retourne un statut HTTP 201 (Created)
                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(new MessageResponse("User registered successfully!"));
        }
}
