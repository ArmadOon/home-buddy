# API Gateway - User Stories & Tasks

## Epic: Basic API Gateway Setup (Phase 1, Week 2)

**Goal:** Create a basic API Gateway that routes requests to auth-service and validates JWT tokens.

---

## Story 1: Project Setup & Configuration
**As a** developer
**I want** to set up the API Gateway project with Micronaut
**So that** I have a foundation to build routing and security features

### Tasks:
- [ ] Create `api-gateway` module in the project
- [ ] Add `build.gradle.kts` with Micronaut dependencies
- [ ] Configure `application.yml` with gateway settings (port 8080)
- [ ] Add gateway to `settings.gradle.kts`
- [ ] Create basic `Application.kt` main class
- [ ] Verify gateway starts successfully on port 8080

### Acceptance Criteria:
- Gateway starts without errors on port 8080
- Can access health endpoint at `/health`
- Gradle build succeeds

### Technical Notes:
```yaml
# Required dependencies:
- micronaut-http-client
- micronaut-security-jwt
- micronaut-management
```

---

## Story 2: Configure HTTP Client for Auth Service
**As a** gateway
**I want** to configure an HTTP client to communicate with auth-service
**So that** I can proxy requests to the authentication service

### Tasks:
- [ ] Add HTTP client configuration in `application.yml`
- [ ] Configure auth-service base URL (http://localhost:8081)
- [ ] Create `AuthServiceClient` interface with declarative client
- [ ] Add health check endpoint that verifies auth-service connectivity
- [ ] Test client can reach auth-service

### Acceptance Criteria:
- HTTP client successfully connects to auth-service
- Can call auth-service health endpoint through client
- Connection timeout and retry logic configured

### Technical Notes:
```kotlin
@Client("\${auth-service.url}")
interface AuthServiceClient {
    @Get("/health")
    fun healthCheck(): HttpResponse<String>
}
```

---

## Story 3: Implement Auth Service Routing
**As a** client
**I want** to access auth endpoints through the gateway
**So that** I don't need to know the internal auth-service URL

### Tasks:
- [ ] Create `AuthGatewayController` to handle `/auth/*` routes
- [ ] Implement POST `/auth/register` proxy
- [ ] Implement POST `/auth/login` proxy
- [ ] Implement POST `/auth/household/create` proxy
- [ ] Implement POST `/auth/household/join` proxy
- [ ] Implement GET `/auth/household/validate-invite/{code}` proxy
- [ ] Implement GET `/auth/household/info` proxy
- [ ] Forward all headers and body to auth-service
- [ ] Return auth-service responses unchanged

### Acceptance Criteria:
- All auth endpoints accessible through gateway at `/auth/*`
- Request/response bodies pass through correctly
- HTTP status codes preserved
- Registration and login work through gateway

### Technical Notes:
```kotlin
@Controller("/auth")
class AuthGatewayController(
    private val authClient: AuthServiceClient
) {
    @Post("/register")
    fun register(@Body request: RegisterRequest): HttpResponse<RegisterResponse> {
        return authClient.register(request)
    }
}
```

---

## Story 4: JWT Token Validation
**As a** gateway
**I want** to validate JWT tokens before forwarding requests
**So that** only authenticated users can access protected endpoints

### Tasks:
- [ ] Configure JWT validation settings in `application.yml`
- [ ] Use same JWT secret as auth-service
- [ ] Configure security rules for public vs protected endpoints
- [ ] Mark `/auth/register` and `/auth/login` as public
- [ ] Mark `/auth/household/*` endpoints as requiring authentication
- [ ] Extract user info from JWT and add to request headers
- [ ] Test token validation with valid JWT
- [ ] Test rejection of invalid/expired JWT

### Acceptance Criteria:
- Public endpoints accessible without token
- Protected endpoints require valid JWT
- Invalid tokens return 401 Unauthorized
- Valid tokens are successfully validated
- User info extracted from JWT

### Technical Notes:
```yaml
micronaut:
  security:
    token:
      jwt:
        signatures:
          secret:
            generator:
              secret: ${JWT_SECRET}
```

---

## Story 5: CORS Configuration
**As a** frontend developer
**I want** CORS properly configured on the gateway
**So that** my browser-based app can make requests

### Tasks:
- [ ] Configure CORS in `application.yml`
- [ ] Allow origins: `http://localhost:3000`, `http://localhost:8080`
- [ ] Allow methods: GET, POST, PUT, DELETE, OPTIONS
- [ ] Allow headers: Authorization, Content-Type
- [ ] Allow credentials: true
- [ ] Test preflight OPTIONS requests
- [ ] Test actual requests with CORS headers

### Acceptance Criteria:
- Browser can make requests from localhost:3000
- Preflight requests succeed
- CORS headers present in responses
- Credentials can be sent

### Technical Notes:
```yaml
micronaut:
  server:
    cors:
      enabled: true
      configurations:
        web:
          allowedOrigins:
            - http://localhost:3000
```

---

## Story 6: Error Handling & Logging
**As a** developer
**I want** proper error handling and logging
**So that** I can debug issues and provide good error messages

### Tasks:
- [ ] Configure logging in `logback.xml`
- [ ] Log all incoming requests (method, path, headers)
- [ ] Log all outgoing requests to backend services
- [ ] Create global exception handler for common errors
- [ ] Handle auth-service connection errors gracefully
- [ ] Handle JWT validation errors with clear messages
- [ ] Return appropriate HTTP status codes
- [ ] Test error scenarios

### Acceptance Criteria:
- All requests logged with timestamp and details
- Connection errors return 503 Service Unavailable
- Auth errors return 401 Unauthorized
- Validation errors return 400 Bad Request
- Error responses include helpful messages

### Technical Notes:
```kotlin
@Singleton
@Requires(classes = [ExceptionHandler::class])
class GlobalExceptionHandler : ExceptionHandler<Exception, HttpResponse<*>> {
    override fun handle(request: HttpRequest<*>, exception: Exception): HttpResponse<*> {
        // Handle different exception types
    }
}
```

---

## Story 7: Health Check & Monitoring
**As an** operator
**I want** health check endpoints
**So that** I can monitor gateway and backend service status

### Tasks:
- [ ] Enable Micronaut management endpoints
- [ ] Create `/health` endpoint
- [ ] Add health indicator for auth-service connectivity
- [ ] Add `/health/liveness` endpoint
- [ ] Add `/health/readiness` endpoint
- [ ] Test health checks return correct status
- [ ] Document health check endpoints

### Acceptance Criteria:
- `/health` returns 200 when all services healthy
- `/health` returns 503 when auth-service unreachable
- Health checks include auth-service status
- Liveness and readiness probes work

---

## Story 8: Docker Integration
**As a** DevOps engineer
**I want** the gateway dockerized
**So that** it can run in containers alongside other services

### Tasks:
- [ ] Create `Dockerfile` for api-gateway
- [ ] Use multi-stage build for optimization
- [ ] Configure environment variables for service URLs
- [ ] Update `docker-compose.yml` to include gateway
- [ ] Configure gateway to use Docker service names
- [ ] Set up Docker network for inter-service communication
- [ ] Test gateway in Docker with auth-service

### Acceptance Criteria:
- Gateway builds successfully in Docker
- Can communicate with auth-service in Docker network
- Environment variables properly configured
- `docker-compose up` starts all services

### Technical Notes:
```yaml
# docker-compose.yml
services:
  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    environment:
      - AUTH_SERVICE_URL=http://auth-service:8081
      - JWT_SECRET=${JWT_SECRET}
```

---

## Story 9: Integration Testing
**As a** developer
**I want** integration tests for the gateway
**So that** I can ensure routing and security work correctly

### Tasks:
- [ ] Create test configuration with test auth-service URL
- [ ] Write test for public endpoint access (register/login)
- [ ] Write test for protected endpoint without token (401)
- [ ] Write test for protected endpoint with valid token
- [ ] Write test for invalid token rejection
- [ ] Write test for CORS headers
- [ ] Write test for error handling
- [ ] Ensure all tests pass

### Acceptance Criteria:
- All integration tests pass
- Test coverage >80%
- Tests can run in CI/CD pipeline
- Tests use embedded test server

---

## Story 10: Documentation & Deployment
**As a** developer
**I want** comprehensive documentation
**So that** team members can understand and maintain the gateway

### Tasks:
- [ ] Create `README.md` with setup instructions
- [ ] Document environment variables
- [ ] Document API endpoints and routing rules
- [ ] Document authentication flow
- [ ] Add examples of cURL requests
- [ ] Document Docker deployment
- [ ] Create Makefile commands for gateway
- [ ] Test documentation by following setup guide

### Acceptance Criteria:
- README includes all necessary information
- New developer can set up gateway using docs
- All environment variables documented
- Examples are accurate and working

---

## Definition of Done

A story is considered **DONE** when:
- [ ] All tasks completed
- [ ] Code reviewed
- [ ] Tests written and passing
- [ ] Documentation updated
- [ ] Works locally
- [ ] Works in Docker
- [ ] No critical bugs

---

## Technical Architecture

```
┌─────────────────┐
│   Frontend      │
│  (Port 3000)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  API Gateway    │◄──── JWT Validation
│  (Port 8080)    │◄──── CORS
│                 │◄──── Request Logging
└────────┬────────┘
         │
         ├─────────────────┐
         │                 │
         ▼                 ▼
┌─────────────────┐  ┌─────────────────┐
│  Auth Service   │  │ Future Services │
│  (Port 8081)    │  │ (Calendar, etc) │
└─────────────────┘  └─────────────────┘
```

---

## Environment Variables

```bash
# Required
PORT=8080
AUTH_SERVICE_URL=http://localhost:8081
JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long

# Optional
LOG_LEVEL=INFO
CORS_ALLOWED_ORIGINS=http://localhost:3000
REQUEST_TIMEOUT=5000
```

---

## Success Metrics

### Week 2 Goal Completion:
- [ ] Gateway routing works for all auth endpoints
- [ ] JWT validation functional
- [ ] CORS configured
- [ ] Can register user through gateway
- [ ] Can login and get JWT through gateway
- [ ] Can access protected endpoints with JWT
- [ ] Gateway runs in Docker
- [ ] Basic tests passing

### Ready for Phase 2 when:
- All user stories completed
- Integration tests passing
- Documentation complete
- Can demo end-to-end flow through gateway
