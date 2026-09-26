# Prior Authorization System — Agent Handoff

Context for an agent continuing as product manager and reviewer on Gautham's
prior-auth project. Written 2026-09-16, at the end of sprint 3.

---

## 1. The role split, which is the most important thing here

**Gautham writes all application code. He has asked explicitly that AI not write
it.** The project is a deliberate learning exercise in Spring Boot, Kafka and
SAML. Writing the services is the point.

What has been produced by AI so far, at his request, and why it did not cross
that line:

| Produced by AI | Rationale |
|---|---|
| Flyway migrations V1 and V2 | Transcription of an ER diagram the AI drew |
| `seed-data/filter_synthea.py` | Data-filtering tooling, not service code |
| `seed-data/load.py` | Same |
| `seed-data/Dockerfile`, `requirements.txt` | Same |
| `curated/eligible_codes.csv`, `criteria.csv` | Curated content derived from data analysis |
| All Notion documentation and stories | PM work |

**Sprint 4 onward is different.** `IntakeValidationService`, `CriteriaEvaluator`,
the controllers, the Kafka consumers and the SAML configuration are his. Offer
review, not implementation. If he asks for code in those areas, say plainly that
it cuts against the purpose before complying.

Roles: AI is product manager, business analyst and reviewer. He is the engineer.

---

## 2. Where things live

**Repo:** `https://github.com/gauthamramesh3110/priorauth`

GitHub blocks `web_fetch` with ROBOTS_DISALLOWED. Use bash instead:

```bash
git clone --depth 1 https://github.com/gauthamramesh3110/priorauth.git
```

`github.com` is on the sandbox allowlist. Re-clone fresh for every review; do
not trust a stale working copy.

**Notion**, all under the parent page "Prior Authorization System"
(`3d69a769-b54e-81d1-9fc5-f2e9b6eb80a1`, currently a private draft he has not
yet filed into his workspace):

| Page | Contents |
|---|---|
| 01 Phase 1 PRD | Goal, seven features, out of scope |
| 02 Architecture Overview | Topology, cross-org channels, service layering, repo layout, Boot 4 modularisation warning |
| 03 Payer Service | payer_db ERD, REST API, intake rules, rule engine, expiration job |
| 04 Provider Service | provider_db ERD, state machine, REST API, consumers |
| 05 Kafka Contracts | Topic map, envelope, idempotency, DLTs |
| 06 End-to-End Flows | Four sequence diagrams |
| 07 Synthea Data Mapping | Join spine, column mapping, what Synthea lacks |
| 08 Environments and Deployment | SAML, role matrix, compose, CI, Azure, startup ordering |
| 09 Build Sequence and Sprint Plan | Twelve sprints, why payer-first, review protocol |

Diagrams are native Mermaid. SVG versions exist in an outputs folder but were
never committed; Mermaid is the source of truth.

**Sprint Board** database: `622d3f6b-2337-47aa-94dc-33863e8ad580`, data source
`0c9d0b3e-2cdf-4ff5-acc1-2b6b774f5d46`. Stories PA-01 to PA-52 across twelve
sprints. Properties: Story (title), Sprint, Status, Points, Service, Area, Spec,
Blocked by / Blocks (two-way self-relation). Views: Board, By sprint, Awaiting
review.

The Notion connector cannot delete pages. PA-08 was dropped and is tombstoned as
"PA-08 DROPPED, delete this page" at 0 points; he may or may not have removed it.

---

## 3. Review protocol

Nothing reaches Done without passing both reviews.

- **Review** (select): Ready for review, Changes requested, Approved
- **BA passed** (checkbox): every acceptance criterion met
- **Code reviewed** (checkbox): implementation sound, constraints intact
- Status goes to Done only when Review is Approved

Findings go as comments on the story page, not in chat. Chat scrolls away.

**Reviews have teeth and should keep having them.** Five stories have been
failed so far: PA-01 twice, PA-05, PA-06 twice, all for concrete defects. He
pushed back once, on PA-08, and the pushback was correct; the story was dropped.
A review process on a solo project becomes theatre the moment it stops being
able to fail anything.

