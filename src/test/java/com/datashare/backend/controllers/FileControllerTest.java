package com.datashare.backend.controllers;

import com.datashare.backend.model.AppUser;
import com.datashare.backend.model.File;
import com.datashare.backend.repository.AppUserRepository;
import com.datashare.backend.repository.FileRepository;
import com.datashare.backend.repository.ShareRepository;
import com.datashare.backend.services.FileStorageService;
import com.datashare.backend.payload.response.MessageResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Tests Unitaires pour FileController.
 * Vérifie la logique métier : Validation extensions, Gestion des droits
 * (Delete).
 */
@ExtendWith(MockitoExtension.class)
public class FileControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private ShareRepository shareRepository;

    @InjectMocks
    private FileController fileController;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @BeforeEach
    public void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * Vérifie le refus des fichiers potentiellement malveillants (.exe).
     */
    @Test
    public void testUploadFileWithForbiddenExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.exe",
                "text/plain",
                "content".getBytes());

        // Mock Security Context (Needed because uploadFile checks auth before
        // extension)
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        lenient().when(userDetails.getUsername()).thenReturn("test@test.com");

        AppUser mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");
        lenient().when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));

        ResponseEntity<?> response = fileController.uploadFile(file, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        MessageResponse body = (MessageResponse) response.getBody();
        assertEquals("Extension non supportée : .exe", body.getMessage());
    }

    /**
     * Teste l'upload standard d'un PDF.
     */
    @Test
    public void testUploadFileSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "content".getBytes());

        // Mock Security Context
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        lenient().when(userDetails.getUsername()).thenReturn("test@test.com");

        // Mock User
        AppUser mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));

        // Mock Storage & Repository
        when(fileStorageService.store(any(MultipartFile.class))).thenReturn("stored_path.pdf");
        when(fileRepository.save(any(File.class))).thenAnswer(i -> {
            File f = i.getArgument(0);
            f.setId(100L); // simulate save
            return f;
        });

        ResponseEntity<?> response = fileController.uploadFile(file, 7);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Fichier téléversé avec succès (Authentifié)", body.get("message"));
    }

    /**
     * Teste la récupération de la liste des fichiers.
     */
    @Test
    public void testGetListFiles() {
        AppUser mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");

        File file1 = new File();
        file1.setId(10L);
        file1.setOriginalName("file1.txt");
        file1.setSize(123L);
        file1.setCreatedAt(LocalDateTime.now());
        file1.setExpirationDate(LocalDateTime.now().plusDays(1));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(fileRepository.findByOwnerId(1L)).thenReturn(java.util.List.of(file1));
        when(shareRepository.findByFileId(10L)).thenReturn(Optional.empty());

        ResponseEntity<java.util.List<com.datashare.backend.payload.response.FileResponse>> response = fileController
                .getListFiles();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("file1.txt", response.getBody().get(0).getOriginalName());
    }

    /**
     * Teste suppression valide (Propriétaire = Demandeur).
     */
    @Test
    public void testDeleteFile_Success() {
        AppUser mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");

        File file = new File();
        file.setId(20L);
        file.setOwner(mockUser);
        file.setStoragePath("path/to/file");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(fileRepository.findById(20L)).thenReturn(Optional.of(file));

        ResponseEntity<?> response = fileController.deleteFile(20L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * Teste suppression interdite (Propriétaire != Demandeur).
     */
    @Test
    public void testDeleteFile_Forbidden() {
        AppUser owner = new AppUser();
        owner.setId(1L);

        AppUser hacker = new AppUser();
        hacker.setId(2L);
        hacker.setEmail("hacker@test.com");

        File file = new File();
        file.setId(20L);
        file.setOwner(owner); // Owner is ID 1

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("hacker@test.com");
        when(userRepository.findByEmail("hacker@test.com")).thenReturn(Optional.of(hacker)); // Current user is ID 2
        when(fileRepository.findById(20L)).thenReturn(Optional.of(file));

        ResponseEntity<?> response = fileController.deleteFile(20L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /**
     * Teste l'upload anonyme (US07).
     */
    @Test
    public void testUploadFileAnonymous() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "anonymous.txt",
                "text/plain",
                "content".getBytes());

        when(fileStorageService.store(any(MultipartFile.class))).thenReturn("anon_path.txt");
        when(fileRepository.save(any(File.class))).thenAnswer(i -> {
            File f = i.getArgument(0);
            f.setId(300L);
            return f;
        });

        ResponseEntity<?> response = fileController.uploadFileAnonymous(file, 2);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Fichier téléversé avec succès (Anonyme)", body.get("message"));
    }

    /**
     * Teste l'ajout d'un tag (US08).
     */
    @Test
    public void testAddTag_Success() {
        AppUser mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");

        File file = new File();
        file.setId(50L);
        file.setOwner(mockUser);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(fileRepository.findById(50L)).thenReturn(Optional.of(file));

        Map<String, String> payload = Map.of("tag", "Urgent");
        ResponseEntity<?> response = fileController.addTag(50L, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, file.getTags().size());
        assertEquals("Urgent", file.getTags().iterator().next());
    }

    /**
     * Teste la suppression d'un tag (US08).
     */
    @Test
    public void testRemoveTag_Success() {
        AppUser mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");

        File file = new File();
        file.setId(50L);
        file.setOwner(mockUser);
        file.getTags().add("ToReview");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(fileRepository.findById(50L)).thenReturn(Optional.of(file));

        ResponseEntity<?> response = fileController.removeTag(50L, "ToReview");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, file.getTags().size());
    }
}
