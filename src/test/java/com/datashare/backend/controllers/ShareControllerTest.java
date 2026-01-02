
package com.datashare.backend.controllers;

import com.datashare.backend.model.File;
import com.datashare.backend.model.Share;
import com.datashare.backend.repository.ShareRepository;
import com.datashare.backend.services.FileStorageService;
import com.datashare.backend.TestConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tests Unitaires pour ShareController.
 * Gère l'accès public (non authentifié) aux fichiers via Token.
 */
@ExtendWith(MockitoExtension.class)
public class ShareControllerTest {

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private ShareController shareController;

    /**
     * Récupération des métadonnées (Nom, Taille) avec un token valide.
     */
    @Test
    void getShareMetadata_Success() {
        String token = "valid-token";
        File file = new File();
        file.setOriginalName("test.txt");
        file.setSize(123L);
        Share share = new Share();
        share.setFile(file);

        when(shareRepository.findByUniqueToken(token)).thenReturn(Optional.of(share));

        ResponseEntity<?> response = shareController.getShareMetadata(token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("test.txt", body.get("fileName"));
        assertEquals(123L, body.get("size"));
        assertEquals(false, body.get("isProtected"));
    }

    @Test
    void getShareMetadata_Protected() {
        String token = "protected-token";
        File file = new File();
        file.setOriginalName("secret.txt");
        file.setPasswordHash(TestConstants.TEST_FILE_PASSWORD_HASH); // set password hash
        Share share = new Share();
        share.setFile(file);

        when(shareRepository.findByUniqueToken(token)).thenReturn(Optional.of(share));

        ResponseEntity<?> response = shareController.getShareMetadata(token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(true, body.get("isProtected"));
    }

    @Test
    void getShareMetadata_NotFound() {
        String token = "invalid-token";
        when(shareRepository.findByUniqueToken(token)).thenReturn(Optional.empty());

        ResponseEntity<?> response = shareController.getShareMetadata(token);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * Vérifie le code 410 (Gone) si le fichier associé est expiré.
     */
    @Test
    void getShareMetadata_Expired() {
        String token = "expired-token";
        File file = new File();
        file.setExpirationDate(LocalDateTime.now().minusDays(1));
        Share share = new Share();
        share.setFile(file);

        when(shareRepository.findByUniqueToken(token)).thenReturn(Optional.of(share));

        ResponseEntity<?> response = shareController.getShareMetadata(token);

        assertEquals(HttpStatus.GONE, response.getStatusCode()); // 410 Gone
    }

    /**
     * GET Download: Should return 403 if protected, 200/Blob if not.
     * Note: Current implementation blocks GET for protected files?
     * The controller: downloadFile(GET) calls processDownload(null).
     * If file has passwordHash -> 403 Forbidden.
     */
    @Test
    void downloadFile_GET_Success_NotProtected() throws MalformedURLException {
        String token = "valid-token";
        File file = new File();
        file.setOriginalName("test.txt");
        file.setStoragePath("path/to/test.txt");
        Share share = new Share();
        share.setFile(file);

        // Mock Resource
        Resource resource = mock(Resource.class);

        when(shareRepository.findByUniqueToken(token)).thenReturn(Optional.of(share));
        when(fileStorageService.loadFileAsResource("path/to/test.txt")).thenReturn(resource);

        ResponseEntity<?> response = shareController.downloadFile(token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(shareRepository).save(share); // Check download count increment
        assertEquals(1, share.getDownloadCount());
    }

    @Test
    void downloadFile_GET_Forbidden_Protected() {
        String token = "protected-token";
        File file = new File();
        file.setPasswordHash(TestConstants.TEST_FILE_PASSWORD_HASH);
        Share share = new Share();
        share.setFile(file);

        when(shareRepository.findByUniqueToken(token)).thenReturn(Optional.of(share));

        ResponseEntity<?> response = shareController.downloadFile(token);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /**
     * POST Download: Protected file with correct password.
     */
    @Test
    void downloadFile_POST_Success_Protected() throws MalformedURLException {
        String token = "protected-token";
        String password = TestConstants.TEST_FILE_PASSWORD;
        File file = new File();
        file.setOriginalName("secret.txt");
        file.setStoragePath("path/to/secret.txt");
        file.setPasswordHash(TestConstants.TEST_FILE_PASSWORD_HASH); // Mocked hash
        Share share = new Share();
        share.setFile(file);

        Resource resource = mock(Resource.class);

        when(shareRepository.findByUniqueToken(token)).thenReturn(Optional.of(share));
        when(passwordEncoder.matches(password, TestConstants.TEST_FILE_PASSWORD_HASH)).thenReturn(true);
        when(fileStorageService.loadFileAsResource("path/to/secret.txt")).thenReturn(resource);

        Map<String, String> payload = Map.of("password", password);
        ResponseEntity<?> response = shareController.downloadFileProtected(token, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void downloadFile_POST_Forbidden_WrongPassword() {
        String token = "protected-token";
        String password = TestConstants.TEST_WRONG_PASSWORD; // This is intentionally wrong for the test
        File file = new File();
        file.setPasswordHash(TestConstants.TEST_FILE_PASSWORD_HASH);
        Share share = new Share();
        share.setFile(file);

        when(shareRepository.findByUniqueToken(token)).thenReturn(Optional.of(share));
        when(passwordEncoder.matches(password, TestConstants.TEST_FILE_PASSWORD_HASH)).thenReturn(false);

        Map<String, String> payload = Map.of("password", password);
        ResponseEntity<?> response = shareController.downloadFileProtected(token, payload);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void downloadFile_NotFound() {
        String token = "invalid-token";
        when(shareRepository.findByUniqueToken(token)).thenReturn(Optional.empty());

        ResponseEntity<?> response = shareController.downloadFile(token);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void downloadFile_Expired() {
        String token = "expired-token";
        File file = new File();
        file.setExpirationDate(LocalDateTime.now().minusDays(1));
        Share share = new Share();
        share.setFile(file);

        when(shareRepository.findByUniqueToken(token)).thenReturn(Optional.of(share));

        ResponseEntity<?> response = shareController.downloadFile(token);

        assertEquals(HttpStatus.GONE, response.getStatusCode());
    }
}
