# 🧠 AiNote Backend

This is the backend for the **AiNote** project — a Spring Boot application that leverages AI capabilities to process and summarize user notes using the Gemini (Google AI) model. It includes user authentication, note management, and integrations with modern Spring libraries like Spring AI and Hibernate.

---

## 🚀 Features

- ✍️ Note summarization using Google Gemini (via Spring AI)
- 🔐 JWT-based authentication (DurityToken)
- 🧠 Chat-like API endpoint for real-time AI interactions
- 🗃️ PostgreSQL persistence via Spring Data JPA
- 🌐 RESTful API structure
- 📄 Swagger UI for interactive documentation

---

## 🛠 Tech Stack

- **Java 21+**
- **Spring Boot 3.5+**
- **Spring AI** (for Gemini integration)
- **Spring Security** (JWT/DurityToken)
- **Spring Data JPA** (Hibernate)
- **PostgreSQL**
- **Maven**
- **Lombok**
- **Swagger / OpenAPI**
- **H2 (for tests)**

---

## 📦 Requirements

- Java 21+
- Maven 3.8+
- PostgreSQL running locally (or cloud DB)
- Optional: `.env` file with secrets (for local testing)

---

## ⚙️ Installation

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/ainote-backend.git
cd ainote-backend

## 2. Configure the Database and Environment

You can configure database connection and API keys using either a `.env` file or directly via `application.properties`.

### 📄 Option A: Use `.env` file (recommended)

Create a `.env` file in the root of the project:

```env
# Gemini API Key for Google Gemini (used by Spring AI)
GEMINI_API_KEY=your_actual_gemini_api_key_here

# JWT secret key for  authentication
JWT_SECRET=your_secure_jwt_secret_here

# PostgreSQL database connection
DB_URL=jdbc:postgresql://localhost:5432/ainote
DB_USERNAME=postgres
DB_PASSWORD=your_db_password
```

These variables will be used in `application.properties` like this:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.ai.googleai.api-key=${GEMINI_API_KEY}
jwt.secret=${JWT_SECRET}
```

✅ If using IntelliJ, go to `Run → Edit Configurations → Environment variables` and load `.env` entries manually or use a plugin like **.env files support**.

---

### 📄 Option B: Direct values in `application.properties`

Instead of using a `.env` file, you can hardcode values in:

📁 `src/main/resources/application.properties`

```properties
# PostgreSQL settings
spring.datasource.url=jdbc:postgresql://localhost:5432/ainote
spring.datasource.username=postgres
spring.datasource.password=your_db_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA & Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Gemini API via Spring AI
spring.ai.googleai.api-key=your_actual_gemini_api_key_here
spring.ai.openai.chat.base-url=https://generativelanguage.googleapis.com
spring.ai.openai.chat.completions-path=/v1beta/openai/chat/completions
spring.ai.openai.chat.options.model=gemini-2.0-flash

# JWT for DurityToken auth
jwt.secret=your_secure_jwt_secret_here

# Server port
server.port=8080
```

---

## 3. Create PostgreSQL Database

Make sure PostgreSQL is running locally, then create your database:

```sql
CREATE DATABASE ainote;
```

(Optional) Create a new user:

```sql
CREATE USER ainote_user WITH PASSWORD 'your_password';
```

Update your `.env` or `application.properties` accordingly.

---

## 4. Run the App

To build and start the backend server:

```bash
mvn clean spring-boot:run
```
If you set .env , then
```bash
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

The app will be running at:

> 🌐 http://localhost:8080

---

## 5. Swagger UI (API Docs)

Once the app is running, open Swagger UI in your browser:

🔗 http://localhost:8080/swagger-ui/index.html

You can authorize using your JWT token and test endpoints like:

- `/api/chat/string`
- `/api/notes`
