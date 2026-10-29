# Gymio – Fitness Booking App

Gymio is a secure fitness booking REST API built with **Java and Spring Boot**. It allows customers to browse fitness classes, manage bookings, purchase memberships, and receive notifications, while trainers and administrators manage classes and other platform resources.

## Main Features

- User registration, login, and email verification
- JWT authentication and role-based authorization
- Customer, Trainer, and Admin roles
- Password reset, password change, and verification email resending
- User profiles and profile picture uploads
- Fitness class creation, updating, cancellation, and deletion
- Fitness class search, filtering, pagination, and sorting
- Booking management and cancellation
- Membership plan management and activation/deactivation
- Membership payments and payment history
- Waitlist management, including accepting and declining waitlist offers
- Email notifications
- Real-time notifications using Server-Sent Events (SSE)
- Audit logging
- Input validation and exception handling
- IP-based rate limiting for authentication-related operations
- Database seeding
- Automated tests using JUnit and Mockito

## Technologies

- Java 17
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven
- Swagger/OpenAPI
- Server-Sent Events (SSE)
- JUnit / Mockito
- Git / GitHub

## Architecture

Gymio follows a layered architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

- **Controller:** Handles incoming API requests and HTTP responses.
- **Service:** Contains business logic, validation rules, and application workflows.
- **Repository:** Handles database operations through Spring Data JPA.
- **Entity/Model:** Represents database entities and relationships.
- **DTO:** Defines request and response data transferred through the API.

## General Approach

The application was developed using a layered architecture, starting with database entities and relationships, followed by repositories, services, and REST controllers.

Security and business rules were added throughout development, including JWT authentication, role-based authorization, email verification, booking validation, class availability, membership management, payment processing, waitlist workflows, audit logging, notifications, and rate limiting.

Automated tests are used to verify important business logic and help prevent regressions.

## API Documentation

Swagger/OpenAPI provides interactive API documentation, including endpoint details, request parameters, schemas, and response information.

**Swagger UI**

```text
http://localhost:8080/swagger-ui/index.html
```

**OpenAPI JSON**

```text
http://localhost:8080/v3/api-docs
```

## API Endpoint Reference

The following tables summarize the main API routes. Check Swagger UI for the exact request schemas, query parameters, response bodies, and security requirements.

### Authentication

Base path: `/auth/users`

| Method | Endpoint | Functionality | Access |
|---|---|---|---|
| POST | `/auth/users/register` | Register a user | Public |
| POST | `/auth/users/login` | Authenticate a user | Public |
| GET | `/auth/users/verify` | Verify email address | Public |
| POST | `/auth/users/resend-verification` | Resend verification email | Public |
| POST | `/auth/users/forgot-password` | Request password reset | Public |
| POST | `/auth/users/reset-password` | Reset password | Public |
| PUT | `/auth/users/change-password` | Change password | Authenticated |

Authentication-related operations include rate limiting to help reduce abuse.

### Fitness Classes

Base path: `/fitness-classes`

| Method | Endpoint | Functionality | Access |
|---|---|---|---|
| POST | `/fitness-classes` | Create a fitness class | Trainer / Admin |
| GET | `/fitness-classes` | Search and filter classes | Authenticated |
| GET | `/fitness-classes/{id}` | Retrieve a class | Authenticated |
| GET | `/fitness-classes/trainer/{trainerId}` | Retrieve classes by trainer | Authenticated |
| PUT | `/fitness-classes/{id}` | Update a class | Trainer / Admin |
| PUT | `/fitness-classes/{id}/cancel` | Cancel a class | Trainer / Admin |
| DELETE | `/fitness-classes/{id}` | Delete a class | Admin |

Class search supports filtering by attributes such as type, level, and status, together with pagination and sorting.

**Example search request**

```http
GET /fitness-classes?type=HIIT&level=BEGINNER&status=SCHEDULED&page=0&size=10&sort=startTime,asc
```

### Bookings

Base path: `/bookings`

| Method | Endpoint | Functionality | Access |
|---|---|---|---|
| POST | `/bookings/fitness-class/{fitnessClassId}` | Book a fitness class | Customer |
| GET | `/bookings/my` | Retrieve personal bookings | Customer |
| GET | `/bookings/{id}` | Retrieve a booking | Customer |
| PUT | `/bookings/{id}/cancel` | Cancel a booking | Customer |
| GET | `/bookings/fitness-class/{fitnessClassId}` | Retrieve bookings for a class | Trainer / Admin |
| GET | `/bookings` | Retrieve all bookings | Admin |
| DELETE | `/bookings/{id}` | Delete a booking | Admin |

Booking rules include class availability, capacity validation, and duplicate booking prevention.

### Membership Plans

Base path: `/membership-plans`

| Functionality | Access |
|---|---|
| Create a membership plan | Admin |
| Update a membership plan | Admin |
| Retrieve membership plans | Authenticated |
| Retrieve a membership plan by ID | Authenticated |
| Deactivate a membership plan | Admin |
| Reactivate a membership plan | Admin |

Membership plan management allows administrators to maintain available plans and their active status. The service determines which plans are available to different users.

Refer to Swagger UI for the exact HTTP methods and paths for each membership plan operation.

### Payments

Base path: `/payments`

| Functionality | Access |
|---|---|
| Create a membership payment | Customer |
| Retrieve personal payment history | Customer |

