# NeuroBalance Backend — Полная Техническая Документация

> Версия: 3.0 · Дата: 2026-06-07 · Для диплома и разработки

---

## СОДЕРЖАНИЕ

**Часть I — Архитектура системы**
- [1. Обзор и контекст проекта](#1-обзор-и-контекст-проекта)
- [2. Микросервисная архитектура](#2-микросервисная-архитектура)
- [3. Технологический стек](#3-технологический-стек)
- [4. Как запустить проект](#4-как-запустить-проект)

**Часть II — Сервисы: API и бизнес-логика**
- [5. NBAuthService — Аутентификация и пользователи (порт 8081)](#5-nbauthservice--аутентификация-и-пользователи-порт-8081)
- [6. NBCheckinService — Чекины и данные здоровья (порт 8082)](#6-nbcheckinservice--чекины-и-данные-здоровья-порт-8082)
- [7. NoteAI-backend — Заметки с AI (порт 8083)](#7-noteai-backend--заметки-с-ai-порт-8083)

**Часть III — Machine Learning**
- [8. ML Service — Архитектура и роль](#8-ml-service--архитектура-и-роль)
- [9. Датасет и признаки](#9-датасет-и-признаки)
- [10. Модели: обучение и параметры](#10-модели-обучение-и-параметры)
- [11. Метрики качества моделей](#11-метрики-качества-моделей)
- [12. Формулы Health Metrics (M-Rest / M-Ready / M-Balance)](#12-формулы-health-metrics-m-rest--m-ready--m-balance)
- [13. Почему XGBoost и CatBoost лучше альтернатив](#13-почему-xgboost-и-catboost-лучше-альтернатив)
- [14. Как ML-сервис интегрирован в систему](#14-как-ml-сервис-интегрирован-в-систему)

**Часть IV — Инфраструктура**
- [15. Kafka — Асинхронные события](#15-kafka--асинхронные-события)
- [16. Базы данных](#16-базы-данных)
- [17. JWT и безопасность](#17-jwt-и-безопасность)

**Часть V — Игровая система**
- [18. XP, персонажи, стрики, бейджи](#18-xp-персонажи-стрики-бейджи)

**Часть VI — Полные сценарии использования**
- [19. Примеры флоу от начала до конца](#19-примеры-флоу-от-начала-до-конца)

**Часть VII — Для защиты диплома**
- [20. Что рассказать на защите: бэкенд](#20-что-рассказать-на-защите-бэкенд)
- [21. Что рассказать на защите: ML часть](#21-что-рассказать-на-защите-ml-часть)
- [22. Сравнение с аналогами и научное обоснование](#22-сравнение-с-аналогами-и-научное-обоснование)

---

---

# ЧАСТЬ I — АРХИТЕКТУРА СИСТЕМЫ

---

## 1. Обзор и контекст проекта

NeuroBalance — это мобильное приложение для iOS, которое ежедневно отслеживает когнитивное благополучие пользователя. Каждое утро пользователь проходит короткий чекин (настроение, сон, стресс, физическая активность), после чего система автоматически:

- вычисляет три метрики здоровья: **M-Rest**, **M-Ready**, **M-Balance**
- предсказывает **когнитивный скор** (0–100) через ML-модели
- генерирует **персонализированные рекомендации** на основе реальных данных пользователя
- обновляет **геймификацию**: XP, стрик, уровень персонажа

Система закрывает задокументированный рыночный пробел: фитнес-трекеры измеряют физическую активность, но ни одно массовое приложение не предоставляет непрерывный, ML-управляемый индекс когнитивного благополучия из данных ежедневного самоотчёта.

**Целевая аудитория:** работающие специалисты и студенты 22–40 лет, испытывающие стресс, проблемы со сном или снижение концентрации.

---

## 2. Микросервисная архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│                     iOS ПРИЛОЖЕНИЕ (SwiftUI)                     │
│              React Native / Flutter — фронтенд                   │
└────────┬──────────────┬───────────────┬────────────────────────┘
         │              │               │
         ▼              ▼               ▼
  ┌────────────┐  ┌─────────────┐  ┌────────────┐
  │NBAuthService│  │NBCheckin    │  │NoteAI      │
  │  :8081      │  │Service      │  │backend     │
  │             │  │  :8082      │  │  :8083     │
  │ PostgreSQL  │  │ PostgreSQL  │  │ PostgreSQL │
  │ (auth_db)   │  │(checkin_db) │  │(noteai_db) │
  └────────────┘  └─────┬───────┘  └─────┬──────┘
                        │                │
              ┌─────────▼──────┐         │
              │  ML Service     │         │
              │   :5001 Flask   │         │
              │  XGBoost +      │         │
              │  CatBoost       │         │
              └─────────────────┘         │
                        │                 │
                        ▼                 ▼
              ┌──────────────────────────────┐
              │       Apache Kafka           │
              │  :9092 (internal Docker)     │
              │  :29092 (external)           │
              └──────────────────────────────┘
```

### Сервисы и их роли

| Сервис | Порт | База данных | Роль |
|---|---|---|---|
| **NBAuthService** | 8081 | `auth_db` (PostgreSQL) | Регистрация, вход, JWT, онбординг, профиль пользователя |
| **NBCheckinService** | 8082 | `checkin_db` (PostgreSQL) | Ежедневные чекины, сон, настроение, игры, XP, ML-рекомендации |
| **NoteAI-backend** | 8083 | `noteai_db` (PostgreSQL) | Заметки, дневник, AI-анализ через Groq LLM |
| **ML Service** | 5001 | — (in-memory cache) | XGBoost + CatBoost: когнитивный скор, рекомендации, M-Rest/M-Ready/M-Balance |
| **Kafka** | 29092 | — | Асинхронный обмен событиями (расцепление сервисов) |
| **Zookeeper** | 2181 | — | Координация Kafka |

### Принцип разделения ответственности

Каждый сервис имеет **собственную изолированную базу данных** (Database-per-Service pattern, Newman 2015). Это:

- устраняет coupling по схеме данных
- позволяет каждому сервису развиваться независимо
- перекрёстные ссылки (например, `userId` в `checkin_db`) — мягкие ссылки без FK-constraints через базы

---

## 3. Технологический стек

### Бэкенд (Java)

| Компонент | Версия | Зачем |
|---|---|---|
| **Spring Boot** | 4.0.2 | Конвенции, embedded Tomcat, DI, auto-configuration |
| **Java** | 17 (LTS) | Sealed classes, pattern matching, G1 GC с малыми паузами |
| **Spring Security 6** | 6.x | Stateless JWT, BCrypt пароли (cost factor 10) |
| **Spring Data JPA / Hibernate** | 6.x | ORM, репозитории, JPQL запросы |
| **HikariCP** | — | Пул соединений (max pool = 10, timeout 30 сек) |
| **Apache Kafka** | 7.6 | Асинхронное расцепление сервисов |
| **springdoc-openapi** | 2.8.8 | Swagger UI автогенерация на /swagger-ui.html |
| **JJWT** | 0.12.6 | Создание и валидация JWT (HS256) |
| **Spring AI** | 1.0.0-M6 | Groq API для NoteAI (Llama 3.3-70b) |

### ML Service (Python)

| Компонент | Версия | Зачем |
|---|---|---|
| **Python** | 3.11 | ML runtime |
| **Flask** | 3.0 | REST API для ML inference |
| **XGBoost** | 1.7.6 | Регрессия когнитивного скора + классификация состояния |
| **CatBoost** | 1.2 | Ранжирование рекомендаций |
| **scikit-learn** | 1.3.2 | StandardScaler, LabelEncoder, метрики |
| **Flasgger** | 0.9.7 | Swagger UI для ML-сервиса |
| **SHAP** | 0.45 | Интерпретируемость важности признаков |
| **joblib** | 1.3 | Сериализация моделей (.pkl файлы) |

### iOS Frontend

| Компонент | Описание |
|---|---|
| **Swift 5.9 + SwiftUI** | Декларативный UI, MVVM, iOS 16+ |
| **URLSession async/await** | Асинхронные сетевые запросы |
| **Keychain API** | Хранение JWT (шифрование через Secure Enclave) |
| **Combine** | Реактивная валидация форм с debounce |
| **Core Data + UserDefaults** | Offline-first кэш |

### Инфраструктура

| Компонент | Описание |
|---|---|
| **Docker Compose** | 10 контейнеров: 3 Spring Boot + 3 PostgreSQL + ML + Kafka + Zookeeper + Swagger |
| **DigitalOcean Droplet** | Production деплой |
| **pgAdmin** | Веб-UI для PostgreSQL (порт 5050) |

---

## 4. Как запустить проект

### Требования

- Docker Desktop >= 4.x
- Java 21 (для локальной разработки без Docker)
- Python 3.10+ (для локальной разработки ML сервиса)

### Быстрый запуск

```bash
# 1. Скопировать .env файл
cp .env.example .env

# 2. Заполнить переменные (JWT_SECRET, пароли БД, GROQ_API_KEY и т.д.)
nano .env

# 3. Запустить все сервисы
docker-compose up --build -d

# 4. Проверить статус
docker-compose ps
```

### Порядок запуска контейнеров (Docker зависимости)

```
zookeeper → kafka
postgres → nbauthservice
postgres-checkin → nb-checkin-service
ml-service → nb-checkin-service
kafka → nb-checkin-service
postgres-noteai → noteai-service
nbauthservice → nb-checkin-service, noteai-service
nb-checkin-service → noteai-service
```

### Переменные окружения (.env)

```bash
# Auth DB
AUTH_DB_NAME=auth_db
AUTH_DB_USER=auth_user
AUTH_DB_PASSWORD=secret
AUTH_DB_PORT=5432

# CheckIn DB
CHECKIN_DB_NAME=checkin_db
CHECKIN_DB_USER=checkin_user
CHECKIN_DB_PASSWORD=secret
CHECKIN_DB_PORT=5433

# NoteAI DB
NOTEAI_DB_NAME=noteai_db
NOTEAI_DB_USER=noteai_user
NOTEAI_DB_PASSWORD=secret
NOTEAI_DB_PORT=5434

# JWT
JWT_SECRET=your-very-long-secret-key-here
JWT_EXPIRATION=86400000

# Mail (email-верификация)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your@email.com
MAIL_PASSWORD=app_password

# Twilio (SMS верификация)
TWILIO_ACCOUNT_SID=...
TWILIO_AUTH_TOKEN=...
TWILIO_PHONE_NUMBER=+1234567890

# Groq (LLM для NoteAI)
GROQ_API_KEY=gsk_...

# PgAdmin
PGADMIN_EMAIL=admin@admin.com
PGADMIN_PASSWORD=admin
PGADMIN_PORT=5050
```

### Swagger UI (интерактивная документация)

- Auth: `http://localhost:8081/swagger-ui.html`
- CheckIn: `http://localhost:8082/swagger-ui.html`
- NoteAI: `http://localhost:8083/swagger-ui.html`
- ML: `http://localhost:5001/apidocs`

---

---

# ЧАСТЬ II — СЕРВИСЫ: API И БИЗНЕС-ЛОГИКА

---

## 5. NBAuthService — Аутентификация и пользователи (порт 8081)

**Base URL:** `http://localhost:8081/api/v1`

### Как работает авторизация

Все защищённые эндпойнты требуют JWT в заголовке:

```
Authorization: Bearer <jwt_token>
```

Поток авторизации:
1. Пользователь логинится — получает `accessToken` + `refreshToken`
2. `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) валидирует токен при каждом запросе
3. Извлекает `userId` из claim `id` и кладёт в `request.getAttribute("userId")`
4. Контроллеры читают `userId` напрямую — SecurityContext не используется в CheckIn и NoteAI

**Структура JWT:**
- `id` — числовой ID пользователя
- `sub` — username
- `roles` — `ROLE_USER`, `ROLE_ADMIN`
- `exp` — время истечения (миллисекунды, задаётся через `JWT_EXPIRATION`)
- Алгоритм подписи: **HS256** (HMAC-SHA-256)
- Хранение паролей: **BCrypt**, cost factor 10 (соответствует NIST SP 800-63B)

Refresh-токен хранится в таблице `users` в auth_db. Используется чтобы получить новый accessToken без повторного логина.

---

### 5.1 Регистрация и вход

#### POST /auth/register — Регистрация нового пользователя

Онбординг можно передать сразу при регистрации (все поля опциональны).

**Тело запроса:**
```json
{
  "name": "Аман Гельди",
  "email": "user@example.com",
  "username": "amangeldi",
  "phone": "+77001234567",
  "password": "Password123",
  "sex": "male",
  "heightCm": 175,
  "weightKg": 70.5,
  "birthDate": "2001-04-15",
  "characterId": 1,
  "dataConsent": true
}
```

**Ответ (200 OK) — если онбординг передан:**
```json
{
  "message": "Registration successful",
  "userId": 42,
  "username": "amangeldi",
  "email": "user@example.com",
  "isOnboarded": true,
  "onboardingCompleted": true
}
```

**Ответ — если онбординг не передан:**
```json
{
  "isOnboarded": false,
  "onboardingCompleted": false,
  "info": "You can complete onboarding later via /api/v1/onboarding"
}
```

---

#### POST /auth/login — Вход

```json
{
  "username": "amangeldi",
  "password": "Password123"
}
```

**Ответ:**
```json
{
  "id": 42,
  "username": "amangeldi",
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}
```

---

#### POST /auth/refresh — Обновление access токена

Тело запроса — raw string (refresh token). Ответ такой же, как при логине.

---

#### GET /auth/verify-email?token=`<uuid>` — Подтверждение email

Ссылка из письма. После успешной верификации `isVerified = true`. Ответ: строка `"Email verified successfully"`.

---

#### POST /auth/verify-user-manual?username=`<username>` — Ручная верификация (для тестов)

Верифицирует пользователя без email. Не защищён JWT.

---

#### POST /auth/register-phone — Регистрация по телефону

Отправляет SMS через Twilio.

#### POST /auth/verify-phone?phone=`+77001234567`&code=`123456` — Верификация SMS

---

### 5.2 Онбординг

Все эндпойнты требуют JWT.

#### POST /onboarding — Создать или полностью обновить данные онбординга

```json
{
  "sex": "male",
  "heightCm": 175,
  "weightKg": 70.5,
  "birthDate": "2001-04-15",
  "characterId": 1,
  "dataConsent": true
}
```

#### GET /onboarding — Получить данные онбординга
#### GET /onboarding/status — Проверить завершён ли онбординг
#### PATCH /onboarding — Частично обновить (только изменённые поля)
#### DELETE /onboarding — Удалить данные онбординга
#### GET /onboarding/user/{userId} — Онбординг конкретного пользователя (admin)

---

### 5.3 Профиль пользователя

#### GET /users/me — Полный профиль текущего пользователя

```json
{
  "id": 42,
  "name": "Аман Гельди",
  "email": "user@example.com",
  "username": "amangeldi",
  "phone": "+77001234567",
  "isOnboarded": true,
  "onboarding": {
    "sex": "male",
    "heightCm": 175,
    "weightKg": 70.5,
    "birthDate": "2001-04-15"
  }
}
```

#### GET /users/{id} — Пользователь по ID
#### PUT /users — Обновить профиль (обязательно передать `id`)
#### GET /users/search?q=`<query>` — Поиск по имени/username (возвращает `id`, `username`, `name`)
#### DELETE /users/{id} — Удалить пользователя

---

## 6. NBCheckinService — Чекины и данные здоровья (порт 8082)

**Base URL:** `http://localhost:8082/api/v1`

Все эндпойнты (кроме `/checkins/health`) требуют `Authorization: Bearer <token>`.

Временная зона: **Asia/Almaty (UTC+5)** — используется во всех датах «сегодня».

---

### 6.1 Ежедневный чекин (/checkins)

Чекин — ежедневная форма данных о самочувствии. **Один чекин в день.** После создания через Kafka автоматически пересчитываются Health Metrics и ML-рекомендации.

#### POST /checkins — Создать чекин на сегодня

```json
{
  "morningMood": 4,
  "eveningMood": 3,
  "sleepQuality": 7,
  "sleepHours": 7.5,
  "sleepBedtime": "23:00",
  "sleepWaketime": "06:30",
  "energyLevel": 6,
  "stressLevel": 4,
  "physicalActivityMinutes": 30,
  "physicalActivityType": "walk",
  "didExercise": true,
  "ateHealthy": true,
  "hadSocialInteraction": false,
  "playedCognitiveGameToday": false,
  "cognitiveGameCount": 0
}
```

| Поле | Тип | Диапазон | Обязательно |
|---|---|---|---|
| `sleepQuality` | int | 1–10 | **да** |
| `energyLevel` | int | 1–10 | **да** |
| `stressLevel` | int | 1–10 | **да** |
| `morningMood` | int | 1–5 | нет |
| `eveningMood` | int | 1–5 | нет |
| `sleepHours` | decimal | 0–24 | нет |
| `sleepBedtime` / `sleepWaketime` | time | HH:mm | нет |
| `physicalActivityMinutes` | int | 0+ | нет |
| `physicalActivityType` | string | walk/gym/yoga/sports/none | нет |
| `didExercise`, `ateHealthy`, `hadSocialInteraction` | boolean | — | нет |

**Что происходит после создания чекина:**
- Kafka публикует `checkin.created` — Health Metrics пересчитываются (через ML Service)
- Kafka публикует `checkin.created` — ML-рекомендации пересчитываются
- Задача `COMPLETE_CHECKIN` в Daily Tasks автоматически отмечается выполненной (+50 XP)
- Стрик обновляется

---

#### GET /checkins/today — Сегодняшний чекин (404 если не создан)
#### GET /checkins/today/exists — Был ли чекин сегодня
```json
{ "exists": true, "canCheckIn": false }
```
#### GET /checkins/{date} — Чекин за конкретную дату (формат: yyyy-MM-dd)
#### PUT /checkins/{date} — Обновить чекин за дату
#### DELETE /checkins/{date} — Удалить чекин за дату
#### GET /checkins/recent — Последние 30 дней
#### GET /checkins?startDate=...&endDate=... — Диапазон дат
#### GET /checkins/calendar?year=2026&month=6 — Даты выполненных чекинов для календаря
#### GET /checkins/stats/weekly — Статистика за неделю (количество, средние значения)
#### GET /checkins/stats/monthly — Статистика за месяц
#### GET /checkins/stats?startDate=...&endDate=... — Статистика за произвольный период
#### GET /checkins/streak — Информация о стрике
#### POST /checkins/streak/recalculate — Пересчитать стрик вручную

---

### 6.2 Настроение (/mood)

Логи настроения — можно несколько в день.

#### POST /mood — Записать настроение
```json
{
  "moodScore": 4,
  "note": "Хорошее утро, много энергии",
  "triggers": ["work", "sleep"],
  "loggedAt": "2026-06-06T09:00:00"
}
```
Автоматически отмечает задачу `LOG_MOOD` как выполненную (+20 XP).

#### GET /mood — Все логи
#### GET /mood/recent — Последние 20 логов
#### GET /mood/today — Логи за сегодня
#### GET /mood/date/{date} — Логи за конкретную дату
#### GET /mood/range?startTime=...&endTime=... — Диапазон
#### GET /mood/{id} — По ID
#### PUT /mood/{id} — Обновить
#### DELETE /mood/{id} — Удалить
#### GET /mood/average?startTime=...&endTime=... — Среднее настроение за период
```json
{ "userId": 42, "averageMood": 3.7 }
```
#### GET /mood/trigger/{trigger} — Логи по конкретному триггеру

---

### 6.3 Сон (/sleep)

#### POST /sleep — Записать данные о сне
```json
{
  "sleepDate": "2026-06-06",
  "bedtime": "23:00",
  "wakeTime": "07:00",
  "durationHours": 8.0,
  "qualityScore": 8,
  "deepSleepMinutes": 90,
  "remSleepMinutes": 80,
  "notes": "Спал хорошо"
}
```
После создания: Kafka `sleep.logged` — пересчёт Health Metrics + ML-рекомендаций. Задача `LOG_SLEEP` выполнена (+25 XP).

#### GET /sleep — Все записи
#### GET /sleep/recent — Последние записи
#### GET /sleep/today — Запись за сегодня (404 если нет)
#### GET /sleep/today/exists — Была ли запись сна сегодня
#### GET /sleep/{id} — По ID
#### GET /sleep/date/{date} — По дате
#### GET /sleep/range?startDate=...&endDate=... — Диапазон
#### PUT /sleep/{id} — Обновить по ID
#### PUT /sleep/date/{date} — Обновить по дате
#### DELETE /sleep/{id} / DELETE /sleep/date/{date} — Удалить

---

### 6.4 Ежедневные задачи (/tasks)

Каждый день для пользователя автоматически создаётся набор из 5 задач. Все выполняются автоматически через соответствующие эндпойнты.

| TaskType | Что нужно сделать | Автовыполнение | XP |
|---|---|---|---|
| `COMPLETE_CHECKIN` | Ежедневный чекин | POST /checkins | +50 |
| `LOG_MOOD` | Записать настроение | POST /mood | +20 |
| `LOG_SLEEP` | Записать сон | POST /sleep | +25 |
| `PLAY_GAME` | Сыграть в когнитивную игру | POST /brain-games/submit | +30–80 |
| `WRITE_NOTE` | Написать заметку | POST /journal | +45 |

#### GET /tasks/today — Задачи на сегодня
```json
[
  {
    "taskType": "COMPLETE_CHECKIN",
    "title": "Ежедневный чекин",
    "description": "Заполните ежедневную форму самочувствия",
    "isCompleted": true,
    "completedAt": "2026-06-06T08:30:00",
    "xpReward": 50,
    "taskDate": "2026-06-06"
  }
]
```

#### POST /tasks/complete?taskType=LOG_MOOD&date=2026-06-06 — Ручное выполнение задачи
#### GET /tasks/stats — Статистика выполнения задач
#### GET /tasks/date/{date} — Задачи за дату (создаёт если нет)
#### GET /tasks/history?startDate=...&endDate=... — История задач
#### POST /tasks/note-written?date=2026-06-06 — Отметить WRITE_NOTE (вызывается автоматически из NoteAI)

---

### 6.5 Метрики здоровья (/health-metrics)

Метрики вычисляются автоматически через ML Service после каждого чекина. Формулы подробно описаны в разделе 12.

**Три метрики:**
- **M-Rest** (0–100) — качество восстановления и сна
- **M-Ready** (0–100) — когнитивная готовность к дню
- **M-Balance** (0–100) — эмоциональный баланс

**Метки:** `Excellent` (>=80) / `Good` (>=60) / `Fair` (>=40) / `Poor` (<40)

#### GET /health-metrics/today — Метрики за сегодня
```json
{
  "userId": 42,
  "date": "2026-06-06",
  "mRest": 78.5,
  "mReady": 72.3,
  "mBalance": 65.1,
  "overall": 71.9,
  "mRestLabel": "Good",
  "mReadyLabel": "Good",
  "mBalanceLabel": "Good",
  "overallLabel": "Good"
}
```

#### GET /health-metrics/{date} — Метрики за дату
#### GET /health-metrics/recent?days=7 — Последние N дней (1–90)
#### GET /health-metrics/history — Полная история
#### POST /health-metrics/recalculate?date=2026-06-06 — Принудительный пересчёт

---

### 6.6 ML-рекомендации (/ml)

Персонализированные рекомендации. Обновляются автоматически через Kafka при каждом чекине, сне или игре.

**Важно:** все основные параметры (сон, стресс, возраст, пол) берутся из реальных данных пользователя в БД — поля из тела запроса используются только как дополнительные предпочтения.

#### POST /ml/recommendations — Основной эндпойнт для фронтенда
```json
// Тело (всё опционально — дополнительные предпочтения)
{
  "dailyScreenTime": 6.0,
  "caffeineIntake": 2,
  "dietType": "Non-Vegetarian"
}
```

```json
// Ответ
{
  "userId": 42,
  "cognitiveScore": 68.3,
  "cognitiveState": "Medium",
  "recommendations": [
    {
      "type": "SLEEP_INCREASE",
      "title": "Оптимизация сна",
      "priority": "HIGH",
      "predictedImprovement": 8.5,
      "actions": ["Будильник на отбой в 23:00", "Затемните спальню"],
      "scientificBasis": "Walker (2017): Why We Sleep",
      "baseline": 6.0,
      "recommendedTarget": 7.5
    }
  ],
  "totalPotential": 22.4,
  "summary": "Ваш когнитивный счет: 68.3. Рекомендации могут дать +22.4 балла."
}
```

**Типы рекомендаций:**
- `SLEEP_INCREASE` — увеличить продолжительность/качество сна
- `STRESS_DECREASE` — снизить уровень стресса
- `EXERCISE_INCREASE` — больше физической активности
- `SCREEN_DECREASE` — меньше времени у экрана

#### GET /ml/recommendations — То же самое (GET для браузера)
#### GET /ml/recommendations/history — История рекомендаций из таблицы `daily_ml_recommendation`
#### POST /ml/recommendations/refresh — Форс-пересчёт (только для тестирования)

---

### 6.7 Стрики (/streaks)

#### GET /streaks — Текущий стрик
```json
{
  "currentStreak": 7,
  "longestStreak": 14,
  "totalCheckins": 42,
  "totalXpEarned": 2100,
  "nextMilestone": 10,
  "canCheckinToday": false
}
```

#### POST /streaks/recalculate — Пересчитать из истории чекинов
#### GET /streaks/leaderboard/streak — Топ-10 по стрику
#### GET /streaks/leaderboard/xp — Топ-10 по XP
#### GET /streaks/rank — Ранг текущего пользователя

---

### 6.8 Персонаж (/character)

**Типы:** `CAT`, `DOG`, `RABBIT`, `PANDA`, `FOX`

#### GET /character — Текущий персонаж
```json
{
  "characterType": "CAT",
  "level": 3,
  "xp": 450,
  "xpToNextLevel": 200,
  "happiness": 80,
  "energy": 70,
  "justLeveledUp": false
}
```

#### POST /character/select — Выбрать персонажа (один раз при онбординге)
#### POST /character/change-type?characterType=DOG — Сменить тип
#### POST /character/feed — Покормить (+10 счастья, +15 энергии)
#### POST /character/play — Поиграть (+15 счастья, -5 энергии)
#### GET /character/progression — Прогресс к следующему уровню

---

### 6.9 Когнитивные игры (/brain-games)

Игры: `NUMBER_SEQUENCE`, `MEMORY_PAIRS`. Результат отправляется после прохождения.

#### POST /brain-games/submit — Отправить результат
```json
{
  "gameType": "NUMBER_SEQUENCE",
  "score": 850,
  "durationSeconds": 45,
  "difficultyLevel": "MEDIUM",
  "isCompleted": true,
  "correctAnswers": 8,
  "totalQuestions": 10
}
```

После отправки: Kafka `game.completed` — ML-рекомендации пересчитываются. Задача `PLAY_GAME` выполнена.

**Адаптивная сложность:** если когнитивный скор (XGBoost) > 75 — HARD, 50–75 — MEDIUM, < 50 — EASY.

#### GET /brain-games/stats — Агрегированная статистика
#### GET /brain-games/history?gameType=NUMBER_SEQUENCE — История (с фильтром)
#### GET /brain-games/history/date?date=2026-06-06 — История за день
#### GET /brain-games/progression — Прогресс к повышению уровня персонажа
#### POST /brain-games/level-up — Повысить уровень (по недельным результатам)

---

### 6.10 Игровые сессии — Fun Games (/game-sessions)

Простые игры: `DONUT_GAME`, `CHARACTER_CARE`.

#### POST /game-sessions — Записать сессию
```json
{
  "gameType": "DONUT_GAME",
  "score": 500,
  "durationSeconds": 60,
  "isCompleted": true,
  "gameDate": "2026-06-06"
}
```

**Правила начисления XP:**
- Минимум: `isCompleted = true` И `durationSeconds >= 20`
- Максимум 3 XP-сессии на один тип игры в день
- XP зависит от победы, длительности, количества попыток + множитель стрика

#### GET /game-sessions/history?date=... — История за дату
#### GET /game-sessions/today-stats — Статистика за сегодня
#### GET /game-sessions/played-today — Играл ли сегодня
#### GET /game-sessions/recent?limit=10 — Последние N игр

---

### 6.11 Новые игровые сессии (/new-game-sessions)

`DONUT_GAME` и `NUMBER_SEQUENCE_GAME` с бонусами за скорость и попытки.

#### POST /new-game-sessions — Записать сессию
```json
{
  "gameType": "NUMBER_SEQUENCE_GAME",
  "score": 750,
  "durationSeconds": 35,
  "isCompleted": true,
  "attemptsCount": 2,
  "gameDate": "2026-06-06"
}
```

#### GET /new-game-sessions/highscore?type=DONUT_GAME — Личный рекорд
```json
{ "gameType": "DONUT_GAME", "personalBest": 180, "unit": "seconds (max)" }
```

#### GET /new-game-sessions/today-stats — Статистика за сегодня
#### GET /new-game-sessions/played-today — Играл ли сегодня
#### GET /new-game-sessions/history/date?date=... — История за дату

---

### 6.12 Награды и бейджи (/rewards)

#### GET /rewards — Все награды с прогрессом
```json
[
  {
    "rewardType": "STREAK_7",
    "title": "Неделя стрика",
    "description": "7 дней подряд без пропуска",
    "isUnlocked": true,
    "unlockedAt": "2026-06-05T10:00:00",
    "xpBonus": 100,
    "xpMultiplier": 1.1
  },
  {
    "rewardType": "STREAK_30",
    "title": "Месяц стрика",
    "isUnlocked": false,
    "progress": 7,
    "target": 30
  }
]
```

#### GET /rewards/unlocked — Только разблокированные
#### POST /rewards/check — Проверить и разблокировать новые
#### GET /rewards/xp-multiplier — Текущий множитель XP

---

## 7. NoteAI-backend — Заметки с AI (порт 8083)

**Base URL:** `http://localhost:8083`

AI-анализ работает через **Groq API** с моделью `llama-3.3-70b-versatile` (temperature = 0.3 для детерминированных, фактических ответов). Groq использует LPU (Language Processing Unit) — специализированное железо для LLM inference, обеспечивающее latency < 1 секунды.

---

### 7.1 Дневник (/api/v1/journal)

Основной модуль для ведения дневника. Поддерживает auto-save через PATCH с дебаунсом 2–3 секунды.

#### POST /api/v1/journal — Создать запись

Автоматически выполняет задачу `WRITE_NOTE` (+45 XP) через NBCheckinService.

```json
// Запрос
{
  "title": "Мои мысли сегодня",
  "content": "Сегодня был тяжёлый день на работе...",
  "moodScore": 3,
  "tags": "работа,стресс",
  "isFavorite": false
}

// Ответ (201)
{
  "id": 15,
  "userId": 42,
  "title": "Мои мысли сегодня",
  "content": "Сегодня был тяжёлый день на работе...",
  "moodScore": 3,
  "tags": "работа,стресс",
  "isFavorite": false,
  "createdAt": "2026-06-06T14:30:00",
  "updatedAt": "2026-06-06T14:30:00"
}
```

#### GET /api/v1/journal — Все записи (новые первыми)
#### GET /api/v1/journal/today — Записи за сегодня (Asia/Almaty)
#### GET /api/v1/journal/date/{date} — За конкретную дату
#### GET /api/v1/journal/range?startDate=...&endDate=... — Диапазон
#### GET /api/v1/journal/favorites — Только isFavorite=true
#### GET /api/v1/journal/{id} — По ID
#### PUT /api/v1/journal/{id} — Полное обновление (WRITE_NOTE не дублируется)

#### PATCH /api/v1/journal/{id} — Частичное обновление / auto-save

Фронтенд вызывает с задержкой 2–3 сек после паузы в наборе текста. Обновляет только переданные поля.

```json
{ "content": "...обновлённый текст..." }
```

#### DELETE /api/v1/journal/{id} — Удалить запись

---

#### POST /api/v1/journal/{id}/ai/summary — AI резюме записи

1–2 предложения на языке записи.

```json
{
  "noteId": 15,
  "noteTitle": "Мои мысли сегодня",
  "type": "summary",
  "summary": "Автор испытывает стресс от рабочих дедлайнов и жалуется на нехватку энергии.",
  "generatedAt": "2026-06-06 14:35:00"
}
```

#### POST /api/v1/journal/{id}/ai/analyze — AI wellness-анализ

```json
{
  "noteId": 15,
  "type": "analysis",
  "tone": "тревожный",
  "themes": ["рабочий стресс", "усталость", "нехватка сна"],
  "wellnessInsight": "Высокий стресс и усталость снижают M-Balance ниже 50 и давят на M-Ready.",
  "suggestion": "Сделайте 10-минутную прогулку после обеда — снижает кортизол на 15-20%.",
  "summary": "Автор перегружен работой и плохо спит.",
  "generatedAt": "2026-06-06 14:35:00"
}
```

#### POST /api/v1/journal/ai/chat — AI чат-ассистент

```json
// Запрос
{ "message": "Что влияет на мой M-Balance?", "noteId": 15 }

// Ответ
{
  "type": "chat",
  "answer": "Судя по записи, основное влияние оказывает хронический стресс от дедлайнов...",
  "generatedAt": "2026-06-06 14:36:00"
}
```

---

### 7.2 Заметки — Notes (/api/v1/notes)

Отдельный модуль заметок (отдельная таблица от Journal).

#### GET /api/v1/notes — Все заметки
#### GET /api/v1/notes/{id} — По ID
#### PUT /api/v1/notes/{id} — Обновить (title + content)
#### DELETE /api/v1/notes/{id} — Удалить
#### POST /api/v1/notes/{id}/ai/summary — AI резюме
#### POST /api/v1/notes/{id}/ai/analyze — AI анализ
#### POST /api/v1/notes/ai/chat — AI чат

---

---

# ЧАСТЬ III — MACHINE LEARNING

---

## 8. ML Service — Архитектура и роль

### Общая концепция

ML Service — это отдельный микросервис на Python/Flask, который решает задачи **предсказания** и **ранжирования**. Он не доступен напрямую с фронтенда — только через Java бэкенд по внутренней Docker-сети.

**Три основные задачи ML:**

| Задача | Модель | Endpoint |
|---|---|---|
| Предсказание когнитивного скора (0–100) | XGBoost Regressor | POST /predict |
| Классификация когнитивного состояния (High/Medium/Low) | XGBoost Classifier | POST /predict |
| Ранжирование и выбор топ-3 рекомендаций | CatBoost Regressor | POST /recommend/top3 |
| Расчёт M-Rest / M-Ready / M-Balance | XGBoost x 3 + формулы | POST /health-metrics/calculate |

**Версия:** 5.1.0 (Ultimate Edition)

**Файлы моделей** (загружаются при старте Flask, хранятся в памяти):
```
models/
├── cognitive_score_model.pkl         # XGBoost регрессор
├── cognitive_state_model.pkl         # XGBoost классификатор
├── scaler.pkl                        # StandardScaler для нормализации
├── label_encoder.pkl                 # LabelEncoder (HIGH/MEDIUM/LOW → 0/1/2)
├── catboost_recommendation_model.cbm # CatBoost модель рекомендаций
├── smart_recommendation_selector.pkl # Класс-обёртка для селектора
├── top3_selector.pkl                 # Топ-3 селектор
├── feature_names.txt                 # Список из 17 признаков
├── hm_m_rest_model.pkl               # XGBoost для M-Rest
├── hm_m_ready_model.pkl              # XGBoost для M-Ready
├── hm_m_balance_model.pkl            # XGBoost для M-Balance
├── hm_m_rest_scaler.pkl              # Scaler для M-Rest модели
├── hm_m_ready_scaler.pkl             # Scaler для M-Ready модели
├── hm_m_balance_scaler.pkl           # Scaler для M-Balance модели
└── hm_model_metadata.json            # Метаданные: версия, метрики качества
```

### In-memory кэш пользователей

ML-сервис поддерживает per-user кэш (словарь `_user_cache`):
- Ключ: `str(user_id)`
- Значение: `{'date': 'YYYY-MM-DD', 'result': dict}`
- Кэш поддерживает Asia/Almaty timezone
- При наступлении нового дня — кэш считается устаревшим, пересчёт происходит автоматически
- Java бэкенд обновляет кэш через `POST /internal/cache/update` после каждого Kafka-события

---

## 9. Датасет и признаки

### Исходный датасет

**Файл:** `ml/human_cognitive_performance.csv`

Датасет содержит физиологически валидированные данные о когнитивной производительности людей с описанием их образа жизни. Используется для обучения XGBoost-моделей когнитивного скора.

**Ключевые исходные признаки:**

| Признак | Диапазон | Описание |
|---|---|---|
| `Sleep_Duration` | 4–10 часов | Продолжительность сна |
| `Stress_Level` | 1–10 | Уровень стресса |
| `Daily_Screen_Time` | 1–14 часов | Экранное время |
| `Exercise_Frequency_Num` | 0–7 | Частота тренировок в неделю |
| `Caffeine_Intake` | 0–6 порций | Потребление кофеина |
| `Reaction_Time` | 200–500 мс | Время реакции |
| `Memory_Test_Score` | 40–100 | Результат теста памяти |
| `Age` | 18–65 | Возраст |
| `Gender_Encoded` | 0/1 | Пол (закодирован) |
| `Diet_*` | 0/1 | One-hot: Non-Vegetarian / Vegan / Vegetarian |

### Инженерия признаков (Feature Engineering)

Из исходных признаков создаются **7 производных**:

```python
# 1. CFI (Cognitive Fatigue Index) — индекс когнитивного утомления
r_norm     = (reaction_time - 200) / 400
s_norm     = (stress_level - 1) / 9
sleep_debt = max(0, 7 - sleep_duration) / 3
screen_fat = max(0, daily_screen_time - 8) / 4
CFI = (0.30 * r_norm + 0.25 * s_norm + 0.25 * sleep_debt + 0.20 * screen_fat) * 100

# 2. Sleep_Debt — недосып относительно нормы 7 часов
sleep_debt = max(0, 7 - sleep_duration)

# 3. Memory_Efficiency — эффективность памяти с учётом реакции
memory_efficiency = (memory_test_score / reaction_time) * 1000

# 4. Lifestyle_Balance — общий баланс образа жизни (0–100)
sleep_sc = clip((sleep_duration - 4) / 6, 0, 1)
ex_sc    = clip(exercise_frequency / 7, 0, 1)
st_sc    = 1 - ((stress_level - 1) / 9)
scr_sc   = 1 - clip((daily_screen_time - 1) / 11, 0, 1)
lifestyle_balance = (0.30*sleep_sc + 0.25*st_sc + 0.20*ex_sc + 0.15*scr_sc) * 100

# 5. Sleep_Exercise_Interaction — взаимодействие сна и упражнений
sleep_exercise_interaction = sleep_duration * exercise_frequency

# 6. Sleep_Performance_Ratio
sleep_performance_ratio = memory_test_score / max(sleep_duration, 1)

# 7. Stress_Resilience
stress_resilience = memory_test_score * (1 - stress_level / 10)
```

**Итого:** 17 финальных признаков (`FINAL_FEATURES`) для XGBoost.

### Датасет для рекомендаций

**Файл:** `ml/notebooks/recommendation_training_data_v3.csv`

Специально созданный датасет для обучения CatBoost. Содержит ~50,000 записей симулированных пользователей с различными профилями и рассчитанными `improvement` (улучшение когнитивного скора) для каждого типа рекомендации:

| Столбец | Описание |
|---|---|
| `original_score` | Базовый когнитивный скор пользователя |
| `improvement` | Предсказанное улучшение при выполнении рекомендации |
| `action_type` | Тип: SLEEP_INCREASE / STRESS_DECREASE / EXERCISE_INCREASE / SCREEN_DECREASE |
| `action_magnitude` | Величина изменения (например, +1.5 часа сна) |
| `baseline_value` | Текущее значение параметра |
| `is_critical` | 1 если параметр находится в критической зоне |

### Датасет для Health Metrics

**Файл:** `ml/notebooks/cognitive_performance_engineered.csv`

Обучающие данные для трёх XGBoost-моделей (M-Rest, M-Ready, M-Balance):
- 12,000 синтетических записей
- Создан с физиологически валидированными формулами (Stults-Kolehmainen & Sinha, 2014) как target values
- Признаки: sleep_hours, sleep_quality, energy_level, morning_mood, evening_mood, stress_level, did_exercise, ate_healthy, had_social_interaction, felt_rested, deep_sleep_minutes, rem_sleep_minutes, total_sleep_minutes + 4 производных

---

## 10. Модели: обучение и параметры

### Модель 1: XGBoost Regressor — Когнитивный скор

**Задача:** регрессия, предсказание `Cognitive_Score` в диапазоне 0–100

**Параметры обучения:**
```python
XGBRegressor(
    n_estimators=500,        # количество деревьев
    max_depth=8,             # глубина дерева
    learning_rate=0.05,      # шаг обучения
    subsample=0.8,           # доля выборки на каждое дерево
    colsample_bytree=0.8,    # доля признаков на каждое дерево
    gamma=0.1,               # минимальный gain для сплита
    reg_alpha=0.1,           # L1-регуляризация
    reg_lambda=1.0,          # L2-регуляризация
    random_state=42,
    n_jobs=-1,
    early_stopping_rounds=30
)
```

**Разбивка данных:** 70% train / 10% val / 20% test (стратифицированная по `Cognitive_State`)

**Нормализация:** StandardScaler (fit только на train, transform на val/test — защита от data leakage)

---

### Модель 2: XGBoost Classifier — Когнитивное состояние

**Задача:** 3-классовая классификация — `HIGH` / `MEDIUM` / `LOW`

**Параметры:**
```python
XGBClassifier(
    n_estimators=200,
    max_depth=6,
    learning_rate=0.1,
    subsample=0.8,
    colsample_bytree=0.8,
    random_state=42,
    n_jobs=-1,
    early_stopping_rounds=20
)
```

Метки кодируются через `LabelEncoder`: HIGH=0, LOW=1, MEDIUM=2. После предсказания обратное преобразование через `inverse_transform`.

---

### Модель 3: CatBoost Regressor — Рекомендации

**Задача:** предсказание `improvement` (насколько вырастет когнитивный скор при выполнении рекомендации)

**Параметры:**
```python
CatBoostRegressor(
    iterations=1000,
    learning_rate=0.05,
    depth=8,
    loss_function='RMSE',
    eval_metric='RMSE',
    random_seed=42,
    verbose=200,
    use_best_model=True  # выбор лучшей итерации по val set
)
```

**Входные признаки (24):** 17 признаков пользователя + тип рекомендации (one-hot: 4 типа) + `action_delta`, `baseline_value`, `baseline_score`, `is_critical`, `is_optimal_zone`

**Класс SmartRecommendationSelector:**
- Предсказывает `improvement` для каждого из 4 типов рекомендаций и нескольких сценариев `action_magnitude`
- Берёт лучший сценарий по `predicted_improvement`
- Дедупликация по типу рекомендации (не более одной на тип)
- Сортировка по `predicted_improvement` descending
- Минимальный порог confidence 0.45 — подавление слабых рекомендаций

---

### Модели 4–6: XGBoost x 3 — Health Metrics

**Задача:** предсказание M-Rest, M-Ready, M-Balance как непрерывных значений 0–100

**Параметры:** аналогичные когнитивному скору (n_estimators=300, max_depth=6)

**Режим работы:** при недоступности моделей (cold start) — автоматический fallback на детерминированные формулы с флагом `source: "formula_fallback"` в ответе.

---

## 11. Метрики качества моделей

### Метрики и их смысл

| Метрика | Формула | Что показывает |
|---|---|---|
| **R²** (коэффициент детерминации) | 1 - SS_res/SS_tot | Доля объяснённой дисперсии. R²=1 — идеал, R²=0 — модель не лучше среднего |
| **RMSE** (Root Mean Squared Error) | sqrt(sum((y-y_hat)^2)/n) | Среднеквадратичное отклонение в единицах целевой переменной |
| **MAE** (Mean Absolute Error) | sum(abs(y-y_hat))/n | Среднее абсолютное отклонение (менее чувствителен к выбросам) |
| **Accuracy** | (TP+TN) / Total | Доля верных классификаций |

### Результаты обучения

**Когнитивный скор (XGBoost Regressor):**
```
Test R²   = 0.9990  (99.9% объяснённой дисперсии)
Test RMSE = 0.73    (ошибка < 1 балла из 100)
```

**Когнитивное состояние (XGBoost Classifier):**
```
Test Accuracy = 97.9%
```

**Рекомендации (CatBoost Regressor):**
```
Test R²   = 0.9656
Test RMSE = 1.81    (ошибка прогноза улучшения < 2 баллов)
```

**Health Metrics (XGBoost x 3):**

| Метрика | R² | MAE | RMSE |
|---|---|---|---|
| **M-Rest** | 0.917 | 2.01 | 2.52 |
| **M-Ready** | 0.971 | 2.02 | 2.56 |
| **M-Balance** | 0.967 | 2.13 | 2.66 |

### Как измеряется корректность на проде

1. **Graceful degradation:** при недоступности ML Service Java возвращает метрики по формулам с флагом `ml_unavailable: true`
2. **Inference latency:** XGBoost на 23 признаках — 2–5 мс; CatBoost — 10–20 мс (все операции в памяти, без disk I/O)
3. **Кэш согласованности:** Java инвалидирует кэш каждый день в 00:00 по Asia/Almaty через Kafka-события

---

## 12. Формулы Health Metrics (M-Rest / M-Ready / M-Balance)

Метрики вычисляются как детерминированными формулами (быстро, надёжно), так и через XGBoost-модели (ML-predict). При сравнении используется значение ML если модели загружены.

### M-Rest — качество восстановления и сна

```
M-Rest = min(sleep_hours / 8, 1) * 50
       + (sleep_quality / 10) * 30
       + felt_rested * 20

Диапазон: 0–100  (зажато clamp в [0, 100])
```

| Компонент | Вес | Обоснование |
|---|---|---|
| sleep_hours / 8 | 50% | Норма 8 часов (Walker, 2017) |
| sleep_quality / 10 | 30% | Субъективное качество коррелирует с deep sleep (Killgore, 2010) |
| felt_rested | 20% | Субъективное ощущение восстановления |

Альтернативная формула ML-сервиса (более детальная):
```
M-Rest = (sleepHours / 8.0) * 40
       + (sleepQuality / 10.0) * 40
       + (deepSleepMinutes / 90.0) * 20
```

---

### M-Ready — когнитивная готовность к дню

```
M-Ready = (energy_level / 10) * 40
        + (morning_mood / 5) * 30
        + M-Rest * 0.30

Диапазон: 0–100
```

| Компонент | Вес | Обоснование |
|---|---|---|
| energy_level | 40% | Главный предиктор готовности к когнитивным задачам |
| morning_mood | 30% | Утреннее настроение = прокси когнитивного состояния |
| M-Rest | 30% | Качество сна предопределяет дневную продуктивность |

---

### M-Balance — эмоциональный и поведенческий баланс

```
M-Balance = max((10 - stress_level) / 9, 0) * 40
          + did_exercise * 13.3
          + ate_healthy * 13.3
          + had_social_interaction * 13.4
          + (evening_mood / 5) * 20

Диапазон: 0–100
```

| Компонент | Вес | Обоснование |
|---|---|---|
| stress | 40% | Основной разрушитель баланса (McEwen, 2007) |
| lifestyle habits (3) | 40% | Stults-Kolehmainen & Sinha (2014): упражнения + питание + социальная активность |
| evening_mood | 20% | Итоговая оценка дня |

### Итоговый Overall Wellness Score

```
overall = round((M-Rest + M-Ready + M-Balance) / 3.0)
```

---

## 13. Почему XGBoost и CatBoost лучше альтернатив

### Выбор алгоритма: сравнение

| Алгоритм | Наш датасет (табличный) | Плюсы | Минусы |
|---|---|---|---|
| **XGBoost** | лучший выбор | L1/L2 регуляризация, early stopping, high R² | Требует нормализации и энкодинга |
| **CatBoost** | лучший для рекомендаций | Нативная работа с категориями, no target leakage | Медленнее обучается |
| Random Forest | хуже | Устойчивость к переобучению | Ниже точность на нашем датасете |
| Linear Regression | не подходит | Простота | Не улавливает нелинейные зависимости |
| Neural Network / MLP | не подходит | Теоретически мощнее | Требует в 10–100x больше данных, не интерпретируем |
| LightGBM | близко | Быстрее обучается | Хуже с малыми датасетами |

### Почему не нейросети

1. **Размер данных:** у нас ~12,000 строк для health metrics и синтетический датасет для рекомендаций. Нейросеть здесь переобучится без massive augmentation.
2. **Интерпретируемость:** XGBoost + SHAP даёт объяснение на уровне признаков. Важно для медицинского домена.
3. **Инференс:** 2–5 мс vs 50–200 мс у MLP при том же качестве.
4. **Табличные данные:** Shwartz-Ziv & Armon (2022) показали, что gradient boosting превосходит DL на табличных данных в 90% случаев.

### Почему CatBoost для рекомендаций

- **Ordered Target Statistics** (Prokhorenkova et al., 2018) — исключает target leakage при обучении на категориальных признаках (`action_type`)
- **use_best_model=True** — автоматический выбор лучшей итерации по validation set, защита от переобучения
- **Встроенная работа с категориями** — не нужен ручной one-hot encoding для части признаков

---

## 14. Как ML-сервис интегрирован в систему

### API эндпойнты ML Service

**Base URL:** `http://localhost:5001` (внутренний Docker: `http://ml-service:5001`)

#### GET /health — Проверка состояния
```json
{ "status": "healthy", "version": "5.1.0" }
```

#### POST /predict — Предсказать когнитивный скор
```json
// Запрос
{
  "sleep_duration": 7.0, "stress_level": 5.0,
  "daily_screen_time": 8.0, "exercise_frequency": 3.0,
  "caffeine_intake": 2.0, "reaction_time": 300.0,
  "memory_test_score": 75.0, "age": 25.0,
  "gender": "Male", "diet_type": "Non-Vegetarian"
}

// Ответ
{
  "status": "success",
  "cognitive_score": 72.4,
  "cognitive_state": "Medium",
  "cfi_score": 31.2
}
```

`cognitive_state`: `High` (>75) / `Medium` (50–75) / `Low` (<50)

#### POST /recommend/top3 — Топ-3 рекомендации
#### POST /recommend/single — Одна лучшая рекомендация

#### POST /health-metrics/calculate — Рассчитать M-Rest/M-Ready/M-Balance
```json
// Запрос
{
  "sleep_hours": 7.5, "sleep_quality": 8, "energy_level": 7,
  "morning_mood": 4, "evening_mood": 3, "stress_level": 4,
  "did_exercise": true, "ate_healthy": true, "had_social_interaction": false,
  "deep_sleep_minutes": 90, "rem_sleep_minutes": 80,
  "total_sleep_minutes": 450, "felt_rested": true
}

// Ответ
{
  "status": "success",
  "m_rest": 78.5, "m_ready": 72.3, "m_balance": 65.1, "overall": 71.9,
  "m_rest_label": "Good", "m_ready_label": "Good",
  "m_balance_label": "Good", "overall_label": "Good"
}
```

#### POST /health-metrics/trend — Тренд за N дней

Возвращает средние значения и тренд: `improving` / `declining` / `stable` по каждой метрике.

#### POST /health-metrics/ml-predict — Предсказание через XGBoost модели

При незагруженных моделях: формульный расчёт с флагом `source: "formula_fallback"`.

#### POST /internal/cache/update — Обновить кэш (только от Java)

---

---

# ЧАСТЬ IV — ИНФРАСТРУКТУРА

---

## 15. Kafka — Асинхронные события

Apache Kafka использован для **расцепления сервисов**. Весь обмен между CheckIn Service и ML/Health/игровой подсистемой — через события. Это обеспечивает:

- Ответ пользователю < 30 мс (только запись в БД), ML inference (до 200 мс) — асинхронно
- Устойчивость: если ML-сервис недоступен, чекин сохранён, метрики пересчитаются позже
- Ordered processing: сообщения кейсируются по `userId` — все события одного пользователя идут в одну партицию

### Топики Kafka

| Топик | Партиции | Продюсер | Потребители | Когда |
|---|---|---|---|---|
| `checkin.created` | 3 | NBCheckinService | HealthMetricsSaver, MLRecommendationConsumer | POST /checkins |
| `sleep.logged` | 3 | NBCheckinService | SleepLogKafkaConsumer | POST /sleep |
| `game.completed` | 3 | NBCheckinService | HealthMetricsKafkaConsumer, ML refresh | POST /brain-games/submit |
| `character.leveled-up` | 3 | NBCheckinService | CharacterProgressionConsumer | После level-up |

### Полный флоу после создания чекина

```
POST /checkins  →  HTTP 201  (< 30 мс, ответ пользователю)
     │
     ▼
DailyCheckInService.createCheckIn()  [@Transactional]
     ├── Сохраняет DailyCheckIn в БД
     ├── Обновляет UserStreak (increment или reset)
     ├── Автовыполняет задачу COMPLETE_CHECKIN (+50 XP)
     └── Публикует CheckInCreatedApplicationEvent (Spring Event)
              │
              ▼ (после коммита транзакции)
     TransactionalKafkaPublisher
              │
     kafkaTemplate.send("checkin.created", userId.toString(), event)
              │
              ├── → HealthMetricsSaver [groupId: health-metrics-group]
              │          │
              │          └── POST /predict (ML Service)
              │                  → сохраняет HealthMetrics в БД
              │
              └── → MLRecommendationConsumer [groupId: ml-recommendations-group]
                         │
                         └── MLRecommendationCacheService.asyncRefresh()
                                 │
                                 └── POST /recommend/top3 (internal=true)
                                         → сохраняет в daily_ml_recommendation
                                         → обновляет Python кэш через /internal/cache/update
```

### Graceful Degradation

Если ML Service недоступен при обработке `checkin.created`:
1. `HealthMetricsSaver` вычисляет M-Rest/M-Ready/M-Balance по детерминированным формулам
2. Сохраняет в `health_metrics` с флагом `mlScore = null`
3. Пользователь видит метрики без когнитивного скора, до восстановления ML Service

---

## 16. Базы данных

### auth_db (NBAuthService, порт 5432)

| Таблица | Ключевые поля |
|---|---|
| `users` | id, name, email, username, phone, password_hash (BCrypt), is_verified, is_onboarded, refresh_token |
| `user_onboarding` | user_id (FK), sex, height_cm, weight_kg, birth_date, character_id, data_consent |
| `verification_tokens` | token (UUID), user_id, expires_at |

### checkin_db (NBCheckinService, порт 5433)

| Таблица | Ключевые поля |
|---|---|
| `daily_check_ins` | id, user_id, date, morning_mood, evening_mood, sleep_quality, sleep_hours, energy_level, stress_level |
| `mood_logs` | id, user_id, mood_score, note, triggers, logged_at |
| `sleep_logs` | id, user_id, sleep_date, bedtime, wake_time, duration_hours, quality_score, deep_sleep_minutes, rem_sleep_minutes |
| `daily_tasks` | id, user_id, task_type, task_date, is_completed, completed_at, xp_reward |
| `health_metrics` | id, user_id, date, m_rest, m_ready, m_balance, overall, ml_score |
| `daily_ml_recommendation` | id, user_id, date, cognitive_score, cognitive_state, recommendations_json, trigger_source |
| `user_streaks` | user_id, current_streak, longest_streak, total_checkins, total_xp_earned, last_checkin_date |
| `user_rewards` | id, user_id, reward_type, unlocked_at, xp_bonus, xp_multiplier |
| `user_characters` | id, user_id, character_type, level, xp, happiness, energy |
| `brain_game_results` | id, user_id, game_type, score, duration_seconds, difficulty_level, correct_answers, total_questions |
| `game_sessions` | id, user_id, game_type, score, duration_seconds, is_completed, game_date |
| `new_game_sessions` | id, user_id, game_type, score, duration_seconds, attempts_count, game_date |
| `user_game_stats` | user_id, game_type, total_games, best_score, total_xp |

### noteai_db (NoteAI-backend, порт 5434)

| Таблица | Ключевые поля |
|---|---|
| `notes` | id, user_id, title, content, created_at, updated_at |
| `note_users` | id, username (зеркало из auth_db) |
| `journal_entries` | id, user_id, title, content, mood_score, tags, is_favorite, created_at, updated_at |

---

## 17. JWT и безопасность

### Алгоритм JWT

- Подпись: **HS256** (HMAC-SHA-256) — симметричный, секрет задаётся через `JWT_SECRET`
- Срок действия: 86,400 секунд (24 часа), настраивается через `JWT_EXPIRATION`
- Refresh-токен хранится в БД как UUID-строка

### BCrypt пароли

- Cost factor: **10** (соответствует NIST SP 800-63B)
- Адаптивная стоимость: при росте железа — увеличивается время хэширования
- `PasswordEncoder` интерфейс Spring Security: смена алгоритма в будущем без изменений логики

### Хранение JWT на iOS

- **Keychain API** (kSecClassGenericPassword)
- Шифрование через **Secure Enclave** устройства
- Недоступно другим приложениям (OWASP MASVS-STORAGE-1)

### Фильтр авторизации (JwtAuthenticationFilter)

```java
// extends OncePerRequestFilter
protected void doFilterInternal(request, response, chain) {
    String token = extractBearerToken(request);
    if (token != null && jwtUtil.validateToken(token)) {
        Long userId = jwtUtil.extractUserId(token);
        request.setAttribute("userId", userId);
        // UsernamePasswordAuthenticationToken в SecurityContextHolder
    }
    chain.doFilter(request, response);
}
```

---

---

# ЧАСТЬ V — ИГРОВАЯ СИСТЕМА

---

## 18. XP, персонажи, стрики, бейджи

### Начисление XP

| Действие | XP | Частота |
|---|---|---|
| Ежедневный чекин | +50 | Раз в день |
| Написать заметку/дневник | +45 | Раз в день |
| Когнитивная игра | +30–80 (зависит от результата) | До 3 раз в день |
| Записать сон | +25 | Раз в день |
| Записать настроение | +20 | Раз в день |
| Milestone стрик 7 дней | +200 (единоразово) | Раз |
| Milestone стрик 30 дней | +500 (единоразово) | Раз |

### XP-множители

| Условие | Множитель |
|---|---|
| Стрик >= 7 дней | x 1.1 |
| Стрик >= 30 дней | x 1.25 |

### Уровни персонажа (экспоненциальная кривая)

```
XP_threshold(N) = N^2 * 100
```

| Уровень | Нужно XP |
|---|---|
| 2 | 400 |
| 3 | 900 |
| 4 | 1,600 |
| 5 | 2,500 |
| N | N^2 * 100 |

Это предотвращает инфляцию уровней в начале и поддерживает долгосрочную вовлечённость.

### Адаптивная сложность игр

Уровень сложности выбирается на основе когнитивного скора (XGBoost):
- Скор > 75 — **HARD** (реализует принцип flow theory, Csikszentmihalyi, 1990)
- Скор 50–75 — **MEDIUM**
- Скор < 50 — **EASY**

### Бейджи (Rewards)

| Бейдж | Условие | XP-бонус | XP-множитель |
|---|---|---|---|
| `FIRST_CHECKIN` | Первый чекин | — | — |
| `STREAK_7` | 7 дней стрика подряд | +100 | x 1.1 |
| `STREAK_30` | 30 дней стрика подряд | +500 | x 1.25 |
| `GAME_MASTER` | Определённое количество игр | — | — |

### Логика стрика

- Количество **последовательных дней** с чекином
- Пропуск одного дня сбрасывает в 0
- Если `last_checkin_date == today - 1` — `current_streak += 1`, иначе `current_streak = 1`

---

---

# ЧАСТЬ VI — ПОЛНЫЕ СЦЕНАРИИ ИСПОЛЬЗОВАНИЯ

---

## 19. Примеры флоу от начала до конца

### Флоу 1: Первый запуск приложения

```
1. POST /api/v1/auth/register
   → Создаётся пользователь, isOnboarded=false

2. POST /api/v1/auth/login
   → accessToken + refreshToken в Keychain

3. POST /api/v1/onboarding
   → Пол, рост, вес, дата рождения, ID персонажа

4. POST /api/v1/character/select  (CheckIn Service)
   → Выбираем: CAT / DOG / RABBIT / PANDA / FOX
```

### Флоу 2: Ежедневный чекин

```
1. GET /checkins/today/exists
   → { "exists": false, "canCheckIn": true }

2. POST /checkins
   → { morningMood:4, sleepQuality:7, energyLevel:6, stressLevel:4, ... }
   → HTTP 201 за < 30 мс

   (Фоново через Kafka):
   → Health Metrics: M-Rest=78, M-Ready=72, M-Balance=65
   → ML-рекомендации обновлены
   → Задача COMPLETE_CHECKIN = выполнена (+50 XP)

3. GET /health-metrics/today
   → { mRest:78.5, mReady:72.3, mBalance:65.1, overall:71.9 }

4. POST /ml/recommendations
   → Топ-3 персонализированных рекомендации с predicted_improvement
```

### Флоу 3: Ведение дневника с AI

```
1. POST /api/v1/journal
   → { title:"Мои мысли", content:"...", moodScore:4 }
   → Задача WRITE_NOTE = выполнена, +45 XP

2. Пользователь пишет →
   PATCH /api/v1/journal/{id}  (автосейв с debounce 2–3 сек)
   → { content:"...обновлённый текст..." }

3. POST /api/v1/journal/{id}/ai/analyze
   → { tone, themes, wellnessInsight, suggestion, summary }

4. POST /api/v1/journal/ai/chat
   → { message:"Как улучшить сон?", noteId:15 }
   → AI отвечает с контекстом записи
```

### Флоу 4: Обновление access токена

```
1. Получаем 401 Unauthorized (истёк токен)

2. POST /api/v1/auth/refresh
   Body (raw string): <refresh_token>

3. Получаем новые accessToken + refreshToken

4. Повторяем оригинальный запрос с новым токеном
```

### Флоу 5: Игра — рост персонажа — бейдж

```
1. POST /brain-games/submit
   → { gameType:"NUMBER_SEQUENCE", score:850, durationSeconds:45 }

   (Фоново):
   → Kafka: game.completed → ML-рекомендации обновляются
   → Задача PLAY_GAME = выполнена

2. GET /brain-games/progression
   → { weeklyGames:3, requiredGames:5, xpThisWeek:240, xpRequired:400 }

3. POST /brain-games/level-up  (после набора XP за неделю)
   → { level:4, justLeveledUp:true }

4. POST /rewards/check
   → Проверка и выдача новых бейджей
```

---

---

# ЧАСТЬ VII — ДЛЯ ЗАЩИТЫ ДИПЛОМА

---

## 20. Что рассказать на защите: бэкенд

### Ключевые архитектурные решения (для слайдов)

**1. Зачем микросервисы, а не монолит?**
- Независимое развёртывание: ML Service обновляется без остановки Auth
- Изоляция отказов: падение NoteAI не влияет на чекины
- Масштабирование: ML Service можно вынести на GPU-сервер отдельно
- Database-per-Service: нет shared schema coupling (Newman, 2015)

**2. Зачем Kafka, а не прямые HTTP-вызовы?**
- Ответ пользователю < 30 мс (только запись в БД), ML inference — асинхронно
- При падении ML сервиса: чекин сохранён, метрики пересчитаются позже
- Ordered processing по userId = гарантия последовательности событий

**3. Зачем три разные БД?**
- Auth Service хранит пароли (BCrypt) — изолирован для безопасности
- CheckIn DB — высокая нагрузка (ежедневные запросы), оптимизированные индексы
- NoteAI DB — blob-данные заметок, отдельные миграции

**4. Как решена аутентификация между сервисами?**
- Единый JWT токен из NBAuthService
- CheckIn и NoteAI самостоятельно валидируют через shared JWT_SECRET
- SecurityContext не используется — userId из `request.getAttribute` (stateless)

### Технические детали для вопросов комиссии

| Вопрос | Ответ |
|---|---|
| Как хранятся пароли? | BCrypt, cost factor 10, адаптивный алгоритм |
| Как работает refresh token? | UUID в БД, при /auth/refresh выдаётся новая пара токенов |
| Что происходит при падении ML? | Graceful degradation: формульный расчёт Health Metrics |
| Как избежать дублирования чекинов? | Уникальный constraint (user_id + date) в БД |
| Как работает стрик? | last_checkin_date сравнивается с today-1 по Asia/Almaty |
| Как Kafka гарантирует порядок? | Ключ сообщения = userId → один partition = один порядок |
| Как автовыполняются задачи? | Spring Event (CheckInCreatedEvent) → DailyTaskService.completeTask() |
| Сколько контейнеров в Docker? | 10: 3 Spring Boot + 3 PostgreSQL + ML + Kafka + Zookeeper + Swagger |

---

## 21. Что рассказать на защите: ML часть

### Структура рассказа о ML (8–10 минут)

**Шаг 1 — Проблема:**
«Мы хотим предсказывать когнитивное состояние человека из данных ежедневного самоотчёта. Это задача регрессии (скор 0–100) и классификации (High/Medium/Low).»

**Шаг 2 — Данные:**
«Исходный датасет — `human_cognitive_performance.csv` с физиологически валидированными данными. 10 исходных признаков + 7 производных = 17 итоговых признаков.»

**Шаг 3 — Feature Engineering:**
«Мы не просто передаём сырые признаки. Создаём `CFI` (Cognitive Fatigue Index) — взвешенную комбинацию реакции, стресса, недосыпа и экранного времени. Это composite feature улавливает взаимодействия, которые отдельные признаки не могут выразить.»

**Шаг 4 — Выбор модели:**
«XGBoost выбран как benchmark winner для табличных данных (29/32 Kaggle побед, Chen & Guestrin 2016). CatBoost — для рекомендаций, потому что нативно обрабатывает категориальные признаки (тип рекомендации) без target leakage (Prokhorenkova et al., 2018).»

**Шаг 5 — Обучение:**
«Разбивка: 70/10/20 (train/val/test). Стратифицированная по Cognitive_State. StandardScaler — fit только на train. Early stopping по validation RMSE.»

**Шаг 6 — Результаты:**
«XGBoost Regressor: R²=0.999, RMSE=0.73. Ошибка менее 1 балла из 100. Классификатор: Accuracy=97.9%. CatBoost для рекомендаций: R²=0.966, RMSE=1.81.»

**Шаг 7 — Health Metrics:**
«Три отдельных XGBoost-модели для M-Rest, M-Ready, M-Balance. Обучены на 12,000 синтетических записей. M-Ready: R²=0.971, MAE=2.02.»

**Шаг 8 — Интеграция:**
«Модели загружаются в память при старте Flask (zero disk I/O при inference). XGBoost: 2–5 мс, CatBoost: 10–20 мс. ML Service недоступен снаружи Docker — только Java бэкенд обращается к нему.»

**Шаг 9 — Graceful Degradation:**
«При недоступности ML Service Java автоматически переключается на детерминированные формулы. Пользователь всегда видит метрики.»

### Возможные вопросы комиссии по ML

| Вопрос | Ответ |
|---|---|
| Почему синтетические данные? | Реальные данные по когнитивной производительности недоступны публично в нужном объёме. Синтетика создана на основе физиологически валидированных распределений из литературы. |
| Откуда такой высокий R²=0.999? | Модель обучена на синтетических данных с известными закономерностями. На реальных данных R² будет ниже (~0.85–0.92). |
| Как проверяли переобучение? | Early stopping по validation set + отдельный test set который не участвовал в подборе параметров. Разница train/val RMSE незначительна. |
| Почему не нейронные сети? | Shwartz-Ziv & Armon (2022): gradient boosting превосходит DL на табличных данных. Плюс: интерпретируемость (SHAP), скорость inference, меньше данных. |
| Что такое CFI? | Cognitive Fatigue Index — составной признак: 30% реакция + 25% стресс + 25% недосып + 20% экранное время. Имитирует исследования Killgore (2010). |
| Как интерпретируете модель? | SHAP (SHapley Additive exPlanations) — инструмент для объяснения XGBoost. Sleep_Duration и Stress_Level имеют наибольшую важность. |
| Почему 3 отдельные модели для метрик? | Каждая метрика имеет разный профиль признаков. M-Rest определяется сном, M-Balance — стрессом и lifestyle. Одна модель с 3 таргетами теряет специализацию. |
| Как обновляются рекомендации? | Kafka-событие → Java → ML /recommend/top3 → кэш в daily_ml_recommendation. Обновляется после каждого чекина, сна или игры. |

---

## 22. Сравнение с аналогами и научное обоснование

### Конкурентная карта

| Приложение | ML скор | Рекомендации | AI дневник | Геймификация | Трекинг сна+настроения |
|---|---|---|---|---|---|
| **NeuroBalance** | XGBoost | CatBoost | Groq LLM | да | да |
| Headspace | нет | контент-based | нет | нет | нет |
| Calm | нет | контент-based | нет | нет | нет |
| Daylio | нет | нет | нет | нет | да (базовый) |
| Apple Health | нет | нет | нет | нет | частично |

**Уникальное преимущество NeuroBalance:** единственное приложение, которое сочетает ежедневный ML-driven когнитивный скор + персонализированные рекомендации + AI-дневник + геймификацию.

### Научное обоснование компонентов системы

| Исследование | Применение в системе |
|---|---|
| Killgore (2010) — сон и когниция | Основа для M-Rest, CFI index, приоритет sleep в XGBoost |
| Walker (2017) — «Why We Sleep» | Норма 8 часов, REM/deep sleep компоненты, scientific_basis для SLEEP_INCREASE |
| McEwen (2007) — Allostatic Load | Стресс как ключевой компонент M-Balance, scientific_basis для STRESS_DECREASE |
| Ratey (2008) — «Spark», BDNF | Физическая активность повышает BDNF (нейропластичность), scientific_basis для EXERCISE_INCREASE |
| Newport (2016) — «Digital Minimalism» | Экранное время > 8 часов = cognitive fragmentation, scientific_basis для SCREEN_DECREASE |
| Stults-Kolehmainen & Sinha (2014) | Упражнения + социальная активность + питание = основа M-Balance формулы |
| Csikszentmihalyi (1990) — Flow Theory | Адаптивная сложность игр по когнитивному скору (Easy/Medium/Hard) |
| Shwartz-Ziv & Armon (2022) | XGBoost/CatBoost > нейросети для табличных данных |
| Chen & Guestrin (2016) | Основная статья XGBoost |
| Prokhorenkova et al. (2018) | Основная статья CatBoost |

### Ключевые числа для слайдов

```
XGBoost Regressor:       R² = 0.999,  RMSE = 0.73  (на 100-балльной шкале)
XGBoost Classifier:      Accuracy = 97.9%
CatBoost рекомендации:   R² = 0.966,  RMSE = 1.81

M-Rest model:    R² = 0.917,  MAE = 2.01
M-Ready model:   R² = 0.971,  MAE = 2.02
M-Balance model: R² = 0.967,  MAE = 2.13

API latency:     < 30 мс (чекин) + 200 мс (ML, async)
ML inference:    2–5 мс (XGBoost), 10–20 мс (CatBoost)
Training data:   12,000 записей (health metrics) + синтетика (когнитивный скор)
Features:        17 финальных признаков (10 исходных + 7 инженерных)
Docker:          10 контейнеров
```

---

### Структура презентации для диплома (рекомендуемая)

**Слайд 1 — Проблема:** рынок wellness-приложений, что не так с существующими решениями

**Слайд 2 — Решение:** NeuroBalance, уникальные фичи, целевая аудитория

**Слайд 3 — Архитектура системы:** диаграмма микросервисов, Docker Compose, 10 контейнеров

**Слайд 4 — Поток данных:** как чекин → Kafka → ML → метрики → кэш (диаграмма флоу)

**Слайд 5 — JWT и безопасность:** HS256, BCrypt, Keychain на iOS

**Слайд 6 — ML: постановка задачи:** 3 задачи, датасет, признаки, feature engineering

**Слайд 7 — XGBoost модель:** параметры, метрики (R²=0.999, RMSE=0.73), важность признаков (SHAP)

**Слайд 8 — CatBoost рекомендации:** зачем, 4 типа рекомендаций, predicted_improvement, scientific basis

**Слайд 9 — Health Metrics:** 3 метрики, формулы, R² каждой модели, labels

**Слайд 10 — Сравнение с конкурентами:** таблица фичей vs Headspace/Calm/Daylio

**Слайд 11 — Демо:** скриншоты iOS приложения (12 экранов пользовательского пути)

**Слайд 12 — Итоги и развитие:** B2B tier, корпоративные дашборды, wearables интеграция

---

*Документ обновлён: 2026-06-07. Версия 3.0.*
