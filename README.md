# Personal Expense Tracker - Backend API

Production-ready REST API for Personal Expense Tracker built with **Java 21**, **Spring Boot 3.3.5**, **Spring Security 6**, **JJWT (JSON Web Tokens)**, and **MySQL**.

---

## Features

- **Stateless Authentication**: JWT-based authentication with HS256/HS384 signatures and BCrypt password encryption (10 rounds).
- **User-Isolated Expense Management**: Complete CRUD operations for personal expenses strictly isolated by authenticated user ID.
- **Aggregations & Analytics**: User-scoped total expense calculations, counts, and category breakdowns.
- **Filtering & Search**: Category filtering and title search.
- **Robust Exception Handling**: Centralized error handler returning structured, sanitized JSON error responses without exposing internal stack traces.
- **Comprehensive Test Suite**: 37 automated tests across controllers, services, repositories, and Spring Security authorization.
- **Production Containerization**: Multi-stage Docker build utilizing Eclipse Temurin Java 21 JRE for minimal footprint and non-root execution.
- **Render Ready**: Infrastructure-as-code `render.yaml` specification for immediate cloud deployment.

---

## Prerequisites

- **Java Development Kit (JDK)**: Java 21 or higher (e.g. OpenJDK 21, Eclipse Temurin 21)
- **Maven**: Version 3.8+ (or use the included `./mvnw` wrapper)
- **MySQL**: Version 8.0 or higher (or compatible managed MySQL like AWS RDS, Aiven, or PlanetScale)
- **Docker** (optional): For containerized local development or deployment

Verify your Java version:
```bash
java -version
# Expected: openjdk version "21.x.x" ...
```

---

## Getting Started

### 1. Database Setup

Ensure MySQL is running locally and create the database (or let Hibernate auto-create it):
```sql
CREATE DATABASE IF NOT EXISTS expensetracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Environment Variables

The application can be configured via environment variables or a local `.env` file (never commit real secrets):

| Variable | Description | Default (Local Development) | Required in Production |
| :--- | :--- | :--- | :--- |
| `PORT` | HTTP port the server listens on | `8080` | Managed by Render / Cloud |
| `SPRING_DATASOURCE_URL` | JDBC connection URL for MySQL | `jdbc:mysql://localhost:3306/expensetracker?...` | **Yes** |
| `SPRING_DATASOURCE_USERNAME` | MySQL username | `root` | **Yes** |
| `SPRING_DATASOURCE_PASSWORD` | MySQL password | *(empty)* | **Yes** |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate DDL mode | `update` | `update` or `validate` |
| `FRONTEND_URL` | Allowed CORS frontend origin | `http://localhost:5173` | **Yes** (e.g. `https://your-app.vercel.app`) |
| `JWT_SECRET` | 256-bit+ HMAC cryptographic key | *(Built-in development key)* | **Yes** (Strong random 64+ char secret) |
| `JWT_EXPIRATION` | Token validity in milliseconds | `86400000` (24 hours) | Optional |

### 3. Running Locally

Make the Maven wrapper executable and run the Spring Boot application:

```bash
chmod +x mvnw
SPRING_DATASOURCE_USERNAME=root SPRING_DATASOURCE_PASSWORD=your_password ./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

Verify health check:
```bash
curl http://localhost:8080/api/health
# Response: {"status":"UP","database":"CONNECTED","timestamp":"..."}
```

---

## Running Automated Tests

The test suite runs against an in-memory H2 database in MySQL compatibility mode, requiring no external database:

```bash
# Run all tests
./mvnw clean test

# Run a specific test class
./mvnw test -Dtest=SecurityAuthorizationTest
```

Test coverage includes:
- `AuthControllerTest`: Registration validation, duplicate checks, login, and `/me` endpoint.
- `ExpenseControllerTest`: Web layer CRUD endpoints, status codes, and input validation.
- `ExpenseServiceTest`: Business logic, total sums, and category filtering.
- `ExpenseRepositoryTest`: Data JPA user queries and custom repository aggregation queries.
- `SecurityAuthorizationTest`: Anonymous route protection, JWT authentication enforcement, and cross-user data isolation.

---

## API Endpoints Reference

### Public Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Backend service health and database status |
| `POST` | `/api/auth/register` | Register a new user account |
| `POST` | `/api/auth/login` | Authenticate user and receive JWT token |

#### Registration Request Example
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Vasavi Pulluri",
    "email": "vasavi@example.com",
    "password": "securePassword123"
  }'
```

#### Successful Response (`201 Created`):
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "type": "Bearer",
  "id": 1,
  "name": "Vasavi Pulluri",
  "email": "vasavi@example.com"
}
```

---

### Protected Endpoints (Requires `Authorization: Bearer <token>`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/auth/me` | Fetch authenticated user profile |
| `POST` | `/api/auth/logout` | Client token invalidation |
| `GET` | `/api/expenses` | Retrieve all expenses for authenticated user (newest first) |
| `POST` | `/api/expenses` | Create a new expense for authenticated user |
| `GET` | `/api/expenses/{id}` | Get specific expense by ID (user-isolated) |
| `PUT` | `/api/expenses/{id}` | Update existing expense (user-isolated) |
| `DELETE` | `/api/expenses/{id}` | Delete expense (user-isolated) |
| `GET` | `/api/expenses/total` | Get sum of expenses and item count for authenticated user |
| `GET` | `/api/expenses/category/{category}` | Filter expenses by category for authenticated user |

#### Example: Create Expense
```bash
curl -X POST http://localhost:8080/api/expenses \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Grocery Shopping",
    "amount": 1450.75,
    "category": "Food",
    "date": "2026-10-27"
  }'
```

---

## Docker & Containerization

### Build Local Docker Image
```bash
docker build -t personal-expense-tracker-backend .
```

### Run Docker Container
```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/expensetracker?useSSL=false&allowPublicKeyRetrieval=true" \
  -e SPRING_DATASOURCE_USERNAME="root" \
  -e SPRING_DATASOURCE_PASSWORD="your_password" \
  -e FRONTEND_URL="http://localhost:5173" \
  personal-expense-tracker-backend
```

---

## Deployment to Render

### Option A: Using Blueprint (`render.yaml`)
1. Connect your GitHub repository to [Render](https://render.com).
2. Create a new **Blueprint** and point it to `render.yaml`.
3. Provide the secret environment variables in the Render dashboard:
   - `SPRING_DATASOURCE_URL` (MySQL connection string from Render MySQL, Aiven, or PlanetScale)
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `FRONTEND_URL` (e.g. `https://your-frontend.vercel.app`)
   - `JWT_SECRET` (A strong random 64-character secret)

### Option B: Manual Web Service on Render
1. In Render, select **New +** -> **Web Service**.
2. Select repository `personal-expense-tracker-backend`.
3. Choose **Docker** environment.
4. Set **Health Check Path** to `/api/health`.
5. Add the environment variables listed above.
6. Click **Create Web Service**.

---

## License

This project is licensed under the MIT License.
