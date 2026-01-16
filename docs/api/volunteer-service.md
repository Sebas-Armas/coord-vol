# Volunteer Service – API Contract

Base Path: /volunteers

## Purpose:
Manages volunteer-specific information such as status, skills, and availability.
This service applies only to users with role = VOLUNTEER.

## POST /volunteers

### Description:
Creates a volunteer profile for a user.
Typically called by Auth or User Service after registration.

### Authentication:
Service-to-service or Admin token

### Request Body:
```json
{
  "userId": "UUID"
}
```

### Response – 201 Created:
```json
{
  "volunteerId": "UUID",
  "userId": "UUID",
  "status": "ACTIVE",
  "joinedAt": "ISO-8601 timestamp"
}
```

### Errors:
- 400 Bad Request – invalid userId
- 409 Conflict – volunteer profile already exists

## GET /volunteers/{volunteerId}

### Description:
Returns the volunteer profile.

### Authentication:
Required (Bearer token)

### Accessible by:
- ADMIN
- COORDINATOR
- The volunteer themselves

### Response – 200 OK:
```json
{
  "volunteerId": "UUID",
  "userId": "UUID",
  "status": "ACTIVE | INACTIVE",
  "joinedAt": "ISO-8601 timestamp"
}
```

### Errors:
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – access denied
- 404 Not Found – volunteer not found

## PATCH /volunteers/{volunteerId}/status

### Description:
Activates or deactivates a volunteer.

### Authentication:
Required (Bearer token)

### Accessible by:
- ADMIN only

### Request Body:
```json
{
  "status": "ACTIVE | INACTIVE"
}
```

### Response – 200 OK:
```json
{
  "volunteerId": "UUID",
  "status": "ACTIVE | INACTIVE"
}
```

### Errors:
- 400 Bad Request – invali
