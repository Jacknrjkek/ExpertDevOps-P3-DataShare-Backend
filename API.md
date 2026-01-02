# Documentation API REST (DataShare)

Cette documentation décrit les endpoints disponibles pour interagir avec le Backend DataShare.
L'API est sécurisée via JWT (Bearer Token) pour les opérations privées.

## 1. Authentification (`/api/auth`)
Endpoints publics pour la gestion des comptes.

| Méthode | URL | Description | Accès | Payload (Corps) |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Création d'un nouveau compte utilisateur. | Public | `{"email": "...", "password": "..."}` |
| `POST` | `/api/auth/login` | Connexion et récupération du Token JWT. | Public | `{"email": "...", "password": "..."}` |

## 2. Gestion des Fichiers (`/api/files`)
Endpoints pour manipuler les fichiers. Nécessite le header `Authorization: Bearer <token>` (sauf upload anonyme).

| Méthode | URL | Description | Accès | Paramètres / Body |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/files` | Liste les fichiers de l'utilisateur connecté. | **Privé** | Aucun |
| `POST` | `/api/files/upload` | Téléversement d'un fichier lié au compte. | **Privé** | Multipart: `file`, `expirationTime` (int), `password` (string) |
| `POST` | `/api/files/upload/anonymous` | Téléversement sans compte (US07). | Public | Multipart: `file`, `expirationTime`, `password` |
| `DELETE` | `/api/files/{id}` | Suppression définitive d'un fichier. | **Privé** | Path: `id` |
| `POST` | `/api/files/{id}/tags` | Ajout d'un tag à un fichier. | **Privé** | JSON: `{"tag": "..."}` |
| `DELETE` | `/api/files/{id}/tags/{tag}` | Suppression d'un tag. | **Privé** | Path: `id`, `tag` |

## 3. Partage et Téléchargement (`/api/share` & `/api/download`)
Endpoints publics pour accéder au contenu partagé via le Token Unique (UUID).

| Méthode | URL | Description | Accès | Paramètres |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/share/{token}` | Récupère les métadonnées (nom, taille, expiration). | Public | Path: `token` |
| `GET` | `/api/download/{token}` | Télécharge le fichier (si non protégé). | Public | Path: `token` |
| `POST` | `/api/download/{token}` | Télécharge un fichier protégé par mot de passe. | Public | JSON: `{"password": "..."}` |

## Codes HTTP Retournés
- **200 OK** : Succès.
- **201 Created** : Ressource créée (Register, Upload).
- **401 Unauthorized** : Token JWT manquant ou invalide.
- **403 Forbidden** : Mot de passe incorrect (Fichier protégé) ou Accès interdit.
- **404 Not Found** : Fichier introuvable.
- **410 Gone** : Lien expiré.
