# ADR-0034: Mapper Pattern for Entity-Domain Separation

**Status**: Accepted
**Date**: 2025-12-01
**Owners**: Justin Guida

**Related**: ADR-0024, ADR-0032

## Context
- We have domain objects (`Contact`, `Task`, `Appointment`) with validation and business logic.
- We have JPA entities (`ContactEntity`, `TaskEntity`, `AppointmentEntity`) for database persistence.
- We needed to decide: use domain objects directly as JPA entities, or keep them separate?

## Decision
Keep domain and JPA entities **separate**, connected by **mapper components**.

## Why Separate?
- JPA needs a no-arg constructor; our domain validation runs in constructor
- Hibernate creates proxy objects that bypass constructors
- Keeping them separate means domain stays pure (no @Entity annotations)

## The Key Insight: Re-Validation on Load
When we load from database, the mapper calls the domain constructor - so validation runs again. Data corruption in DB is caught immediately.

## Interview Explanation
"We keep domain objects and JPA entities separate so the domain stays pure. The mapper calls the domain constructor when loading, so validation always runs - even on database data."