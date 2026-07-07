
## UI (Screenshots to add)

This section will include screenshots of the front-end UI showing example usage. 
- Home page with the URL input form and optional alias field.
- Shortened URL result with copy button and analytics panel.

<img width="1917" height="669" alt="Home" src="https://github.com/user-attachments/assets/923eba1a-398f-4290-b39f-0569cf3b10bd" />

<img width="1920" height="888" alt="Create" src="https://github.com/user-attachments/assets/b0ba6d8b-c44a-4267-aa2e-a7022163dc3e" />

<img width="1920" height="1020" alt="Analytics" src="https://github.com/user-attachments/assets/73e580c5-91b4-4ed0-b74d-d5e2e2b62870" />

## Swagger / Backend (Screenshots to add)

This section will include screenshots of the Swagger (OpenAPI) UI demonstrating the API endpoints and example requests/responses.

- POST /shorten example request and response.
- GET /{code} redirect behavior and GET /analytics/{code} response schema.


<img width="1920" height="1020" alt="Swagger-UI" src="https://github.com/user-attachments/assets/82fc3274-9bc5-4c8d-8a3f-0ccfce27132f" />

<img width="1920" height="961" alt="POST_1" src="https://github.com/user-attachments/assets/26968147-b358-4281-8a6f-46b6551dfc1f" />

<img width="1920" height="972" alt="POST_2" src="https://github.com/user-attachments/assets/baddbbb7-6be8-4d66-b8a3-fd703856e81f" />

# URL Shortener

This repository implements a production-oriented URL Shortener using Java 21, Spring Boot 3, PostgreSQL, Flyway, JPA, Thymeleaf and Bootstrap.

## Architecture
- Layered design: controller, service, repository, model, dto, config, util, exception.
- Fast redirect path: Redis caching recommended (not yet implemented) + DB fallback.
- Async analytics: buffer click events (Redis streams/Kafka) and batch persist.
- ID generation: DB sequence -> Base62 encoding for compact short codes.

## Database
- Flyway-managed migrations in `src/main/resources/db/migration`.
- Core table: `url_mapping` (id, original_url, short_code, created_at, click_count).
- Constraints: unique short_code, CHECK on short_code format, original_url length <= 2048.

## API
- POST /shorten
  - Request: { originalUrl, customAlias? }
  - Response: 201 Created, body: { shortCode, shortUrl, createdAt }
  - Errors: 400 (validation), 409 (alias conflict)
- GET /{code}
  - Redirects (302) to the original URL or 404 if not found
- GET /analytics/{code}
  - Returns aggregated stats: totalClicks, createdAt, dailyClicks

OpenAPI docs available via SpringDoc (configure dependency and visit /swagger-ui/index.html).

## Setup
- JDK 21, Maven or Gradle
- PostgreSQL 13+
- Configure `spring.datasource.*` in `application.yml` and run Flyway migrations on startup

## Running
- Build: `mvn -DskipTests package`
- Run: `java -jar target/url-shortener-*.jar` or `mvn spring-boot:run`
- Dev UI: visit `/` for the Bootstrap UI and `/swagger-ui/index.html` for API docs

## Testing
- Unit tests: `mvn test` (includes Mockito + JUnit5 tests)
- Integration tests: use `@DataJpaTest` and real Postgres in CI if needed

## Future improvements
- Add Redis cache for short_code -> original_url lookups
- Buffer click events in Redis/Kafka and persist asynchronously
- Add rate limiting and API keys for abuse prevention
- Add link expiration, owner/tenant support, and custom analytics dashboards
- Hardening: URL scanning, XSS/CSRF protections, input sanitization

## Tradeoffs
- Sequence + Base62: very short codes but enumerable; Snowflake/UUID offers unguessability with longer tokens
- click_count in DB: simple but write-heavy; Redis counters with periodic flush scale better

## Folder structure (partial)
- src/main/java/com/paytm/urlshortener/
  - config, controller, service, service.impl, repository, model, dto, util, exception
- src/main/resources/db/migration (Flyway SQL files)
- src/main/resources/templates (Thymeleaf UI)
- src/test/java (unit & integration tests)

## Design decisions (summary)
- Constructor injection, Lombok builders for entities, Java records for DTOs
- Validation via Jakarta Validation + DB constraints
- Global exception handler maps domain exceptions to HTTP status codes
- Stateless services to enable horizontal scaling

---

For deployment, ensure secrets and DB credentials are supplied via environment or externalized configuration. If you'd like, the next steps can add Redis caching or CI workflow.

