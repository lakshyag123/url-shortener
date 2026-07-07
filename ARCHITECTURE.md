# URL Shortener — Architecture

This document describes the architecture for a production-quality URL Shortener built with Java 21, Spring Boot 3, PostgreSQL, Flyway, JPA, Thymeleaf, and Bootstrap. It follows layered architecture, SOLID principles, constructor injection, Lombok, and Java records for DTOs.

## Goals & Constraints
- Low-latency redirect path (primary).
- Safe, unique short codes with custom alias support.
- Scalable read-heavy redirect traffic; write-light create flow.
- Eventual-consistent analytics (high throughput).
- Secure and rate-limited create APIs and admin UI.

## Package Structure (root: com.paytm.urlshortener)
- config: AppConfig, SecurityConfig, DataSourceConfig, CacheConfig (Redis)
- controller: RestUrlController, WebController, RedirectController, AdminController
- service: UrlService, RedirectService, ShortCodeService, AnalyticsService, QuotaService
- repository: UrlRepository, ClickEventRepository, ApiKeyRepository
- model: UrlMapping, ClickEvent, ApiClient/User entities
- dto: Java records for requests/responses (CreateShortUrlRequest, CreateShortUrlResponse, UrlStatsResponse)
- mapper: UrlMapper (MapStruct or manual)
- util: Base62Encoder, UrlValidator, IpUtils
- exception: Custom exceptions and GlobalExceptionHandler
- job: SyncCountersJob, ExpireCleanupJob
- security: ApiKeyFilter, CorsConfig

## Core Entities
- UrlMapping
  - id (BIGINT, PK), shortCode (VARCHAR, UNIQUE), longUrl (TEXT), ownerId (UUID), customAlias (BOOLEAN),
    createdAt (TIMESTAMP), expireAt (TIMESTAMP), active (BOOLEAN), hitCount (BIGINT)
- ClickEvent
  - id (BIGINT), urlMappingId (FK), ip, userAgent, referer, country, createdAt

## Repositories
- UrlRepository: findByShortCode(shortCode), existsByShortCode(shortCode)
- ClickEventRepository: save events, query by urlMappingId + time range

## Services & Responsibilities
- ShortCodeService
  - Allocate IDs (DB sequence or distributed ID generator) and encode to Base62.
  - Validate custom aliases and enforce allowed character set.
  - Tradeoff: sequential IDs -> short tokens but enumerable; use Snowflake for unguessable tokens at scale.

- UrlService
  - Validate request, check rate limits, persist UrlMapping.
  - Handle custom alias conflict (catch DB unique constraint -> 409).

- RedirectService
  - Fast-path: check Redis cache for shortCode -> mapping (O(1)).
  - On cache miss: DB lookup by indexed short_code, populate cache.
  - Record click asynchronously via Redis INCR & stream/queue.

- AnalyticsService
  - Consume events from stream, persist ClickEvent or aggregate into analytics store.
  - Support sampling or aggregation for cost control.

## Request Flow
- Create (POST /api/shorten)
  - Controller -> UrlService -> ShortCodeService -> UrlRepository (DB insert) -> return CreateShortUrlResponse
  - Complexity: O(1) amortized; dominated by DB insert.

- Redirect (GET /{shortCode})
  - Controller -> RedirectService -> Redis cache
  - On miss -> UrlRepository (DB) -> populate cache
  - Async: Redis INCR for hitCount + push minimal click payload to Redis stream/Kafka
  - Respond 302 -> longUrl
  - Complexity: O(1) (cache hit); DB lookup indexed O(log N) practically constant.

## Database Schema (core)
- url_mapping
  - id BIGSERIAL PRIMARY KEY
  - short_code VARCHAR(32) NOT NULL UNIQUE
  - long_url TEXT NOT NULL
  - owner_id UUID NULL
  - custom_alias BOOLEAN DEFAULT FALSE
  - created_at TIMESTAMPTZ NOT NULL
  - expire_at TIMESTAMPTZ NULL
  - active BOOLEAN DEFAULT TRUE
  - hit_count BIGINT DEFAULT 0
  - Indexes: UNIQUE(short_code), INDEX(owner_id), PARTIAL INDEX on active

- click_event
  - id BIGSERIAL PRIMARY KEY
  - url_mapping_id BIGINT REFERENCES url_mapping(id)
  - ip INET or VARCHAR(45)
  - user_agent TEXT
  - referer TEXT
  - country VARCHAR(64)
  - created_at TIMESTAMPTZ NOT NULL
  - Index on (url_mapping_id, created_at)

## Flyway
- Migrations to create tables, add constraints and indexes, and initial admin user. Use non-blocking changes for high-availability migrations.

## Sequence Diagram (textual)
- Create:
  Client -> RestUrlController -> UrlService -> ShortCodeService -> UrlRepository -> DB -> UrlService -> RestUrlController -> Client

- Redirect:
  Client -> RedirectController -> RedirectService -> Redis
  alt cache miss
    RedirectService -> UrlRepository -> DB -> RedirectService -> Redis (populate)
  RedirectService -> AsyncRecorder -> Redis stream/Kafka -> BackgroundWorker -> ClickEventRepository -> DB
  RedirectController -> Client (302)

## Scalability & Rationale
- Read scaling:
  - Redis caches hot mappings; app servers stateless for horizontal scaling behind LB.
  - DB read replicas for analytic/secondary reads.

- Write & analytics scaling:
  - Creates handled by primary DB (less frequent).
  - High-volume click events buffered via Redis streams/Kafka and consumed by workers to persist or aggregate.
  - Hit counters incremented in Redis and flushed in batches to Postgres by SyncCountersJob.

- ID generation:
  - DB sequence + Base62 is simple and yields minimal token length; Snowflake recommended at very large scale.

- Partitioning & sharding:
  - Partition click_event by date or url_mapping_id; shard url_mapping by hash prefix if needed.

## Edge Cases
- Custom alias race condition: enforce DB unique constraint and return 409 on violation.
- Expired/disabled links: return 410 Gone.
- Malicious URLs: validate and optionally scan; block internal IPs.
- Very large URLs: enforce max length (e.g., 2048 characters).
- Bot/traffic fraud: detect via heuristics and filter sampling.

## Tradeoffs
- Sequential IDs: short tokens but enumerable.
- Hash-based tokens: non-enumerable but longer and collision-prone.
- Counters in Redis: high throughput with eventual consistency vs authoritative DB counters.

## Time Complexity Summary
- Shorten: O(1) average (DB insert dominates)
- Redirect: O(1) cache hit; DB indexed lookup O(log N) in theory, practically O(1)
- Counter increment: O(1) (Redis INCR)
- Analytics queries: depends on aggregation; pre-aggregated queries faster, raw scans linear in events

## Operational Concerns
- Monitoring (error rate, cache hit ratio, QPS), backups, retention for click_event, rate-limiting, and abuse detection.

---

End of architecture document.
