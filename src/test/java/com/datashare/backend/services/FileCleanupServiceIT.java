package com.datashare.backend.services;

import com.datashare.backend.model.File;
import com.datashare.backend.repository.FileRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class FileCleanupServiceIT {

    @Autowired
    private FileCleanupService fileCleanupService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    void testCleanupIntegration() throws Exception {
        // 1. Create a file expired yesterday
        File expired = new File();
        expired.setOriginalName("expired_it.txt");
        expired.setStoragePath("expired_it.txt");
        expired.setSize(100L);
        expired.setCreatedAt(LocalDateTime.now().minusDays(8));
        expired.setExpirationDate(LocalDateTime.now().minusDays(1));
        fileRepository.save(expired);

        // 2. Create a valid file
        File valid = new File();
        valid.setOriginalName("valid_it.txt");
        valid.setStoragePath("valid_it.txt");
        valid.setSize(100L);
        valid.setCreatedAt(LocalDateTime.now());
        valid.setExpirationDate(LocalDateTime.now().plusDays(7));
        fileRepository.save(valid);

        // Ensure both exist
        Assertions.assertEquals(2, fileRepository.count());

        // 3. Run Cleanup
        fileCleanupService.cleanupExpiredFiles();

        // 4. Verify
        Assertions.assertEquals(1, fileRepository.count());
        Assertions.assertFalse(fileRepository.findById(expired.getId()).isPresent());
        Assertions.assertTrue(fileRepository.findById(valid.getId()).isPresent());
    }
}
