# Pokemon GO Trade Tracker: Architecture

## 1. Purpose

A small web app to track Pokemon GO trade deals made across many platforms (WhatsApp, Discord, Telegram, Reddit, and so on), so no deal is forgotten. Each deal has a priority, a planned trade timeline (immediate, 7-day, 30-day, custom), and a direct link back to the chat where the deal was made.

Assumption: single user (the owner). Multi-user support is listed under future work.

## 2. Goals and Non-Goals

**Goals**
- Log a deal in under 15 seconds.
- Show what is due today, what is overdue, and what is coming up.
- Show days left for every planned trade.
- Priority button (High / Medium / Low) on every deal.
- Link button that opens the original chat.
- Track the one special trade per day.
- Run 100% on free tiers.

**Non-Goals (v1)**
- No Pokemon GO account integration (no official API).
- No real-time chat or messaging.
- No multi-user or friend sharing.

## 3. Tech Stack (all free)

| Layer | Choice | Notes |
|---|---|---|
| UI | React (Vite), PWA | Hosted on Netlify. Installable on phone. |
| Backend | Java 17, Spring Boot 3 | Hosted on Render (free web service). Sleeps when idle, so first request is slow. |
| Database | PostgreSQL on Neon (free) | 0.5 GB per project. Scale-to-zero, so first query after idle is slow. |
| Migrations | Flyway | SQL files in the repo. |
| Tests | JUnit 5, Mockito, Spring Boot Test | Repository tests use Testcontainers or H2 in Postgres mode. |
| Build | Maven | |

## 4. High-Level Architecture

```
[ Phone / Browser ]
        |
        v
[ Netlify: React PWA ]
        |  HTTPS, JSON
        v
[ Render: Spring Boot REST API ]
        |  JDBC (SSL)
        v
[ Neon: PostgreSQL ]
```

CORS on the backend allows only the Netlify domain and localhost during development.

## 5. Domain Model

### Deal
| Field | Type | Description |
|---|---|---|
| id | UUID | Primary key |
| friendName | String | Trainer name or in-game name |
| trainerCode | String, optional | Friend code |
| pokemonGive | String | Pokemon you give |
| pokemonGet | String | Pokemon you get |
| tradeType | Enum | `IMMEDIATE`, `DAYS_7`, `DAYS_30`, `CUSTOM` |
| planDays | Integer | Days until trade. 0 for immediate. Set from tradeType, editable when CUSTOM. |
| dealDate | Date | Day the deal was made |
| dueDate | Date | `dealDate + planDays` |
| priority | Enum | `HIGH`, `MEDIUM`, `LOW` |
| status | Enum | `PLANNED`, `DONE`, `CANCELLED` |
| specialTrade | Boolean | True if this trade will use the daily special trade |
| platform | String, optional | WhatsApp, Discord, Telegram, Reddit, other |
| chatLink | String, optional | Direct link to the chat. Must start with `https://`. |
| notes | String, optional | Free text |
| completedOn | Date, optional | Set when status becomes DONE |
| createdAt, updatedAt | Timestamp | Audit |

### Derived values (computed in the service, not stored)
- `daysLeft = dueDate - today` (negative means overdue).
- `overdue = status == PLANNED && daysLeft < 0`.

## 6. Database Schema

```sql
CREATE TABLE deal (
    id            UUID PRIMARY KEY,
    friend_name   VARCHAR(100) NOT NULL,
    trainer_code  VARCHAR(20),
    pokemon_give  VARCHAR(100) NOT NULL,
    pokemon_get   VARCHAR(100) NOT NULL,
    trade_type    VARCHAR(20)  NOT NULL,
    plan_days     INTEGER      NOT NULL DEFAULT 0 CHECK (plan_days >= 0),
    deal_date     DATE         NOT NULL,
    due_date      DATE         NOT NULL,
    priority      VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    status        VARCHAR(12)  NOT NULL DEFAULT 'PLANNED',
    special_trade BOOLEAN      NOT NULL DEFAULT FALSE,
    platform      VARCHAR(30),
    chat_link     VARCHAR(500),
    notes         TEXT,
    completed_on  DATE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_deal_status_due ON deal (status, due_date);
CREATE INDEX idx_deal_priority   ON deal (priority);
```

## 7. REST API

Base path: `/api/v1`

| Method | Path | Purpose |
|---|---|---|
| POST | `/deals` | Create a deal |
| GET | `/deals` | List deals. Filters: `status`, `priority`, `tradeType`, `platform`, `overdue`. Sort: `dueDate`, `priority`. |
| GET | `/deals/{id}` | Get one deal |
| PUT | `/deals/{id}` | Update a deal |
| PATCH | `/deals/{id}/priority` | Change priority only (for the priority button) |
| PATCH | `/deals/{id}/status` | Mark DONE or CANCELLED |
| DELETE | `/deals/{id}` | Delete a deal |
| GET | `/dashboard/today` | Due today, overdue, upcoming, special-trade status |

Error format:

```json
{ "status": 400, "error": "VALIDATION_ERROR", "message": "chatLink must start with https://" }
```

