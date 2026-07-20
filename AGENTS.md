# AGENTS.md

## Cursor Cloud specific instructions

This repo is a Maven monorepo for **Krezar Tavern**, a Telegram MMORPG. There is no parent aggregator POM — each module builds independently. Toolchain (Temurin JDK 24, Maven, PostgreSQL 16) is preinstalled in the VM snapshot; the startup update script only refreshes Maven dependencies.

### Modules / services

| Path | What it is | Standard commands |
| --- | --- | --- |
| `game/` | Spring Boot Telegram bot (the core game). Owns the DB schema via Liquibase (`classpath:migrations/main-changelog.xml`, applied automatically on startup). Config: `game/src/main/resources/application.properties`. | Build/test/lint: `mvn -f game/pom.xml clean package` (runs Checkstyle in the `validate` phase + 285 JUnit tests). Run: `java -jar game/target/project-seeker-1.2.jar` |
| `website-backend/` | Spring Boot REST API serving battle data from the same DB (`/api/launched-event/{id}/battle-init` and `/battle-log`). Reads the `event_battle_log` table. Has no tests. | Build: `mvn -f website-backend/pom.xml clean package`. Run: `java -jar website-backend/target/project-seeker-website-backend-1.0.jar` (port 8080) |
| `website/battle-visualizer/` | Static single-file `index.html` battle replay UI. No build step. In prod, Caddy (`deploy/Caddyfile`) serves it and reverse-proxies `/api/*` to the backend. | Serve as static files. Load a battle via `?launchedEventId=ID` (goes through `/api`), or `?init=URL&log=URL` for raw JSON files. |

### Non-obvious caveats

- **PostgreSQL must be started manually** each session: `sudo service postgresql start`. The dev DB is `seeker` with role `dev`/`dev` on `localhost:5432` (matches the app defaults). The DB cluster (schema + any seeded data) persists in the VM snapshot.
- **The DB schema is created only by the `game` module's Liquibase migrations**, which run during Spring context startup. `website-backend` does NOT run migrations, so start (at least once) the game jar against an empty `seeker` DB before using the backend.
- **Running the game bot requires real Telegram credentials.** `TELEGRAM_TOKEN` and `TELEGRAM_USERNAME` have no defaults. With a dummy token the app boots fully (applies migrations, loads game data) and then throws only at the final `botsApplication.registerBot(...)` step (`TelegramApiErrorResponseException`) because it long-polls the real Telegram API. This is expected without a valid token — everything before bot registration is healthy. Set `LOCAL_BOT_ENABLED=true` (+ `LOCAL_BOT_HOST/PORT/SCHEMA`) to point at a local Telegram Bot API server instead of the real one.
- `homyakin.seeker.init-game-data.type=TEST` (default in the `dev` profile) loads test game-data TOML on startup, seeding `event`, items, etc.
- **CORS:** the backend sets no CORS headers, so the visualizer must be served from the same origin as `/api` (as Caddy does in prod). For local browser testing, put a reverse proxy in front that serves `website/` statically and proxies `/api/*` to `localhost:8080`.
- Generate sample battle JSON (for the visualizer / to seed `event_battle_log`) without a running game by executing the `@Disabled` simulator test: `mvn -f game/pom.xml test -Dtest='BattleSimulator#logTest' -Djunit.jupiter.conditions.deactivate='*' -Dcheckstyle.skip=true -Dsurefire.failIfNoSpecifiedTests=false`. Output: `game/target/battle-v4-log/battle-init.json` and `battle-action-log.json`.
