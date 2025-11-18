# QA Summary Example

GitHub Actions now publishes a per-matrix summary using
`scripts/ci_metrics_summary.py`. The snippet below shows what the table looks
like when all gates pass. Update this file (or embed an image) whenever the
format changes.

```
| Metric | Result | Details |
| --- | --- | --- |
| Tests | 30 passing | Total runtime 0.11s |
| Line coverage (JaCoCo) | 100.0% ████████████████████ | 44 / 44 lines covered |
| Mutation score (PITest) | 72.4% ██████████████░░░░░░ | 21 killed, 8 survived out of 29 mutations |
| Dependency-Check | _not run_ | Report missing (probably skipped when `NVD_API_KEY` was not provided). |
```

You can also view the live version in any workflow run by opening the “Summary”
tab and scrolling to the job in question.
