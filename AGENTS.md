# Prior Authorization System: project guidance and state

Consolidated at the user's request on 2026-09-26. This file replaces the original
2026-09-16 handoff. Stable guidance is below; dated snapshots are evidence of past
state, not a substitute for checking current source, tests, or Notion.

## Collaboration

- This is Gautham's Spring Boot, Kafka, and SAML learning project. He normally
  writes application code; the assistant teaches, reviews, and helps with product
  and business analysis. Implement code or tests when he explicitly requests it.
- Keep responses concise and use no em dashes. Explain decisions with concrete
  examples. Review the current local working tree, including uncommitted edits.
- Preserve unrelated user changes. Do not re-clone over the workspace for reviews.
- Report actionable defects, distinguish source review from executed tests, and
  avoid repeatedly raising acknowledged minor cleanup or adding unrelated scope.
- Project notes do not authorize external messages or changes. Post Notion
  comments or update story status only when the current conversation authorizes it.
- A story is Done only after its acceptance criteria and code review pass.
  Notion fields: Review = Approved, BA passed = checked, Code reviewed = checked.
  A passing class review or test run alone does not mark a story Done.

## Architecture and conventions

- Repository: https://github.com/gauthamramesh3110/priorauth
- Maven multi-module project; root aggregates and manages versions. No shared or
  common Maven module across provider and payer services.
- Java 25, Spring Boot 4.1.x, PostgreSQL 16, Flyway. Check the POM for exact versions.
- Payer package: com.priorauth.payer. Existing layout: domain, repository, service,
  config. Match existing organization rather than reorganizing during small tasks.
- Intended ports: provider 8081, payer 8082, payer PostgreSQL host port 5434.
  API base is /api/v1. Confirm current configuration before connecting.
- Services own separate databases. Never connect one service to the other's DB.
- Business logic accepts domain arguments, never HTTP requests or Kafka records.
- Each Kafka consumer must check processed_event in the same transaction as its
  state change. Transport wiring and persistence must be verified independently
  from entity/schema presence.
- IDs follow the schema: reference/request UUIDs, but Coverage, PatientCondition,
  and PriorAuthCriteria use Long for BIGSERIAL. Composite keys use embedded IDs.
- TIMESTAMPTZ maps to Instant; DATE maps to LocalDate. Use UTC deliberately when
  converting timestamps to dates. Inject Clock for rules that depend on today.
- Entities use Lombok getters and no-argument constructors, explicit constructors
  for required creation fields, and no blanket @Data. Reference data is read-only
  at runtime; avoid setters and cascades. Enums map as strings in VARCHAR columns.
- Boot 4 modularized starters: the project uses spring-boot-starter-flyway.
  Check version-appropriate documentation for Kafka and Security/SAML integration.

## Business and data decisions

- Ultrasound (US, IMAGING) is intentionally eligible without a criterion. Preserve
  this gap so NO_CRITERION_DEFINED remains reachable.
- Committed seed-data/working-set is source data. Synthea is randomized and curated
  criteria reference particular condition codes; do not regenerate casually.
- Selected payer is Humana, d47b3510-2895-3b70-9897-342d681c769d. If changing payer,
  verify lapsed coverage and out-of-network cases remain available.
- Network participation is derived from encounters: in-network organizations were
  seen under this payer; out-of-network organizations treated these patients only
  under other coverage. It is not an arbitrary randomized holdout.
- prior_auth_review intentionally has no foreign keys to reference caches. Incoming
  requests can reference data that has not reached the cache. Its appeal link is
  separate. Reference-table reseeding must preserve application review state.
- Null decision means pending; null expired_at supports idempotent expiration.
- Appeals create a new review row; the parent stays DENIED. The intended appeal
  flow skips intake already established by the parent.
- Intake order: eligibility, coverage for current year, provider organization,
  active in-network participation. Stop at the first failure.
- Clinical evaluation order: missing criterion -> NO_CRITERION_DEFINED;
  auto-approval disabled -> ALWAYS_PHYSICIAN_REVIEW; otherwise approve only if a
  matching required condition began before submission and was not resolved before
  submission. Resolution on submission day counts as eligible; onset on submission
  day does not. Otherwise escalate with REQUIRED_CONDITION_ABSENT, not DENIED.
- ClinicalEvaluator is a pure domain evaluator with no repository calls. PA-17's
  orchestration is intended to retrieve inputs and invoke it.

## Current development snapshot: 2026-09-26

These notes combine source inspection and this conversation's recorded results.
Refresh Notion before reporting live board status; no board refresh was performed
as part of this consolidation.

- Historical completion: PA-01 through PA-07 and PA-09 through PA-12 were Done at
  the original handoff. PA-08 was deliberately dropped as unnecessary orchestration.