Customers can initiate membership payments and retrieve their own payment records. Payment operations are handled through the payment service, which applies the relevant business rules.

Refer to Swagger UI for the exact payment endpoint paths, request format, and response schemas.

### User Profiles

Base path: `/profile`

| Functionality | Access |
|---|---|
| Create a user profile | Authenticated |
| Retrieve a user profile | Authenticated |
| Update a user profile | Authenticated |
| Upload a profile picture | Authenticated |

Profile functionality allows users to manage their profile information and upload an image through a multipart request.

### Waitlist

Base path: `/api/waitlist`

| Functionality | Access |
|---|---|
| Join a class waitlist | Authenticated |
| Retrieve personal waitlist entries | Authenticated |
| Leave a waitlist | Authenticated |
| Accept a waitlist offer | Authenticated |
| Decline a waitlist offer | Authenticated |

The waitlist workflow allows users to join a waitlist for a class and respond to an offer when a place becomes available.

### Admin User Management

| Method | Endpoint | Functionality | Access |
|---|---|---|---|
| GET | `/admin/users` | Retrieve all users | Admin |
| PUT | `/admin/users/{id}` | Update user role or status | Admin |

### Notifications

| Method | Endpoint | Functionality | Access |
|---|---|---|---|
| GET | `/notifications/subscribe` | Subscribe to real-time SSE notifications | Authenticated |

Server-Sent Events provide a persistent connection for delivering real-time notifications to the authenticated user.

## HTTP Status Codes

The API uses appropriate HTTP status codes, including:

- `200 OK` – Request completed successfully
- `201 Created` – Resource created successfully
- `204 No Content` – Operation completed without a response body
- `400 Bad Request` – Invalid request or validation failure
- `401 Unauthorized` – Authentication is required or invalid
- `403 Forbidden` – Insufficient permissions
- `404 Not Found` – Resource not found
- `409 Conflict` – Request conflicts with existing data
- `429 Too Many Requests` – Rate limit exceeded, where applicable
- `500 Internal Server Error` – Unexpected server error

## Database and ERD

Gymio uses PostgreSQL to persist application data.

The main entities include:

- User
- UserProfile
- FitnessClass
- Booking
- MembershipPlan
- Payment
- WaitlistEntry
- AuditLog

The database entities and their relationships are documented in the project ERD.

**[View Gymio ERD](https://lucid.app/lucidchart/598af788-d8df-4489-9cfb-f7c5ad1e2727/edit?viewport_loc=-30%2C334%2C1701%2C988%2C0_0&invitationId=inv_8b9b2cb2-928e-4913-9493-a27254033a95)**

## User Stories & Planning

**[View Gymio Trello Board](https://trello.com/b/F1kSMzth/gymio)**

The Trello board contains the project's user stories, tasks, timeline, scope, and progress.

## Installation

### 1. Clone the repository

```bash
git clone https://github.com/SxRx246/gymio-fitness-booking-app.git
cd gymio-fitness-booking-app
```

### 2. Configure PostgreSQL

Create a PostgreSQL database for Gymio.

### 3. Configure environment variables

Set the database connection details in your environment or application configuration:

```text
DB_URL=jdbc:postgresql://localhost:5432/gymio
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

Use the property names expected by your application configuration. Do not commit database credentials or other secrets to GitHub.

### 4. Run the application

**Windows**

```bash
mvnw.cmd spring-boot:run
```

**macOS/Linux**

```bash
./mvnw spring-boot:run
```

The application runs at:

```text
http://localhost:8080
```

The development profile includes seed data for supported application entities, such as users, classes, bookings, profiles, and audit logs.

## Testing

Run the automated tests using:

**Windows**

```bash
mvnw.cmd test
```

**macOS/Linux**

```bash
./mvnw test
```

The project includes automated tests for important service-layer business logic. Test coverage should be kept aligned with new features and changes.

## Major Challenges

- Implementing JWT authentication and role-based authorization
- Enforcing booking rules, including capacity and duplicate booking prevention
- Managing fitness class status and cancellation
- Implementing email verification and password recovery
- Managing membership plans and payment workflows
- Coordinating waitlist operations
- Maintaining authenticated SSE connections and real-time notifications
- Adding audit logging and rate limiting
- Testing business logic with JUnit and Mockito

## Current Limitations

- The rate limiter and SSE connection tracking use in-memory storage and are intended for a single application instance.
- The exact endpoint paths and request schemas should be verified through Swagger UI as the API evolves.

## Future Improvements

- Build a frontend application
- Expand integration and end-to-end test coverage
- Add Docker-based deployment
- Consider distributed storage for rate limiting and SSE connection management when scaling to multiple application instances

## Credits & External Resources

| Resource | Used For |
|---|---|
| [Spring Boot](https://spring.io/projects/spring-boot) | Backend application development |
| [Spring Security](https://docs.spring.io/spring-security/reference/) | Authentication and authorization |
| [Spring Data JPA](https://spring.io/projects/spring-data-jpa) | Database access |
| [Springdoc OpenAPI](https://springdoc.org/) | API documentation |
| [JWT](https://jwt.io/) | Token-based authentication |
| [Maven](https://maven.apache.org/) | Build management and dependencies |

## Repository

**[Gymio GitHub Repository](https://github.com/SxRx246/gymio-fitness-booking-app)**
