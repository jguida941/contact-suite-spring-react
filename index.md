# CS320 Milestone 1 - Repository Index

Index for easy navigation of the CS320 Milestone 1 codebase.

## Folders

| Path                                                                       | Purpose                                                                                                                      |
|----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| [`src/`](../src)                                                           | Java source tree. <br/>`src/main/java/contactapp` contains application code; <br/>`src/test/java/contactapp` contains tests. |
| [`CI-CD/`](CI-CD/)                                                         | CI/CD design notes and pipeline planning artifacts.                                                                          |
| [`requirements/contact-requirements/`](requirements/contact-requirements/) | Contact assignment requirements (now collocated under `docs/`).                                                             |
| [`requirements/task-requirements/`](requirements/task-requirements/)       | Task assignment requirements (task object/service specs + checklist).                                                       |

## Key Files

| Path                                                                                                      | Description                                                                            |
|-----------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| [`src/main/java/contactapp/Contact.java`](../src/main/java/contactapp/Contact.java)                       | `Contact` domain object with all field validation rules.                               |
| [`src/main/java/contactapp/ContactService.java`](../src/main/java/contactapp/ContactService.java)         | In-memory service that adds, updates, and deletes contacts.                            |
| [`src/main/java/contactapp/Validation.java`](../src/main/java/contactapp/Validation.java)                 | Shared helper with not-blank, length, and 10-digit checks.                             |
| [`src/main/java/contactapp/Main.java`](../src/main/java/contactapp/Main.java)                             | Optional `main` entry point for manual checks/demos.                                   |
| [`src/test/java/contactapp/ContactTest.java`](../src/test/java/contactapp/ContactTest.java)               | JUnit tests covering the `Contact` validation requirements.                            |
| [`src/test/java/contactapp/ContactServiceTest.java`](../src/test/java/contactapp/ContactServiceTest.java) | JUnit tests covering add, delete, and update behavior.                                 |
| [`pom.xml`](../pom.xml)                                                                                   | Maven project file defining dependencies and plugins.                                  |
| [`config/checkstyle/checkstyle.xml`](../config/checkstyle/checkstyle.xml)                                 | Custom Checkstyle rules enforced in CI.                                                |
| [`config/owasp-suppressions.xml`](../config/owasp-suppressions.xml)                                       | Placeholder suppression list for OWASP Dependency-Check.                               |
| [`scripts/ci_metrics_summary.py`](../scripts/ci_metrics_summary.py)                                       | Prints the QA metrics table (tests/coverage/mutations/dependencies) in GitHub Actions. |
| [`docs/logs/backlog.md`](logs/backlog.md)                                                                 | Backlog for reporting and domain enhancements.                                         |
| [`.github/workflows`](../.github/workflows)                                                               | GitHub Actions pipelines (CI, release packaging, CodeQL).                              |

## Requirements & Notes

| Path                                                                                                                         | Description                                  |
|------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|
| [`requirements/contact-requirements/requirements.md`](requirements/contact-requirements/requirements.md)                     | Contact assignment requirements.             |
| [`requirements/contact-requirements/requirements_checklist.md`](requirements/contact-requirements/requirements_checklist.md) | Contact requirements checklist.              |
| [`requirements/task-requirements/requirements.md`](requirements/task-requirements/requirements.md)                           | Task assignment requirements (task object/service). |
| [`requirements/task-requirements/requirements_checklist.md`](requirements/task-requirements/requirements_checklist.md)       | Task requirements checklist.                 |
| [`index.md`](index.md)                                                                                                       | Documentation index and navigation entry.    |
