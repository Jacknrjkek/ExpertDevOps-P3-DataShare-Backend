package com.datashare.backend.services;

import com.datashare.backend.model.File;
import com.datashare.backend.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileCleanupService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // Run every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredFiles() {
        System.out.println("Running scheduled file cleanup task at " + LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();
        List<File> expiredFiles = fileRepository.findByExpirationDateBefore(now);

        if (expiredFiles.isEmpty()) {
            System.out.println("No expired files to clean up.");
            return;
        }

        System.out.println("Found " + expiredFiles.size() + " expired files. Deleting...");

        for (File file : expiredFiles) {
            try {
                // Delete physical file
                fileStorageService.delete(file.getStoragePath());

                // Entity deletion will be handled by repository deletion loop or batch
                // Here we can just delete from repository, or collect IDs and delete batch.
                // Simple approach: delete individually to ensure physical delete happens.
                fileRepository.delete(file);

                System.out.println("Deleted expired file: " + file.getOriginalName() + " (ID: " + file.getId() + ")");
            } catch (Exception e) {
                System.err.println("Failed to delete file " + file.getId() + ": " + e.getMessage());
            }
        }
    }
}
