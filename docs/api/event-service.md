# Event Service – API Contract

Base Path: /events

## Purpose:
Manages events, skill requirements, and volunteer participation.
Acts as the coordination hub between Coordinators and Volunteers.

## POST /events

### Description:
Creates a new event.

### Authentication:
Required (Bearer token)

### Accessible by:
- COORDINATOR only

### Request Body:
```json
{
  "title": "string",
  "description": "string",
  "startDateTime": "ISO-8601 timestamp",
  "endDateTime": "ISO-8601 timestamp",
  "capacity": number
}
```

### Response – 201 Created:
```json
{
  "eventId": "UUID",
  "title": "string",
  "description": "string",
  "startDateTime": "ISO-8601 timestamp",
  "endDateTime": "ISO-8601 timestamp",
  "capacity": number,
  "createdByCoordinatorId": "UUID",
  "createdAt": "ISO-8601 timestamp"
}
```

### Errors:
- 400 Bad Request – invalid dates or capacity
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – insufficient permissions

## GET /events/{eventId}

### Description:
Returns details of a specific event.

### Authentication:
Required (Bearer token)

### Response – 200 OK:
```json
{
  "eventId": "UUID",
  "title": "string",
  "description": "string",
  "startDateTime": "ISO-8601 timestamp",
  "endDateTime": "ISO-8601 timestamp",
  "capacity": number,
  "createdByCoordinatorId": "UUID",
  "createdAt": "ISO-8601 timestamp"
}
```

### Errors:
- 401 Unauthorized – missing or invalid token
- 404 Not Found – event not found

## GET /events

### Description:
Returns a list of available events.

### Authentication:
Required (Bearer token)

### Query Parameters:
- upcomingOnly (boolean, optional)

### Response – 200 OK:
```json
[
  {
    "eventId": "UUID",
    "title": "string",
    "startDateTime": "ISO-8601 timestamp",
    "endDateTime": "ISO-8601 timestamp",
    "capacity": number
  }
]
```

### Errors:
- 401 Unauthorized – missing or invalid token

## POST /events/{eventId}/requirements

### Description:
Adds a required skill to an event.

### Authentication:
Required (Bearer token)

### Accessible by:
- COORDINATOR (event owner)

### Request Body:
```json
{
  "skillName": "string"
}
```

### Response – 201 Created:
```json
{
  "requirementId": "UUID",
  "skillName": "string"
}
```

### Errors:
- 400 Bad Request – invalid skill
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – not event owner
- 404 Not Found – event not found

## GET /events/{eventId}/requirements

### Description:
Returns required skills for an event.

### Authentication:
Required (Bearer token)

### Response – 200 OK:
```json
[
  {
    "requirementId": "UUID",
    "skillName": "string"
  }
]
```

### Errors:
- 401 Unauthorized – missing or invalid token
- 404 Not Found – event not found

## POST /events/{eventId}/registrations

### Description:
Registers a volunteer for an event.

### Authentication:
Required (Bearer token)

### Accessible by:
- VOLUNTEER only

### Request Body:
```json
{
  "volunteerId": "UUID"
}
```

### Response – 201 Created:
```json
{
  "registrationId": "UUID",
  "eventId": "UUID",
  "volunteerId": "UUID",
  "status": "REGISTERED",
  "registeredAt": "ISO-8601 timestamp"
}
```

### Errors:
- 400 Bad Request – event full or already registered
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – insufficient permissions
- 404 Not Found – event or volunteer not found

## GET /events/{eventId}/registrations

### Description:
Returns all registrations for an event.

### Authentication:
Required (Bearer token)

### Accessible by:
- COORDINATOR (event owner)
- ADMIN

### Response – 200 OK:
```json
[
  {
    "registrationId": "UUID",
    "volunteerId": "UUID",
    "status": "REGISTERED | CANCELLED | ATTENDED"
  }
]
```

### Errors:
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – access denied
- 404 Not Found – event not found

## PATCH /events/{eventId}/registrations/{registrationId}

### Description:
Updates the status of a volunteer registration.

### Authentication:
Required (Bearer token)

### Accessible by:
- COORDINATOR (event owner)

### Request Body:
```json
{
  "status": "CANCELLED | ATTENDED"
}
```

### Response – 200 OK:
```json
{
  "registrationId": "UUID",
  "status": "CANCELLED | ATTENDED"
}
```

### Errors:
- 400 Bad Request – invalid status
- 401 Unauthorized – missing or invalid token
- 403 Forbidden – insufficient permissions
- 404 Not Found – registration not found

## Security Notes:
- Coordinator ownership is enforced via token userId
- Volunteer existence validated via Volunteer Service
- No direct database joins across services

## Non-Responsibilities:
- User authentication
- Volunteer profile management
- Notifications (delegated to Notification Service)
