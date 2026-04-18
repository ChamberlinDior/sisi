# PNIS Backend – Plateforme Nationale Mutualisée de Renseignement (Gabon)

**Version :** 2.0.0 | **Avril 2026** | **Spring Boot 3.4.1 / Java 21**

---

## Aperçu

Backend unifié de la **Plateforme Nationale d'Information et de Sécurité (PNIS)** du Gabon.
Architecture modulaire sécurisée couvrant l'intégralité du [Cahier des Charges Unifié v1.0](./docs/CDC_PNIS_v1.0.docx) :
cycle complet du renseignement, multi-tenancy, isolation institutionnelle, canal privé chiffré,
audit WORM, workflow, notifications temps réel, cartographie et interface commune inter-services.

---

## Stack technique

| Couche            | Technologie                              |
|-------------------|------------------------------------------|
| Langage / Runtime | Java 21                                  |
| Framework         | Spring Boot 3.4.1 LTS                    |
| Build             | Maven                                    |
| ORM               | Spring Data JPA + Hibernate              |
| Migrations        | Flyway 10                                |
| Base de données   | MySQL 8.x                                |
| Auth              | JWT (JJWT 0.12.6) + MFA + refresh token |
| Temps réel        | WebSocket / STOMP                        |
| Documentation     | OpenAPI 3 / Swagger UI                   |
| Monitoring        | Actuator + Micrometer / Prometheus       |
| Tests             | JUnit 5 + Mockito                        |

---

## Modules implémentés (16/16 CDC)

| # | Module                          | Package                | Couverture CDC |
|---|---------------------------------|------------------------|----------------|
| 1 | Identité, Sécurité, Habilitations | `auth`               | §7.1 ✅        |
| 2 | Multi-tenant & Souveraineté      | `tenant`              | §7.2 ✅        |
| 3 | Collecte & Acquisition           | `collection`          | §7.3 ✅        |
| 4 | Registre des Entités             | `intelligence`        | §7.4 ✅        |
| 5 | Dossiers, Affaires, Missions     | `intelligence`        | §7.5 ✅        |
| 6 | Analyse & Corrélation            | `intelligence`        | §7.6 ✅ (partiel) |
| 7 | Cartographie & SIG               | `geo`                 | §7.7 ✅        |
| 8 | Interface Commune & Alertes      | `alert` + `publication` | §7.8 ✅    |
| 9 | Couloir Privé Institutionnel     | `corridor`            | §7.9 ✅        |
| 10 | Reporting & Production doc.     | `document`            | §7.10 ✅       |
| 11 | Workflow, Tâches, Escalades     | `workflow`            | §7.11 ✅       |
| 12 | Notifications & Événements      | `notification`        | §7.12 ✅       |
| 13 | Audit & Conformité (WORM)       | `audit`               | §7.13 ✅       |
| 14 | Administration & Référentiels   | `admin`               | §7.14 ✅       |
| 15 | Connecteurs & Interopérabilité  | (extensible)          | §7.15 ⏳       |
| 16 | Observabilité & Exploitation    | Actuator + Prometheus | §7.16 ✅       |

---

## Connexions inter-modules (§8 CDC – toutes implémentées)

| Source          | Cible              | Implémentation                                          |
|-----------------|--------------------|---------------------------------------------------------|
| Collecte        | Workflow           | `CollectionService.submit()` → `WorkflowService.onCollectionReceived()` |
| Collecte        | Audit              | `AuditService.log()` à chaque création                  |
| Collecte        | Entités            | `EntityService.tryLinkCollectedDataAsync()` (async)     |
| Dossiers        | Workflow           | `IntelligenceCaseService.create()` → `WorkflowService.onCaseCreated()` |
| Workflow        | Notifications      | `WorkflowService` → `NotificationService.sendWorkflowNotification()` |
| Analyse         | Alertes            | `AlertService.triggerAutoAlert()`                       |
| Cartographie    | Alertes            | `GeoService` → `AlertService.triggerAutoAlert()` (géofencing) |
| Interface commune | Multi-tenant     | `PublicationService.publish()` vérifie `SharingContractRepository` |
| Couloir privé   | Identity & Access  | `CorridorController` : `@PreAuthorize("hasRole('CORRIDOR_USER')")` + audit séparé |
| Administration  | Tous modules       | `ReferenceDataRepository` alimenté par `DataInitializer` |

---

## Démarrage rapide

### Prérequis
- Java 21
- Maven 3.9+
- MySQL 8.x

### Configuration
```bash
cp src/main/resources/application.properties application-local.properties
# Modifier : datasource.url, username, password, jwt.secret
```

### Lancement
```bash
# Compilation
mvn clean package -DskipTests

# Démarrage
java -jar target/pnis-backend-2.0.0.jar

# Ou avec Maven
mvn spring-boot:run
```

