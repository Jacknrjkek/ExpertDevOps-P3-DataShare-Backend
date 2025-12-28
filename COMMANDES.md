# Pense-bête / Cheat Sheet

Toutes les commandes utiles pour piloter le projet DataShare (Backend & Frontend).

## Base de Données (PostgreSQL)

**Lancer la BDD avec Docker :**
```bash
docker run --name datashare-db \
  -e POSTGRES_PASSWORD=root \
  -e POSTGRES_DB=datashare \
  -p 5432:5432 \
  -d postgres
```
*Vérifier si elle tourne : `docker ps`*

---

## Backend (Spring Boot)

*Dossier : `ExpertDevOps-P3-DataShare-Backend/`*

### Lancer l'application
```bash
# Avec variables d'environnement explicites
export DB_USERNAME=postgres
export DB_PASSWORD=root
./mvnw spring-boot:run
```

### Tests
```bash
# Lancer tous les tests (Unitaires + Intégration)
./mvnw test

# Lancer uniquement les tests unitaires
./mvnw test -Dtest=*Test

# Lancer uniquement les tests d'intégration (IT)
./mvnw test -Dtest=*IT
```

### Générer le rapport de couverture (Jacoco)
```bash
./mvnw clean test jacoco:report
# Le rapport sera dans : target/site/jacoco/index.html
```

---

## Frontend (Angular)

*Dossier : `ExpertDevOps-P3-DataShare-Frontend/`*

### Installation & Lancement
```bash
# Installer les dépendances
npm install

# Lancer le serveur de dev (http://localhost:4200)
npm start
```

### Tests
```bash
# Tests Unitaires (Jest)
npm test
# Avec couverture
npm run test:coverage

# Tests E2E (Cypress) - Backend et Frontend DOIVENT être lancés
npx cypress open  # Interface graphique
npx cypress run   # Mode headless (console)
```

### Qualité & Lint
```bash
# Vérifier le formatage (Prettier)
npx prettier --check .
```
