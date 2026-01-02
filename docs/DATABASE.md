# Documentation Base de Données (PostgreSQL)

## Modèle Logique de Données (MLD)

```mermaid
erDiagram
    APP_USER {
        bigint id PK
        varchar email "Unique"
        varchar password_hash
        timestamp created_at
    }

    FILE_UPLOADS {
        bigint id PK
        varchar original_name
        varchar storage_path "Unique"
        bigint size
        timestamp created_at
        timestamp expiration_date
        varchar password_hash "Nullable"
        bigint owner_id FK "Nullable (Anonymous)"
    }

    SHARE {
        bigint id PK
        varchar unique_token "Unique (UUID)"
        integer download_count
        bigint file_id FK
    }

    FILE_TAGS {
        bigint file_id FK
        varchar tag
    }

    APP_USER ||--o{ FILE_UPLOADS : "owns"
    FILE_UPLOADS ||--o{ SHARE : "has shares"
    FILE_UPLOADS ||--o{ FILE_TAGS : "has tags"
```

## Description des Tables

### 1. `app_user`
Table stockant les utilisateurs enregistrés.
- **id** : Clé primaire auto-incrémentée.
- **email** : Identifiant unique de connexion.
- **password_hash** : Mot de passe haché (BCrypt).
- **created_at** : Date d'inscription.

### 2. `file_uploads`
Table principale stockant les métadonnées des fichiers.
*Note : Renommée depuis `file` pour éviter les conflits avec le mot-clé SQL.*
- **id** : Clé primaire.
- **original_name** : Nom d'origine du fichier (ex: `photo.jpg`).
- **storage_path** : Nom unique sur le disque (UUID ou Timestamp + Nom).
- **size** : Taille en octets.
- **created_at** : Date d'upload.
- **expiration_date** : Date de suppression planifiée (Max 7 jours).
- **password_hash** : (Optionnel) Hash du mot de passe de protection du fichier.
- **owner_id** : (FK) Référence vers `app_user`. `NULL` si upload anonyme.

### 3. `share`
Table gérant les liens de partage publics.
- **id** : Clé primaire.
- **unique_token** : UUID v4 unique utilisé dans l'URL de partage.
- **download_count** : Compteur de téléchargements.
- **file_id** : (FK) Référence vers le fichier partagé.

### 4. `file_tags`
Table de jointure (Collection) pour les tags associés aux fichiers.
- **file_id** : (FK) Référence vers le fichier.
- **tag** : Libellé du tag (max 30 caractères).
