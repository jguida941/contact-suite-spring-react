# Backlog

Tracking potential improvements beyond the CS320 Milestone 1 requirements.
Move items here once they no longer fit inside the README so the project
landing page stays focused.

## Reporting & Observability
- Attach richer visual artifacts (HTML dashboards or charts) that combine the
  JaCoCo, PITest, Dependency-Check, and Codecov data already posted in the job
  summary.
- Continue updating `docs/CI-CD/ci_cd_plan.md` as workflow enhancements land.

## Domain Enhancements
- Add constructor-level JavaDoc to `Contact` noting that invalid data triggers
  `IllegalArgumentException`.
- Implement `toString`, `equals`, and `hashCode` on `Contact` so logging and
  collection usage become easier once `ContactService` stores contacts in maps
  or sets.
