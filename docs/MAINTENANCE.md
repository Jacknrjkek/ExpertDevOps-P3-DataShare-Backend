# Documentation de Maintenance (MAINTENANCE.md)

## Mise à Jour des Dépendances
- **Fréquence recommandeé** : Mensuelle.
- **Risques** : Cassure de la build (Breaking Changes), régressions fonctionnelles.
- **Meilleures Pratiques** :
    - Utiliser le [Semantic Versioning](https://semver.org/) (Major.Minor.Patch).
    - Toujours valider via `mvn test` (Backend) et `npm test` (Frontend) après chaque montée de version.
    - Utiliser `mvn versions:display-dependency-updates` pour identifier les paquets obsolètes.

## Architecture & Authentification

### Architecture Technique (REST API)
L'application suit une architecture en couches classique Spring Boot :
1.  **Controller Layers** (`com.datashare.backend.controllers`) : Points d'entrée de l'API REST.
2.  **Service Layers** (`com.datashare.backend.services`) : Logique métier (ex: Stockage fichiers).
3.  **Repository Layers** (`com.datashare.backend.repository`) : Accès aux données (JPA/Hibernate).
4.  **Security Layers** (`com.datashare.backend.security`) : Gestion de l'authentification et des filtres.

### Authentification JWT
- **Type** : Stateless (Sans session serveur).
- **Flux** : Le client envoie ses credentials -> Serveur retourne un Token JWT signé.
- **Transport** : Header HTTP `Authorization: Bearer <token>`.
- **Validation** : Filtre `AuthTokenFilter` intercepte chaque requête protégée pour valider la signature et l'expiration via `JwtUtils`.

## Validation & Gestion des Erreurs

### Validation des Données
1.  **Frontend (Angular)** :
    - Utilisation de **Reactive Forms** avec `Validators` (Required, Email, MinLength).
    - Feedback visuel immédiat (champs rouges, messages d'erreur).
2.  **Backend (Spring Boot)** :
    - Utilisation de **Bean Validation** (`@Valid`, `@NotBlank`, `@Size`) sur les DTOs (`SignupRequest`).
    - Protection intègre contre les requêtes malformées contournant le front.

### Gestion d'Erreurs
- **GlobalExceptionHandler** (`@ControllerAdvice`) :
    - Capture centralisée des exceptions (`MethodArgumentNotValidException`).
    - Retourne des réponses JSON standardisées (Code HTTP + Message + Liste des champs en erreur).

## Accessibilité (PSH)
L'application doit rester accessible aux Personnes en Situation de Handicap (PSH) :
- **Attributs ARIA** : Utilisation de `aria-label` sur les boutons icônes (ex: Suppression, Copie lien).
- **Contraste** : Vérifier le ratio de contraste textes/fonds (> 4.5:1).
- **Navigation Clavier** : Tous les éléments interactifs doivent être focusables (Tab-index).

## Scripts de Déploiement

### Pré-requis
- Docker & Docker Compose installés sur le serveur cible.

### Installation & Configuration (Exemple `docker-compose.yml`)
```yaml
version: '3.8'
services:
  db:
    image: postgres:15
    environment:
      POSTGRES_DB: datashare
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    volumes:
      - db-data:/var/lib/postgresql/data

  backend:
    build: ./ExpertDevOps-P3-DataShare-Backend
    ports:
      - "8080:8080"
    environment:
      DB_HOST: db
      DB_NAME: datashare
      DB_USERNAME: user
      DB_PASSWORD: password
    depends_on:
      - db

  frontend:
    build: ./ExpertDevOps-P3-DataShare-Frontend
    ports:
      - "4200:80"
```
**Commandes** :
1. `docker compose build` : Construit les images.
2. `docker compose up -d` : Lance l'application en arrière-plan.

