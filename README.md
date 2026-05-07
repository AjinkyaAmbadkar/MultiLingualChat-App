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
| Logging | SLF4J + Logback (Spring Boot default) |
| Build Tool | Maven |

---

## ✅ Features Built So Far

- **User management** — create users, each with a preferred language
- **Message storage** — all messages (original + translated) persisted in PostgreSQL
- **Server-side translation** — OpenAI translates every message automatically; client never handles translation
- **Real-time delivery** — WebSocket (STOMP over SockJS) pushes messages instantly to receiver
- **REST API** — traditional HTTP endpoints still available alongside WebSocket
- **Logging** — structured SLF4J logging across all service layers
- **Browser test client** — WhatsApp-style HTML page, no frontend framework needed

---

## 🏗️ Project Structure

```
MutiLingual Chat App/
├── backend/
│   └── chat-app/
│       └── src/main/java/com/multilingual/chat/app/
│           ├── config/
│           │   └── WebSocketConfig.java          # STOMP broker + endpoint setup
│           ├── controller/
│           │   ├── ChatWebSocketController.java  # WebSocket message handler
│           │   ├── MessageController.java         # REST: send message, chat history
│           │   ├── UserController.java            # REST: create/get users
│           │   └── TestController.java            # Health check endpoint
│           ├── service/
│           │   ├── TranslationService.java        # Interface
│           │   ├── MessageService.java            # Core message + translation logic
│           │   ├── UserService.java               # User CRUD logic
│           │   └── impl/
│           │       ├── OpenAiTranslationServiceImpl.java  # Real OpenAI integration (@Primary)
│           │       └── MockTranslationServiceImpl.java    # Stub for testing
│           ├── repository/
│           │   ├── MessageRepository.java         # JPA + custom JPQL chat history query
│           │   └── UserRepository.java
│           ├── entity/
│           │   ├── Message.java                   # Messages table
│           │   └── User.java                      # Users table
│           ├── dto/
│           │   ├── SendMessageRequestDto.java     # Incoming message payload
│           │   ├── MessageResponseDto.java        # Outgoing message payload
│           │   └── ChatHistoryResponseDto.java
│           └── exception/
│               └── GlobalExceptionHandler.java    # Centralized error handling
├── frontend/
│   └── chat-test.html                             # Browser WebSocket test client
├── DB Files/
│   └── initial_DB_setup.sql                       # PostgreSQL setup script
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
Run this once to create the DB user and database:
```sql
-- From DB Files/initial_DB_setup.sql
CREATE USER chatapp_user WITH PASSWORD 'Alpha#GAmma@123';
CREATE DATABASE chatapp OWNER chatapp_user;
GRANT ALL PRIVILEGES ON DATABASE chatapp TO chatapp_user;
```

### 3. Set your OpenAI API key
**Never hardcode secrets.** Set it as an environment variable:

```bash
# macOS / Linux
export OPENAI_API_KEY=sk-your-key-here

# Windows (PowerShell)
$env:OPENAI_API_KEY="sk-your-key-here"
```

Or in **IntelliJ**: Run → Edit Configurations → Environment variables → add `OPENAI_API_KEY=sk-...`

### 4. Start the server
```bash
cd backend/chat-app
./mvnw spring-boot:run
```

Server starts at `http://localhost:8080`

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

### REST Endpoints

#### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/users` | Create a new user |
| `GET` | `/api/users` | Get all users |
| `GET` | `/api/users/{id}` | Get user by ID |
| `GET` | `/test` | Health check |

**Create user example:**
```json
POST /api/users
{
  "name": "Ajinkya",
  "email": "ajinkya@example.com",
  "preferredLanguage": "English"
}
```

#### Messages
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/messages/send` | Send a message (REST) |
| `GET` | `/api/messages/history?user1Id=1&user2Id=2` | Get chat history |

**Send message example:**
```json
POST /api/messages/send
{
  "senderId": 1,
  "receiverId": 2,
  "originalText": "Hello, how are you?",
  "originalLanguage": "English",
  "targetLanguage": "Spanish"
}
```

**Response:**
```json
{
  "id": 5,
  "senderId": 1,
  "receiverId": 2,
  "originalText": "Hello, how are you?",
  "translatedText": "Hola, ¿cómo estás?",
  "originalLanguage": "English",
  "targetLanguage": "Spanish",
  "timestamp": "2026-05-06T10:30:00"
}
```

---

### WebSocket (STOMP)

**Connect endpoint:** `ws://localhost:8080/ws` (SockJS fallback included)

| Direction | Destination | Purpose |
|---|---|---|
| Client → Server | `/app/chat.send` | Send a message |
| Server → Client | `/topic/user.{userId}` | Receive messages |

**Subscribe (JavaScript):**
```javascript
stompClient.subscribe('/topic/user.1', (frame) => {
  const message = JSON.parse(frame.body); // MessageResponseDto
});
```

**Publish (JavaScript):**
```javascript
stompClient.publish({
  destination: '/app/chat.send',
  body: JSON.stringify({
    senderId: 1,
    receiverId: 2,
    originalText: "Hello!",
    originalLanguage: "English",
    targetLanguage: "Spanish"
  })
});
```

---

## 🧪 Testing with the Browser Client

The app ships with a ready-made chat UI at:
```
http://localhost:8080/chat-test.html
```

**To simulate two users chatting:**
1. Open the URL in **Tab 1** → connect as User ID `1` (English)
2. Open the URL in **Tab 2** → connect as User ID `2` (Spanish)
3. Send a message from Tab 1 → Tab 2 receives the Spanish translation instantly

**To test with a friend on the same WiFi:**
- Find your IP: `ipconfig getifaddr en0` (Mac)
- Share: `http://<your-ip>:8080/chat-test.html`
- The WebSocket URL auto-detects from the page origin — no manual config needed

---

## 🔑 Key Design Decisions

### Why `@Primary` on `OpenAiTranslationServiceImpl`?
Both `OpenAiTranslationServiceImpl` and `MockTranslationServiceImpl` implement `TranslationService`. Spring needs to know which to inject. `@Primary` marks the real one as the default, while the mock stays available for unit tests via `@Qualifier`.

### Why remove `translatedText` from the request DTO?
Originally the client was expected to send the translated text — which defeats the purpose. The server now owns translation entirely. The client sends `originalText` + languages, and gets back `translatedText`.

### Why keep REST endpoints alongside WebSocket?
REST is useful for: testing with Postman/curl, building non-real-time clients, chat history retrieval. WebSocket is for live messaging. Both reuse the exact same `MessageService` — no logic duplication.

### Why `server.address=0.0.0.0`?
Spring Boot defaults to `127.0.0.1` (localhost only). `0.0.0.0` tells it to accept connections on all network interfaces, making it reachable from other devices on the same WiFi.

---

## 🗺️ Roadmap (What's Next)

- [ ] User authentication (Spring Security + JWT)
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
