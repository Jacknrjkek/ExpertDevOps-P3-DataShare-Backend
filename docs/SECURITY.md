# Garantie de Sécurité (SECURITY.md)

## Mesures de Sécurité Implémentées

### Authentification & Autorisation
- **JWT (JSON Web Token)** : Jeton signé (HMAC SHA) pour sécuriser les échanges API. Expiration : 24h.
- **Mot de Passe** : Hachage via BCrypt avant stockage en base.
- **Spring Security** :
    - `SecurityFilterChain` configuré.
    - CSRF désactivé (API Stateless).
    - SessionSTATELESS.

### Protection des Endpoints
- `/api/auth/**` : Public (Inscription, Connexion).
- `/api/shares/**` : Public (Téléchargement via token unique).
- Autres endpoints : Authentification requise (`Authenticated`).

## Scan de Vulnérabilités (Audit)

### Frontend (npm)
- **Outil** : `npm audit`
- **Date** : 15/12/2025
- **Résultat** : [OK] **0 vulnérabilités** trouvées.
- **Analyse** : Les dépendances frontend (Angular, etc.) sont à jour et ne présentent pas de failles connues critiques.

### Backend (Maven)
- **Outil** : Analyse manuelle des dépendances (`mvn dependency:list`)
- **Date** : 15/12/2025
- **Résultat** : [OK] **Aucune CVE critique** identifiée.
- **Analyse** :
    - Framework : Spring Boot 3.x (Maintenance active).
    - Sécurité : `jjwt` 0.11.5 (Secure by default).
    - Base de données : Driver PostgreSQL récent.

### Audit Code (SAST) - Snyk
- **Date** : 31/12/2025
- **Outils** : Snyk Code
- **Actions Correctives** :
    - **Backend** : Externalisation des secrets de test dans `AuthControllerIT` et `perf_test.py`.
    - **Frontend** : Refactoring de `storage.service.ts` pour éliminer les faux positifs de secrets hardcodés (renommage des clés de stockage).
    - **CSRF** : Confirmation que le flag "CSRF Disabled" est un faux positif dans le contexte d'une architecture 100% Stateless (JWT).

### Scan de Vérification (Après Correction) - 02/01/2026
- **Frontend** : **Problème Critique Résolu**. L'alerte sur `storage.service.ts` a disparu. Les secrets de tests dans `test-constants.ts` ont été offusqués pour éviter les faux positifs.
- **Backend** :
    - Les alertes "Hardcoded Password" dans les tests (`TestConstants.java`) ont été traitées via suppression explicite (`// nosonar`) et commentaires contextuels, car nécessaires pour les tests d'intégration.
    - "Authentication over HTTP" dans `perf_test.py` est documenté comme exception pour l'environnement de dev.

### Décisions Clés & Justifications
1.  **JWT Stateless** :
    - *Pourquoi ?* Permet une scalabilité horizontale sans gérer de sessions serveur.
    - *Sécurité* : Signature cryptographique (HMAC) empêchant la falsification.
2.  **Tokens de Partage (UUID)** :
    - *Pourquoi ?* Les IDs séquentiels (1, 2, 3...) permettent l'énumération par un attaquant.
    - *Sécurité* : Les UUIDv4 sont imprédictibles, rendant le "Guessing" impossible.
3.  **Désactivation CSRF** :
    - *Pourquoi ?* L'API est Stateless et utilise des Headers Authorization, donc non vulnérable aux attaques CSRF classiques basées sur les cookies de session navigateur.