### Accès
| Ressource         | URL                                              |
|-------------------|--------------------------------------------------|
| API base          | `http://localhost:8080/api/v1`                   |
| Swagger UI        | `http://localhost:8080/api/v1/docs/swagger`      |
| OpenAPI JSON      | `http://localhost:8080/api/v1/docs/api`          |
| Health check      | `http://localhost:8080/api/v1/actuator/health`   |
| Métriques         | `http://localhost:8080/api/v1/actuator/prometheus` |
| WebSocket         | `ws://localhost:8080/api/v1/ws`                  |

### Compte super-administrateur initial
```
Username : superadmin
Password : Admin@PNIS2026!
Email    : admin@pnis.gov.ga
```
> ⚠️ Le mot de passe **doit être changé** à la première connexion (`must_change_password = true`).

---

## Endpoints principaux

| Domaine         | Prefix         | Méthodes clés                                       |
|-----------------|----------------|-----------------------------------------------------|
| Auth            | `/auth`        | `POST /login`, `POST /mfa/verify`, `POST /refresh`, `POST /logout` |
| Agents          | `/agents`      | CRUD + `/unlock`, `/disable`                        |
| Collecte        | `/collection`  | `POST /`, `PATCH /{id}/validate`, `PATCH /{id}/reject` |
| Entités         | `/entities`    | CRUD + `/aliases`, `/links`, `/merge`               |
| Dossiers        | `/cases`       | CRUD + `/notes`, `/status`, `/close`                |
| Alertes         | `/alerts`      | CRUD + `/publish`, `/acknowledge`, `/lift`          |
| Workflow        | `/workflow`    | `/my-tasks`, `/tasks/{id}/complete`, `/escalate`    |
| Couloir privé   | `/corridor`    | `/send`, `/messages`, `/messages/{id}`, `/revoke`   |
| Cartographie    | `/maps`        | CRUD + `/radius`, `/entity/{id}`, `/case/{id}`      |
| Publications    | `/publications`| CRUD + `/submit`, `/publish`, `/retract`, `/pnis-feed` |
| Documents       | `/documents`   | `POST /` (multipart), `/download`, `/preview`, `/seal` |
| Notifications   | `/notifications` | `/`, `/unread-count`, `/{id}/read`, `/read-all`  |
| Audit           | `/audit`       | Recherche + historique objet                        |
| Administration  | `/admin`       | Référentiels, tenants, unités                       |

---

## Sécurité

- **JWT** : access token (1h) + refresh token (7j) + token MFA temporaire (5min)
- **MFA** : TOTP obligatoire sur rôles sensibles (`ADMIN`, `SUPER_ADMIN`, `ANALYST_SENIOR`, `CORRIDOR_USER`)
- **Lockout** : verrouillage progressif après N tentatives échouées
- **RBAC + ABAC** : rôles + niveau de classification + tenant + mission
- **Audit WORM** : chaînage SHA-256, transaction indépendante, conservation 10 ans
- **Chiffrement canal privé** : chiffrement applicatif par canal (clé dérivée SHA-256)
- **CORS** : origines configurables via `app.cors.allowed-origins`
- **Suppression logique** : aucune suppression physique sur les données métier sensibles

---

## Structure du projet

```
src/main/java/com/pnis/backend/
├── common/          # Socle commun (AbstractBaseEntity, ApiResponse, exceptions, utils)
├── security/        # JWT, filtres, SecurityConfig
├── auth/            # Authentification, utilisateurs, rôles
├── tenant/          # Multi-tenancy, unités, contrats de partage
├── collection/      # Collecte terrain, imports batch
├── intelligence/    # Entités, dossiers, notes chronologiques
├── workflow/        # Tâches, validations, escalades
├── alert/           # Alertes opérationnelles
├── geo/             # Cartographie, SIG, géofencing
├── corridor/        # Canal privé chiffré
├── document/        # Gestion documentaire, SHA-256, antivirus
├── publication/     # Interface commune PNIS inter-services
├── notification/    # Notifications in-app, WebSocket
├── audit/           # Journal WORM, chaînage d'intégrité
├── admin/           # Référentiels, paramétrage central
├── websocket/       # Configuration WebSocket/STOMP
└── config/          # DataInitializer, configurations Spring
```

---

## Phases de réalisation (§16 CDC)

| Phase | Statut | Contenu                                                   |
|-------|--------|-----------------------------------------------------------|
| 1     | ✅     | Socle sécurité, tenants, utilisateurs, rôles, audit, admin |
| 2     | ✅     | Collecte, documents, dossiers, workflows, recherche       |
| 3     | ✅     | Alertes, cartographie, notifications, publications        |
| 4     | ✅     | Interface commune, partage sélectif, couloir privé        |
| 5     | ⏳     | Connecteurs OSINT, sync offline, pentest, optimisation    |

---

## Licence
Propriétaire – Usage gouvernemental exclusif – République Gabonaise.
#   s i s i  
 