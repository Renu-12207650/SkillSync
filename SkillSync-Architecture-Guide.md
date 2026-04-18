# SkillSync — Architecture

## Overall Architecture Flow

```
Frontend (React)
    │
    ▼
API Gateway (9080) ── CORS + JWT Validation + Routing
    │   lb://
    ▼
Eureka Server (9761) ── Service Discovery
    │
    ├── Auth Service (9081) ─── Login/Register/JWT Issuing
    ├── User Service (9082) ─── User Profiles + Caching
    ├── Mentor Service (9083) ── Mentor Onboarding + Caching
    ├── Skill Service (9084) ── Skill Catalogue + Caching
    ├── Session Service (9085) ── Booking Lifecycle
    │       │
    │       ▼ RabbitMQ (async event)
    │       │
    ├── Notification Service (9088) ── Email via SMTP
    │       │
    │       ▼ Feign (sync call to Auth for email lookup)
    │
    └── Config Server (9888) ── Centralized YAML from GitHub

Observability:
    Logs ──→ Loki ──→ Grafana
    Metrics → Prometheus → Grafana
    Traces ─→ Zipkin ──→ Grafana
```

---

## 1. Why Caching? Why Not Redis?

**Q: Why did you use caching?**

In microservices, services like User, Mentor, and Skill receive repeated reads for the same data (e.g., fetching a mentor profile). Without caching, every request hits MySQL, adding latency and load.

**Implementation:** `ConcurrentMapCacheManager` in `skillsync-common` with `@EnableCaching`. Cache names: `"userProfiles"`, `"mentorProfiles"`, `"skills"`.
- `@Cacheable` on GET operations — returns cached data if available
- `@CacheEvict` on UPDATE/DELETE — invalidates stale cache entries

**Q: Why not Redis?**

`ConcurrentMapCacheManager` is in-memory and per-instance — fine for a demo/single-instance setup. In production with multiple instances, you'd switch to Redis (shared, distributed cache). The code is already cache-abstracted — just swap the `CacheManager` bean to `RedisCacheManager`.

---

## 2. Why JWT and Not Session-Based Auth?

**Q: Why JWT over traditional sessions?**

| Session-Based | JWT (Our Project) |
|---|---|
| Server stores session state | Stateless — token carries identity |
| Doesn't scale in microservices (session stickiness needed) | Any service can validate independently |
| Single-server friendly | Distributed-system friendly |

**Flow:**
1. **Auth Service** issues JWT (HS256, 15min access / 7day refresh) with claims: `userId`, `email`, `role`
2. **API Gateway** (`JwtAuthenticationFilter`, GlobalFilter, order -100) validates the token on every request
3. Gateway injects `X-User-Id`, `X-User-Role`, `X-User-Email` headers into downstream requests
4. **Downstream services** (`ServiceJwtFilter`) read those trusted headers and set `SecurityContext` for `@PreAuthorize`

**Why this two-layer approach?** Gateway does the expensive crypto validation once. Downstream services just read headers — no repeated JWT parsing. Services trust the gateway (internal network).

---

## 3. What is Zipkin and Why Use It?

**Q: What does Zipkin do?**

Zipkin is a **distributed tracing** system. In microservices, a single user request (e.g., "book a session") flows through Gateway → Session Service → RabbitMQ → Notification Service → Feign call to Auth Service. Without tracing, debugging latency or failures across services is nearly impossible.

**Implementation:**
- `micrometer-tracing-bridge-brave` generates trace IDs + span IDs on every request
- `zipkin-reporter-brave` sends spans to Zipkin at `localhost:9411`
- `management.tracing.sampling.probability: 1.0` — 100% of requests are traced
- Zipkin collects traces from RabbitMQ too (`RABBIT_ADDRESSES` in Docker Compose)

**Interview answer:** *"Each request gets a unique trace ID that propagates across all services. We can open Zipkin, search by trace ID, and see the full call chain with timing for each hop — which service was slow, where errors occurred."*

---

## 4. How is Load Balancing Implemented?

**Q: How does load balancing work?**

**Client-side load balancing** via Spring Cloud LoadBalancer + Eureka:

1. All services register with **Eureka Server** (port 9761)
2. API Gateway routes use `lb://skillsync-user-service` — the `lb://` prefix triggers Spring Cloud LoadBalancer
3. LoadBalancer queries Eureka for all instances of `skillsync-user-service`
4. Uses **Round Robin** (default) to distribute requests across instances

