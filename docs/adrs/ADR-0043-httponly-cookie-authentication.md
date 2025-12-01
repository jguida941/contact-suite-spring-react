# ADR-0043: HttpOnly Cookie-Based Authentication

## Status

Accepted

## Date

2025-12-01

## Context

The CODEBASE_AUDIT.md identified a critical security vulnerability (FRONTEND-SEC-01): JWT tokens were stored in localStorage, making them accessible to any JavaScript code including potential XSS attacks. Additionally, user profile data was also stored in localStorage (FRONTEND-SEC-03).

**Problems with localStorage token storage:**
1. XSS attacks can steal tokens via `localStorage.getItem('auth_token')`
2. Any malicious script (including compromised dependencies) can access tokens
3. Tokens persist even after browser close, increasing theft window
4. No automatic expiration handling by browser

## Decision

Migrate from localStorage-based Bearer token authentication to HttpOnly cookie-based authentication.

### Backend Changes

1. **AuthController.java**:
   - Login/register endpoints now set HttpOnly cookies instead of returning tokens in response body
   - Logout endpoint clears the auth cookie
   - Cookie attributes: `HttpOnly`, `Secure` (in prod), `SameSite=Lax`, `Path=/`

2. **JwtAuthenticationFilter.java**:
   - Now extracts JWT from cookie first, falls back to Authorization header
   - Maintains backward compatibility for API clients using Bearer tokens

3. **application.yml**:
   - Added cookie security configuration
   - `COOKIE_SECURE` env var controls Secure flag (false for dev, true for prod)

### Frontend Changes

1. **api.ts**:
   - Removed localStorage token storage
   - Added `credentials: 'include'` to all fetch calls
   - User info stored in sessionStorage (clears on tab close)
   - `isAuthenticated()` now checks for user session, not token

2. **useProfile.ts**: Changed from localStorage to sessionStorage

3. **SettingsPage.tsx**: Updated to clear sessionStorage

## Cookie Attributes Rationale

| Attribute | Value | Reason |
|-----------|-------|--------|
| HttpOnly | true | Prevents JavaScript access, blocks XSS token theft |
| Secure | true (prod) | Ensures cookie only sent over HTTPS |
| SameSite | Lax | Defense-in-depth (not sole CSRF protection) |
| Path | / | Cookie available to all endpoints |
| Max-Age | JWT expiration | Browser auto-clears expired cookies |

## CSRF Protection

**Important**: Cookie-based authentication requires explicit CSRF protection. SameSite=Lax alone is NOT sufficient.

### Implementation

1. **Backend**: Spring Security's `CookieCsrfTokenRepository.withHttpOnlyFalse()` sets `XSRF-TOKEN` cookie
2. **Frontend**: Reads `XSRF-TOKEN` cookie and sends `X-XSRF-TOKEN` header on mutating requests
3. **Endpoint**: `/api/auth/csrf-token` returns token for initial page load

### CSRF-Exempt Endpoints

Only `/api/auth/login` and `/api/auth/register` are exempt (user has no CSRF token yet).
All other `/api/**` endpoints require valid CSRF token.

## Consequences

### Positive

- **XSS Protection**: Tokens completely inaccessible to JavaScript
- **Automatic Handling**: Browser manages cookie lifecycle
- **Session Scoping**: sessionStorage clears on tab close
- **Backward Compatibility**: API clients can still use Authorization header

### Negative

- **CORS Complexity**: Must use `credentials: 'include'` on all requests
- **CSRF Overhead**: Frontend must read CSRF cookie and send X-XSRF-TOKEN header
- **Debugging Harder**: Can't inspect JWT in DevTools (by design)
- **Cross-Domain Issues**: Cookies don't work across different domains

### Neutral

- **Same Security Model**: Backend JWT validation unchanged
- **Test Updates**: Required test modifications for cookie assertions

## Migration Notes

1. Existing localStorage tokens should be cleared by users logging out
2. Frontend now stores user metadata in sessionStorage, not localStorage
3. Authorization header still supported for programmatic API access

## References

- OWASP JWT Best Practices: https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html
- SameSite Cookie Attribute: https://web.dev/samesite-cookies-explained/
- CODEBASE_AUDIT.md: FRONTEND-SEC-01, FRONTEND-SEC-03
