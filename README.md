# 🌐 MultiLingual Chat App

A real-time chat application that breaks the language barrier — like WhatsApp, but with automatic translation powered by OpenAI.

**The idea:** Ajinkya speaks English, his friend Carlos speaks only Spanish. They can't chat. This app lets them — each user sets their preferred language once, and every message they receive is automatically translated into it. In real time.

---

## 🛠️ Tech Stack

### Backend
| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.x (Spring Framework 7.x) |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate 7 |
| Real-time | WebSocket + STOMP protocol (via SockJS) |
| Translation | OpenAI GPT-4o-mini (Chat Completions API) |
| HTTP Client | Spring `RestClient` (Spring 6.1+) |
| Security | Spring Security 7 + JWT (JJWT 0.12.x) |
| OAuth2 | Google Identity Services (frontend-initiated flow) |
| Logging | SLF4J + Logback |
| Build | Maven |

### Frontend
| Layer | Technology |
|---|---|
| Framework | React 19 (Vite) |
| Styling | Tailwind CSS v4 + inline styles |
| WebSocket | STOMP.js + SockJS client |

---

## ✅ Features

- **JWT Authentication** — register, login, refresh token, logout; BCrypt password hashing
- **Google OAuth2 Login** — frontend-initiated flow; server verifies Google ID token, issues own JWT pair
- **Language Preference** — each user sets their preferred language once; all incoming messages are translated into it automatically
- **Smart Translation** — OpenAI is called **only** when sender and receiver speak different languages; same-language chats have zero API cost
- **Real-time Messaging** — WebSocket (STOMP over SockJS) pushes messages instantly to both sender and receiver
- **Conversation List** — sidebar shows all conversations with last message preview and timestamp
- **User Discovery** — "New Chat" modal lists all registered users with live search; click to open a chat
- **Secure WebSocket** — JWT validated on STOMP CONNECT frame, not just HTTP; sender identity derived from JWT, never from client payload
- **Profile Picture** — stored from Google accounts; initials avatar fallback for email users
- **Message History** — full chat history loads when opening a conversation
- **Session Persistence** — login state survives page refresh via localStorage
- **React Frontend** — full UI with login/register page, sidebar, chat window, message bubbles

---

## 🏗️ Project Structure

```
MutiLingual Chat App/
├── backend/chat-app/
│   ├── .env                                          # ⚠️ Local secrets (never committed)
│   └── src/main/java/com/multilingual/chat/app/
│       ├── config/
│       │   ├── SecurityConfig.java                   # Spring Security filter chain
│       │   └── WebSocketConfig.java                  # STOMP broker + JwtChannelInterceptor
│       ├── controller/
│       │   ├── AuthController.java                   # /auth/register, login, refresh, logout, google
│       │   ├── ChatWebSocketController.java          # @MessageMapping /chat.send
│       │   ├── MessageController.java                # REST: send, history, conversations
│       │   └── UserController.java                   # REST: users, /me, language update
│       ├── security/
│       │   ├── JwtService.java                       # Token generation & validation
│       │   ├── JwtAuthFilter.java                    # HTTP request JWT filter
│       │   ├── JwtChannelInterceptor.java            # STOMP CONNECT frame JWT validator
│       │   └── UserDetailsServiceImpl.java
│       ├── service/
│       │   ├── AuthService.java                      # Auth logic + Google token verification
│       │   ├── MessageService.java                   # Message + translation + conversation logic
│       │   ├── UserService.java                      # User CRUD + language update
│       │   ├── TranslationService.java               # Interface
│       │   └── impl/
│       │       ├── OpenAiTranslationServiceImpl.java # Real OpenAI integration (@Primary)
│       │       └── MockTranslationServiceImpl.java   # Stub for testing
│       ├── repository/
│       │   ├── MessageRepository.java                # Chat history + conversation list queries
│       │   ├── UserRepository.java
│       │   └── RefreshTokenRepository.java
│       ├── entity/
│       │   ├── User.java                             # id, name, email, passwordHash, provider, preferredLanguage, pictureUrl
│       │   ├── Message.java                          # originalText, translatedText, originalLanguage, targetLanguage
│       │   ├── RefreshToken.java
│       │   └── AuthProvider.java                     # Enum: LOCAL / GOOGLE
│       └── dto/
│           ├── AuthResponseDto.java                  # accessToken, refreshToken, userId, email, name
│           ├── GoogleLoginRequestDto.java            # idToken (from Google Identity Services)
│           ├── SendMessageRequestDto.java            # receiverId + originalText only
│           ├── MessageResponseDto.java               # Full message with senderId, translatedText etc.
│           ├── ConversationDto.java                  # Sidebar entry: partner info + last message
│           ├── UserSummaryDto.java                   # Safe public user info (no passwordHash)
│           ├── RegisterRequestDto.java
│           ├── LoginRequestDto.java
│           └── RefreshTokenRequestDto.java
├── frontend/
│   ├── src/
│   │   ├── api/                    # auth.js, users.js, messages.js
│   │   ├── context/                # AuthContext.jsx, ChatContext.jsx
│   │   ├── hooks/                  # useWebSocket.js
│   │   ├── components/
│   │   │   ├── Auth/               # AuthPage.jsx (login + register + Google)
│   │   │   ├── Sidebar/            # Sidebar, ConversationItem, NewChatModal
│   │   │   └── Chat/               # ChatWindow, MessageBubble, MessageInput
│   │   ├── utils/time.js
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── vite.config.js              # Tailwind plugin + proxy to :8080
│   └── package.json
├── DB Files/
│   ├── initial_DB_setup.sql        # PostgreSQL user + database creation
│   └── auth_migration.sql          # Auth columns + refresh_tokens table
└── README.md
```