**Gateway discovery locator** is also enabled:
```yaml
spring.cloud.gateway.discovery.locator.enabled: true
```
This auto-creates routes for every Eureka-registered service.

**Why client-side, not server-side (Nginx)?** Spring Cloud LoadBalancer is service-aware, integrates with Eureka health checks, and requires no external infrastructure. A failing instance gets removed from the pool automatically.

---

## 5. How Does CORS Work?

**Q: How is CORS configured?**

CORS is configured **only at the API Gateway** level in `GatewayConfig.java` using a reactive `CorsWebFilter`:

- **Allowed Origins:** `*` (all — for development)
- **Allowed Methods:** GET, POST, PUT, DELETE, OPTIONS, PATCH
- **Allowed Headers:** `*`
- **Max Age:** 3600 seconds (browser caches preflight for 1 hour)

**Why only at the Gateway?** The Gateway is the single entry point. Frontend only talks to `localhost:9080`. Individual services are never called directly from browsers, so they don't need CORS.

**Production note:** Replace `*` with specific frontend domain (e.g., `https://skillsync.com`).

---

## 6. Why Grafana? What's the Observability Stack?

**Q: Why use Grafana?**

Grafana is the **single dashboard** that unifies three observability pillars:

| Pillar | Tool | How It Works |
|--------|------|-------------|
| **Logs** | Loki | `loki-logback-appender` in each service pushes logs with labels (`app`, `host`, `level`) → Grafana queries Loki |
| **Metrics** | Prometheus | Scrapes `/actuator/prometheus` from all 9 services every 10s → Grafana visualizes CPU, memory, request rates, error rates |
| **Traces** | Zipkin | Brave sends spans → Zipkin collects → Grafana queries via Zipkin datasource |

**Why not just Zipkin UI or Prometheus UI?** Each tool has its own UI, but Grafana correlates them. You can jump from a spike in error-rate metrics → to the failing logs → to the trace that caused it, all in one place.

**Provisioning:** Datasources (Prometheus, Zipkin, Loki) are auto-provisioned via `grafana/provisioning/datasources/datasources.yml` — no manual setup.

---

## 7. How Does SonarQube Work?

**Q: How is code quality enforced?**

**JaCoCo** (configured in `skillsync-parent/pom.xml`) generates test coverage reports:
1. `prepare-agent` goal injects a Java agent before tests run
2. Tests execute and the agent records which lines were hit
3. `report` goal (verify phase) generates `jacoco.xml`

**SonarQube** reads those reports:
```
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000
```

**Coverage exclusions** (not counted against coverage):
- `**/model/**`, `**/dto/**`, `**/entity/**` — POJOs, no logic
- `**/config/**`, `**/security/**` — infrastructure code
- `**/*Application.java` — just `main()` method
- `**/exception/**` — simple exception classes

**Interview answer:** *"We exclude boilerplate (DTOs, entities, configs) from coverage to focus on testing actual business logic. SonarQube analyzes code smells, bugs, vulnerabilities, and coverage percentage."*

---

## 8. RabbitMQ — Why Async Messaging?

**Q: Why RabbitMQ instead of direct REST calls for notifications?**

| Synchronous (REST) | Asynchronous (RabbitMQ) |
|---|---|
| Session Service waits for Notification to respond | Session Service publishes event and moves on |
| If Notification is down, session booking fails | Message stays in queue, processed when service recovers |
| Tight coupling | Loose coupling |

**Implementation:**
- **Exchange:** `skillsync.events` (TopicExchange, durable)
- **Queue:** `skillsync.notification.queue` (durable — survives RabbitMQ restart)
- **Routing pattern:** `session.#` — matches `session.booked`, `session.accepted`, `session.cancelled`, etc.
- **Publisher:** `SessionEventPublisher` in session-service sends JSON messages via `RabbitTemplate`
- **Consumer:** `NotificationEventConsumer` in notification-service with `@RabbitListener`, switches on `eventType` and sends emails
- **Serialization:** `Jackson2JsonMessageConverter` for automatic JSON ↔ Java object conversion

---

## 9. OpenFeign — Cross-Service Communication

**Q: How do services talk to each other?**

Two patterns are used:

