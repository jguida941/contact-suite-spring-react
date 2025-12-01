# Threat Model: Contact Suite Application

**Version**: 1.0
**Date**: 2025-12-01
**Owner**: Justin Guida

## 1. System Overview

The Contact Suite is a multi-tenant contact management application with:
- **Frontend**: React SPA served from same origin
- **Backend**: Spring Boot REST API with JWT authentication
- **Database**: PostgreSQL with per-user data isolation
- **Deployment**: Docker containers behind reverse proxy

### Architecture Diagram

```
┌─────────────────┐     HTTPS      ┌──────────────────┐
│   React SPA     │◄──────────────►│  Spring Boot API │
│  (Browser)      │                │  (Port 8080)     │
└─────────────────┘                └────────┬─────────┘
                                            │
                                   ┌────────▼─────────┐
                                   │   PostgreSQL     │
                                   │   (Port 5432)    │
                                   └──────────────────┘
```

## 2. Trust Boundaries

| Boundary | Description | Controls |
|----------|-------------|----------|
| **Browser ↔ API** | Untrusted network; user-controlled client | TLS, CORS, CSRF tokens, JWT auth |
| **API ↔ Database** | Internal network; trusted connection | Connection pooling, parameterized queries |
| **User ↔ User Data** | Multi-tenant isolation | Per-user FK, service-level filtering |

## 3. Threat Actors

| Actor | Motivation | Capabilities |
|-------|------------|--------------|
| **Unauthenticated Attacker** | Account takeover, data theft | Network access, automated tools |
| **Authenticated User** | Access other users' data | Valid JWT, API knowledge |
| **Malicious Script** | XSS exploitation | JavaScript execution in victim's browser |
| **Internal Attacker** | Data exfiltration | Database access, log access |

## 4. Threats and Mitigations

### 4.1 Authentication Threats

| Threat | STRIDE | Risk | Mitigation | Status |
|--------|--------|------|------------|--------|
| **Credential Stuffing** | Spoofing | High | Rate limiting: 5 login attempts/min per IP | Implemented |
| **Brute Force** | Spoofing | High | Rate limiting + password strength (upper/lower/digit) | Implemented |
| **Session Hijacking** | Spoofing | High | HttpOnly cookies, Secure flag, SameSite=Lax | Implemented |
| **JWT Token Theft** | Spoofing | Medium | HttpOnly cookie storage (not localStorage) | Implemented |
| **Weak Passwords** | Spoofing | Medium | Bean Validation regex requiring mixed characters | Implemented |

### 4.2 Authorization Threats

| Threat | STRIDE | Risk | Mitigation | Status |
|--------|--------|------|------------|--------|
| **Horizontal Privilege Escalation** | Elevation | Critical | Per-user FK on all data; service-level filtering | Implemented |
| **Vertical Privilege Escalation** | Elevation | High | @PreAuthorize annotations; role checks | Implemented |
| **IDOR (Insecure Direct Object Reference)** | Information Disclosure | High | User ID extracted from JWT, not request | Implemented |

### 4.3 Cross-Site Attacks

| Threat | STRIDE | Risk | Mitigation | Status |
|--------|--------|------|------------|--------|
| **Cross-Site Scripting (XSS)** | Tampering | High | CSP: `script-src 'self'`; React auto-escapes | Implemented |
| **Cross-Site Request Forgery (CSRF)** | Tampering | High | CSRF tokens via CookieCsrfTokenRepository | Implemented |
| **Clickjacking** | Tampering | Medium | X-Frame-Options: SAMEORIGIN; CSP frame-ancestors | Implemented |

### 4.4 Injection Threats

| Threat | STRIDE | Risk | Mitigation | Status |
|--------|--------|------|------------|--------|
| **SQL Injection** | Tampering | Critical | JPA parameterized queries; no raw SQL | Implemented |
| **NoSQL Injection** | Tampering | N/A | Not applicable (PostgreSQL only) | N/A |
| **Command Injection** | Tampering | N/A | No shell execution in application | N/A |
| **Log Injection** | Tampering | Low | User agent sanitization; structured logging | Implemented |

### 4.5 Information Disclosure

| Threat | STRIDE | Risk | Mitigation | Status |
|--------|--------|------|------------|--------|
| **PII in Logs** | Information Disclosure | Medium | PiiMaskingConverter masks phone/address | Implemented |
| **Error Message Leakage** | Information Disclosure | Low | Generic error messages; no stack traces in prod | Implemented |
| **User Enumeration** | Information Disclosure | Low | Generic "invalid credentials" message | Implemented |

### 4.6 Availability Threats

| Threat | STRIDE | Risk | Mitigation | Status |
|--------|--------|------|------------|--------|
| **Denial of Service (API)** | Denial of Service | High | Rate limiting: 100 req/min per user | Implemented |
| **Denial of Service (Auth)** | Denial of Service | High | Rate limiting: 5 login, 3 register per min | Implemented |
| **Resource Exhaustion** | Denial of Service | Medium | Connection pooling; request size limits | Implemented |

