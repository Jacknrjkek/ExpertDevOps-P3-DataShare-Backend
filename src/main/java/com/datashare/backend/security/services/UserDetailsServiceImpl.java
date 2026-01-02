package com.datashare.backend.security.services;

// === Imports Métier ===
import com.datashare.backend.model.AppUser;
import com.datashare.backend.repository.AppUserRepository;

// === Imports Spring Security ===
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Service implémentant l'interface standard UserDetailsService de Spring
 * Security.
 *
 * Permet de charger les données d'un utilisateur depuis la base de données
 * lors du processus d'authentification.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    // Repository pour l'accès aux données persistence
    private final AppUserRepository userRepository;

    public UserDetailsServiceImpl(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Charge l'utilisateur par son nom (ici, l'email).
     *
     * @param email Identifiant de connexion
     * @return UserDetails objet compréhensible par Spring Security
     * @throws UsernameNotFoundException si l'utilisateur n'existe pas
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Recherche en base
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        System.out.println("DEBUG: Found user in DB: " + user.getEmail());
        System.out.println("DEBUG: Stored Hash: " + user.getPasswordHash());

        // Conversion en UserDetails
        // Note: Nous n'utilisons pas de rôles/autorités spécifiques pour le moment
        // (Collections.emptyList())
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.emptyList());
    }
}
