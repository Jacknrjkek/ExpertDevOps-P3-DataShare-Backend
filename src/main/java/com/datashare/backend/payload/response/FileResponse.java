package com.datashare.backend.payload.response;

import java.time.LocalDateTime;

/**
 * DTO retournant les détails d'un fichier au client frontend.
 * Contient les métadonnées utiles (Expirations, Token de partage, Stats).
 */
public class FileResponse {
    private Long id;
    private String originalName;
    private Long size;
    private LocalDateTime uploadDate;
    private LocalDateTime expirationDate;
    private String shareToken; // Token unique pour le lien de partage
    private Integer downloadCount;
    private java.util.Set<String> tags;

    public FileResponse(Long id, String originalName, Long size, LocalDateTime uploadDate, LocalDateTime expirationDate,
            String shareToken, Integer downloadCount, java.util.Set<String> tags) {
        this.id = id;
        this.originalName = originalName;
        this.size = size;
        this.uploadDate = uploadDate;
        this.expirationDate = expirationDate;
        this.shareToken = shareToken;
        this.downloadCount = downloadCount;
        this.tags = tags;
    }

    // Getters and Setters
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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public java.util.Set<String> getTags() {
        return tags;
    }

    public void setTags(java.util.Set<String> tags) {
        this.tags = tags;
    }
}
