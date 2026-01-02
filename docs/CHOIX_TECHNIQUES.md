# Choix Technologiques Justifiés

Ce document détaille l'architecture technique retenue pour le projet **DataShare**. Il explique les décisions prises concernant les langages, frameworks, bases de données et outils, en les comparant aux alternatives standards du marché.

## 1. Vue d'ensemble (Tableau Synthétique)

| Élément | Technologie Choisie | Alternatives Considérées | Justification Principale |
| :--- | :--- | :--- | :--- |
| **Langage Backend** | **Java (v21)** | Python, Node.js, C# | Robustesse, typage fort, écosystème mature et performance à long terme. Standard industriel. |
| **Framework Backend** | **Spring Boot (v4)** | Django, Express, NestJS | Facilité de démarrage, injection de dépendances puissante, sécurité intégrée (Spring Security). |
| **Langage Frontend** | **TypeScript** | JavaScript (ES6+), Dart | Typage statique réduisant les erreurs à l'exécution, maintenabilité accrue pour les gros projets. |
| **Framework Frontend** | **Angular (v21)** | React, Vue.js | Structure "batteries-included", architecture modulaire stricte, idéal pour les applications d'entreprise. |
| **Base de Données** | **PostgreSQL (SQL)** | MySQL, MongoDB (NoSQL) | Conformité ACID indispensable pour les données critiques, support JSON avancé, fiabilité éprouvée. |
| **Authentification** | **JWT (Stateless)** | Sessions (Stateful), OAuth2 | Extensibilité (Mobile/Web), absence d'état serveur (scalabilité horizontale), standard sécurisé. |
| **Stockage Fichiers** | **Système de Fichiers (Local)** | S3, Azure Blob Storage | Simplicité de mise en œuvre initiale. Supporte des uploads jusqu'à 1Go. Abstraction via Service permettant une migration Cloud facile. |

---

## 2. Détails des Choix Techniques

### 2.1 Backend : Java & Spring Boot
**Pourquoi Java ?**
Java reste le standard incontournable pour le développement backend d'entreprise. La version 21 (LTS - Long Term Support) offre des améliorations de performance significatives (Virtual Threads via Project Loom) et une syntaxe modernisée, tout en garantissant une stabilité exemplaire.

**Pourquoi Spring Boot ?**
Spring Boot accélère considérablement le développement en éliminant la complexité de configuration ("Convention over Configuration").
*   **Spring Security** : Intégration native pour gérer l'authentification JWT et les rôles utilisateurs.
*   **Spring Data JPA** : Abstraction puissante pour interagir avec la base de données sans écrire de SQL répétitif.
*   **Testabilité** : Support excellent pour les tests unitaires et d'intégration (JUnit, Mockito, MockMvc).

### 2.2 Frontend : TypeScript & Angular
**Pourquoi TypeScript ?**
Contrairement à JavaScript, TypeScript impose un typage strict. Cela permet de détecter les erreurs dès la compilation (et non chez le client) et améliore l'autocomplétion dans les IDE, rendant le code plus "auto-documenté" et facile à refactoriser.

**Pourquoi Angular ?**
Angular est un framework complet. Là où React nécessite l'assemblage de multiples librairies (routeur, gestion d'état, formulaires), Angular fournit une solution tout-en-un cohérente.
*   **Architecture Composants** : Réutilisabilité du code (Header, Footer, FileList).
*   **RxJS** : Gestion puissante des flux asynchrones (Requêtes HTTP, Événements UI).
*   **CLI** : Outils de génération de code et de build optimisés.

### 2.3 Base de Données : PostgreSQL
Le choix s'est porté sur une base de données **Relationnelle (SQL)** car la structure des données est clairement définie et relationnelle (Utilisateurs -> Fichiers -> Partages).
**PostgreSQL** a été préféré à MySQL pour :
*   Sa conformité stricte aux standards SQL.
*   Sa gestion avancée des contraintes d'intégrité.
*   Sa capacité à évoluer vers des types de données complexes si nécessaire.

---

## 3. Outils et Environnement de Développement

### Outils de Développement (Dev Tools)
*   **IDE** : IntelliJ IDEA (Backend) et VS Code (Frontend) sont utilisés pour leur excellent support respectif de Java et TypeScript.
*   **Git** : Gestion de version décentralisée indispensable pour le travail collaboratif.
*   **Maven & NPM** : Gestionnaires de dépendances standards pour assurer la reproductibilité des builds.

### Qualité du Code et Tests
*   **Formatage** : **Prettier** (Frontend) pour garantir un style de code uniforme (imposé via configuration `package.json`).
*   **Tests** :
    *   **JUnit 5 & Mockito** (Backend) : Pour valider la logique métier isolée.
    *   **Jest** (Frontend) : Framework de test rapide et puissant, choisi pour sa simplicité (Zero Config), ses snapshots et sa compatibilité parfaite avec Angular.
    *   **Cypress** (E2E) : Pour valider les parcours utilisateurs critiques (Connexion, Upload).

### Pistes d'Amélioration (CI/CD)
Bien que non implémenté à ce stade, une chaîne **CI/CD** (Intégration et Déploiement Continus) est préconisée pour l'industrialisation :
*   **GitHub Actions** : Pour lancer automatiquement les tests à chaque `push`.
*   **Docker** : Pour conteneuriser l'application (Backend + Frontend + Base de données) et faciliter le déploiement sur n'importe quel serveur.