- PA-13 entities/repositories were completed, and Notion was explicitly updated to
  Done / Approved with both review checkboxes checked at the user's request.
- PA-14 IntakeValidationService is implemented with Clock injection. Its source
  review passed after correcting provider/organization lookup and date boundaries.
- PA-15 IntakeValidationServiceTest exists, including a NOT_COVERED test added
  since an earlier review. Earlier findings included missing real repository
  coverage-year boundary verification. Do not infer full PA-15 completion from
  service mocks; confirm current tests and acceptance criteria.
- PA-16 is implemented as ClinicalEvaluator, with CriteriaEvaluationOutcome and
  EvaluationResult. Latest source review found no remaining correctness defects.
  ClinicalEvaluatorTest ran successfully on 2026-09-26: 19 cases, no failures,
  errors, or skips. It covers escalation precedence, code matching, onset and
  resolution boundaries, string equality, and multiple-condition behavior.
- ClinicalEvaluator tests instantiate the evaluator directly and use real domain
  objects. ReflectionTestUtils is confined to test fixture fields lacking setters
  or constructor parameters. No Spring context or database is needed.
- PA-16 was last fetched as In progress. Its successful tests have not themselves
  authorized or performed a Done/status update.
- Reference repositories are narrow lookup interfaces. PriorAuthReviewRepository
  currently exposes lookup; do not claim end-to-end request persistence based only
  on its entity or migration. Verify actual save/orchestration paths when needed.
- Future interest: reviewer UI, then LLM assistance, RAG over synthetic policy
  documents, and MCP tools. This is a direction discussed, not implemented scope.

## Seeding and verification

- Historical working-set baseline: 234 patients, 319 organizations and providers,
  307 coverage spans, 2,051 conditions, 20 eligible codes, 19 criteria. These are
  source-data expectations, not claims about current database contents.
- A recent runtime investigation found patient_condition empty because the seed
  container contained an old load.py that truncated but did not populate it.
  Bind-mounting working-set does not refresh scripts baked into the image.
- Current seed-data/Dockerfile includes COPY curated/ ./curated/. Rebuild the seed
  image after script or packaging changes. A restart alone is insufficient.
- Ensure Flyway has created tables before seeding. Check current compose ordering;
  do not assume PostgreSQL health alone proves migrations have run.
- load.py prepares inputs before a transaction that reloads reference/config tables.
  Verify its current target list before rerunning; preserve prior_auth_review.
- Useful commands from the repository root:
  - .\mvnw.cmd -pl payer-service -Dtest=ClinicalEvaluatorTest test
  - .\mvnw.cmd -pl payer-service -Dtest=IntakeValidationServiceTest test
  - docker compose build seed
  - docker compose run --rm seed python load.py --dry-run
  - docker compose run --rm seed
- Seed commands are guidance, not standing authorization to change a database.
- Maven and Docker have worked with the required sandbox escalation in this
  environment. Do not carry forward the original handoff's blanket claim that
  they are unavailable. Report actual current limitations.
- Windows PowerShell Set-Content -Encoding utf8 can add a BOM that javac rejects.
  Write Java files as UTF-8 without a BOM.
- PayerApplicationTests uses a Spring context and may require PostgreSQL; focused
  evaluator tests do not validate application startup, persistence, or Kafka.
- Previously acknowledged cleanup includes redundant flyway-core, web starter
  naming, and future CI treatment of the context test. Do not repeatedly block
  focused reviews on these without new impact.

## Notion navigation

Project: https://app.notion.com/p/3d69a769b54e81d19fc5f2e9b6eb80a1
Sprint board: https://app.notion.com/p/622d3f6b233747aa94dc33863e8ad580
Data source: collection://0c9d0b3e-2cdf-4ff5-acc1-2b6b774f5d46

Documentation under the project: 01 Phase 1 PRD; 02 Architecture Overview;
03 Payer Service; 04 Provider Service; 05 Kafka Contracts; 06 End-to-End Flows;
07 Synthea Data Mapping; 08 Environments and Deployment;
09 Build Sequence and Sprint Plan. Native Mermaid diagrams are the source.

Useful page IDs:
- Architecture: 3d69a769b54e81099ee6f45380427aa0
- Payer documentation: 3d69a769b54e818585f2ef885d7160ce
- PA-13: 3d79a769b54e811ab9f9fa204eb2b7a4
- PA-14: 3d79a769b54e813e9ea9fc802828c3a8
- PA-15: 3d79a769b54e8110bd03cfbbf437eb04
- PA-16: 3d79a769b54e81129fc6fef60b85c4f8
- PA-17: 3d79a769b54e81559715c1f00b5a142d
