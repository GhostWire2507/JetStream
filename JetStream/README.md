# JetStream — Unified JavaFX App

Requirements:
- Java 17+
- Maven
- MySQL (for local) OR configure Postgres for cloud in config.properties

To build & run (with JavaFX Maven plugin):
mvn clean javafx:run

Edit config.properties to set db.mode (local or cloud) and DB credentials.

Note:
- Project uses plain Statement-based queries (per PRD). Sanitize inputs where appropriate.
- Extend models and services to match your DB schema.
