# High-Level Architecture

## Logical Architecture

```
[ Angular Frontend ]
        |
        v
[ API Gateway ]
        |
------------------------------------------------
|        |         |          |                |
Auth   User   Volunteer   Event        Notification
Svc    Svc      Svc        Svc               Svc
```

Each service:

* Spring Boot
* Own database (logical separation)
* REST API
* Dockerized



## Frontend (Angular)

Responsibilities:

* Authentication & role-based routing
* Language switching (EN / ES)
* Dashboards per role
* Forms for event creation and signup

Key modules:

* AuthModule
* AdminModule
* CoordinatorModule
* VolunteerModule
* SharedModule


## Backend Microservices

### Auth Service

* Login / logout
* JWT token issuance
* Role validation

### User Service

* User profiles
* Role assignment (Admin only)
* Language preference

### Volunteer Service

* Volunteer profiles
* Skills & availability
* Participation history

### Event Service

* Event creation
* Time slots
* Volunteer assignment

### Notification Service

* Email notifications
* Event reminders

## Database
The platform uses a **single relational database engine** across all environments (development, staging, production) to ensure consistency and reduce operational complexity.

### Database Strategy
- Same database technology for all environments
- Separate schemas per microservice
- Environment isolation handled via configuration (not technology changes)

### Database Technology
- PostgreSQL
  - Open source
  - Cloud-friendly
  - Strong transactional guarantees
  - Well supported by Spring Boot

## Internationalization (EN / ES)

### Frontend

* Angular i18n or Transloco
* Language selector in UI
* Language stored in user profile

### Backend

* Error messages returned as codes
* Frontend handles translation



## 7. Engineering Practices to Enforce

* TDD for business logic
* JUnit unit tests
* Integration tests per service
* GitHub Actions CI
* Docker Compose for local dev

