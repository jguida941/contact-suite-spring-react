package contactapp.api;

import contactapp.api.dto.AuthResponse;
import contactapp.api.dto.ErrorResponse;
import contactapp.api.dto.LoginRequest;
import contactapp.api.dto.RegisterRequest;
import contactapp.api.exception.DuplicateResourceException;
import contactapp.security.JwtService;
import contactapp.security.Role;
import contactapp.security.User;
import contactapp.security.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication operations (login and registration).
 *
 * <p>Provides endpoints at {@code /api/auth} for user authentication per ADR-0018 and ADR-0043.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>POST /api/auth/login - Authenticate user, set HttpOnly cookie, return user info (200 OK)</li>
 *   <li>POST /api/auth/register - Register new user, set HttpOnly cookie, return user info (201 Created)</li>
 *   <li>POST /api/auth/logout - Clear auth cookie (204 No Content)</li>
 * </ul>
 *
 * <h2>Security</h2>
 * <p>These endpoints are publicly accessible (no JWT required). Authentication uses
 * HttpOnly, Secure, SameSite=Lax cookies to protect against XSS token theft.
 * The browser automatically includes the cookie on subsequent requests.
 *
 * @see LoginRequest
 * @see RegisterRequest
 * @see AuthResponse
 * @see JwtService
 */
@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "User authentication and registration")
public class AuthController {

    /** Cookie name for JWT token storage. */
    public static final String AUTH_COOKIE_NAME = "auth_token";

    private static final String COOKIE_PATH = "/";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${server.servlet.session.cookie.secure:true}")
    private boolean secureCookie;

    /**
     * Creates a new AuthController with the required dependencies.
     *
     * @param authenticationManager Spring Security authentication manager
     * @param userRepository repository for user persistence
     * @param passwordEncoder encoder for password hashing (BCrypt)
     * @param jwtService service for JWT token generation
     */
    public AuthController(
            final AuthenticationManager authenticationManager,
            final UserRepository userRepository,
            final PasswordEncoder passwordEncoder,
            final JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Exposes the CSRF token so the SPA can include it in state-changing requests.
     *
     * <p>{@link org.springframework.security.web.csrf.CookieCsrfTokenRepository CookieCsrfTokenRepository}
     * issues the {@code XSRF-TOKEN} cookie with
     * {@code HttpOnly=false}, and this endpoint returns the same token for double-submit
     * protection. Clients should call this endpoint before POST/PUT/PATCH/DELETE requests.
     *
     * @param csrfToken Spring Security CSRF token injected by the filter chain
     * @return a JSON payload containing the CSRF token value
     */
    @Operation(summary = "Fetch CSRF token for SPA clients")
    @ApiResponse(responseCode = "200", description = "CSRF token returned")
    @GetMapping("/csrf-token")
    public Map<String, String> csrfToken(final CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    /**
     * Authenticates a user and sets an HttpOnly cookie with the JWT token.
     *
     * @param request the login credentials
     * @param response HTTP response for setting the auth cookie
     * @return authentication response with user info (token in cookie, not body)
     * @throws BadCredentialsException if credentials are invalid
     */
    @Operation(summary = "Authenticate user and set auth cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse login(
            @Valid @RequestBody final LoginRequest request,
            final HttpServletResponse response) {
        // Authenticate via Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        // Load user and generate token
        final User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        final String token = jwtService.generateToken(user);

        // Set HttpOnly cookie with JWT
        setAuthCookie(response, token, jwtService.getExpirationTime());

        return new AuthResponse(
                null, // Token is in HttpOnly cookie, not response body
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                jwtService.getExpirationTime()
        );
    }

    /**
     * Registers a new user and sets an HttpOnly cookie with the JWT token.
     *
     * @param request the registration data
     * @param response HTTP response for setting the auth cookie
     * @return authentication response with user info (token in cookie, not body)
     * @throws DuplicateResourceException if username or email already exists
     */
    @Operation(summary = "Register new user and set auth cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registration successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody final RegisterRequest request,
            final HttpServletResponse response) {
        // Check for existing username or email (use generic message to prevent enumeration)
        if (userRepository.existsByUsername(request.username())
                || userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Username or email already exists");
        }

        // Create new user with hashed password
        final User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );

        // Save with race condition handling - database constraint catches concurrent inserts
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request inserted the same username/email
            throw new DuplicateResourceException("Username or email already exists");
        }

        // Generate token for immediate login
        final String token = jwtService.generateToken(user);

        // Set HttpOnly cookie with JWT
        setAuthCookie(response, token, jwtService.getExpirationTime());

        return new AuthResponse(
                null, // Token is in HttpOnly cookie, not response body
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                jwtService.getExpirationTime()
        );
    }

    /**
     * Logs out the current user by clearing the auth cookie.
     *
     * <p>Clears the HttpOnly auth cookie and provides a hook for:
     * <ul>
     *   <li>Future token blacklisting implementation</li>
     *   <li>Audit logging of logout events</li>
     * </ul>
     *
     * @param response HTTP response for clearing the auth cookie
     */
    @Operation(summary = "Logout current user")
    @ApiResponse(responseCode = "204", description = "Logout successful")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(final HttpServletResponse response) {
        // Clear the auth cookie by setting it with maxAge=0
        clearAuthCookie(response);
        // Future: Add token to blacklist if implementing token revocation
    }

    /**
     * Sets the authentication cookie with HttpOnly, Secure, and SameSite attributes.
     *
     * @param response the HTTP response
     * @param token the JWT token
     * @param expirationMs token expiration time in milliseconds
     */
    private void setAuthCookie(final HttpServletResponse response, final String token, final long expirationMs) {
        final ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secureCookie)
                .path(COOKIE_PATH)
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(expirationMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Clears the authentication cookie.
     *
     * @param response the HTTP response
     */
    private void clearAuthCookie(final HttpServletResponse response) {
        final ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path(COOKIE_PATH)
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