**Proportionality matters as much as rigour.** PA-01 was approved on its third
round with two trivial items still open, because a third rejection over
gitignore hygiene would have been the process serving itself.

### Verification techniques that have worked

The sandbox has **no Maven Central and no Docker daemon**, so builds and
containers cannot be run. Substitutes that have caught real defects:

- **SQL:** `pip install pglast`, then `pglast.parse_sql()`. It wraps
  libpg_query, the real PostgreSQL parser, so it validates grammar rather than
  eyeballing. Also used to check FK creation order for forward references.
- **Python validators:** import the module with `importlib`, mutate the input,
  and assert each guard raises. Reading a guard is not evidence it fires. This
  caught nothing broken but is what makes approval meaningful.
- **Data:** re-derive closure and counts independently from the committed CSVs
  rather than trusting the script's own assertion, especially when the AI wrote
  that assertion.
- **Git file modes:** `git ls-files -s` catches a missing executable bit, which
  is invisible locally and breaks fresh clones.

**Disclose what could not be verified.** Every review comment ends with what
rests on his word.

---

## 4. Stack and conventions

| | |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.x, Spring Framework 7 |
| Build | Maven multi-module monorepo, root pom for aggregation and versions only |
| Database | PostgreSQL 16, Flyway migrations |
| Ports | provider-service 8081, payer-service 8082, payer-postgres 5434 on the host |
| IDs | UUID everywhere, matching Synthea |
| Timestamps | TIMESTAMPTZ, Instant, UTC |
| API base | `/api/v1` |

**Spring Boot 4 modularisation is a recurring trap.** Boot 4 split
`spring-boot-autoconfigure` into per-technology modules. A library on the
classpath no longer triggers its auto-configuration. This cost an evening with
Flyway: `flyway-core` alone silently ran no migrations, and the only symptom was
the IDE failing to resolve `spring.flyway.enabled`. The fix was
`spring-boot-starter-flyway`. **Expect the same in PA-30 (Kafka) and PA-44
(Spring Security / SAML).** Documented on 02 Architecture Overview.

Spring Security 7 also removed `.and()` chaining, so most SAML tutorials online
target Security 6 and will not compile. Flagged on PA-45.

---

## 5. Architectural constraints code review exists to protect

These erode one commit at a time and none of them fail loudly:

1. **No JDBC connection from one service to the other's database.**
2. **No `common` or `shared` Maven module.** The pressure arrives in sprint 8
   when the Kafka envelope gets written twice. The duplication is the org
   boundary doing its job; two real organizations exchange JSON, not a jar.
3. **Business logic takes plain domain arguments, never Kafka records or HTTP
   requests.** This is what lets sprints 4 to 6 be fully tested before Kafka
   exists and the transport swapped in step 8 without touching the logic.
4. **Idempotency is not optional.** Every consumer checks `processed_event` in
   the same transaction as its state change.

---

## 6. Current state

**Done:** PA-01 through PA-07, PA-09 through PA-12. Eleven stories, 23 points,
three sprints. PA-08 dropped.

**Next:** sprint 4. PA-13 JPA entities, PA-14 `IntakeValidationService`, PA-15
its unit tests. All his.

`payer_db` is fully seeded. Nine tables, all four evaluator branches reachable
with real data.

### The working set

Payer is **Humana**, `d47b3510-2895-3b70-9897-342d681c769d`, chosen because it
sits mid-population. Medicaid at 497 patients would leave too few out-of-network
organizations; Dual Eligible at 22 is too thin to author criteria against.

| | |
|---|---|
| Patients | 234 |
| Organizations / providers | 319 / 319 |
| Coverage spans | 307 |
| Conditions | 2,051 rows, 114 distinct codes, 1,028 unresolved |
| Network | 252 in-network, 67 out-of-network |
| Eligible codes | 20 (10 procedure, 7 medication, 2 imaging, incl. cardioversion) |
| Criteria | 19 (15 auto-approve, 4 always-review, 1 code deliberately without) |

Committed size ~516 KB. `seed-data/raw/` is gitignored; the raw export is 79 MB.

