package com.datashare.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un fichier téléversé.
 *
 * Contient les métadonnées du fichier (nom, chemin, taille)
 * et gère la relation avec le propriétaire et les partages.
 */
@Entity
@Table(name = "file")
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_name")
    private String originalName;

    // Chemin relatif/interne sur le disque
    @Column(name = "storage_path", unique = true)
    private String storagePath;

    @Column(name = "size")
    private Long size;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Date de suppression logique/physique prévue
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    // Relation Many-to-One : Un fichier appartient à un seul utilisateur (ou null
    // si anonyme)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = true)
    private AppUser owner;

    // Relation One-to-Many : Un fichier peut avoir plusieurs liens de partages
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Share> shares = new java.util.ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password_hash")
    private String passwordHash;

    @ElementCollection
    @CollectionTable(name = "file_tags", joinColumns = @JoinColumn(name = "file_id"))
    @Column(name = "tag", length = 30)
    private java.util.Set<String> tags = new java.util.HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public java.util.List<Share> getShares() {
        return shares;
    }

    public void setShares(java.util.List<Share> shares) {
        this.shares = shares;
    }

    public java.util.Set<String> getTags() {
        return tags;
    }

    public void setTags(java.util.Set<String> tags) {
        this.tags = tags;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
