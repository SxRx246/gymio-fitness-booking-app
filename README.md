# Gymio

## Fitness Booking App

Gymio is a fitness booking app built with Java and Spring Boot. It allows customers to browse available fitness classes, view scheduled sessions, choose trainers, and book classes.

The application manages customers, trainers, administrators, fitness classes, schedules, bookings, and user profiles.

## Main Features

* User management with Customer, Trainer, and Admin roles
* Customer profiles
* Fitness class management
* Trainer management
* Class scheduling
* Browse available fitness classes
* Book fitness classes
* Cancel bookings
* Booking status tracking
* Audit logging

## Fitness Classes

Gymio supports different types of fitness classes, including:

* Full Body
* Upper Body
* Lower Body
* Glutes and Core
* HIIT
* Strength Training
* Zumba

Classes can also be associated with different experience levels:

* All Levels
* Beginner
* Medium
* Advanced

## Main Workflow

```text
Customer
   ↓
Browse Fitness Classes
   ↓
View Class Schedule
   ↓
Choose Trainer & Session
   ↓
Book Class
```

## ERD

The Entity-Relationship Diagram for Gymio is available here:

[View the Gymio ERD on Lucidchart](https://lucid.app/lucidchart/598af788-d8df-4489-9cfb-f7c5ad1e2727/edit?viewport_loc=-30%2C334%2C1701%2C988%2C0_0&invitationId=inv_8b9b2cb2-928e-4913-9493-a27254033a95)

## Repository

[View the Gymio GitHub Repository](https://github.com/SxRx246/gymio-fitness-booking-app)

## Project Structure

The application follows a layered architecture:

* **Controller** – Handles HTTP requests and API endpoints
* **Service** – Contains business logic
* **Repository** – Handles database operations
* **Model/Entity** – Represents the database entities
* **DTO** – Handles data transfer between the API and application layers

## Technologies

* Java
* Spring Boot
* Spring Data JPA / Hibernate
* PostgreSQL
* Spring Security
* JWT
* REST API

## Project Status

Currently under development.
