# 🌐 MultiLingual Chat App

A real-time chat application that breaks the language barrier between users — like WhatsApp, but with automatic translation powered by OpenAI.

**The idea:** Ajinkya speaks English, his Barber speaks only Spanish. They can't chat. This app lets them — each user types in their own language, the other receives it in theirs. Automatically. In real time.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.x |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Real-time | WebSocket + STOMP protocol (via SockJS) |
| Translation | OpenAI GPT-4o-mini (Chat Completions API) |
| HTTP Client | Spring `RestClient` (Spring 6.1+) |
| Security | Spring Security 7 + JWT (JJWT 0.12.x) |
| Logging | SLF4J + Logback (Spring Boot default) |
| Build Tool | Maven |

---

## ✅ Features Built So Far

- **User management** — create users, each with a preferred language
- **JWT Authentication** — register, login, token refresh and logout
- **Auth providers** — supports LOCAL (email + password) and GOOGLE OAuth2 (foundation laid)
- **Refresh tokens** — persisted in DB, revocable on logout
- **Message storage** — all messages (original + translated) persisted in PostgreSQL
- **Server-side translation** — OpenAI translates every message automatically; client never handles translation
- **Real-time delivery** — WebSocket (STOMP over SockJS) pushes messages instantly to receiver
- **REST API** — traditional HTTP endpoints available alongside WebSocket
- **Logging** — structured SLF4J logging across all service layers
- **Secrets management** — all secrets via `.env` file, nothing hardcoded
- **Browser test client** — WhatsApp-style HTML page, no frontend framework needed

---

## 🏗️ Project Structure

```
MutiLingual Chat App/
├── backend/
│   └── chat-app/
│       ├── .env                                      # ⚠️ Local secrets (never committed)
│       ├── .gitignore
│       └── src/main/java/com/multilingual/chat/app/
│           ├── config/
│           │   ├── SecurityConfig.java               # Spring Security filter chain + JWT wiring
│           │   └── WebSocketConfig.java              # STOMP broker + endpoint setup
│           ├── controller/
│           │   ├── AuthController.java               # /auth/register, login, refresh, logout
│           │   ├── ChatWebSocketController.java      # WebSocket message handler
│           │   ├── MessageController.java            # REST: send message, chat history
│           │   ├── UserController.java               # REST: create/get users
│           │   └── TestController.java               # Health check endpoint
│           ├── security/
│           │   ├── JwtService.java                   # JWT generation & validation (JJWT)
│           │   ├── JwtAuthFilter.java                # Per-request JWT filter (OncePerRequestFilter)
│           │   └── UserDetailsServiceImpl.java       # Bridges User entity ↔ Spring Security
│           ├── service/
│           │   ├── AuthService.java                  # Register, login, refresh, logout logic
│           │   ├── TranslationService.java           # Interface
│           │   ├── MessageService.java               # Core message + translation logic
│           │   ├── UserService.java                  # User CRUD logic
│           │   └── impl/
│           │       ├── OpenAiTranslationServiceImpl.java  # Real OpenAI integration (@Primary)
│           │       └── MockTranslationServiceImpl.java    # Stub for testing
│           ├── repository/
│           │   ├── MessageRepository.java            # JPA + custom JPQL chat history query
│           │   ├── RefreshTokenRepository.java       # Token lookup + revocation
│           │   └── UserRepository.java
│           ├── entity/
│           │   ├── AuthProvider.java                 # Enum: LOCAL / GOOGLE
│           │   ├── Message.java                      # Messages table
│           │   ├── RefreshToken.java                 # Refresh tokens table
│           │   └── User.java                         # Users table
│           ├── dto/
│           │   ├── AuthResponseDto.java              # accessToken + refreshToken response
│           │   ├── LoginRequestDto.java              # email + password
│           │   ├── RegisterRequestDto.java           # name, email, password, language
│           │   ├── RefreshTokenRequestDto.java       # refreshToken string
│           │   ├── SendMessageRequestDto.java        # Incoming message payload
│           │   ├── MessageResponseDto.java           # Outgoing message payload
│           │   └── ChatHistoryResponseDto.java
│           └── exception/
│               └── GlobalExceptionHandler.java       # Centralized error handling
├── frontend/
│   └── chat-test.html                                # Browser WebSocket test client
├── DB Files/
│   ├── initial_DB_setup.sql                          # PostgreSQL user + database creation
│   └── auth_migration.sql                            # Auth columns + refresh_tokens table
└── README.md
```

---

## ⚙️ Setup & Running

### 1. Prerequisites
- Java 21+
- Maven
- PostgreSQL running locally
- OpenAI API key

### 2. Database setup
Run this once in pgAdmin or psql to create the DB user and database:
```sql
CREATE USER chatapp_user WITH PASSWORD 'your-password';
CREATE DATABASE chatapp OWNER chatapp_user;
GRANT ALL PRIVILEGES ON DATABASE chatapp TO chatapp_user;
GRANT ALL ON SCHEMA public TO chatapp_user;
```
Then start the app — Hibernate automatically creates all tables (`users`, `messages`, `refresh_tokens`) on first run via `ddl-auto=update`.

> **Note:** `auth_migration.sql` is only needed if you have an existing `users` table from before the auth phase. For a fresh database, Hibernate handles everything.

### 3. Create your `.env` file
```bash
cd backend/chat-app
cp .env.example .env   # or create manually
```

Fill in `backend/chat-app/.env`:
```
JWT_SECRET=<run: openssl rand -base64 32>
OPENAI_API_KEY=sk-your-openai-key-here
DB_PASSWORD=your-db-password-here
```

> `.env` is in `.gitignore` — it will never be committed. Never hardcode secrets in `application.properties`.

