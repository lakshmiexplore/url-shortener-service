# Repository Instructions for GitHub Copilot

## Architecture & Coding Principles
- **Language & Framework:** Java 21 / Spring Boot 3.x.
- **Separation of Concerns:** Strict layer boundaries: Controller -> Service -> Repository. DTOs must always decouple internal entity models from external API contracts.
- **No Business Logic in Controllers:** Controllers handle HTTP parameter validation and response mapping only.
- **Database & Concurrency:** Use Spring Data JPA with PostgreSQL. Ensure write operations are transactional, but avoid holding `@Transactional` locks across external network calls (such as Redis or third-party APIs).
- **Short-Code Generation:** Use Base62 encoding derived from database sequence IDs. Avoid non-deterministic hashing with truncation.

## Testing & Quality Standards
- Every new service must have companion integration tests utilizing Testcontainers for PostgreSQL and Redis.
- Validate URL inputs against Open Redirect and SSRF attacks (e.g., rejecting localhost/internal network addresses).
- Favor explicit, declarative code over dense one-liners.