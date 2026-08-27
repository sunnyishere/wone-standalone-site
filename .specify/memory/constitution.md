<!--
Sync Impact Report
==================
Version change: N/A → 1.0.0 (initial constitution)
Modified principles: N/A (initial creation)
Added sections:
  - Core Principles (5 principles)
  - Technology Stack Constraints
  - Development Workflow
  - Governance
Removed sections: None
Follow-up TODOs: None
-->
# wone-standalone-site Constitution

## Core Principles

### I. Static-First Generation
All pages MUST be pre-rendered as static HTML files served by Nginx. No runtime dynamic
rendering occurs in the production request path. The `/page/**` route generates and caches
static files on first access; subsequent requests are served directly from the filesystem.

**Rationale**: Eliminates backend dependency at serving time, maximizing availability and
performance. Static files are CDN-friendly and require no application server for delivery.

### II. API-Driven Content
All content MUST be sourced from the backend CMS API via `RockwillKnowledgeService`. No
hardcoded content, no embedded data, no local content databases. Content changes in the CMS
propagate through the static generation pipeline only.

**Rationale**: Single source of truth in the CMS. Ensures content consistency across all
domains and languages. Decouples content authoring from site deployment.

### III. Multi-Tenant Domain Isolation
Each domain (primary + `brand.domain-list`) MUST be independently configurable with its own
output directory, Cloudflare zone, Google Tag ID, and language set. Cross-domain leakage
of configuration or generated content is forbidden.

**Rationale**: The service hosts multiple brand sites from a single deployment. Isolation
prevents configuration errors from cascading across tenants.

### IV. API Security by Signature
All `/api/staticize/**` endpoints MUST be protected by MD5 signature verification with a
5-minute timestamp validity window. Requests failing signature validation MUST be rejected
before any business logic executes.

**Rationale**: The trigger and sync APIs mutate the static file output and CDN cache.
Unauthenticated access could lead to cache poisoning or denial of service.

### V. Simplicity & YAGNI
Every feature MUST have a demonstrable, current need. No speculative abstractions, no
"configurability" without a concrete use case, no frameworks or libraries added without
direct justification. Follow the project's simplicity-first guidelines.

**Rationale**: The service has a focused, well-defined scope. Premature complexity increases
maintenance burden and failure surface with no commensurate benefit.

## Technology Stack Constraints

- **Java**: MUST target Java 8. No Java 9+ APIs or language features.
- **Framework**: Spring Boot 2.6.13. No version upgrades without explicit approval and
  regression testing.
- **Template Engine**: Thymeleaf via `SpringTemplateEngine`. All templates MUST reside in
  `src/main/resources/templates/`.
- **Build**: Maven (`mvn clean package`). The output artifact is a single executable JAR.
- **HTTP Client**: `RestTemplate` (primary) and Apache `HttpClient` for external API calls.
- **JSON**: fastjson2 2.0.43 for serialization, Jackson for deserialization. Do not mix
  JSON libraries within a single module.
- **HTML Parsing**: Jsoup 1.10.2 for DOM manipulation.
- **CDN**: Cloudflare API for cache purging. CDN integration MUST be toggleable via
  `cdn.enabled`.

## Development Workflow

- **Build**: `mvn clean package` produces `target/wone-standalone-site.jar`.
- **Run**: `java -jar target/wone-standalone-site.jar` or `mvn spring-boot:run`.
- **Static Output**: Generated files are written to `./static-output/{domain}/` (or the
  configured `brand.static-output` path). Do not commit static output to version control.
- **Templates**: 25 Thymeleaf templates in `src/main/resources/templates/`. Template
  changes MUST be validated against all supported languages and domains.
- **Configuration**: All environment-specific values in `application.yml`. No hardcoded
  URLs, credentials, or secrets in source code.
- **Testing**: JUnit + spring-test. API signature verification, URL path matching, and
  static page generation are the primary test targets.

## Governance

This constitution supersedes all other development practices and conventions for this
project. Any deviation MUST be documented and justified in the relevant pull request.

**Amendment Process**:
1. Propose changes via pull request with a clear rationale.
2. Update the version per semantic versioning rules.
3. Record the amendment in the Sync Impact Report at the top of this file.

**Versioning Policy**:
- MAJOR: Backward-incompatible governance or principle removals/redefinitions.
- MINOR: New principle or section added, or materially expanded guidance.
- PATCH: Clarifications, wording fixes, non-semantic refinements.

**Compliance**: All code reviews MUST verify alignment with the Core Principles. Complexity
introduced without a corresponding principle or documented exception MUST be rejected.

**Version**: 1.0.0 | **Ratified**: 2026-08-03 | **Last Amended**: 2026-08-03