### 4. Start the server
```bash
cd backend/chat-app
source .env && ./mvnw spring-boot:run
```

Server starts at `http://localhost:8080`

---

## 🔐 Authentication Flow

```
POST /auth/register  →  hash password → save user → issue access + refresh token
POST /auth/login     →  verify credentials → issue access + refresh token
POST /auth/refresh   →  validate refresh token from DB → issue new access token
POST /auth/logout    →  revoke refresh token in DB

Every protected request:
  Authorization: Bearer <accessToken>
        │
        ▼
  JwtAuthFilter → validate signature + expiry → set SecurityContext
        │
        ▼
  Controller proceeds
```

**Token lifetimes:**
- Access token: **15 minutes** (stateless JWT, cannot be revoked)
- Refresh token: **7 days** (stored in DB, can be revoked on logout)

---

## 🌐 How the Message Flow Works

```
User A types "Hello"
        │
        ▼
ChatWebSocketController  (receives via WebSocket /app/chat.send)
        │
        ▼
MessageService.sendMessage()
        │
        ├─ Look up sender & receiver from DB
        │
        ├─ TranslationService.isTranslationRequired()?
        │       YES → OpenAiTranslationServiceImpl.translate()
        │               └─ POST https://api.openai.com/v1/chat/completions
        │               └─ Returns "Hola"
        │       NO  → use original text as-is
        │
        ├─ Save Message (originalText + translatedText) to PostgreSQL
        │
        └─ SimpMessagingTemplate.push to:
                /topic/user.{receiverId}  → receiver gets translated text
                /topic/user.{senderId}    → sender gets confirmation with saved ID
```

---

## 📡 API Reference

### Auth Endpoints (public — no token required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/register` | Create account → returns tokens |
| `POST` | `/auth/login` | Login → returns tokens |
| `POST` | `/auth/refresh` | Get new access token using refresh token |
| `POST` | `/auth/logout` | Revoke refresh token |

**Register:**
```json
POST /auth/register
{
  "name": "Ajinkya",
  "email": "ajinkya@example.com",
  "password": "password123",
  "preferredLanguage": "English"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "550e8400-e29b-...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "ajinkya@example.com",
  "name": "Ajinkya"
}
```

---

### Protected Endpoints (require `Authorization: Bearer <accessToken>`)

#### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/users` | Get all users |
| `GET` | `/api/users/{id}` | Get user by ID |
| `GET` | `/test` | Health check |

#### Messages
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/messages/send` | Send a message (REST) |
| `GET` | `/api/messages/history?user1Id=1&user2Id=2` | Get chat history |

**Send message:**
```json
POST /api/messages/send
Authorization: Bearer <accessToken>

{
  "senderId": 1,
  "receiverId": 2,
  "originalText": "Hello, how are you?",
  "originalLanguage": "English",
  "targetLanguage": "Spanish"
}
```

---

### WebSocket (STOMP)

**Connect endpoint:** `ws://localhost:8080/ws` (SockJS fallback included)

| Direction | Destination | Purpose |
|---|---|---|
| Client → Server | `/app/chat.send` | Send a message |
| Server → Client | `/topic/user.{userId}` | Receive messages |

---

## 🧪 Testing with the Browser Client

```
http://localhost:8080/chat-test.html
```

1. Open in **Tab 1** → connect as User ID `1` (English)
2. Open in **Tab 2** → connect as User ID `2` (Spanish)
3. Send a message from Tab 1 → Tab 2 receives the Spanish translation instantly

**Test with a friend on the same WiFi:**
- Find your IP: `ipconfig getifaddr en0` (Mac)
- Share: `http://<your-ip>:8080/chat-test.html`
- The WebSocket URL auto-detects from the page origin — no config needed

---

## 🔑 Key Design Decisions

### Why JWT over sessions?
Sessions require server-side state — a problem when scaling to multiple servers. JWT is stateless: the token carries all needed info, verified by signature math alone. No DB lookup needed per request.

### Why two tokens (access + refresh)?
Access tokens are short-lived (15 min) and stateless — cannot be revoked. Refresh tokens are long-lived and stored in DB — can be revoked on logout. This balances security with user convenience.

### Why `@Primary` on `OpenAiTranslationServiceImpl`?
Both `OpenAiTranslationServiceImpl` and `MockTranslationServiceImpl` implement `TranslationService`. `@Primary` marks the real one as the default while the mock stays available for unit tests via `@Qualifier`.

### Why remove `translatedText` from the request DTO?
Originally the client sent the translated text — defeating the purpose. The server now owns translation entirely. Client sends `originalText` + languages, gets back `translatedText`.

### Why keep REST endpoints alongside WebSocket?
REST is useful for Postman testing, non-real-time clients, and chat history retrieval. WebSocket is for live messaging. Both reuse the same `MessageService` — no logic duplication.

### Why `.env` file for secrets?
`application.properties` is committed to Git. Hardcoding secrets there exposes them publicly. The `.env` file is in `.gitignore` — secrets stay on your machine only.

---

## 🗺️ Roadmap (What's Next)

- [x] User authentication (Spring Security + JWT)
- [x] Refresh token management
- [ ] Google OAuth2 login (foundation already in `AuthProvider` enum + `User` entity)
- [ ] Language auto-detection (detect source language without user specifying it)
- [ ] Typing indicators over WebSocket
- [ ] Message read receipts
- [ ] Support for group chats
- [ ] Swap in-memory STOMP broker for Redis/RabbitMQ (production scale)
- [ ] Proper React/Vue frontend
- [ ] Containerize with Docker

---

## 👨‍💻 Developer

**Ajinkya Ambadkar** — Built during Masters (2024–) to stay sharp with Java + Spring Boot while exploring LLM integration and real-time systems.
