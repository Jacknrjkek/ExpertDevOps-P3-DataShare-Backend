package com.datashare.backend.controllers;

// === Imports métier ===
import com.datashare.backend.model.AppUser;
import com.datashare.backend.model.File;
import com.datashare.backend.model.Share;
import com.datashare.backend.payload.response.FileResponse;
import com.datashare.backend.payload.response.MessageResponse;
import com.datashare.backend.repository.AppUserRepository;
import com.datashare.backend.repository.FileRepository;
import com.datashare.backend.repository.ShareRepository;
import com.datashare.backend.services.FileStorageService;

// === Imports Spring ===
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// === Utilitaires ===
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller responsable de la gestion des fichiers utilisateurs.
 *
 * - Upload de fichiers (avec vérification d'extension)
 * - Listing des fichiers de l'utilisateur connecté
 * - Suppression (physique et logique)
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    /**
     * Service de stockage physique (disque dur).
     */
    @Autowired
    FileStorageService fileStorageService;

    /**
     * Repository pour les métadonnées des fichiers.
     */
    @Autowired
    FileRepository fileRepository;

    /**
     * Repository pour récupérer l'utilisateur connecté.
     */
    @Autowired
    AppUserRepository userRepository;

    /**
     * Repository pour gérer les liens de partage associés.
     */
    @Autowired
    ShareRepository shareRepository;

    @Autowired
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // Liste des extensions interdites pour des raisons de sécurité
    private static final List<String> FORBIDDEN_EXTENSIONS = java.util.Arrays.asList(
            "exe", "msi", "bat", "cmd", "ps1", "vbs", "js", "jar", "com", "scr", "dll", "sys");

    /**
     * Endpoint d'upload de fichier (Authentifié).
     *
     * @param file           le fichier binaire reçu (Multipart)
     * @param expirationTime durée de validité optionnelle en jours
     * @param password       mot de passe optionnel pour protéger le fichier
     * @return statut de l'opération et token de partage généré
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "expirationTime", required = false) Integer expirationTime,
            @RequestParam(value = "password", required = false) String password) {

        try {
            // Récupère l'utilisateur connecté
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            AppUser user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return processFileUpload(file, expirationTime, password, user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new MessageResponse(
                    "Erreur lors de l'upload authentifié : " + e.getMessage()));
        }
    }

    /**
     * Endpoint d'upload de fichier (Anonyme - US07).
     */
    @PostMapping("/upload/anonymous")
    public ResponseEntity<?> uploadFileAnonymous(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "expirationTime", required = false) Integer expirationTime,
            @RequestParam(value = "password", required = false) String password) {
        return processFileUpload(file, expirationTime, password, null);
    }

    /**
     * Méthode commune pour traiter l'upload.
     */
    private ResponseEntity<?> processFileUpload(MultipartFile file, Integer expirationTime, String password,
            AppUser owner) {
        // 1. Vérifie l'extension du fichier (Sécurité)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) {
                extension = originalFilename.substring(i + 1).toLowerCase();
            }
            if (FORBIDDEN_EXTENSIONS.contains(extension)) {
                return ResponseEntity.badRequest().body(new MessageResponse(
                        "Extension non supportée : ." + extension));
            }
        }

        try {
            // 2. Stocke physiquement le fichier
            String fileName = fileStorageService.store(file);

            // 3. Crée l'entrée en base de données
            File fileEntity = new File();
            fileEntity.setOriginalName(file.getOriginalFilename());
            fileEntity.setStoragePath(fileName);
            fileEntity.setSize(file.getSize());
            fileEntity.setOwner(owner); // Peut être null pour anonyme

            // Validation et hashage du mot de passe
            if (password != null && !password.isBlank()) {
                if (password.length() < 6) {
                    return ResponseEntity.badRequest()
                            .body(new MessageResponse("Le mot de passe doit contenir au moins 6 caractères."));
                }
                fileEntity.setPasswordHash(passwordEncoder.encode(password));
            }

            // 4. Calcule la date d'expiration (Max 7 jours)
            int days = (expirationTime != null) ? Math.min(expirationTime, 7) : 7;
            if (days < 1)
                days = 1; // Minimum 1 jour
            fileEntity.setExpirationDate(LocalDateTime.now().plusDays(days));

            fileRepository.save(fileEntity);

            // 5. Crée automatiquement un lien de partage
            Share share = new Share();
            share.setFile(fileEntity);
            share.setUniqueToken(UUID.randomUUID().toString());
            shareRepository.save(share);

            // 6. Retourne la réponse succès avec les IDs
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "fileId", fileEntity.getId(),
                    "shareToken", share.getUniqueToken(),
                    "message", "Fichier téléversé avec succès (" + (owner != null ? "Authentifié" : "Anonyme") + ")"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new MessageResponse(
                    "Impossible de téléverser le fichier : " + file.getOriginalFilename() + ". Erreur : "
                            + e.getMessage()));
        }
    }

    /**
     * Endpoint de listing des fichiers.
     *
     * @return liste des fichiers appartenant à l'utilisateur connecté
     */
    @GetMapping
    public ResponseEntity<List<FileResponse>> getListFiles() {
        // Récupère l'utilisateur courant
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        AppUser user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        // Récupère les fichiers en base
        List<File> files = fileRepository.findByOwnerId(user.getId());
        System.out.println("FileController: getListFiles called for user " + user.getEmail() + ". Found " + files.size()
                + " files.");

        // Transforme les entités JPA en DTOs (FileResponse)
        List<FileResponse> fileResponses = files.stream().map(dbFile -> {
            Share share = shareRepository.findByFileId(dbFile.getId()).orElse(null);
            String token = (share != null) ? share.getUniqueToken() : null;
            Integer downloadCount = (share != null) ? share.getDownloadCount() : 0;

            return new FileResponse(
                    dbFile.getId(),
                    dbFile.getOriginalName(),
                    dbFile.getSize(),
                    dbFile.getCreatedAt(),
                    dbFile.getExpirationDate(),
                    token,
                    downloadCount,
                    dbFile.getTags());
        }).collect(Collectors.toList());

        return ResponseEntity.ok(fileResponses);
    }

    /**
     * Endpoint de suppression (via POST, legacy).
     */
    @PostMapping("/delete/{id}")
    public ResponseEntity<?> deleteFilePost(@PathVariable Long id) {
        return deleteFile(id);
    }

    /**
     * Endpoint de suppression de fichier.
     *
     * @param id ID du fichier à supprimer
     * @return statut de l'opération
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        // Vérification de sécurité : l'utilisateur est-il propriétaire ?
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        AppUser user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        return fileRepository.findById(id).map(file -> {
            if (!file.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Suppression du fichier physique
            fileStorageService.delete(file.getStoragePath());

            // Suppression des métadonnées en base
            fileRepository.delete(file);

            return ResponseEntity.ok(Map.of("message", "Fichier supprimé avec succès"));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Endpoint d'ajout de tag à un fichier.
     */
    @PostMapping("/{id}/tags")
    public ResponseEntity<?> addTag(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        AppUser user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        return fileRepository.findById(id).map(file -> {
            if (!file.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String tag = payload.get("tag");
            if (tag == null || tag.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Tag cannot be empty"));
            }

            if (tag.length() > 30) {
                return ResponseEntity.badRequest().body(new MessageResponse("Tag too long (max 30 chars)"));
            }

            file.getTags().add(tag.trim());
            fileRepository.save(file);

            return ResponseEntity.ok(new MessageResponse("Tag added successfully"));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Endpoint de suppression de tag d'un fichier.
     */
    @DeleteMapping("/{id}/tags/{tag}")
    public ResponseEntity<?> removeTag(@PathVariable Long id, @PathVariable String tag) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        AppUser user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        return fileRepository.findById(id).map(file -> {
            if (!file.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            file.getTags().remove(tag);
            fileRepository.save(file);

            return ResponseEntity.ok(new MessageResponse("Tag removed successfully"));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
