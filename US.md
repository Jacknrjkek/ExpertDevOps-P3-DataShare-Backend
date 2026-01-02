```
# User Stories (Spécifications Fonctionnelles)

Ce document liste l'ensemble des exigences fonctionnelles du projet **DataShare**, référencées dans le plan de tests (`TESTING.md`) et les commentaires du code.

| ID | Titre | Description | Critères d'Acceptation |
| :--- | :--- | :--- | :--- |
| **US01** | **Upload de ficher** | En tant qu'utilisateur connecté, je veux pouvoir téléverser un fichier pour le sauvegarder.<br>![Upload File](screenshots/upload.png)<br>*Interface d'ajout de fichiers* | - Fichier stocké sur le serveur.<br>- Entrée créée en BDD.<br>- Refus des extensions interdites (ex: .exe). |
| **US02** | **Partage de fichier** | En tant qu'utilisateur, je veux générer un lien unique pour partager un fichier avec des tiers. | - Génération d'un token unique.<br>- URL publique accessible sans authentification. |
| **US03** | **Inscription** | En tant que visiteur, je veux créer un compte pour accéder au service. | - Email unique requis.<br>- Mot de passe sécurisé.<br>- Redirection vers login après succès. |
| **US04** | **Connexion** | En tant qu'utilisateur inscrit, je veux me connecter pour accéder à mes fichiers. | - Authentification par Email/Mot de passe.<br>- Réception d'un jeton JWT.<br>- Accès aux routes protégées. |
| **US05** | **Historique des fichiers** | En tant qu'utilisateur connecté, je veux voir la liste de mes fichiers téléversés. | - Affichage du nom, taille et date.<br>- Ne montre QUE les fichiers de l'utilisateur courant. |
| **US06** | **Suppression** | En tant que propriétaire, je veux supprimer un fichier pour libérer de l'espace. | - Suppression physique du fichier.<br>- Suppression des métadonnées en BDD.<br>- Impossible de supprimer le fichier d'un autre. |
| **US07** | **Upload Anonyme** | En tant que visiteur, je veux téléverser un fichier sans créer de compte. | - Accessible publiquement.<br>- Génération de lien de partage.<br>- Fichier non lié à un utilisateur. |
| **US08** | **Tagging** | En tant qu'utilisateur connecté, je veux ajouter des tags pour organiser mes fichiers. | - Ajout de tags (texte libre, max 30 chars).<br>- Suppression de tags.<br>- Affichage des tags dans la liste. |
| **US09** | **Protection par Mot de Passe** | En tant qu'expéditeur, je veux protéger l'accès à mon fichier par un mot de passe. | - Mot de passe défini à l'upload.<br>- Hashage du mot de passe en base.<br>- Mot de passe requis pour le téléchargement. |
| **US10** | **Expiration Automatique** | En tant qu'expéditeur, je veux que mon fichier soit supprimé automatiquement après une durée définie. | - Durée configurable (par défaut 1 jour, max 7 jours).<br>- Suppression automatique du fichier et des données après expiration. |

---

## Matrice de Traçabilité

| User Story | Backend Controller | Frontend Component | Tests Associés |
| :--- | :--- | :--- | :--- |
| **US01** | `FileController.uploadFile` | `FileUploadComponent` | `FileControllerTest`, `file-flow.cy.ts` |
| **US02** | `ShareController` | `ShareViewComponent` | `ShareControllerTest`, `file-flow.cy.ts` |
| **US03** | `AuthController.register` | `RegisterComponent` | `AuthControllerTest`, `auth-flow.cy.ts` |
| **US04** | `AuthController.login` | `LoginComponent` | `AuthControllerTest`, `auth-flow.cy.ts` |
| **US05** | `FileController.listFiles` | `FileListComponent` | `FileListSpec`, `file-flow.cy.ts` |
| **US06** | `FileController.deleteFile` | `FileListComponent` | `FileControllerTest`, `file-flow.cy.ts` |
| **US07** | `FileController.uploadFileAnonymous` | `AnonymousUploadComponent` | `FileControllerTest`, `full-features.cy.ts` |
| **US08** | `FileController.addTag` | `FileListComponent` | `FileControllerTest`, `full-features.cy.ts` |
| **US09** | `FileController.uploadFile`, `ShareController` | `FileUpload`, `ShareView` | `ShareControllerTest`, `full-features.cy.ts` |
| **US10** | `FileCleanupService` | `FileUpload`, `AnonymousUpload` | `FileCleanupServiceTest`, `FileCleanupServiceIT` |
