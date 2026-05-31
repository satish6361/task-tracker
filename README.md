# Team Task Tracker API

Spring Boot REST API for team-based task tracking with JWT authentication, RBAC, PostgreSQL persistence, Redis-backed task-list caching, and Docker Compose deployment.

## Run With Docker Compose

Prerequisite: Docker Desktop or Docker Engine must be running.

```bash
docker compose up --build
```

The API starts on:

```text
http://localhost:8080/api/v1
```

The React task board starts on:

```text
http://localhost:3000
```

Compose starts four services:

- `app`: Spring Boot API
- `frontend`: React task board served by Nginx
- `postgres`: PostgreSQL database on port `5432`
- `redis`: Redis cache on port `6379`

To stop the stack:

```bash
docker compose down
```

To remove persisted database/cache volumes:

```bash
docker compose down -v
```

## Local Development

If running the app outside Docker, start PostgreSQL and Redis locally, then run:

```bash
./mvnw spring-boot:run
```

Default local configuration:

- PostgreSQL: `jdbc:postgresql://localhost:5432/task_tracker`
- Redis: `localhost:6379`
- API context path: `/api/v1`

## Frontend

The React board lives in `frontend/`.

Run it locally:

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

The board supports login, project loading by organization id, task filtering, column grouping by status, status advancement, analytics counters, and SSE live status notifications.

## Caching Strategy

Redis caches task list responses per assignee. Cache keys include:

- `projectId`
- `assigneeId`
- `page`
- `limit`
- `status`
- `priority`

This prevents one user's task list or filter result from being reused for another request shape.

Cache TTL defaults to 300 seconds and can be changed with:

```text
TASK_LIST_CACHE_TTL_SECONDS
```

## Cache Invalidation

Task-list cache entries are invalidated when task data changes:

- task created
- task updated
- task status changed
- task deleted
- task reassigned

On reassignment, cache entries for both the previous assignee and the new assignee are removed.

## Analytics

Managers and admins can fetch task analytics for a project:

```http
GET /api/v1/projects/{projectId}/tasks/analytics
```

The response includes each project member's overdue task count and average completion time in seconds. Completion time is calculated from task creation time to the moment the task transitions to `DONE`.

## Real-Time Notifications

Authenticated users can subscribe to task status notifications with Server-Sent Events:

```http
GET /api/v1/notifications/stream
```

When a task's status changes, the task service publishes a `TaskStatusChangedEvent`. The notification service listens for that event and pushes a `task-status-changed` SSE event to the assigned user.

Example using curl:

```bash
curl -N -H "Authorization: Bearer <access-token>" http://localhost:8080/api/v1/notifications/stream
```

## Database Design Decision

Tasks belong to projects, and projects belong to organizations. Users also belong to organizations. This keeps organization isolation simple: project and task access can be checked through the user's organization.

Indexes are added on frequently queried task fields:

- `status`
- `assignee_id`
- `due_date`
- `project_id`

`assignee_id` supports member task-list lookups and the Redis cache access pattern. `status` and `due_date` support filtering and future analytics such as overdue-task reports.

## Improvements With More Time

- Add integration tests for login, refresh-token rotation, RBAC, and task status transitions.
- Add OpenAPI/Swagger documentation or a Postman collection.
- Replace `ddl-auto=update` with versioned Flyway or Liquibase migrations.
- Add structured API error responses matching `{ status, code, message }` everywhere.