1. **Synchronous (Feign):** Notification service calls Auth service to get user email:
   ```java
   @FeignClient(name = "skillsync-auth-service")
   public interface AuthClient {
       @GetMapping("/auth/internal/email/{userId}")
       String getUserEmail(@PathVariable Long userId);
   }
   ```
   Feign uses Eureka to resolve the service name → load-balanced HTTP call.

2. **Asynchronous (RabbitMQ):** Session → Notification for event-driven communication.

**Rule of thumb:** Use Feign when you need a response right now. Use RabbitMQ for fire-and-forget events.

---

## 10. Security Architecture

**Auth Service SecurityConfig:**
- `SecurityFilterChain` with `DaoAuthenticationProvider` + `CustomUserDetailsService`
- `BCryptPasswordEncoder` for password hashing
- Session policy: `STATELESS`
- CSRF: disabled (stateless JWT-based APIs don't need CSRF)

**Downstream Service SecurityConfig:**
- `ServiceJwtFilter` added before `UsernamePasswordAuthenticationFilter`
- `@EnableMethodSecurity(prePostEnabled = true)` for role-based access via `@PreAuthorize`

**Global Exception Handling:**
- `@RestControllerAdvice` in `skillsync-common` shared across all services
- Handles: validation errors (400), resource not found (404), duplicate entries (409), unauthorized (403), generic (500)

---

## 11. Swagger/OpenAPI

- Library: `springdoc-openapi 2.3.0`
- JWT auth: `@SecurityScheme(name = "bearerAuth", type = HTTP, scheme = "bearer")`
- Server URL: `http://localhost:9080` (forces all calls through API Gateway)
- Gateway aggregates all service docs at `http://localhost:9080/swagger-ui.html`
- Each service exposes `/v3/api-docs`, gateway rewrites paths

---

## 12. Config Server

- Spring Cloud Config Server reads from GitHub: `https://github.com/Renu-12207650/skillsync-configs`
- Services import config via: `spring.config.import: optional:configserver:http://localhost:9888`
- Centralized configuration management — change properties in Git, refresh services without redeployment

---

## Quick Reference — Ports & Services

| Service | Port | Description |
|---------|------|-------------|
| Eureka Server | 9761 | Service Discovery |
| Config Server | 9888 | Centralized Configuration |
| API Gateway | 9080 | Routing + JWT + CORS + Swagger |
| Auth Service | 9081 | Login/Register/JWT |
| User Service | 9082 | User CRUD + Caching |
| Mentor Service | 9083 | Mentor Onboarding + Caching |
| Skill Service | 9084 | Skill Catalogue + Caching |
| Session Service | 9085 | Booking Lifecycle + RabbitMQ Publisher |
| Notification Service | 9088 | Email Notifications + RabbitMQ Consumer |
| RabbitMQ | 5672 / 15672 | Message Broker / Management UI |
| Zipkin | 9411 | Distributed Tracing |
| Prometheus | 9090 | Metrics Collection |
| Loki | 3100 | Log Aggregation |
| Grafana | 3000 | Unified Dashboard |

---

## How to Run

### Option A: Services locally + Infrastructure in Docker
```powershell
# 1. Start infrastructure
cd e:\SprintSkillSync
docker-compose up -d

# 2. Build and start all Java services
cd e:\SprintSkillSync\skillsync-parent
mvn clean install -DskipTests
cd e:\SprintSkillSync
.\start-all.ps1
```

### Option B: Everything in Docker
```powershell
# 1. Build JARs
cd e:\SprintSkillSync\skillsync-parent
mvn clean package -DskipTests

# 2. Build Docker images
cd e:\SprintSkillSync
docker build -t skillsync-eureka-server ./skillsync-eureka-server
docker build -t skillsync-config-server ./skillsync-config-server
docker build -t skillsync-api-gateway ./skillsync-api-gateway
docker build -t skillsync-auth-service ./skillsync-auth-service
docker build -t skillsync-user-service ./skillsync-user-service
docker build -t skillsync-mentor-service ./skillsync-mentor-service
docker build -t skillsync-skill-service ./skillsync-skill-service
docker build -t skillsync-session-service ./skillsync-session-service
docker build -t skillsync-notification-service ./skillsync-notification-service

# 3. Start infrastructure + services
docker-compose up -d
docker run -d --name eureka-server --network sprintskillsync_monitor-net -p 9761:9761 skillsync-eureka-server
# ... (start remaining services as shown in Docker steps)
```
