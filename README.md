# TumarSuperApp

Мобильное приложение для цифрового кошелька и денежных переводов, ориентированное на рынок Казахстана (валюта KZT, формат номеров +7XXXXXXXXXX).

---

## Содержание

- [О проекте](#о-проекте)
- [Функциональность](#функциональность)
- [Стек технологий](#стек-технологий)
- [Архитектура](#архитектура)
- [Установка и запуск](#установка-и-запуск)
- [API документация](#api-документация)
- [База данных](#база-данных)
- [Безопасность](#безопасность)
- [Структура проекта](#структура-проекта)

---

## О проекте

**TumarSuperApp** — это финтех-приложение, состоящее из Android-клиента и Node.js-бэкенда. Приложение позволяет пользователям регистрироваться, входить по PIN-коду, переводить средства по номеру телефона и просматривать историю транзакций.

---

## Функциональность

- **Регистрация и авторизация** — регистрация по email, вход с JWT-токеном и PIN-кодом
- **Профиль** — просмотр баланса, номера телефона, имени
- **Денежные переводы** — перевод средств другому пользователю по номеру телефона (+7XXXXXXXXXX)
- **История транзакций** — список всех входящих и исходящих операций
- **Управление картами** — создание, переименование, блокировка, перевыпуск и удаление виртуальных карт
- **Акции** — раздел с баннерами и промо-предложениями

---

## Стек технологий

### Android (клиент)

| Компонент         | Технология                    |
|-------------------|-------------------------------|
| Язык              | Java                          |
| Min SDK           | 27 (Android 8.1)              |
| Target SDK        | 35                            |
| HTTP-клиент       | Retrofit 2.9.0 + OkHttp 4.11.0 |
| Локальная БД      | Room 2.6.1                    |
| Сериализация      | Gson                          |
| UI                | Material Design, ConstraintLayout, BottomNavigationView |
| Аутентификация    | JWT (SharedPreferences)       |

### Backend

| Компонент         | Технология                    |
|-------------------|-------------------------------|
| Среда             | Node.js                       |
| Фреймворк         | Express.js 5.1.0              |
| База данных       | MySQL 5.5.62                  |
| Драйвер БД        | mysql2 3.14.1                 |
| Аутентификация    | jsonwebtoken 9.0.2            |
| Хэширование паролей | bcryptjs 3.0.2              |
| Прочее            | cors 2.8.5, dotenv 16.5.0     |

---

## Архитектура

```
┌─────────────────────────────────────┐
│          Android App                │
│  ┌──────────────────────────────┐   │
│  │  Activities (Login, PIN, Main)│   │
│  └──────────────┬───────────────┘   │
│  ┌──────────────▼───────────────┐   │
│  │  Fragments (Home, Card,      │   │
│  │  Transfer, History, Profile) │   │
│  └──────────────┬───────────────┘   │
│  ┌──────────────▼───────────────┐   │
│  │  Retrofit + ApiService       │   │
│  └──────────────┬───────────────┘   │
└─────────────────┼───────────────────┘
                  │ HTTP/JSON
┌─────────────────▼───────────────────┐
│          Node.js Backend            │
│  Express.js + JWT Middleware        │
└─────────────────┬───────────────────┘
                  │ SQL
┌─────────────────▼───────────────────┐
│          MySQL Database             │
│  users | balances | transactions    │
└─────────────────────────────────────┘
```

---

## Установка и запуск

### 1. База данных

```bash
mysql -u root -p < tumar_super_app_db.sql
```

### 2. Бэкенд

```bash
cd tumar-super-app-backend

# Установить зависимости
npm install

# Настроить переменные окружения (файл .env уже содержит настройки по умолчанию)
# DB_HOST=localhost
# DB_USER=root
# DB_PASSWORD=
# DB_NAME=tumar_super_app_db
# PORT=3000
# JWT_SECRET=<ваш секретный ключ>

# Запустить сервер
node server.js
```

### 3. Android-приложение

```bash
# Сборка проекта
./gradlew build

# Установка на устройство/эмулятор
./gradlew installDebug
```

> **Важно:** В файле `ApiClient.java` укажите правильный базовый URL:
> - Эмулятор: `http://10.0.2.2:3000/`
> - Физическое устройство: `http://<IP-адрес вашего компьютера>:3000/`

---

## API документация

Все защищённые эндпоинты требуют заголовка `Authorization: Bearer <token>`.

| Метод  | Эндпоинт            | Описание                          | Защита |
|--------|---------------------|-----------------------------------|--------|
| POST   | `/api/register`     | Регистрация нового пользователя   | Нет    |
| POST   | `/api/login`        | Вход и получение JWT-токена       | Нет    |
| GET    | `/api/profile`      | Получение данных профиля          | JWT    |
| POST   | `/api/transfer`     | Перевод средств                   | JWT    |
| GET    | `/api/transactions` | История транзакций                | JWT    |

### Примеры запросов

**Регистрация**
```json
POST /api/register
{
  "name": "Иван Иванов",
  "email": "ivan@example.com",
  "phone": "+71234567890",
  "age": 25,
  "password": "securepassword"
}
```

**Перевод средств**
```json
POST /api/transfer
{
  "recipient_phone": "+79876543210",
  "amount": 5000
}
```

---

## База данных

Схема базы данных определена в файле `tumar_super_app_db.sql`.

```sql
-- Пользователи
users (id, name, email, phone, age, password_hash)

-- Балансы
balances (id, user_id, balance, currency)

-- Транзакции
transactions (id, sender_id, recipient_id, amount, type, created_at)
```

Переводы выполняются в рамках транзакций БД с блокировкой строк (`FOR UPDATE`) для обеспечения согласованности данных.

---

## Безопасность

- Пароли хэшируются через **bcrypt** (10 раундов соли)
- Аутентификация через **JWT** (срок действия: 1 сутки)
- PIN-код как дополнительный уровень защиты на устройстве
- Автоматическое добавление токена к запросам через `AuthInterceptor`

> **Внимание:** В файле `.env` замените значение `JWT_SECRET` на надёжный секретный ключ перед деплоем в продакшн.

---

## Структура проекта

```
TumarSuperApp/
├── app/                              # Android-модуль
│   └── src/main/
│       ├── java/com/digitalcompany/tumarsuperapp/
│       │   ├── MainActivity.java
│       │   ├── LoginActivity.java
│       │   ├── RegistrationActivity.java
│       │   ├── PinSetupActivity.java
│       │   ├── PinEntryActivity.java
│       │   ├── HomeFragment.java
│       │   ├── CardFragment.java
│       │   ├── CardManagementFragment.java
│       │   ├── TransferFragment.java
│       │   ├── HistoryFragment.java
│       │   ├── ProfileFragment.java
│       │   ├── PromotionsFragment.java
│       │   ├── SecurityUtils.java
│       │   ├── network/              # Retrofit API-клиент и модели
│       │   ├── db/                   # Room база данных
│       │   └── adapter/              # RecyclerView адаптеры
│       └── res/                      # Ресурсы (layout, strings, colors, themes)
│
├── tumar-super-app-backend/          # Node.js бэкенд
│   ├── server.js                     # Основной файл сервера
│   ├── package.json
│   └── .env                          # Конфигурация окружения
│
├── tumar_super_app_db.sql            # SQL-схема базы данных
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```