## 8. Business Rules

1. `tradeType` sets default `planDays`: IMMEDIATE = 0, DAYS_7 = 7, DAYS_30 = 30. CUSTOM requires `planDays` from the user.
2. `dueDate = dealDate + planDays`. Recomputed on update.
3. Marking a deal DONE sets `completedOn = today`.
4. Only one DONE deal per day may have `specialTrade = true`. A second one is rejected with a clear message.
5. `chatLink` must be a valid `https://` URL, max 500 characters.
6. Default list order: overdue first, then HIGH priority, then nearest `dueDate`.

## 9. Backend Structure

```
com.pokemontracker
  PokemonTrackerApplication.java
  config/
    CorsConfig.java
    SecurityConfig.java        // API key filter for v1
  controller/
    DealController.java
    DashboardController.java
  service/
    DealService.java
    DashboardService.java
  repository/
    DealRepository.java        // Spring Data JPA
  model/
    Deal.java                  // JPA entity
    TradeType.java             // enum
    Priority.java              // enum
    DealStatus.java            // enum
  dto/
    DealRequest.java
    DealResponse.java          // includes daysLeft, overdue
    DashboardResponse.java
    PriorityUpdateRequest.java
    StatusUpdateRequest.java
  exception/
    GlobalExceptionHandler.java
    DealNotFoundException.java
    SpecialTradeAlreadyUsedException.java
  mapper/
    DealMapper.java
src/main/resources
  application.yml
  db/migration/V1__create_deal.sql
src/test/java/com/pokemontracker
  service/DealServiceTest.java
  service/DashboardServiceTest.java
  controller/DealControllerTest.java
  repository/DealRepositoryTest.java
```

### Test plan
- **DealServiceTest:** dueDate calculation for each trade type, special trade limit, status change sets completedOn, invalid chatLink rejected.
- **DashboardServiceTest:** overdue, today and upcoming buckets, daysLeft math around midnight.
- **DealControllerTest (MockMvc):** validation errors, 404 on unknown id, filter and sort params.
- **DealRepositoryTest:** status and dueDate queries return expected rows.

## 10. Frontend Structure

Screens:
- **Today:** overdue deals, due today, special trade used or not, upcoming in 7 days.
- **All Deals:** list with filter chips (priority, type, status, platform) and sort.
- **Add / Edit Deal:** one form. Trade type picker auto-fills days.

Deal card:
- Friend, Pokemon give and get.
- Days-left badge (red when overdue).
- Priority button: tap to cycle HIGH, MEDIUM, LOW.
- Open chat button: opens `chatLink` in a new tab with `rel="noopener noreferrer"`.
- Done and Cancel actions.

```
src/
  api/client.js
  components/DealCard.jsx, PriorityButton.jsx, ChatLinkButton.jsx, FilterBar.jsx
  pages/Today.jsx, Deals.jsx, DealForm.jsx
  App.jsx
```

## 11. Security (v1)

- Single user. Backend requires an `X-API-Key` header. The key lives in a Render environment variable and in the Netlify build environment.
- CORS restricted to the Netlify domain.
- Never store passwords, payment data, or Pokemon GO login details.
- Render the chat link only as an anchor with `noopener noreferrer`. Reject non-`https` links.
- Note: a key in a front-end bundle is visible to anyone who inspects the app. It stops casual access only. If the deal data matters, move to real login (see Future Work).

## 12. Deployment

**Neon**
1. Create a project and a database.
2. Copy the connection string.

**Render (backend)**
1. New Web Service from the GitHub repo.
2. Build: `mvn -DskipTests package`. Start: `java -jar target/*.jar`.
3. Environment variables:
   - `SPRING_DATASOURCE_URL=jdbc:postgresql://<neon-host>/<db>?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `APP_API_KEY`
   - `APP_ALLOWED_ORIGIN=https://<your-site>.netlify.app`

**Netlify (UI)**
1. Connect the repo, build `npm run build`, publish `dist`.
2. Environment variables: `VITE_API_BASE_URL`, `VITE_API_KEY`.

**Free-tier behavior to expect**
- Render free service sleeps when idle. First request can take a while. Show a loading state in the UI.
- Neon scales to zero. First query after idle adds a short delay.
- Use a small Hikari pool (`maximum-pool-size: 3`) and a short `max-lifetime` so stale Neon connections are dropped.

## 13. Build Roadmap

1. **Phase 1 (MVP):** Deal CRUD, priority, trade type with days left, chat link, Today screen.
2. **Phase 2:** Filters, sort, special trade rule, status flow.
3. **Phase 3:** Reminders (browser push or email), PWA install, platform filter.
4. **Phase 4 (optional):** Friend list, stardust cost and lucky trade flag, export to CSV, multi-user login.

## 14. Future Work and Open Questions

- Single user or shared with friends? This decides login (JWT) and a `user_id` column on `deal`.
- Reminder channel: browser push, email, or both.
- Timezone handling for "today" and the daily special trade reset. Default: Asia/Kolkata, stored as a setting.
