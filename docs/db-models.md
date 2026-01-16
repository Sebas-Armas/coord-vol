## Database Schemas – Overview

Database Engine: PostgreSQL
Strategy:
- One database engine
- One schema per microservice
- No foreign keys across schemas
- Relationships across services handled via IDs only


## Auth Service – Database Schema (auth_schema)

### Table: auth_users

### Fields:
- id (UUID, PK)
- email (VARCHAR, unique, not null)
- password_hash (VARCHAR, not null)
- role (VARCHAR, not null)   // ADMIN | COORDINATOR | VOLUNTEER
- status (VARCHAR, not null) // ACTIVE | INACIVE
- created_at (TIMESTAMP, not null)
- last_login_at (TIMESTAMP, nullable)

### Indexes:
- unique index on email

### Notes:
- This schema owns authentication and role authority
- No profile or domain data stored here

## User Service – Database Schema (user_schema)

### Table: user_profiles

### Fields:
- user_id (UUID, PK)   // same ID as auth_users.id
- email (VARCHAR, not null) // same as auth_users.email
- first_name (VARCHAR, not null)
- last_name (VARCHAR, not null)
- language (VARCHAR, not null)   // EN | ES
- created_at (TIMESTAMP, not null)
- updated_at (TIMESTAMP, not null)

### Indexes:
- index on email

### Notes:
- user_id is a logical reference to Auth Service
- No credentials or roles stored

## Volunteer Service – Database Schema (volunteer_schema)

### Table: volunteer_profiles

### Fields:
- id (UUID, PK)
- user_id (UUID, not null)
- status (VARCHAR, not null)   // ACTIVE | INACTIVE
- joined_at (TIMESTAMP, not null)

### Indexes:
- unique index on user_id

------------------------------------------------------------

### Table: volunteer_skills

### Fields:
- id (UUID, PK)
- volunteer_id (UUID, not null)
- name (VARCHAR, not null)

### Indexes:
- index on volunteer_id

------------------------------------------------------------

### Table: volunteer_availability

### Fields:
- id (UUID, PK)
- volunteer_id (UUID, not null)
- day_of_week (VARCHAR, not null)
- start_time (TIME, not null)
- end_time (TIME, not null)

### Indexes:
- index on volunteer_id

### Notes:
- volunteer_id is owned by this service
- user_id refers logically to User Service

## Event Service – Database Schema (event_schema)

### Table: events

### Fields:
- id (UUID, PK)
- title (VARCHAR, not null)
- description (TEXT)
- start_datetime (TIMESTAMP, not null)
- end_datetime (TIMESTAMP, not null)
- capacity (INTEGER, not null)
- created_by_coordinator_id (UUID, not null)
- created_at (TIMESTAMP, not null)

### Indexes:
- index on start_datetime
- index on created_by_coordinator_id

------------------------------------------------------------

### Table: event_skill_requirements

### Fields:
- id (UUID, PK)
- event_id (UUID, not null)
- skill_name (VARCHAR, not null)

### Indexes:
- index on event_id

------------------------------------------------------------

### Table: event_registrations

### Fields:
- id (UUID, PK)
- event_id (UUID, not null)
- volunteer_id (UUID, not null)
- status (VARCHAR, not null)   // REGISTERED | CANCELLED | ATTENDED
- registered_at (TIMESTAMP, not null)

### Indexes:
- unique index on (event_id, volunteer_id)
- index on volunteer_id

### Notes:
- volunteer_id refers logically to Volunteer Service
- capacity enforcement handled at service level
