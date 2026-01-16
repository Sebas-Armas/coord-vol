# Auth Service – API Contract

Base Path: /auth

## Purpose:
Handles authentication, authorization, and token issuance.
This service is the authority for user roles and access control.

## POST /auth/register

### Description:
Registers a new user in the system.
This endpoint creates authentication credentials only.
Profile data is handled by the User Service.

### Authentication:
None

### Request Body:
```json
{
  "password": "string",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "role": "COORDINATOR | VOLUNTEER",
  "language": "EN | ES"
}
```

### Response – 201 Created:
```json
{
  "userId": "UUID",
  "role": "COORDINATOR | VOLUNTEER",
  "active": true,
  "createdAt": "ISO-8601 timestamp"
}
```

### Errors:
- 400 Bad Request – invalid data or unsupported role
- 409 Conflict – email already exists

### Side Effects:
- Creates AuthUser in Auth Service
- Triggers creation of UserProfile in User Service (synchronous call or event)

### Notes:
- Admin role cannot be created via registration
- New users are active by default
- Password is stored hashed


## POST /auth/login

### Description:
Authenticates a user and returns a JWT access token.

### Authentication:
None

### Request Body:
```json
{
  "email": "string",
  "password": "string"
}
```

### Response – 200 OK:
```json
{
  "accessToken": "string",
  "tokenType": "Bearer",
  "expiresIn": "number",
  "role": "ADMIN | COORDINATOR | VOLUNTEER"
}
```

### Errors:
- 400 Bad Request – missing or invalid fields
- 401 Unauthorized – invalid credentials
- 403 Forbidden – user inactive

-----------------------------------------

## POST /auth/logout

### Description:
Invalidates the current user session (token revocation handled logically).

### Authentication:
Required (Bearer token)

### Request Body:
None

### Response – 204 No Content

### Errors:
- 401 Unauthorized – missing or invalid token

------------------------------------------

## GET /auth/me

### Description:
Returns information about the currently authenticated user.

### Authentication:
Required (Bearer token)

### Response – 200 OK:
```json
{
  "userId": "UUID",
  "email": "string",
  "role": "ADMIN | COORDINATOR | VOLUNTEER",
  "active": true
}
```

### Errors:
- 401 Unauthorized – missing or invalid token

------------------------------------------

## POST /auth/users

### Description:
Creates a new authentication user.
Only accessible by Admin.

### Authentication:
Required (Bearer token, role = ADMIN)

### Request Body:
```json
{
  "email": "string",
  "password": "string",
  "role": "COORDINATOR | VOLUNTEER"
}
```

### Response – 201 Created:
```json
{
  "id": "UUID",
  "email": "string",
  "role": "COORDINATOR | VOLUNTEER",
  "active": true,
  "createdAt": "ISO-8601 timestamp"
}
```

### Errors:
- 400 Bad Request – invalid role or data
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – insufficient permissions
- 409 Conflict – email already exists

------------------------------------------

## PATCH /auth/users/{userId}/status

### Description:
Updates User Status.

### Authentication:
Required (Bearer token, role = ADMIN)

### Request Body:
```json
{
  "status": "ACTIVE | INACTIVE | DELETED"
}
```

### Response – 200 OK:
```json
{
  "userId": "UUID",
  "status": "ACTIVE | INACTIVE | DELETED"
}
```

### Errors:
- 400 Bad Request – invalid status
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – insufficient permissions
- 404 Not Found – user not found

------------------------------------------

## Security Notes:
- Passwords are never returned or logged
- JWT contains userId and role
- Token expiration is enforced
- Role is authoritative in this service

## Non-Responsibilities:
- No user profile data
- No language preferences
- No volunteer or coordinator domain logic