## 5. CORS Security Model

### Allowed Origins
- Development: `http://localhost:5173`, `http://localhost:8080`
- Production: Configured via `cors.allowed-origins` environment variable

### CORS Policy
```java
allowedOrigins: [configured origins only]
allowedMethods: GET, POST, PUT, DELETE, OPTIONS
allowedHeaders: Authorization, Content-Type, X-Requested-With, X-XSRF-TOKEN
allowCredentials: true
maxAge: 3600 seconds
```

### Trust Assumptions
- CORS origins are configured by trusted operators
- Reverse proxy must NOT forward spoofed Origin headers
- Browser enforcement is trusted (server-side CORS is defense-in-depth)

## 6. JWT Security Model

### Token Properties
| Property | Value | Rationale |
|----------|-------|-----------|
| Algorithm | HMAC-SHA256 | Symmetric; no public key distribution needed |
| Expiration | 24 hours | Balance between security and UX |
| Storage | HttpOnly cookie | Prevents XSS token theft |
| Transmission | Cookie header | Automatic browser inclusion |

### Token Claims
```json
{
  "sub": "username",
  "iat": 1701432000,
  "exp": 1701518400
}
```

### Security Controls
- Secret key: 256-bit minimum, from environment variable
- No refresh tokens (short-lived sessions)
- Logout clears cookie (no server-side blacklist yet)

### Known Limitations
- Token cannot be revoked before expiration
- Logout is client-side only (token remains valid)
- Future: Implement token blacklist for logout/revocation

## 7. Rate Limiting Strategy

### Endpoint Limits
| Endpoint Pattern | Limit | Window | Key | Purpose |
|------------------|-------|--------|-----|---------|
| `/api/auth/login` | 5 | 60s | IP | Prevent credential stuffing |
| `/api/auth/register` | 3 | 60s | IP | Prevent account spam |
| `/api/v1/**` | 100 | 60s | Username | Fair resource allocation |

### Implementation
- Algorithm: Token bucket (bucket4j)
- Storage: In-memory (Caffeine cache)
- Response: HTTP 429 with Retry-After header

### Proxy Considerations
- X-Forwarded-For trusted for IP extraction
- Proxy MUST overwrite (not append) X-Forwarded-For
- Direct internet exposure requires additional validation

## 8. Per-User Data Isolation

### Database Schema
```sql
CREATE TABLE contacts (
    id BIGSERIAL PRIMARY KEY,
    contact_id VARCHAR(10) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    -- other columns
    UNIQUE(contact_id, user_id)
);
```

### Enforcement Layers
1. **Repository**: `findByIdAndUser(id, user)` methods
2. **Service**: Extracts user from SecurityContext
3. **Controller**: No user ID in request (derived from JWT)
4. **Database**: Foreign key constraint

### ADMIN Override
- Query parameter `?all=true` bypasses user filter
- Requires `@PreAuthorize("hasRole('ADMIN')")`
- Audit logged when used

## 9. Security Headers

| Header | Value | Purpose |
|--------|-------|---------|
| Content-Security-Policy | `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'self'` | XSS mitigation |
| X-Content-Type-Options | `nosniff` | MIME sniffing prevention |
| X-Frame-Options | `SAMEORIGIN` | Clickjacking prevention |
| Referrer-Policy | `strict-origin-when-cross-origin` | Referrer leakage prevention |

## 10. Residual Risks

| Risk | Severity | Mitigation Status | Notes |
|------|----------|-------------------|-------|
| JWT token valid until expiration after logout | Medium | Accepted | Stateless design trade-off |
| X-Forwarded-For spoofing behind proxy | Medium | Documented | Proxy must sanitize |
| No account lockout (only rate limiting) | Low | Deferred | Rate limiting sufficient for now |
| No email verification on registration | Low | Deferred | Future enhancement |

## 11. Security Testing

### Automated Testing
- **SAST**: CodeQL analysis in CI
- **SCA**: OWASP Dependency-Check in CI
- **API Fuzzing**: Schemathesis (30,000+ requests)
- **Unit Tests**: 578 tests including security scenarios

### Planned Testing
- **DAST**: OWASP ZAP baseline scan (Phase 5.5)
- **Penetration Testing**: Manual testing before production

## 12. References

- [ADR-0018: Authentication and Authorization Model](../adrs/ADR-0018-authentication-and-authorization-model.md)
- [ADR-0038: Authentication Implementation](../adrs/ADR-0038-authentication-implementation.md)
- [ADR-0039: Phase 5 Security and Observability](../adrs/ADR-0039-phase5-security-observability.md)
- [ADR-0043: HttpOnly Cookie Authentication](../adrs/ADR-0043-httponly-cookie-authentication.md)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [STRIDE Threat Model](https://docs.microsoft.com/en-us/azure/security/develop/threat-modeling-tool-threats)
