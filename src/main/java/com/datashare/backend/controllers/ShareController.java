package com.datashare.backend.controllers;

// === Imports métier ===
import com.datashare.backend.model.File;
import com.datashare.backend.repository.ShareRepository;
import com.datashare.backend.services.FileStorageService;

// === Imports Spring ===
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// === Utilitaires ===
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller responsable de l'accès publique aux fichiers partagés.
 *
 * - Enpoints d'accès Anonyme (pas de vérification JWT)
 * - Récupération des métadonnées via Token
 * - Téléchargement physique du fichier
 */
@RestController
@RequestMapping("/api")
public class ShareController {

    /**
     * Repository pour vérifier la validité des tokens de partage.
     */
    @Autowired
    ShareRepository shareRepository;

    /**
     * Service pour accéder au système de fichiers (stockage).
     */
    @Autowired
    FileStorageService fileStorageService;

    @Autowired
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * Endpoint pour récupérer les infos d'un partage (taille, nom, expiration).
     * Accessible sans authentification.
     *
     * @param token le token unique de partage
     * @return métadonnées du fichier ou erreur 410 (Expiré) / 404 (Introuvable)
     */
    @GetMapping("/share/{token}")
    public ResponseEntity<?> getShareMetadata(@PathVariable String token) {
        return shareRepository.findByUniqueToken(token)
                .map(share -> {
                    File file = share.getFile();

                    // Vérification de la date d'expiration
                    if (file.getExpirationDate() != null && file.getExpirationDate().isBefore(LocalDateTime.now())) {
                        return ResponseEntity.status(410).body(Map.of("message", "Link expired"));
                    }

                    // Construction de la réponse JSON simplifiée
                    java.util.Map<String, Object> response = new java.util.HashMap<>();
                    response.put("fileName", file.getOriginalName());
                    response.put("size", file.getSize());
                    response.put("expiration",
                            file.getExpirationDate() != null ? file.getExpirationDate().toString() : null);
                    response.put("isProtected", file.getPasswordHash() != null && !file.getPasswordHash().isEmpty());

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint de téléchargement du fichier (Public / Non protégé).
     */
    @GetMapping("/download/{token}")
    public ResponseEntity<?> downloadFile(@PathVariable String token) {
        return processDownload(token, null);
    }

    /**
     * Endpoint de téléchargement protégé par mot de passe.
     */
    @PostMapping("/download/{token}")
    public ResponseEntity<?> downloadFileProtected(@PathVariable String token, @RequestBody Map<String, String> body) {
        String password = body.get("password");
        return processDownload(token, password);
    }

    private ResponseEntity<?> processDownload(String token, String password) {
        return shareRepository.findByUniqueToken(token)
                .map(share -> {
                    File file = share.getFile();

                    // 1. Vérification de l'expiration
                    if (file.getExpirationDate() != null && file.getExpirationDate().isBefore(LocalDateTime.now())) {
                        return ResponseEntity.status(410).body(Map.of("message", "Link expired"));
                    }

                    // 2. Vérification du mot de passe (si protégé)
                    if (file.getPasswordHash() != null && !file.getPasswordHash().isEmpty()) {
                        if (password == null || !passwordEncoder.matches(password, file.getPasswordHash())) {
                            return ResponseEntity.status(403).body(Map.of("message", "Mot de passe incorrect"));
                        }
                    }

                    // 3. Incrémente le compteur
                    share.setDownloadCount(share.getDownloadCount() + 1);
                    shareRepository.save(share);

                    // 4. Charge le fichier
                    Resource resource = fileStorageService.loadFileAsResource(file.getStoragePath());
                    String contentType = "application/octet-stream";

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + file.getOriginalName() + "\"")
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
