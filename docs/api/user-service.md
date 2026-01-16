# User Service – API Contract

Base Path: /users

## Purpose:
Manages user profile information and preferences.
This service does NOT handle authentication or authorization logic.

## GET /users/{userId}

### Description:
Returns the profile information for a specific user.

### Authentication:
Required (Bearer token)

### Accessible by:
- ADMIN
- The user themselves

### Response – 200 OK:
```json
{
  "userId": "UUID",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "language": "EN | ES",
  "createdAt": "ISO-8601 timestamp",
  "updatedAt": "ISO-8601 timestamp"
}
```

### Errors:
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – access denied
- 404 Not Found – user not found

## POST /users

### Description:
Creates a user profile.
This endpoint is typically called by the Auth Service during registration.

### Authentication:
Service-to-service or Admin token

### Request Body:
```json
{
  "userId": "UUID",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "language": "EN | ES"
}
```

### Response – 201 Created:
```json
{
  "userId": "UUID",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "language": "EN | ES",
  "createdAt": "ISO-8601 timestamp"
}
```

### Errors:
- 400 Bad Request – invalid data
- 409 Conflict – profile already exists

## PUT /users/{userId}

### Description:
Updates the full user profile.

### Authentication:
Required (Bearer token)

### Accessible by:
- The user themselves
- ADMIN

### Request Body:
```json
{
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "language": "EN | ES"
}
```

### Response – 200 OK:
```json
{
  "userId": "UUID",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "language": "EN | ES",
  "updatedAt": "ISO-8601 timestamp"
}
```

### Errors:
- 400 Bad Request – invalid data
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – access denied
- 404 Not Found – user not found

## PATCH /users/{userId}/language

### Description:
Updates only the preferred language of the user.

### Authentication:
Required (Bearer token)

### Accessible by:
- The user themselves
- ADMIN

### Request Body:
```json
{
  "language": "EN | ES"
}
```

### Response – 200 OK:
```json
{
  "userId": "UUID",
  "language": "EN | ES"
}
```

### Errors:
- 400 Bad Request – unsupported language
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – access denied
- 404 Not Found – user not found

## DELETE /users/{userId}

### Description:
Deletes the user profile.
Used for account removal workflows.

### Authentication:
Required (Bearer token)

### Accessible by:
- ADMIN only

### Response – 204 No Content

### Errors:
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – insufficient permissions
- 404 Not Found – user not found

## Security Notes:
- User Service trusts Auth Service for identity and role
- Authorization is enforced using role + userId comparison
- No passwords or credentials are stored here

## Non-Responsibilities:
- Authentication
- Role management
- Volunteer or coordinator domain data
