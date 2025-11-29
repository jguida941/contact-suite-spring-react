# ADR-0022: Custom Error Controller for JSON Error Responses

## Status
Accepted

## Context
API fuzzing with Schemathesis revealed that some error responses return `text/html` instead of `application/json`, causing the `content_type_conformance` check to fail. This occurs when:

1. **Servlet container-level errors**: Malformed requests (garbled path variables, invalid URI encoding) are rejected by Tomcat BEFORE reaching Spring MVC's `@RestControllerAdvice`.
2. **Missing handler errors**: Requests to non-existent paths trigger Tomcat's default 404 handler.
3. **Method not allowed**: Unsupported HTTP methods on valid paths bypass Spring MVC exception handling.

The existing `GlobalExceptionHandler` only catches exceptions thrown from within controllers. Container-level errors fall through to Tomcat's default HTML error page.

### The Problem
```
PUT /api/v1/contacts/%invalid%path% → Tomcat returns HTML error page
Schemathesis checks content_type_conformance → FAILS (expected JSON, got HTML)
```

### Options Considered
1. **Remove `content_type_conformance` check** - Quick fix, but masks a real API quality issue.
2. **Implement `CustomErrorController`** - Ensures ALL errors return JSON, production-grade solution.

## Decision
Implement a custom `ErrorController` that intercepts the `/error` path and returns consistent JSON responses for ALL errors, including those rejected at the servlet container level.

### Implementation

**CustomErrorController.java**
```java
@RestController
public class CustomErrorController implements ErrorController {
    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request) {
        // Extract status code and message from request attributes
        // Return JSON error response with appropriate HTTP status
    }
}
```

**application.yml changes**
```yaml
server:
  error:
    whitelabel:
      enabled: false  # Disable Tomcat's default HTML error page
    include-message: always
```

### Error Message Strategy
The controller provides user-friendly messages based on HTTP status:
| Status | Default Message |
|--------|-----------------|
| 400 | Bad request |
| 404 | Resource not found |
| 405 | Method not allowed |
| 415 | Unsupported media type |
| 500 | Internal server error |

If the container provides a specific error message, it's used instead.

### Testing Strategy
14 unit tests cover:
- JSON content type verification
- Status code mapping (400, 404, 405, 415, 500)
- Default message fallbacks
- Null/blank message handling
- Custom message pass-through
- Uncommon status codes (418 I'm a teapot as edge case)

## Consequences

### Positive
- **API conformance**: All responses return `application/json`, passing Schemathesis `content_type_conformance` check.
- **Consistent client experience**: API consumers always receive parseable JSON, even for malformed requests.
- **Production-grade**: Proper REST API design expects consistent content types.
- **Security**: No HTML error pages that could leak stack traces or server info.

### Negative
- **Additional complexity**: One more controller to maintain.
- **Test overhead**: 14 new tests to maintain (total test count now ~275).

### Neutral
- **No performance impact**: Error paths are rare compared to happy paths.
- **Schemathesis v4+ compatibility**: Workflow updated to remove deprecated options (`--junit-xml`, `--base-url`, `--hypothesis-*`).

## Related ADRs
- ADR-0016: API Style and Contract (defines JSON error format)
- ADR-0021: REST API Implementation (defines GlobalExceptionHandler)