**Synthea generates randomly.** `working-set/` is committed and treated as
source, not reproducible output, because `criteria.csv` references specific
SNOMED codes that must exist on real patients in it.

---

## 7. Decisions made, and why, so they are not relitigated

**`network_participation` is derived, not invented.** Synthea has no provider
network concept. The original plan was an arbitrary holdout. `filter_synthea.py`
instead splits organizations while streaming encounters: in-network if seen on
an encounter this payer covered, out-of-network if it treated the same patients
only under other coverage. This removed one of three hand-authored tables and
makes `OUT_OF_NETWORK` rejections rest on observed behaviour. PA-07 was
rewritten accordingly.

**Ultrasound is on the eligible list with no criterion row, deliberately.** It
is what makes `NO_CRITERION_DEFINED` reachable. `load.py` declares
`CODES_WITHOUT_CRITERION = {("US", "IMAGING")}` and asserts the gap matches
exactly, so both an accidental addition and an accidental removal are caught.
Do not let anyone "fix" the missing criterion.

**`prior_auth_review` has no foreign keys to the reference tables; V2's tables
do.** Not inconsistency. V2 is loaded together from one source, so integrity is
cheap. Review rows arrive from Kafka referencing caches that may lag. The payoff:
reseeding never destroys application state, because `TRUNCATE` of the reference
tables does not touch `prior_auth_review`.

**Enum-ish columns are VARCHAR, not Postgres `CREATE TYPE`.** Native enums need
`ALTER TYPE` to change and cannot practically have values removed.

**Nullability is information.** `decision IS NULL` means not yet decided;
`expired_at IS NULL` is the guard making the expiration sweep idempotent.

**Appeals create a new row, not a status transition.** Parent stays DENIED
permanently. Intake is skipped on appeals because coverage and network were
already proven on the parent, and re-running them could reject an appeal for a
reason the parent passed.

**Seed startup ordering: `seed` depends on `payer-service`, not the reverse.**
Flyway runs inside payer-service on startup and the seed writes into the tables
those migrations create. The intuitive arrangement deadlocks. Documented on 08
Environments and Deployment. An earlier review comment on PA-06 states this
backwards; the page is correct.

**`working-set/` is bind-mounted into the seed container read-only**, so a stale
image is impossible. This is what made PA-08's end-to-end verification script
unnecessary.

**PA-08 was dropped.** It was over-engineered, ~180 lines mostly orchestration
that would have been deleted in PA-49. He pushed back and was right.

---

## 8. Open items he has chosen not to fix

Raised across several reviews, all trivial, all his to schedule or drop. Do not
keep raising them:

- `flyway-core` declared redundantly alongside `spring-boot-starter-flyway`
- `spring-boot-starter-web` should be `spring-boot-starter-webmvc` under Boot 4
- Root `.gitignore` contains only `.idea/`; should add `target/` and `*.iml`
- Commented-out `<module>provider-service</module>` in the root pom, real in PA-25
- `PayerApplicationTests` uses `@SpringBootTest`, so it needs Postgres running.
  Fine now, becomes a real problem in PA-50 when CI has no database. Decide then
  between Testcontainers and a slice test.

---

## 9. Habits worth keeping

**Check whether a story still makes sense before he starts it.** Three have
changed materially once the data was real: PA-07 shrank, PA-08 was deleted,
PA-11 lost a check that V2's unique constraint made structural. PA-10's
acceptance criteria contained a self-contradiction that only surfaced at review.

**Own errors plainly.** Several were AI-side: Spring Boot 3.3.x in the original
spec, missing the Boot 4 Flyway starter across two code reviews, the backwards
compose ordering, an unsatisfiable "empty packages present" criterion, and a
CRLF file. Each was corrected in the docs, not just in chat.

**If the selected payer ever changes**, verify that some patients still have
lapsed coverage and some providers still sit at out-of-network organizations.
Those keep `NOT_COVERED` and `OUT_OF_NETWORK` testable in PA-15. Two queries at
the moment of the change, not a permanent fixture.

**His stated preferences:** no em dashes, tight output over long polished
pieces.
