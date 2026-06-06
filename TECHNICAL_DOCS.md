# NeuroBalance Backend — Техническая документация

## Содержание
1. [Архитектура проекта](#1-архитектура-проекта)
2. [Запуск](#2-запуск)
3. [NBAuthService — Авторизация](#3-nbauthservice--авторизация-port-8081)
4. [NBCheckinService — Основной сервис](#4-nbcheckinservice--основной-сервис-port-8082)
5. [NoteAI Backend — AI Заметки](#5-noteai-backend--ai-заметки-port-8083)
6. [ML Service — Рекомендации](#6-ml-service--рекомендации-port-5001)
7. [Kafka — Реальное время](#7-kafka--потоки-реального-времени)
8. [Базы данных](#8-базы-данных)
9. [Гейм-система и XP](#9-гейм-система-и-xp)
10. [Примеры запросов по сценариям](#10-примеры-запросов-по-сценариям)

---

## 1. Архитектура проекта

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         МОБИЛЬНОЕ ПРИЛОЖЕНИЕ (Flutter)                   │
└──────┬──────────────┬───────────────────────────┬────────────────────────┘
       │              │                           │
       ▼              ▼                           ▼
┌────────────┐  ┌───────────────────┐   ┌──────────────────┐
│NBAuthService│  │  NBCheckinService │   │  NoteAI Backend  │
│  port 8081  │  │    port 8082      │   │    port 8083      │
│             │  │                   │   │                  │
│ JWT Auth    │  │ Основная логика:  │   │ AI Журнал        │
│ Онбординг   │  │ Чекины, Стрики    │   │ AI Заметки       │
│ Верификация │  │ Игры, Сон         │   │ Gemini/Groq AI   │
│             │  │ Настроение        │   │                  │
│ БД: users   │  │ Health Metrics    │   │ БД: noteai       │
│ PostgreSQL  │  │ ML Рекомендации   │   │ PostgreSQL       │
│ port 5434   │  │ Стрики, XP        │   │ port 5433        │
└─────────────┘  │                   │   └──────────────────┘
       ▲          │ БД: nb_checkin    │          │
       │          │ PostgreSQL        │          │
       │          │ port 5435         │          │
       │          └────────┬──────────┘          │
       │                   │                     │
       │          ┌────────▼──────────┐          │
       │          │  Kafka Broker     │          │
       │          │  port 29092/9092  │          │
       │          │                   │          │
       │          │ Topics:           │          │
       │          │ checkin.created   │          │
       │          │ sleep.logged      │          │
       │          │ game.completed    │          │
       │          │ character.leveled │          │
       │          └────────┬──────────┘          │
       │                   │                     │
       │          ┌────────▼──────────┐          │
       │          │   ML Service      │          │
       │          │   port 5001       │          │
       │          │   Python Flask    │          │
       │          │   XGBoost/CatBoost│          │
       │          │   In-memory cache │          │
       └──────────└───────────────────┘──────────┘
                  JWT forwarding for onboarding
```

### Технологии
| Компонент | Технология |
|-----------|-----------|
| NBAuthService | Java 17, Spring Boot 4, PostgreSQL 15 |
| NBCheckinService | Java 17, Spring Boot 4, PostgreSQL 15, Kafka |
| NoteAI Backend | Java 17, Spring Boot, PostgreSQL 15 |
| ML Service | Python 3.11, Flask, XGBoost 1.7.6, CatBoost, scikit-learn 1.3.2 |
| Message Broker | Apache Kafka (Confluent 7.6.0) + Zookeeper |
| Containerization | Docker + Docker Compose |
| Timezone | Asia/Almaty (UTC+5) — везде без исключений |

---

## 2. Запуск

### Полный запуск через Docker Compose
```bash
cd /path/to/NeuroBalanceBackend

# Запустить всё
docker-compose up -d

# Остановить всё
docker-compose down

# Пересобрать и перезапустить конкретный сервис
docker-compose build nb-checkin-service && docker-compose up -d nb-checkin-service
```

### Адреса сервисов
| Сервис | URL | Swagger/UI |
|--------|-----|-----------|
| NBAuthService | http://localhost:8081 | http://localhost:8081/swagger-ui.html |
| NBCheckinService | http://localhost:8082/api/v1 | http://localhost:8082/api/v1/swagger-ui/index.html |
| NoteAI Backend | http://localhost:8083 | — |
| ML Service | http://localhost:5001 | http://localhost:5001/swagger/ |
| pgAdmin | http://localhost:5051 | логин: admin@admin.com / admin |
| Kafka | localhost:29092 (external) | — |

### pgAdmin — подключение к базам данных
1. Открыть http://localhost:5051, войти (admin@admin.com / admin)
2. В левой панели: Servers → NeuroBalance:
   - **Auth DB** (`postgres`, пароль: `mayddee`) — пользователи, онбординг
   - **Checkin DB** (`postgres-checkin`, пароль: `mayddee`) — всё основное + `daily_ml_recommendation`

---

## 3. NBAuthService — Авторизация (port 8081)

### Описание
Отвечает за регистрацию, вход, JWT-токены, верификацию email/SMS, онбординг пользователя. Все остальные сервисы проверяют JWT, выданный этим сервисом.

### Как работает JWT
1. Пользователь логинится → получает `token` (access token) и `refreshToken`
2. Все запросы к NBCheckinService и NoteAI Backend передают: `Authorization: Bearer <token>`
3. `AuthFilter` в каждом сервисе верифицирует токен и достаёт `userId` из claims
4. В контроллерах: `Long userId = (Long) request.getAttribute("userId")`

---

### Эндпойнты NBAuthService

#### 🔐 Аутентификация — `/api/v1/auth`

**Регистрация**
```
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "madina",
  "password": "Test1234!",
  "email": "madina@example.com",
  "name": "Madina"
}

Ответ 200:
{
  "userId": 2,
  "username": "madina",
  "email": "madina@example.com",
  "message": "Registration successful",
  "isOnboarded": false
}
```

**Вход**
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "madina",
  "password": "Test1234!"
}

Ответ 200:
{
  "id": 2,
  "username": "madina",
  "name": "Madina",
  "email": "madina@example.com",
  "token": "eyJhbGciOiJIUzI1NiJ9...",     ← ACCESS TOKEN (использовать везде)
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "onboarded": true
}
```

**Обновить токен**
```
POST /api/v1/auth/refresh
Body: "eyJhbGciOiJIUzI1NiJ9..."   (строка refreshToken)
```

**Ручная верификация (для тестов — без email)**
```
POST /api/v1/auth/verify-user-manual?username=madina
```

**Верификация email**
```
GET /api/v1/auth/verify-email?token=<verification_token>
```

---

#### 👤 Пользователи — `/api/v1/users`

```
GET  /api/v1/users/me              → данные текущего пользователя
GET  /api/v1/users/{id}            → данные пользователя по ID
GET  /api/v1/users/search?q=madina → поиск пользователей
DELETE /api/v1/users/{id}          → удалить пользователя
```

---

#### 📋 Онбординг — `/api/v1/onboarding`

Онбординг заполняется один раз после регистрации. Данные используются ML-сервисом для персонализированных рекомендаций (age, gender).

**Создать/обновить онбординг**
```
POST /api/v1/onboarding
Authorization: Bearer <token>
Content-Type: application/json

{
  "sex": "MALE",           // MALE | FEMALE | OTHER
  "heightCm": 178,
  "weightKg": 72,
  "birthDate": "2000-05-15",
  "characterId": 1,        // ID персонажа (1-4)
  "dataConsent": true
}

Ответ 200:
{
  "id": 1,
  "userId": 2,
  "sex": "MALE",
  "heightCm": 178,
  "weightKg": 72,
  "birthDate": "2000-05-15",
  "characterId": 1,
  "isCompleted": true
}
```

**Получить онбординг**
```
GET /api/v1/onboarding
Authorization: Bearer <token>

PATCH /api/v1/onboarding    → частичное обновление (только нужные поля)
GET /api/v1/onboarding/status → {"isOnboarded": true, "isCompleted": true}
```

---

## 4. NBCheckinService — Основной сервис (port 8082)

**Base URL:** `http://localhost:8082/api/v1`  
Все эндпойнты требуют `Authorization: Bearer <token>`

### Архитектура сервиса

```
HTTP Request
    ↓
AuthFilter (проверяет JWT, ставит userId в request.getAttribute)
    ↓
Controller (@RequestMapping)
    ↓
Service (@Transactional)
    ↓
Repository (Spring Data JPA) → PostgreSQL (nb_checkin)
    ↓
EventPublisher → @TransactionalEventListener(AFTER_COMMIT)
    ↓
KafkaProducerService → Kafka Topics
    ↓
Kafka Consumers → asyncRefresh ML, HealthMetrics, CharacterProgression
```

---

### 📅 Чек-ины — `/api/v1/checkins`

Ежедневный чек-ин — основное действие пользователя. Автоматически:
- Обновляет стрик
- Помечает задачу `COMPLETE_CHECKIN`
- Если `sleepHours >= 7` — помечает `SLEEP_7_HOURS`
- Обновляет счастье персонажа
- Публикует Kafka: `checkin.created` → пересчёт ML + Health Metrics

**Создать чек-ин**
```
POST /api/v1/checkins
Content-Type: application/json

{
  "morningMood": 4,          // 1-5
  "eveningMood": 3,          // 1-5
  "energyLevel": 8,          // 1-10
  "stressLevel": 3,          // 1-10 (1=мало, 10=много)
  "sleepHours": 7.5,         // часы сна (decimal)
  "sleepQuality": 8,         // 1-10
  "sleepBedtime": "23:00",   // опционально
  "sleepWaketime": "07:30",  // опционально
  "physicalActivityMinutes": 45,
  "physicalActivityType": "Бег",
  "didExercise": true,
  "ateHealthy": true,
  "hadSocialInteraction": false,
  "playedCognitiveGameToday": true,
  "cognitiveGameCount": 2,
  "checkInDate": "2026-06-06" // опционально, дефолт = сегодня (Almaty)
}

Ответ 201:
{
  "id": 1,
  "userId": 2,
  "checkInDate": "2026-06-06",
  "morningMood": 4,
  "wellnessScore": 67.5,     // 0-100 автоматически
  "isComplete": true,
  "streakInfo": {
    "currentStreak": 3,
    "longestStreak": 5,
    "xpEarned": 10,
    "bonusXp": 0,
    "isMilestone": false,
    "message": "3 дня подряд! Продолжай!",
    "nextMilestone": 7
  }
}
```

**Другие эндпойнты чекинов**
```
GET  /api/v1/checkins/today              → чекин за сегодня (404 если нет)
GET  /api/v1/checkins/today/exists       → {"exists": true, "canCheckIn": false}
GET  /api/v1/checkins/{date}             → чекин за дату (формат: 2026-06-06)
PUT  /api/v1/checkins/{date}             → обновить чекин (PATCH-семантика)
DELETE /api/v1/checkins/{date}           → удалить чекин
GET  /api/v1/checkins/recent             → последние 30 дней
GET  /api/v1/checkins?startDate=&endDate= → за диапазон дат
GET  /api/v1/checkins/streak             → текущий стрик
POST /api/v1/checkins/streak/recalculate → пересчитать стрик
GET  /api/v1/checkins/calendar?year=2026&month=6  → даты с чекинами (для календаря)
GET  /api/v1/checkins/stats/weekly       → статистика за неделю
GET  /api/v1/checkins/stats/monthly      → статистика за месяц
```

---

### 😴 Сон — `/api/v1/sleep`

Детальный лог сна. При создании/обновлении:
- Если `totalHours >= 7` → автоматически помечает задачу `SLEEP_7_HOURS`
- Публикует Kafka: `sleep.logged` → пересчёт ML + M-Rest обогащение

```
POST /api/v1/sleep
Content-Type: application/json

{
  "sleepDate": "2026-06-06",          // обязательно
  "bedtime": "23:00",
  "wakeTime": "07:30",
  "fellAsleepTime": "23:20",          // когда реально заснул
  "totalHours": 8.5,                   // можно передать напрямую
  "actualSleepHours": 8.0,
  "qualityScore": 8,                   // 1-10
  "feltRested": true,
  "interruptionsCount": 1,
  "deepSleepMinutes": 90,
  "lightSleepMinutes": 150,
  "remSleepMinutes": 80,
  "awakeMinutes": 20,
  "hadDreams": true,
  "dreamNotes": "Красивый сон про горы",
  "morningMood": 4,                    // 1-5
  "morningEnergy": 8,                  // 1-10
  "caffeineBeforeBed": false,
  "screenTimeBeforeBedMinutes": 30,
  "roomTemperature": 20.0,
  "notes": "Спал хорошо"
}

// Если лог за эту дату уже есть → автоматически обновляет (upsert)
```

```
GET  /api/v1/sleep/today             → сегодняшний лог (404 если нет)
GET  /api/v1/sleep/today/exists      → {"exists": true}
GET  /api/v1/sleep/{id}              → лог по ID
GET  /api/v1/sleep/date/{date}       → лог за дату
GET  /api/v1/sleep/range?startDate=&endDate= → за диапазон
GET  /api/v1/sleep/recent            → последние 30 логов
PUT  /api/v1/sleep/{id}              → обновить по ID
PUT  /api/v1/sleep/date/{date}       → обновить по дате
DELETE /api/v1/sleep/{id}            → удалить по ID
DELETE /api/v1/sleep/date/{date}     → удалить по дате
```

---

### 😊 Настроение — `/api/v1/mood`

При создании/обновлении:
- Автоматически помечает задачу `LOG_MOOD`
- Триггерит пересчёт ML рекомендаций

```
POST /api/v1/mood
Content-Type: application/json

{
  "moodValue": 4,                    // 1-5
  "moodLabel": "Happy",
  "intensity": 3,                    // 1-5
  "contextNote": "Хороший день",
  "location": "Дом",
  "activity": "Работа",
  "triggers": "Завершил проект",
  "physicalSensations": "Легкость",
  "logDate": "2026-06-06",           // опционально
  "logTimestamp": "2026-06-06T14:30:00" // опционально
}
```

```
GET  /api/v1/mood              → все логи настроения
GET  /api/v1/mood/today        → сегодняшние логи
GET  /api/v1/mood/{id}         → конкретный лог
GET  /api/v1/mood/date/{date}  → логи за дату
GET  /api/v1/mood/recent       → последние
GET  /api/v1/mood/range?startDate=&endDate=
GET  /api/v1/mood/average?startDate=&endDate= → среднее значение
GET  /api/v1/mood/trigger/{trigger}           → логи по триггеру
PUT  /api/v1/mood/{id}         → обновить (PATCH-семантика)
DELETE /api/v1/mood/{id}       → удалить
```

---

### 📋 Ежедневные задачи — `/api/v1/tasks`

5 ежедневных задач, сброс в 00:00 по Asia/Almaty. Все выполняются АВТОМАТИЧЕСКИ:

| Задача | Когда выполняется |
|--------|-------------------|
| `COMPLETE_CHECKIN` | При создании чекина |
| `SLEEP_7_HOURS` | При чекине с sleepHours≥7 ИЛИ при создании SleepLog с totalHours≥7 |
| `PLAY_GAME` | При завершении любой игры (BrainGame, GameSession, NewGameSession) |
| `LOG_MOOD` | При создании MoodLog |
| `WRITE_NOTE` | NoteAI-backend вызывает POST /tasks/note-written после создания заметки |

```
GET  /api/v1/tasks/today              → задачи на сегодня со статусами
GET  /api/v1/tasks/date/{date}        → задачи за дату
GET  /api/v1/tasks/stats              → статистика выполнения
GET  /api/v1/tasks/history            → история
POST /api/v1/tasks/complete           → ручное выполнение задачи
POST /api/v1/tasks/note-written       → вызывается NoteAI при создании заметки (JWT required)
```

Ответ `GET /tasks/today`:
```json
[
  {"taskType": "COMPLETE_CHECKIN", "completed": true, "completedAt": "2026-06-06T10:00:00"},
  {"taskType": "SLEEP_7_HOURS", "completed": true, "completedAt": "2026-06-06T08:30:00"},
  {"taskType": "PLAY_GAME", "completed": false},
  {"taskType": "LOG_MOOD", "completed": false},
  {"taskType": "WRITE_NOTE", "completed": false}
]
```

---

### 🧠 Когнитивные игры — `/api/v1/brain-games`

Тип: NUMBER_SEQUENCE, MEMORY_PAIRS. XP: 10–500 за игру.

**Формула XP:**
- Победа: `(50 + speedBonus(0-50) + accuracyBonus(0-50)) × difficulty`
- Поражение: EASY=10, MEDIUM=15, HARD=20
- difficulty: EASY×1.0, MEDIUM×1.5, HARD×2.5

При завершении: Kafka `game.completed` → ML refresh + character level-up check

```
POST /api/v1/brain-games/submit
Content-Type: application/json

{
  "gameType": "NUMBER_SEQUENCE",   // NUMBER_SEQUENCE | MEMORY_PAIRS
  "score": 1200,
  "timeTakenSeconds": 28,
  "isWin": true,
  "difficultyLevel": "HARD",       // EASY | MEDIUM | HARD
  "mistakesCount": 0
}

Ответ:
{
  "id": 1,
  "gameType": "NUMBER_SEQUENCE",
  "score": 1200,
  "xpEarned": 375,
  "isWin": true,
  "difficultyLevel": "HARD"
}
```

```
GET  /api/v1/brain-games/history              → история игр
GET  /api/v1/brain-games/history/date?date=   → за конкретный день
GET  /api/v1/brain-games/stats                → winRate, totalXp, totalGames
GET  /api/v1/brain-games/progression          → прогресс level-up персонажа
POST /api/v1/brain-games/level-up             → повысить уровень (если условия выполнены)
```

**Условия level-up:**
| Уровень | Требования |
|---------|-----------|
| 1→2 | 15 когн. игр/нед, winRate ≥ 50%, 500 XP |
| 2→3 | 20 игр/нед, ≥10 на MEDIUM/HARD, 7 дней стрика, 2000 XP |
| 3→4 | 20 игр/нед, ≥15 на HARD, 14 дней стрика, 4500 XP |
| 4→5 | 25 игр/нед, ≥25 на HARD, 21 день стрика, 8000 XP |

---

### 🎮 Улучшенные игры (новые) — `/api/v1/new-game-sessions`

Тип: DONUT_GAME, NUMBER_SEQUENCE_GAME. До 3 XP-сессий в день на тип.

```
POST /api/v1/new-game-sessions
Content-Type: application/json

{
  "gameType": "NUMBER_SEQUENCE_GAME",   // DONUT_GAME | NUMBER_SEQUENCE_GAME
  "durationSeconds": 45,
  "isCompleted": true,
  "isWon": true,
  "attemptsCount": 1,                    // для NUMBER_SEQUENCE_GAME
  "difficultyLevel": "MEDIUM",
  "gameDate": "2026-06-06"               // опционально
}
```

**Бонусы XP:**
- DONUT_GAME (выживание): >45s=+20, >90s=+40, >150s=+60
- NUMBER_SEQUENCE_GAME (скорость): <30s=+40, <60s=+25, <90s=+15
- Попытки (NUMBER_SEQUENCE): 1=+30, 2=+15, 3=+5
- Стрик-множитель применяется к базовому XP

```
GET  /api/v1/new-game-sessions/highscore?type=NUMBER_SEQUENCE_GAME → рекорд
GET  /api/v1/new-game-sessions/today-stats     → статистика за день
GET  /api/v1/new-game-sessions/played-today    → {"playedToday": true}
GET  /api/v1/new-game-sessions/history/date?date=2026-06-06
```

---

### 🎮 Fun игры (старые) — `/api/v1/game-sessions`

Тип: DONUT_GAME, CHARACTER_CARE. Аналогичная логика XP.

```
POST /api/v1/game-sessions
{
  "gameType": "DONUT_GAME",
  "durationSeconds": 90,
  "isCompleted": true,
  "isWon": true,
  "difficultyLevel": "EASY"
}

GET /api/v1/game-sessions/history?date=2026-06-06
GET /api/v1/game-sessions/today-stats
GET /api/v1/game-sessions/recent?limit=10
GET /api/v1/game-sessions/played-today
```

---

### 🏃 Стрики и лидерборд — `/api/v1/streaks`

```
GET  /api/v1/streaks                   → текущий стрик юзера
POST /api/v1/streaks/recalculate       → пересчитать стрик
GET  /api/v1/streaks/leaderboard/streak → топ по стрику
GET  /api/v1/streaks/leaderboard/xp    → топ по XP
GET  /api/v1/streaks/rank              → позиция текущего юзера
```

**Стрик-мультипликаторы XP** (автоматически применяются к играм):
| Дней | Множитель |
|------|-----------|
| 3 | ×1.1 |
| 7 | ×1.3 |
| 14 | ×1.5 |
| 20 | ×1.7 |
| 21 | ×1.8 |
| 30 | ×2.0 |
| 50 | ×2.5 |
| 100 | ×3.0 |

---

### 🏆 Награды — `/api/v1/rewards`

Разблокируются автоматически при выполнении условий.

```
GET  /api/v1/rewards              → все награды юзера
GET  /api/v1/rewards/unlocked     → только разблокированные
POST /api/v1/rewards/check        → проверить и разблокировать новые
GET  /api/v1/rewards/xp-multiplier → текущий XP-множитель стрика
```

**Достижения:**
| Badge | Условие |
|-------|---------|
| FIRST_GAME_PLAYED | 1-я игра |
| GAME_MASTER_10 | 10 игр |
| GAME_MASTER_50 | 50 игр |
| FIRST_MOOD_LOG | 1-й мод-лог |
| MOOD_TRACKER_7 | 7 мод-логов |
| FIRST_LEVEL_UP | Уровень 2 |
| LEVEL_3_REACHED | Уровень 3 (XP×1.1) |
| PERFECT_DAY | Все 5 задач за день (XP×1.1) |

---

### 👤 Персонаж — `/api/v1/character`

```
GET  /api/v1/character              → данные персонажа (уровень, XP, счастье)
POST /api/v1/character/select       → выбрать персонажа при регистрации
POST /api/v1/character/change-type  → сменить тип персонажа
POST /api/v1/character/feed         → покормить (+счастье)
POST /api/v1/character/play         → поиграть (+счастье)
GET  /api/v1/character/progression  → детальный прогресс level-up
```

Ответ `GET /character`:
```json
{
  "characterType": "BEAR",
  "level": 2,
  "currentXp": 1250,
  "xpToNextLevel": 2000,
  "happinessLevel": 75,
  "totalXpEarned": 1250
}
```

---

### 💚 Health Metrics — `/api/v1/health-metrics`

Три показателя здоровья (0-100), вычисляются автоматически после каждого чекина.

**Формулы:**
- **M-Rest** = `min(sleep/8, 1)×50 + качество сна×30 + субъективная усталость×20`
- **M-Ready** = `(energy/10)×40 + (morningMood/5)×30 + MRest×0.30`
- **M-Balance** = `max((10-stress)/9, 0)×40 + привычки(13.3каждая)×40 + (eveningMood/5)×20`

```
GET /api/v1/health-metrics/today              → метрики за сегодня
GET /api/v1/health-metrics/{date}             → за конкретный день
GET /api/v1/health-metrics/recent?days=7      → за 7 дней
GET /api/v1/health-metrics/history            → вся история
POST /api/v1/health-metrics/recalculate?date= → пересчитать за дату
```

Ответ:
```json
{
  "metricDate": "2026-06-06",
  "mRest": 78,
  "mReady": 72,
  "mBalance": 65,
  "overallWellnessScore": 72,
  "mRestLabel": "Good",
  "mReadyLabel": "Good",
  "mBalanceLabel": "Good"
}
```

---

### 🤖 ML Рекомендации — `/api/v1/ml`

Персонализированные рекомендации на основе реальных данных пользователя.

**Источники данных для ML:**
| Параметр | Источник |
|----------|---------|
| sleep_duration | Среднее sleepHours из чекинов за 7 дней |
| stress_level | Среднее stressLevel из чекинов за 7 дней |
| exercise_frequency | Дней с didExercise=true за 7 дней |
| memory_test_score | WinRate когнитивных игр |
| reaction_time | Лучшее время NUMBER_SEQUENCE |
| age | Онбординг → birthDate (24ч кеш) |
| gender | Онбординг → sex (24ч кеш) |

**Фронтенд вызывает Python напрямую:**
```
POST http://localhost:5001/recommend/top3
Content-Type: application/json

{
  "sleep_duration": 7.0,
  "stress_level": 5,
  "daily_screen_time": 8.0,
  "exercise_frequency": 3,
  "caffeine_intake": 2,
  "reaction_time": 300,
  "memory_test_score": 70,
  "age": 25,
  "gender": "Male",
  "diet_type": "Non-Vegetarian",
  "user_id": 2              ← ОБЯЗАТЕЛЬНО! Иначе кеш не используется
}

Ответ:
{
  "status": "success",
  "cognitive_score": 68.3,
  "summary": "Ваш когнитивный счет: 68.3...",
  "recommendations": [
    {
      "type": "SLEEP_INCREASE",
      "title": "🌙 Оптимизация сна",
      "priority": "HIGH",
      "predicted_improvement": 14.5,
      "baseline": 6.0,
      "recommended_target": 8.0,
      "actions": ["Будильник на отбой в 23:00", "Затемните спальню"],
      "scientific_basis": "Walker (2017): Why We Sleep"
    }
  ],
  "total_potential": 33.1
}
```

**Java бэкенд эндпойнты (альтернатива через Swagger):**
```
POST /api/v1/ml/recommendations           → получить рекомендации из DB/кеша
GET  /api/v1/ml/recommendations           → то же самое (удобно в Swagger)
POST /api/v1/ml/recommendations/refresh   → форс-рефреш (async)
GET  /api/v1/ml/recommendations/history   → история из таблицы daily_ml_recommendation
```

**Как обновляются рекомендации (автоматически):**
```
1. check-in создан   → Kafka: checkin.created   → asyncRefresh(userId)
2. sleep log сохранён → Kafka: sleep.logged     → asyncRefresh(userId)
3. игра завершена    → Kafka: game.completed    → asyncRefresh(userId)
4. mood log создан   → afterCommit()            → asyncRefresh(userId)
5. первый GET/POST   → onboarding loaded        → recompute с реальным age/gender
```

---

## 5. NoteAI Backend — AI Заметки (port 8083)

### Описание
Сервис для ведения AI-журнала с автоматическим анализом записей через Groq AI. Изолированные данные — пользователь видит только свои записи.

**Базовый URL:** `http://localhost:8083`

---

### 📖 Журнал — `/api/v1/journal`

```
POST /api/v1/journal
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Мои мысли",
  "content": "Сегодня был продуктивный день...",
  "moodScore": 4,           // 1-5, опционально
  "tags": "работа,идеи",   // опционально
  "isFavorite": false
}

Ответ 201:
{
  "id": 1,
  "userId": 2,
  "title": "Мои мысли",
  "content": "Сегодня был продуктивный день...",
  "moodScore": 4,
  "createdAt": "2026-06-06T14:30:00"
}
```

При создании → автоматически вызывает `POST /api/v1/tasks/note-written` в NBCheckinService (с тем же JWT) → помечает задачу `WRITE_NOTE` и даёт +45 XP.

```
GET  /api/v1/journal                         → все записи текущего юзера
GET  /api/v1/journal/today                   → записи за сегодня
GET  /api/v1/journal/{id}                    → конкретная запись (403 если чужая!)
GET  /api/v1/journal/date/{date}             → за дату
GET  /api/v1/journal/range?startDate=&endDate=
GET  /api/v1/journal/favorites               → избранные

PATCH /api/v1/journal/{id}                   → обновить (auto-save, PATCH-семантика)
DELETE /api/v1/journal/{id}                  → удалить
```

**AI анализ записи:**
```
POST /api/v1/journal/{id}/ai/analyze
Authorization: Bearer <token>

Ответ:
{
  "noteId": 1,
  "type": "analysis",
  "summary": "Краткое содержание записи...",
  "tone": "Положительный",
  "themes": ["работа", "спорт", "отдых"],
  "wellnessInsight": "Активный день положительно влияет на M-Ready",
  "suggestion": "Попробуйте 10-минутную прогулку перед сном"
}
```

**AI саммари:**
```
POST /api/v1/journal/{id}/ai/summary
Authorization: Bearer <token>
```

---

### 📝 Заметки — `/api/v1/notes`

Старый модуль заметок (без AI-анализа).

```
GET  /api/v1/notes          → все заметки юзера
GET  /api/v1/notes/{id}     → конкретная заметка
```

---

### 💬 AI Чат — `/api/chat`

```
GET /api/chat/string?message=Как улучшить сон?
→ Стриминг ответа AI как plain text
```

---

### 🔒 Безопасность данных

- Все записи фильтруются по `userId` из JWT
- `GET /journal/{id}` чужой записи → **403 Forbidden**
- Пользователь A не может просмотреть, изменить или удалить записи пользователя B

---

## 6. ML Service — Рекомендации (port 5001)

### Описание
Python Flask сервис с обученными ML-моделями:
- **XGBoost** (`cognitive_score_model.pkl`) — предсказывает когнитивный индекс
- **CatBoost** (`catboost_recommendation_model.cbm`) — предсказывает улучшение от действий
- **StandardScaler** + **LabelEncoder** — нормализация данных

### Эндпойнты ML Service

#### Основной — для фронта
```
POST /recommend/top3
Content-Type: application/json

{
  "sleep_duration": 7.0,      // часы сна (среднее за 7 дней)
  "stress_level": 5,          // 1-10
  "daily_screen_time": 8.0,   // часы экрана в день
  "exercise_frequency": 3,    // дней с тренировкой за неделю (0-7)
  "caffeine_intake": 2,       // чашек кофе в день
  "reaction_time": 300,       // мс (проксировано из игры NUMBER_SEQUENCE)
  "memory_test_score": 70,    // % побед в когнитивных играх
  "age": 25,
  "gender": "Male",           // Male | Female
  "diet_type": "Non-Vegetarian", // Non-Vegetarian | Vegetarian | Vegan
  "user_id": 2,               // если есть — возвращает кешированные РЕАЛЬНЫЕ данные
  "internal": true            // только для Java backend (форс-пересчёт, не кеш)
}
```

**Логика кеша:**
- `user_id` без `internal` → проверяет in-memory кеш → если есть запись за сегодня → возвращает её (реальные данные от Java)
- `user_id` без `internal` + нет кеша → вычисляет с переданными параметрами (дефолты) → кеширует
- `user_id` + `internal: true` → всегда вычисляет заново + обновляет кеш (Java backend)
- Без `user_id` → всегда вычисляет (не кеширует)

#### Когнитивный анализ
```
POST /predict
Body: те же параметры без user_id

Ответ: {"cognitive_score": 68.3, "cognitive_state": "Medium", "cfi_score": 31.2}
```

#### Health Metrics через ML
```
POST /health-metrics/calculate     → M-Rest/M-Ready/M-Balance по формулам
POST /health-metrics/ml-predict    → то же через XGBoost модели (если обучены)
POST /health-metrics/trend         → тренд за несколько дней
```

#### Внутренний — Java backend
```
POST /internal/cache/update
Body: {"user_id": 2, "result": {...рекомендации...}}
→ Обновляет in-memory кеш (вызывается Java после Kafka-триггеров)
```

#### Служебные
```
GET /health        → {"status": "healthy", "version": "5.1.0"}
GET /swagger/      → Swagger UI со всеми эндпойнтами
```

---

## 7. Kafka — Потоки реального времени

### Топики и consumer groups

| Топик | Publisher | Consumer Group | Что происходит |
|-------|-----------|---------------|----------------|
| `checkin.created` | DailyCheckInService | `health-metrics-consumer-group` | Вычисляет M-Rest/M-Ready/M-Balance |
| `checkin.created` | DailyCheckInService | `ml-recommendation-consumer-group` | asyncRefresh ML рекомендаций |
| `sleep.logged` | SleepLogService | `health-metrics-consumer-group` | Обогащает M-Rest данными deep/REM сна |
| `sleep.logged` | SleepLogService | `ml-recommendation-consumer-group` | asyncRefresh ML рекомендаций |
| `game.completed` | GameSessionService | `character-progression-consumer-group` | checkAndAutoLevelUp + checkAndUnlockRewards |
| `game.completed` | GameSessionService | `ml-recommendation-consumer-group` | asyncRefresh ML рекомендаций |
| `character.leveled-up` | CharacterProgressionConsumer | — | Уведомление о повышении уровня |

### Полный поток (пример: создание чекина)

```
POST /api/v1/checkins
          ↓
DailyCheckInService.createCheckIn()
    1. Сохранить в DB (daily_check_ins)
    2. updateStreak(userId)         → обновить стрик
    3. updateHappiness(character)   → персонаж счастливее
    4. autoCompleteTask(COMPLETE_CHECKIN)
    5. autoCompleteTask(SLEEP_7_HOURS) если sleepHours >= 7
    6. checkAndUnlockRewards(userId)
    7. publishEvent(CheckInCreatedApplicationEvent)
          ↓
@TransactionalEventListener(AFTER_COMMIT)
    - HealthMetricsSaver.onCheckInCreated()
        → calculateAndSave(M-Rest, M-Ready, M-Balance)
    - TransactionalKafkaPublisher.onCheckInCreated()
        → KafkaProducerService.publishCheckInCreated(userId, date)
          ↓
      Kafka: checkin.created
          ↓
    HealthMetricsKafkaConsumer → (дополнительный пересчёт если нужен)
    MLRecommendationConsumer   → asyncRefresh(userId, "checkin.created")
    CharacterProgressionConsumer → checkAndAutoLevelUp + checkAndUnlockRewards
```

---

## 8. Базы данных

### Auth DB (nb_checkin, контейнер: nbauthservice-db, порт: 5434)

| Таблица | Описание |
|---------|---------|
| `users` | id, username, email, password (bcrypt), is_onboarded |
| `user_onboarding` | sex, heightCm, weightKg, birthDate, characterId |
| `users_roles` | user_id, role (USER/ADMIN) |
| `verification_token` | токены для email верификации |

```sql
-- Подключение
docker exec nbauthservice-db psql -U amangeldimadina -d nbauthservice

-- Посмотреть пользователей
SELECT id, username, email, is_onboarded FROM users;

-- Онбординг
SELECT u.username, o.sex, o.birth_date, o.height_cm, o.weight_kg
FROM users u LEFT JOIN user_onboarding o ON u.id = o.user_id;
```

---

### Checkin DB (nb_checkin, контейнер: nb-checkin-db, порт: 5435)

| Таблица | Описание |
|---------|---------|
| `daily_check_ins` | Ежедневные чекины (mood, stress, sleep, exercise...) |
| `sleep_logs` | Детальные логи сна (deep/REM/light sleep...) |
| `mood_logs` | Логи настроения |
| `brain_game_results` | Результаты когнитивных игр |
| `game_sessions` | Fun-игры (старые) |
| `new_game_sessions` | Fun-игры (новые) |
| `user_game_stats` | Агрегированная статистика игр |
| `user_streaks` | Текущий и максимальный стрик |
| `user_characters` | Персонаж (уровень, XP, счастье) |
| `user_rewards` | Разблокированные награды |
| `daily_tasks` | Задачи дня (5 штук, сброс в 00:00) |
| `health_metrics` | M-Rest, M-Ready, M-Balance (одна запись на день) |
| `daily_ml_recommendation` | ML рекомендации (одна запись на день на юзера) |

```sql
-- Подключение
docker exec nb-checkin-db psql -U amangeldimadina -d nb_checkin

-- Последние ML рекомендации
SELECT user_id, recommendation_date, trigger_source,
       ROUND(cognitive_score::numeric, 1) as score, updated_at
FROM daily_ml_recommendation
ORDER BY updated_at DESC;

-- Полный JSON рекомендаций
SELECT recommendations_json FROM daily_ml_recommendation
WHERE user_id = 2 AND recommendation_date = CURRENT_DATE;

-- Чекины за неделю
SELECT check_in_date, morning_mood, stress_level, sleep_hours, did_exercise
FROM daily_check_ins
WHERE user_id = 2
ORDER BY check_in_date DESC LIMIT 7;

-- Health Metrics
SELECT metric_date, m_rest, m_ready, m_balance, overall_wellness_score
FROM health_metrics WHERE user_id = 2 ORDER BY metric_date DESC;

-- Стрики
SELECT current_streak, longest_streak FROM user_streaks WHERE user_id = 2;
```

---

### NoteAI DB (noteai, контейнер: noteai-db, порт: 5433)

| Таблица | Описание |
|---------|---------|
| `journal_entries` | Записи журнала (title, content, mood, tags) |
| `notes` | Старые заметки |
| `note_users` | Связь заметок с пользователями |

---

## 9. Гейм-система и XP

### Полная система начисления XP

```
Действие                     XP
──────────────────────────────────────────────────
Ежедневный чекин             +10 (+ стрик бонус)
Стрик-бонус 7 дней           +50
Стрик-бонус 14 дней          +100
Стрик-бонус 30 дней          +300

NUMBER_SEQUENCE (win, HARD)  50+50+50 × 2.5 = 375 XP
MEMORY_PAIRS (win, MEDIUM)   50+40+30 × 1.5 ≈ 180 XP

DONUT_GAME (100s, MEDIUM)    100+30+40 × 1.5 = 255... + streak mul
NUMBER_SEQUENCE_GAME (25s, 1 attempt) → (base+30) × streak × 1.5 + 40(time) + 30(attempt)

Написать заметку              +45 (через NoteAI trigger)
```

### Уровни персонажа

| Уровень | XP порог | Стрик |
|---------|---------|-------|
| 1→2 | 500 XP | 0 дней |
| 2→3 | 2000 XP | 7 дней |
| 3→4 | 4500 XP | 14 дней |
| 4→5 | 8000 XP | 21 день |

---

## 10. Примеры запросов по сценариям

### Сценарий A: Полный утренний ритуал

```bash
# 1. Получить токен
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"madina","password":"Test1234!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 2. Создать чекин (+10 XP + стрик + COMPLETE_CHECKIN задача)
curl -s -X POST http://localhost:8082/api/v1/checkins \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "morningMood": 4, "eveningMood": 3, "energyLevel": 8, "stressLevel": 3,
    "sleepHours": 7.5, "sleepQuality": 8, "didExercise": true,
    "ateHealthy": true, "hadSocialInteraction": false
  }'

# 3. Добавить детали сна (SLEEP_7_HOURS задача + ML update + M-Rest обогащение)
curl -s -X POST http://localhost:8082/api/v1/sleep \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sleepDate": "2026-06-06", "totalHours": 7.5, "qualityScore": 8,
    "feltRested": true, "deepSleepMinutes": 90, "remSleepMinutes": 80
  }'

# 4. Записать настроение (LOG_MOOD задача + ML update)
curl -s -X POST http://localhost:8082/api/v1/mood \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"moodValue": 4, "moodLabel": "Energetic", "intensity": 3}'

# 5. Сыграть в когнитивную игру (PLAY_GAME задача + Kafka ML update)
curl -s -X POST http://localhost:8082/api/v1/brain-games/submit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"gameType": "NUMBER_SEQUENCE", "score": 1000, "timeTakenSeconds": 35,
       "isWin": true, "difficultyLevel": "MEDIUM", "mistakesCount": 1}'

# 6. Написать заметку в журнал (WRITE_NOTE задача + +45 XP)
curl -s -X POST http://localhost:8083/api/v1/journal \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "Мои мысли", "content": "Хороший день был...", "moodScore": 4}'

# 7. Посмотреть статус задач
curl -s http://localhost:8082/api/v1/tasks/today \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Сценарий B: Получить рекомендации

```bash
USER_ID=2  # Подставить свой userId

# Вариант 1: Frontend через Python напрямую
curl -s -X POST http://localhost:5001/recommend/top3 \
  -H "Content-Type: application/json" \
  -d "{\"sleep_duration\":7,\"stress_level\":5,\"daily_screen_time\":8,
       \"exercise_frequency\":3,\"caffeine_intake\":2,\"reaction_time\":300,
       \"memory_test_score\":70,\"age\":25,\"gender\":\"Male\",
       \"diet_type\":\"Non-Vegetarian\",\"user_id\":$USER_ID}"

# Вариант 2: Через Java backend (с JWT)
curl -s -X POST http://localhost:8082/api/v1/ml/recommendations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{}'

# Посмотреть историю рекомендаций
curl -s http://localhost:8082/api/v1/ml/recommendations/history \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# Форс-рефреш
curl -s -X POST http://localhost:8082/api/v1/ml/recommendations/refresh \
  -H "Authorization: Bearer $TOKEN"
```

### Сценарий C: AI журнал

```bash
# Создать запись
NOTE_ID=$(curl -s -X POST http://localhost:8083/api/v1/journal \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Сегодня","content":"Провел продуктивный день...","moodScore":4}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

# AI анализ
curl -s -X POST "http://localhost:8083/api/v1/journal/$NOTE_ID/ai/analyze" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# AI саммари
curl -s -X POST "http://localhost:8083/api/v1/journal/$NOTE_ID/ai/summary" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Сценарий D: Мониторинг в реальном времени

```bash
# Логи Kafka-триггеров и ML обновлений
docker logs -f nb-checkin-service 2>&1 | \
  grep -E "ML async|Kafka|game.completed|checkin.created|sleep.logged|mood.logged|ML recommendation"

# Python ML кеш операции
docker logs -f neural-balance-ml 2>&1 | grep -E "cache|user|score"

# Таблица рекомендаций в реальном времени (каждые 5 сек)
watch -n 5 'docker exec nb-checkin-db psql -U amangeldimadina -d nb_checkin -c \
  "SELECT user_id, recommendation_date, trigger_source, updated_at
   FROM daily_ml_recommendation ORDER BY updated_at DESC LIMIT 5;"'
```

---

## Полезные SQL запросы для отладки

```sql
-- Подключение к Checkin DB
docker exec nb-checkin-db psql -U amangeldimadina -d nb_checkin

-- Все данные конкретного пользователя
SELECT u.user_id,
       COUNT(DISTINCT c.id) as checkins,
       MAX(s.current_streak) as streak,
       MAX(ch.level) as char_level,
       MAX(ch.current_xp) as xp
FROM (SELECT DISTINCT user_id FROM daily_check_ins) u
LEFT JOIN daily_check_ins c ON u.user_id = c.user_id
LEFT JOIN user_streaks s ON u.user_id = s.user_id
LEFT JOIN user_characters ch ON u.user_id = ch.user_id
GROUP BY u.user_id;

-- ML рекомендации с тригерами
SELECT user_id, recommendation_date, trigger_source,
       ROUND(cognitive_score::numeric,1) as score,
       TO_CHAR(updated_at, 'HH24:MI:SS') as time
FROM daily_ml_recommendation ORDER BY updated_at DESC;

-- Health Metrics за последнюю неделю
SELECT metric_date, m_rest, m_ready, m_balance, overall_wellness_score
FROM health_metrics WHERE user_id = 2
ORDER BY metric_date DESC LIMIT 7;

-- Игровая статистика
SELECT game_type, COUNT(*) as games,
       SUM(CASE WHEN is_win THEN 1 ELSE 0 END) as wins,
       ROUND(AVG(xp_earned)) as avg_xp
FROM brain_game_results WHERE user_id = 2
GROUP BY game_type;
```

---

## Конфигурация окружения (.env)

```
# Auth DB
AUTH_DB_NAME=nbauthservice
AUTH_DB_USER=amangeldimadina
AUTH_DB_PASSWORD=mayddee
AUTH_DB_PORT=5434

# Checkin DB
CHECKIN_DB_NAME=nb_checkin
CHECKIN_DB_USER=amangeldimadina
CHECKIN_DB_PASSWORD=mayddee
CHECKIN_DB_PORT=5435

# pgAdmin
PGADMIN_EMAIL=admin@admin.com
PGADMIN_PASSWORD=admin
PGADMIN_PORT=5051

# JWT
JWT_SECRET=...
JWT_EXPIRATION=86400000  (24 часа)
```

---

## Ключевые технические решения

| Решение | Описание |
|---------|---------|
| **Timezone** | Везде `ZoneId.of("Asia/Almaty")` (UTC+5), без исключений |
| **JWT Auth** | `request.getAttribute("userId")` — НЕ `@AuthenticationPrincipal` |
| **Kafka Graceful** | Все Kafka-публикации в try-catch, основной flow не блокируется |
| **Sleep Log Upsert** | POST на существующий sleep log → автоматически UPDATE |
| **ML Python cache** | In-memory cache per user_id, обновляется Java через `internal: true` |
| **DB persistence ML** | Таблица `daily_ml_recommendation` хранит историю по дням |
| **@EnableKafka** | Обязательно на main class — иначе `@KafkaListener` молча игнорируются |
| **@EnableAsync** | Обязательно для `asyncRefresh()` — иначе синхронный вызов |
| **PATCH semantics** | userPrefsCache ML никогда не сбрасывает старые значения |
| **Consumer factory** | Явный `KafkaConsumerConfig` с `kafkaListenerContainerFactory` |
