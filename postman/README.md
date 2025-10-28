# Home Buddy - Postman Collections

This directory contains Postman collections for testing the Home Buddy API.

## Collections

### 1. Auth Service Collection
**Location:** `auth-service/postman/Auth-Service.postman_collection.json`

**Endpoints:**
- Health Check
- User Registration (with and without invite code)
- Login
- Get Current User
- Create Household
- Join Household
- Get Household Info
- Get Household Members
- Leave Household

### 2. Calendar Service Collection
**Location:** `calendar-service/postman/Calendar-Service.postman_collection.json`

**Endpoints:**
- Health Check
- Create Event (simple, chore, recurring)
- List Events (with various filters)
- Get Event by ID
- Update Event
- Delete Event
- Update Event Status (complete, cancel, reopen)

### 3. Environment File
**Location:** `postman/Home-Buddy.postman_environment.json`

Contains environment variables for local development:
- `authServiceUrl`: http://localhost:8081
- `calendarServiceUrl`: http://localhost:8082
- `authToken`: Auto-populated after login
- `userId`, `householdId`, `inviteCode`, `eventId`: Auto-populated during workflow

## Import Instructions

### 1. Import Collections

1. Open Postman
2. Click **Import** button (top left)
3. Select **File** tab
4. Navigate to and select:
   - `auth-service/postman/Auth-Service.postman_collection.json`
   - `calendar-service/postman/Calendar-Service.postman_collection.json`
5. Click **Import**

### 2. Import Environment

1. Click **Import** button
2. Select **File** tab
3. Navigate to and select: `postman/Home-Buddy.postman_environment.json`
4. Click **Import**
5. Select the environment from the environment dropdown (top right)

## Quick Start Guide

### Step 1: Start Services

```bash
# Option A: Development mode (local services)
make dev-up    # Start PostgreSQL
make dev-run   # Run auth-service (in one terminal)
# In another terminal, run calendar-service
cd calendar-service && ./gradlew run

# Option B: Production mode (all services in Docker)
make prod-up
```

### Step 2: Test Auth Service

1. **Register a User**
   - Navigate to: `Auth Service > Authentication > Register User`
   - Click **Send**
   - The `userId` will be automatically saved to environment variables

2. **Login**
   - Navigate to: `Auth Service > Authentication > Login`
   - Click **Send**
   - The `authToken` will be automatically saved and used for all subsequent requests

3. **Create Household**
   - Navigate to: `Auth Service > Household Management > Create Household`
   - Click **Send**
   - The `householdId` and `inviteCode` will be automatically saved

### Step 3: Test Calendar Service

1. **Create an Event**
   - Navigate to: `Calendar Service > Events > Create Event`
   - Click **Send**
   - The `eventId` will be automatically saved

2. **List Events**
   - Navigate to: `Calendar Service > Events > List All Events`
   - Click **Send**

3. **Filter Events**
   - Try various filter endpoints:
     - Filter by Date Range
     - Filter by Type
     - Filter by Status
     - My Events Only

4. **Update Event Status**
   - Navigate to: `Calendar Service > Event Status > Mark Event as Completed`
   - Click **Send**

## Common Workflows

### Workflow 1: Single User Setup

1. Register User
2. Login (saves `authToken`)
3. Create Household (saves `householdId`, `inviteCode`)
4. Create Events
5. Manage Events

### Workflow 2: Multi-User Household

**User 1 (Owner):**
1. Register User
2. Login
3. Create Household (note the `inviteCode`)

**User 2 (Member):**
1. Register User with Invite Code (use invite code from User 1)
   - OR: Register User → Login → Join Household
2. Login
3. Create and view household events

### Workflow 3: Event Management

1. Create various events (meetings, chores, appointments)
2. Assign events to household members
3. Filter and view events
4. Update event details
5. Mark events as completed
6. Delete old events

## Authentication

All endpoints (except registration, login, and health checks) require JWT authentication. The auth token is automatically:
- **Extracted** from the login response
- **Saved** to the `authToken` environment variable
- **Applied** to all subsequent requests via Bearer token authentication

## Environment Variables

The collections use the following environment variables:

| Variable | Description | Auto-populated |
|----------|-------------|----------------|
| `authServiceUrl` | Auth service base URL | No |
| `calendarServiceUrl` | Calendar service base URL | No |
| `authToken` | JWT authentication token | Yes (from login) |
| `userId` | Current user ID | Yes (from register/login) |
| `householdId` | Current household ID | Yes (from create/join household) |
| `inviteCode` | Household invite code | Yes (from create household) |
| `eventId` | Last created event ID | Yes (from create event) |
| `choreEventId` | Last created chore ID | Yes (from create chore) |

## Event Types

- `CHORE` - Household chore or task
- `APPOINTMENT` - Meeting or appointment
- `REMINDER` - Reminder or notification
- `SOCIAL` - Social event or gathering
- `OTHER` - Other event types

## Event Statuses

- `PENDING` - Event is pending/upcoming
- `COMPLETED` - Event has been completed
- `CANCELLED` - Event has been cancelled

## Event Priorities

- `LOW` - Low priority
- `MEDIUM` - Medium priority (default)
- `HIGH` - High priority

## Tips

1. **Auto-save Variables**: The collections include test scripts that automatically save IDs and tokens to environment variables
2. **Sequential Execution**: For best results, run requests in the order they appear in the collection
3. **Check Responses**: Each response includes the created/updated object for verification
4. **Error Handling**: Failed requests will show validation errors or permission issues in the response
5. **Date Formats**: Use ISO 8601 format for dates: `2025-11-01T14:00:00Z`

## Troubleshooting

### "Unauthorized" Error
- Make sure you've logged in and the `authToken` is set
- Check the environment is selected (top right dropdown)
- Token might be expired - log in again

### "User must belong to a household" Error
- Calendar service requires a household
- Create or join a household first via Auth Service

### "Event not found" Error
- Check the `eventId` in environment variables
- Make sure you created an event first
- Events are household-scoped - can only access events from your household

### Services Not Running
- Check services are running: `docker ps` or check terminal windows
- Verify ports: auth-service on 8081, calendar-service on 8082
- Check health endpoints first

## Further Documentation

- API Gateway Documentation: (Coming in Phase 3)
- Authentication Flow: See `auth-service/README.md`
- Calendar Service Features: See `calendar-service/README.md`
- Recurring Events: Coming in Story 6

## Support

For issues or questions:
1. Check the project's main README
2. Review the OpenAPI specifications in `shared/openapi-specs/`
3. Run services with DEBUG logging for detailed error messages