---

## ⚙️ Setup & Running

### Prerequisites
- Java 21+
- Maven
- PostgreSQL running locally
- OpenAI API key
- Google Cloud OAuth2 Client ID (Web application type)
- Node.js 18+ (for frontend)

### 1. Database setup
```sql
CREATE USER chatapp_user WITH PASSWORD 'your-password';
CREATE DATABASE chatapp OWNER chatapp_user;
GRANT ALL PRIVILEGES ON DATABASE chatapp TO chatapp_user;
GRANT ALL ON SCHEMA public TO chatapp_user;
```
Hibernate auto-creates all tables on first run via `ddl-auto=update`.

### 2. Create `.env` file
```bash
cd backend/chat-app
```

`backend/chat-app/.env`:
```
JWT_SECRET=<run: openssl rand -base64 32>
OPENAI_API_KEY=sk-your-openai-key-here
DB_PASSWORD=your-db-password-here
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
```

> `.env` is in `.gitignore` and will never be committed.

### 3. Google Cloud setup
1. Go to [Google Cloud Console](https://console.cloud.google.com) → APIs & Services → Credentials
2. Create an OAuth 2.0 Client ID (Web application type)
3. Add `http://localhost:3000` to **Authorized JavaScript origins**
4. Copy the Client ID into `.env` and into `frontend/src/components/Auth/AuthPage.jsx`

### 4. Start the backend
```bash
cd backend/chat-app
source .env && ./mvnw spring-boot:run
```
Backend starts at `http://localhost:8080`

### 5. Start the frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend starts at `http://localhost:3000`

---

## 🔐 Authentication

### Email / Password
```
POST /auth/register  →  hash password → save user → issue JWT pair
POST /auth/login     →  verify credentials → issue JWT pair
POST /auth/refresh   →  validate refresh token → issue new access token
POST /auth/logout    →  revoke refresh token in DB
```

### Google OAuth2 (frontend-initiated)
```
Browser → Google Identity Services JS SDK → Google ID Token
       → POST /auth/google { idToken }
       → Server verifies token with Google tokeninfo endpoint
       → Checks aud == GOOGLE_CLIENT_ID, email_verified == true
       → Find or create User (provider=GOOGLE)
       → Issue same JWT pair as normal login
```

**Token lifetimes:**
- Access token: **15 minutes** (stateless, cannot be revoked)
- Refresh token: **7 days** (stored in DB, revoked on logout)

---

## 🌐 Message Flow

```
User A types "Hello"
        │
        ▼
ChatWebSocketController  (/app/chat.send)
        │
        ▼
MessageService.sendMessage(dto, senderEmail)
        │
        ├─ Look up sender by email from JWT (cannot be forged)
        ├─ Look up receiver by receiverId
        │
        ├─ senderLang = sender.preferredLanguage  (from DB)
        │  receiverLang = receiver.preferredLanguage (from DB)
        │
        ├─ senderLang == receiverLang?
        │       YES → skip OpenAI, use originalText as-is  💰
        │       NO  → call OpenAI to translate             💸
        │
        ├─ Save Message to PostgreSQL
        │
        └─ Push MessageResponseDto to:
                /topic/user.{receiverId}  → receiver sees translated text
                /topic/user.{senderId}    → sender gets server-assigned ID + timestamp
```

---

## 📡 API Reference

### Auth (public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/register` | Register with email + password |
| `POST` | `/auth/login` | Login → returns JWT pair |
| `POST` | `/auth/google` | Login with Google ID token |
| `POST` | `/auth/refresh` | Get new access token |
| `POST` | `/auth/logout` | Revoke refresh token |

### Users (require Bearer token)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/users` | List all users (safe — no passwords) |
| `GET` | `/api/users/{id}` | Get user by ID |
| `GET` | `/api/users/me` | Get current user's profile |
| `PATCH` | `/api/users/me/language` | Update preferred language |

### Messages (require Bearer token)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/messages/send` | Send a message (REST) |
| `GET` | `/api/messages/history?user1Id=&user2Id=` | Chat history |
| `GET` | `/api/messages/conversations` | Sidebar conversation list |

### WebSocket (STOMP)

**Connect:** `ws://localhost:8080/ws` (SockJS)
Pass JWT in STOMP CONNECT headers: `Authorization: Bearer <token>`

| Direction | Destination | Purpose |
|---|---|---|
| Client → Server | `/app/chat.send` | Send `{ receiverId, originalText }` |
| Server → Client | `/topic/user.{userId}` | Receive `MessageResponseDto` |

---

## 🔑 Key Design Decisions

**Why language is stored on the user, not sent per message**
Per-message language selection is friction. Every user sets their preferred language once in their profile. The server reads both users' preferences on every message and decides whether translation is needed — the client never has to think about it.

**Why OpenAI is only called when languages differ**
Translation costs tokens. Two English speakers chatting would waste money on a no-op translation. The server compares sender and receiver languages first; if they match, the original text is delivered as-is.

**Why senderId is not in the message payload**
Any client could forge `senderId: 5` and send messages impersonating another user. The server derives the sender's identity exclusively from the JWT Principal set at STOMP CONNECT time — the client cannot override it.

**Why Google login uses the frontend-initiated flow**
Spring Security's server-side OAuth2 flow is designed around browser redirects and sessions. This app is JWT-based and stateless. The frontend-initiated flow (Google signs in, sends an ID token, server verifies it) maps cleanly onto the existing auth architecture without fighting the framework.

**Why JWT over sessions?**
Sessions require server-side state — a scaling problem. JWT is stateless: every token carries its own proof of authenticity, verified by signature math alone. No DB lookup per request.

---

## 🗺️ Roadmap

- [x] User management — CRUD, preferred language
- [x] JWT auth — register, login, refresh, logout
- [x] OpenAI server-side translation
- [x] WebSocket + STOMP real-time messaging
- [x] Secure WebSocket — JWT on STOMP CONNECT, sender derived from Principal
- [x] Google OAuth2 login (frontend-initiated)
- [x] Language preference system — server derives languages from profiles, smart OpenAI cost control
- [x] React + Tailwind frontend — login, sidebar, chat window, new chat modal
- [x] Conversation list API
- [ ] Typing indicators over WebSocket
- [ ] Message read receipts
- [ ] User profile / settings page (change language, display name)
- [ ] Swap in-memory STOMP broker for Redis/RabbitMQ (production scale)
- [ ] Docker containerization
- [ ] Group chat support

---

## 👨‍💻 Developer

**Ajinkya Ambadkar** — Built during Masters (2024–) to stay sharp with Java + Spring Boot while exploring real-time systems and LLM integration.
