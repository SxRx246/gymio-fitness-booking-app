# Gymio – Fitness Booking App

Gymio is a secure fitness booking REST API built with **Java and Spring Boot**. It allows customers to browse fitness classes, book and manage bookings, while trainers and administrators manage classes and users.

## Main Features

* User registration and email verification
* JWT authentication and role-based authorization
* Customer, Trainer, and Admin roles
* User profiles and profile pictures
* Fitness class management
* Booking and cancellation
* Search, filtering, pagination, and sorting
* Email notifications
* Real-time notifications using SSE
* Audit logging
* Password reset and password change
* Validation and exception handling
* Rate limiting
* Database seeding
* Automated tests

## Technologies

* Java 17
* Spring Boot
* Spring Security
* JWT
* Spring Data JPA / Hibernate
* PostgreSQL
* Maven
* Swagger/OpenAPI
* SSE
* JUnit / Mockito
* Git / GitHub

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

* **Controller:** Handles API requests.
* **Service:** Contains business logic and rules.
* **Repository:** Handles database operations.
* **Entity/Model:** Represents database tables.
* **DTO:** Handles API request and response data.

## General Approach

The application was developed by first creating the database entities and relationships, followed by the repositories, services, and REST controllers.

Security and business rules were then added, including JWT authentication, role authorization, email verification, booking validation, class availability, audit logging, notifications, and rate limiting. Important business logic is covered with automated tests.

## API Documentation

Swagger/OpenAPI provides detailed interactive API documentation.

**Swagger UI:**

```text
http://localhost:8080/swagger-ui/index.html
```

**OpenAPI JSON:**

```text
http://localhost:8080/v3/api-docs
```

## API Endpoint Reference

### Authentication

| Method | Endpoint                          | Functionality             | Access        |
| ------ | --------------------------------- | ------------------------- | ------------- |
| POST   | `/auth/users/register`            | Register user             | Public        |
| POST   | `/auth/users/login`               | Login                     | Public        |
| GET    | `/auth/users/verify`              | Verify email              | Public        |
| POST   | `/auth/users/resend-verification` | Resend verification email | Public        |
| POST   | `/auth/users/forgot-password`     | Request password reset    | Public        |
| POST   | `/auth/users/reset-password`      | Reset password            | Public        |
| PUT    | `/auth/users/change-password`     | Change password           | Authenticated |

### Fitness Classes

| Method | Endpoint                               | Functionality          | Access          |
| ------ | -------------------------------------- | ---------------------- | --------------- |
| POST   | `/fitness-classes`                     | Create class           | Trainer / Admin |
| GET    | `/fitness-classes`                     | Search/filter classes  | Authenticated   |
| GET    | `/fitness-classes/{id}`                | View class             | Authenticated   |
| GET    | `/fitness-classes/trainer/{trainerId}` | View trainer's classes | Authenticated   |
| PUT    | `/fitness-classes/{id}`                | Update class           | Trainer / Admin |
| PUT    | `/fitness-classes/{id}/cancel`         | Cancel class           | Trainer / Admin |
| DELETE | `/fitness-classes/{id}`                | Delete class           | Admin           |

**Example search:**

```text
GET /fitness-classes?type=HIIT&level=BEGINNER&status=SCHEDULED&page=0&size=10&sort=startTime,asc
```

### Bookings

| Method | Endpoint                                   | Functionality       | Access          |
| ------ | ------------------------------------------ | ------------------- | --------------- |
| POST   | `/bookings/fitness-class/{fitnessClassId}` | Book a class        | Customer        |
| GET    | `/bookings/my`                             | View my bookings    | Customer        |
| GET    | `/bookings/{id}`                           | View booking        | Customer        |
| PUT    | `/bookings/{id}/cancel`                    | Cancel booking      | Customer        |
| GET    | `/bookings/fitness-class/{fitnessClassId}` | View class bookings | Trainer / Admin |
| GET    | `/bookings`                                | View all bookings   | Admin           |
| DELETE | `/bookings/{id}`                           | Delete booking      | Admin           |

### Admin Users

| Method | Endpoint            | Functionality           | Access |
| ------ | ------------------- | ----------------------- | ------ |
| GET    | `/admin/users`      | View all users          | Admin  |
| PUT    | `/admin/users/{id}` | Update user role/status | Admin  |

### Notifications

| Method | Endpoint                   | Functionality               | Access        |
| ------ | -------------------------- | --------------------------- | ------------- |
| GET    | `/notifications/subscribe` | Real-time SSE notifications | Authenticated |

## HTTP Status Codes

The API uses appropriate HTTP status codes, including:

* `200 OK` – Successful request
* `201 Created` – Resource created
* `204 No Content` – Successful operation without response body
* `400 Bad Request` – Invalid request
* `401 Unauthorized` – Authentication required
* `403 Forbidden` – Insufficient permissions
* `404 Not Found` – Resource not found
* `409 Conflict` – Request conflicts with existing data
* `500 Internal Server Error` – Unexpected server error

## Database and ERD

Gymio uses PostgreSQL with the following main entities:

* User
* UserProfile
* FitnessClass
* Booking
* AuditLog

**[View Gymio ERD](https://lucid.app/lucidchart/598af788-d8df-4489-9cfb-f7c5ad1e2727/edit?viewport_loc=-30%2C334%2C1701%2C988%2C0_0&invitationId=inv_8b9b2cb2-928e-4913-9493-a27254033a95)**

## User Stories & Planning

**[View Gymio Trello Board](https://trello.com/b/F1kSMzth/gymio)**

The board contains the project's user stories, tasks, timeline, scope, and progress.

## Installation

### 1. Clone the repository

```bash
git clone https://github.com/SxRx246/gymio-fitness-booking-app.git
cd gymio-fitness-booking-app
```

### 2. Configure PostgreSQL

Create a PostgreSQL database for Gymio.

### 3. Configure environment variables

```text
DB_URL=jdbc:postgresql://localhost:5432/gymio
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

Do not commit database credentials to GitHub.

### 4. Run the application

Windows:

```bash
mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

The application runs at:

```text
http://localhost:8080
```

The development profile includes seed data for users, classes, bookings, profiles, and audit logs.

## Testing

Tests can be run using:

```bash
mvnw.cmd test
```

The project includes service tests for booking and fitness class business logic.

## Major Challenges

* Implementing JWT authentication and role-based authorization.
* Enforcing booking rules such as capacity and duplicate booking prevention.
* Managing automatic fitness class status changes.
* Implementing email and real-time SSE notifications.
* Adding audit logging and rate limiting.
* Testing important business logic with JUnit and Mockito.

## Unsolved Problems

The current rate limiter and SSE connections use in-memory storage, so they are intended for a single application instance.

## Future Improvements

* Frontend application
* More integration tests
* Docker deployment

## Credits & External Resources

| Resource                                                             | Used For                                            |
| -------------------------------------------------------------------- | --------------------------------------------------- |
| [Spring Boot](https://spring.io/projects/spring-boot)                | Backend development                                 |
| [Spring Security](https://docs.spring.io/spring-security/reference/) | Authentication and authorization                    |
| [Spring Data JPA](https://spring.io/projects/spring-data-jpa)        | Database access                                     | | Database                                            |
| [Springdoc OpenAPI](https://springdoc.org/)                          | Swagger documentation                               |
| [JWT](https://jwt.io/)                                               | JWT authentication                                  |
| [Maven](https://maven.apache.org/)                                   | Build and dependencies                              |
                                    

## Repository

**[Gymio GitHub Repository](https://github.com/SxRx246/gymio-fitness-booking-app)**
