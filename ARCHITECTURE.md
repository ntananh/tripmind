# Trip Mind – Architecture

## Overview

Trip Mind is a microservice-based transit assistant demonstrating distributed systems concepts, including:

* Service decomposition and REST communication
* Integration with external APIs (Digitransit)
* LLM-assisted intent parsing
* Event-driven reminder notifications via message broker

The system answers natural-language transit queries such as "next bus from Keskustori" and can schedule reminders before departures.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Trip Mind System                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────┐         ┌─────────────────┐                                  │
│   │  User /  │────────▶│   Assistant     │───────────▶ LLM Provider         │
│   │   Siri   │         │   Service       │           (OpenAI / Mock)        │
│   └──────────┘         │      :8080      │                                  │
│                        └────────┬────────┘                                  │
│                                 │                                           │
│                    ┌────────────┼──────────────┐                            │
│                    ▼            │              ▼                            │
│           ┌────────────────┐    │    ┌──────────────────┐                   │
│           │  Transit       │    │    │   Reminder       │                   │
│           │  Service       │    │    │   Service        │                   │
│           │   :8081        │    │    │    :8083         │                   │
│           └───────┬────────┘    │    └─────────┬────────┘                   │
│                   │             │              │                            │
│                   ▼             │              ▼                            │
│           ┌────────────────┐    │    ┌──────────────────┐                   │
│           │  Location      │    │    │  Apache Artemis  │                   │
│           │  Service       │    │    │   (Broker)       │                   │
│           │   :8082        │    │    │    :61616        │                   │
│           └───────┬────────┘    │    └─────────┬────────┘                   │
│                   │             │              │                            │
│                   ▼             │              ▼                            │
│           ┌────────────────┐    │    ┌──────────────────┐                   │
│           │ Digitransit    │    │    │ Notification     │                   │
│           │     API        │    │    │ Consumer         │                   │
│           └────────────────┘    │    └──────────────────┘                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Service Responsibilities

### 1. Assistant Service (8080)

**Role:** Entry point and natural-language interface.

**Responsibilities:**

* Accept `/chat` requests from user or Siri
* Parse intent using LLM or simple mock rules
* Call Transit Service for schedule queries
* Call Reminder Service to create reminders
* Format and return responses

**Depends on:** Transit Service, Reminder Service, LLM Provider

---

### 2. Transit Service (8081)

**Role:** Fetch transit data and orchestrate queries.

**Responsibilities:**

* Handle `/next` departure queries
* Resolve stop information via Location Service
* Query Digitransit (GraphQL) for real-time schedules
* Optional caching for repeated lookups

**Depends on:** Location Service, Digitransit API

---

### 3. Location Service (8082)

**Role:** Resolve user-provided location names.

**Responsibilities:**

* Convert "Keskustori" → stop ID
* Return coordinates or stop metadata
* Store favorite or cached locations in H2

**Depends on:** Local H2 database

---

### 4. Reminder Service (8083)

**Role:** Store and execute reminders.

**Responsibilities:**

* Save reminder requests
* Scheduler periodically checks for due reminders
* Publish events to Apache Artemis
* Consumer logs a notification (course demo output)

**Depends on:** Apache Artemis, Local H2 database

---

## Data Flow Examples

### Flow 1 — "Next bus from Keskustori"

```
User → Assistant → Transit → Location → resolve stop
                                   ↓
                          Digitransit API
                                   ↓
Assistant → formatted result → User
```

### Flow 2 — "Remind me before the next bus"

```
User → Assistant → Reminder → store in DB
                             ↓ (scheduled)
                        Artemis event → Consumer → log/notify
```

---

## Technology Stack

| Component        | Technology          |
|------------------|---------------------|
| Framework        | Spring Boot 4.0.0   |
| Language         | Java 21             |
| Build Tool       | Maven               |
| Databases        | H2 (per service)    |
| Message Broker   | Apache Artemis      |
| HTTP Client      | WebClient (WebFlux) |
| External API     | Digitransit GraphQL |
| Containerization | Docker Compose      |

---

## API Summary

### Assistant Service

* `POST /api/assistant/chat` – natural-language query
* `GET /api/assistant/health` – health check

### Transit Service

* `GET /api/transit/next` – return next departures
* `GET /api/transit/stop/{id}` – stop details
* `GET /api/transit/health` – health check

### Location Service

* `GET /api/locations/search` – search stop by name
* `GET /api/locations/{id}` – location details
* `GET /api/locations` – all stops
* `POST /api/locations` – create location
* `GET /api/locations/health` – health check

### Reminder Service

* `POST /api/reminders` – create reminder
* `GET /api/reminders/{userId}` – list reminders
* `DELETE /api/reminders/{id}` – cancel reminder
* `GET /api/reminders/health` – health check

---

## Event Message Schema

Message sent when a reminder fires (via Artemis JMS):

**Queue:** `reminder.notifications`

```json
{
  "reminderId": "uuid",
  "userId": "user123",
  "message": "Bus 3 departs in 5 minutes from Keskustori",
  "stopId": "tampere:0001",
  "routeName": "3",
  "triggerTime": "2025-12-07T08:10:00",
  "departureTime": "2025-12-07T08:15:00"
}
```

---

## Running the System

### Start Artemis

```bash
docker run -d --name artemis \
  -p 61616:61616 -p 8161:8161 \
  -e ARTEMIS_USER=admin -e ARTEMIS_PASSWORD=admin \
  apache/activemq-artemis:latest
```

### Build & Run Services

```bash
# Build common first
cd common && mvn clean install

# Then each service
cd location && mvn spring-boot:run
cd transit && mvn spring-boot:run
cd reminder && mvn spring-boot:run
cd assistant && mvn spring-boot:run
```

---

## Demo Queries

```bash
# Health checks
curl http://localhost:8080/api/assistant/health
curl http://localhost:8081/api/transit/health
curl http://localhost:8082/api/locations/health
curl http://localhost:8083/api/reminders/health

# Next departure
curl -X POST http://localhost:8080/api/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"userId": "user1", "message": "When is the next bus from Keskustori?"}'

# Set reminder
curl -X POST http://localhost:8080/api/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"userId": "user1", "message": "Remind me 5 minutes before the bus from Keskustori"}'

# Direct location search
curl "http://localhost:8082/api/locations/search?name=Keskustori"

# Direct transit query
curl "http://localhost:8081/api/transit/next?stopName=Keskustori"
```

---

## Course Requirements Mapping

| Requirement               | Implementation                          |
|---------------------------|-----------------------------------------|
| Spring Boot application   | 4 microservices                         |
| Distributed Database      | H2 per service (isolated data stores)   |
| Queues / async processing | Apache Artemis for reminder events      |
| Event handling            | ReminderEvent → Artemis → Consumer      |
| REST communication        | Assistant → Transit, Location, Reminder |
| External API integration  | Digitransit GraphQL client              |
