# Transit Helper System - Architecture

## Overview

A microservice-based transit assistant that demonstrates distributed systems concepts:
- Service separation with REST communication
- External API integration (Digitransit)
- LLM integration for natural language processing
- Asynchronous event handling with RabbitMQ

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Transit Helper System                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────┐         ┌─────────────────┐                                  │
│   │  User /  │────────▶│   Assistant     │◀───────▶ LLM Provider            │
│   │   Siri   │         │   Service       │          (OpenAI/Mock)           │
│   └──────────┘         │   :8080         │                                  │
│                        └────────┬────────┘                                  │
│                                 │                                           │
│                    ┌────────────┼────────────┐                              │
│                    │            │            │                              │
│                    ▼            │            ▼                              │
│           ┌────────────┐       │    ┌────────────────┐                     │
│           │  Transit   │       │    │    Reminder    │                     │
│           │  Service   │       │    │    Service     │                     │
│           │   :8081    │       │    │     :8083      │                     │
│           └──────┬─────┘       │    └───────┬────────┘                     │
│                  │             │            │                              │
│                  ▼             │            ▼                              │
│           ┌────────────┐       │    ┌────────────────┐                     │
│           │  Location  │       │    │   RabbitMQ     │                     │
│           │  Service   │       │    │   (Broker)     │                     │
│           │   :8082    │       │    │    :5672       │                     │
│           └──────┬─────┘       │    └───────┬────────┘                     │
│                  │             │            │                              │
│                  ▼             │            ▼                              │
│           ┌────────────┐       │    ┌────────────────┐                     │
│           │ Digitransit│       │    │  Notification  │                     │
│           │    API     │       │    │   Consumer     │                     │
│           └────────────┘       │    └────────────────┘                     │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Service Responsibilities

### 1. Assistant Service (Port 8080)
- **Role**: Entry point, natural language processing
- **Responsibilities**:
  - Receive user queries via REST API
  - Parse intent using LLM (or mock pattern matching)
  - Route to appropriate service (Transit/Reminder)
  - Format and return responses
- **Dependencies**: Transit Service, Reminder Service, LLM Provider

### 2. Transit Service (Port 8081)
- **Role**: Transit data orchestration
- **Responsibilities**:
  - Handle "next departure" queries
  - Call Location Service to resolve stop names → coordinates
  - Call Digitransit API for real-time schedules
  - Cache frequent queries
- **Dependencies**: Location Service, Digitransit API

### 3. Location Service (Port 8082)
- **Role**: Location resolution
- **Responsibilities**:
  - Convert stop names to stop IDs and coordinates
  - Store known/favorite locations
  - Geocoding support
- **Dependencies**: Own H2 database

### 4. Reminder Service (Port 8083)
- **Role**: Scheduling and notifications
- **Responsibilities**:
  - Store reminder requests
  - Schedule triggers based on departure times
  - Publish events to RabbitMQ when reminders fire
- **Dependencies**: RabbitMQ, own H2 database

## Data Flow Examples

### Flow 1: "Next bus from Keskustori"

```
User ──▶ Assistant ──▶ Transit ──▶ Location ──▶ [resolve "Keskustori"]
                          │                          │
                          │◀─────── stop_id ─────────┘
                          │
                          ▼
                    Digitransit API
                          │
                          ▼
User ◀── formatted ◀── departures
         response
```

### Flow 2: "Remind me 5 min before the 8:15 bus"

```
User ──▶ Assistant ──▶ Reminder ──▶ [store reminder in DB]
                          │
                          │ (at trigger time)
                          ▼
                      RabbitMQ ──▶ Notification Consumer ──▶ [log/alert]
```

## Technology Stack

| Component        | Technology                |
|------------------|---------------------------|
| Framework        | Spring Boot 3.2           |
| Language         | Java 17                   |
| Build Tool       | Maven (multi-module)      |
| Databases        | H2 (in-memory per service)|
| Message Broker   | RabbitMQ                  |
| HTTP Client      | WebClient (WebFlux)       |
| External API     | Digitransit GraphQL       |
| Containerization | Docker Compose            |

## API Endpoints

### Assistant Service
| Method | Endpoint           | Description              |
|--------|-------------------|--------------------------|
| POST   | /api/assistant/chat| Process natural language |
| GET    | /api/assistant/health | Health check          |

### Transit Service
| Method | Endpoint                    | Description           |
|--------|-----------------------------|-----------------------|
| GET    | /api/transit/next           | Next departures       |
| GET    | /api/transit/stop/{stopId}  | Departures from stop  |

### Location Service
| Method | Endpoint                      | Description           |
|--------|-------------------------------|-----------------------|
| GET    | /api/locations/search         | Search by name        |
| GET    | /api/locations/{id}           | Get location details  |
| POST   | /api/locations                | Save location         |

### Reminder Service
| Method | Endpoint                  | Description           |
|--------|---------------------------|-----------------------|
| POST   | /api/reminders            | Create reminder       |
| GET    | /api/reminders/{userId}   | Get user's reminders  |
| DELETE | /api/reminders/{id}       | Cancel reminder       |

## Event Schema (RabbitMQ)

**Exchange**: `transit-events`  
**Queue**: `reminder-notifications`  
**Routing Key**: `reminder.triggered`

```json
{
  "reminderId": "uuid",
  "userId": "user123",
  "message": "Bus 3 departs in 5 minutes from Keskustori",
  "stopId": "HSL:1234",
  "routeName": "3",
  "triggerTime": "2024-01-15T08:10:00",
  "departureTime": "2024-01-15T08:15:00"
}
```

## Running the System

### With Docker Compose
```bash
docker-compose up -d
```

### Manual (for development)
```bash
# Terminal 1 - RabbitMQ
docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# Terminal 2-5 - Services
cd assistant-service && mvn spring-boot:run
cd transit-service && mvn spring-boot:run
cd location-service && mvn spring-boot:run
cd reminder-service && mvn spring-boot:run
```

## Demo Queries

```bash
# Next departure
curl -X POST http://localhost:8080/api/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "next bus from Keskustori"}'

# Set reminder
curl -X POST http://localhost:8080/api/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "remind me 5 minutes before the next bus from Keskustori"}'
```

## Course Requirements Mapping

| Requirement                  | Implementation                           |
|------------------------------|------------------------------------------|
| Spring Boot application      | 4 Spring Boot services                   |
| Distributed Database         | H2 per service (isolated data stores)    |
| Queues/Asynchronization      | RabbitMQ for reminder events             |
| Event handling/distribution  | ReminderEvent → RabbitMQ → Consumer      |
