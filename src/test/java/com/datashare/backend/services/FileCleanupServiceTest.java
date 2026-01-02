package com.datashare.backend.services;

import com.datashare.backend.model.File;
import com.datashare.backend.repository.FileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileCleanupServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileCleanupService fileCleanupService;

    @Test
    void cleanupExpiredFiles_ShouldDeleteExpiredFiles() {
        // Arrange
        File expiredFile1 = new File();
        expiredFile1.setId(1L);
        expiredFile1.setOriginalName("expired1.txt");
        expiredFile1.setStoragePath("path/to/expired1.txt");
        expiredFile1.setExpirationDate(LocalDateTime.now().minusDays(1));

        File expiredFile2 = new File();
        expiredFile2.setId(2L);
        expiredFile2.setOriginalName("expired2.txt");
        expiredFile2.setStoragePath("path/to/expired2.txt");
        expiredFile2.setExpirationDate(LocalDateTime.now().minusHours(1));

        when(fileRepository.findByExpirationDateBefore(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredFile1, expiredFile2));

        // Act
        fileCleanupService.cleanupExpiredFiles();

        // Assert
        verify(fileStorageService).delete("path/to/expired1.txt");
        verify(fileStorageService).delete("path/to/expired2.txt");
        verify(fileRepository).delete(expiredFile1);
        verify(fileRepository).delete(expiredFile2);
    }

    @Test
    void cleanupExpiredFiles_ShouldDoNothingIfNoFilesExpired() {
        // Arrange
        when(fileRepository.findByExpirationDateBefore(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        fileCleanupService.cleanupExpiredFiles();

        // Assert
        verify(fileStorageService, never()).delete(anyString());
        verify(fileRepository, never()).delete(any(File.class));
    }

    @Test
    void cleanupExpiredFiles_ShouldContinueIfOneDeleteFails() {
        // Arrange
        File expiredFile1 = new File();
        expiredFile1.setId(1L);
        expiredFile1.setStoragePath("path/fail");

        File expiredFile2 = new File();
        expiredFile2.setId(2L);
        expiredFile2.setStoragePath("path/success");

        when(fileRepository.findByExpirationDateBefore(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredFile1, expiredFile2));

        doThrow(new RuntimeException("Delete failed")).when(fileStorageService).delete("path/fail");

        // Act
        fileCleanupService.cleanupExpiredFiles();

        // Assert
        verify(fileStorageService).delete("path/fail"); // Attempted
        verify(fileStorageService).delete("path/success"); // Attempted 2nd
        verify(fileRepository, times(1)).delete(expiredFile2); // Only successful one deleted from DB
        verify(fileRepository, never()).delete(expiredFile1); // Failed one NOT deleted from DB (logic in service catch
                                                              // block)
    }
}
