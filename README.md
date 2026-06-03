# TumarSuperApp

**TumarSuperApp** — казахстанский финтех-суперапп: цифровой кошелёк, денежные переводы, оплата услуг, бронирование авиабилетов и туров, встроенный маркетплейс Tumar Market. Целевая аудитория — рынок Казахстана и Центральной Азии (валюта KZT, номера +7).

---

## Содержание

- [Функции и экраны](#функции-и-экраны)
- [Стек технологий](#стек-технологий)
- [Архитектура](#архитектура)
- [Структура проекта](#структура-проекта)
- [Установка и запуск](#установка-и-запуск)
- [API](#api)
- [Tumar Market API](#tumar-market-api)
- [База данных](#база-данных)
- [Безопасность](#безопасность)

---

## Функции и экраны

### Вход и безопасность
- **PIN-код** — 4-значный PIN при каждом запуске (`PinEntryActivity` — точка входа приложения)
- **Регистрация** — email, телефон (+7), имя, фамилия, возраст, пароль; после регистрации пользователь автоматически входит в систему без повторного ввода данных
- **Вход** — email + пароль, JWT-токен (30 дней) сохраняется в `SharedPreferences`
- **Смена PIN** — доступна из профиля

### Главный экран (Home)
- Баланс в KZT и номер телефона пользователя
- Быстрые действия: **Пополнить**, **История**, **Перевести**, **Оплата**
- Карточки-навигаторы в **Путешествия** и **Tumar Market**
- Колокольчик уведомлений в шапке (виден только на главном экране)

### Уведомления
Три вкладки, открываются по нажатию колокольчика:

| Вкладка | Источник данных | Содержимое |
|---|---|---|
| Переводы | Node.js `/api/transactions` | Входящие и исходящие переводы с суммой, именем/телефоном контрагента и комментарием |
| Tumar Market | Go Market `/api/notifications` | Коды выдачи заказов и уведомления маркетплейса |
| Tumar Credit | — | Заглушка, готова к подключению кредитных продуктов |

### Виртуальная карта (Card)
- Генерация виртуальной Visa-карты (16 цифр, срок, CVV)
- Просмотр CVV с автоскрытием через 60 секунд
- Блокировка / разблокировка и переименование карты

### Переводы (Transfer)
Три режима в одном экране:

| Режим | Описание |
|---|---|
| По номеру телефона | Поиск получателя по номеру +7 с автоформатированием `+7 (XXX) XXX XX XX`, подтверждение и необязательный комментарий (до 200 символов) |
| По номеру карты | Ввод 16-значного номера карты с автоформатированием |
| Международный | Страна, имя получателя, IBAN, SWIFT, цель; валюты: USD, EUR, RUB, GBP |

Комментарий к переводу сохраняется в `transactions.description` и виден обоим участникам в разделе «Уведомления → Переводы».

### Оплата услуг (Payments)
30+ провайдеров в шести категориях:
- **Мобильная связь** — Activ/Kcell, Beeline KZ, Tele2, Алтел и др.
- **ЖКХ** — АЛСЕКО, водоканал, теплоснабжение, газ
- **Интернет** — Beeline, АЛМА, MEGANET, Казахтелеком
- **Электронные кошельки** — YURTA, QPLUS, Kaspi
- **Игры и стриминг** — Steam, PlayStation, Xbox, Netflix
- **Транспорт** — Air Astana, FlyArystan, Yandex Такси

### История транзакций (History)
- Полный список операций с суммой, типом и датой
- Тип `MARKET_REFUND` отображается со статусом **«Возвращено»** и зелёной меткой

### Путешествия (Travel)
Хаб с вращающимся баннером и четырьмя разделами:

- **Авиабилеты** — выбор городов (150+ с IATA-кодами), даты, класс → Aviasales
- **Туры** — город вылета, направление, даты, ночи → ht.kz
- **Ж/Д билеты** — города ЦА и России, дата, пассажиры (UI готов)
- **Казахстан** — 8 внутренних направлений
- **Карточки туров** — 10 горящих и рекомендованных туров со стоимостью и рассрочкой

### Tumar Market
- Встроенный мультивендорный маркетплейс в `WebView` (`http://10.0.2.2:8080`)
- JavaScript-мост (`TumarBridge`) для нативного взаимодействия с Android
- Автологин через инъекцию JWT-токена маркетплейса
- **Покупка через Tumar Pay** — списание с кошелька через Node.js, создание заказа в Go
- **Отмена заказа** — возврат средств на Tumar-кошелёк + отмена Go-заказа (транзакция `MARKET_REFUND`)
- **Возврат товара** — покупатель прикладывает фото (2–10 шт.) и указывает причину; продавец принимает возврат или сразу переводит деньги
- Портал продавца: управление товарами, заказами и возвратами
- 5 вкладок для покупателя: Магазин, Каталог, Избранное, Корзина, Заказы

### Профиль
- Смена PIN-кода
- Выход из аккаунта (очистка токена и данных)

---

## Стек технологий

### Android (клиент)
| Компонент | Технология |
|---|---|
| Язык | Java |
| Min SDK / Target SDK | 27 (Android 8.1) / 35 |
| UI | Material Design 3, ConstraintLayout, BottomNavigationView, Fragments |
| Сеть | Retrofit 2.9.0 + OkHttp 4.11.0 |
| Изображения | Glide 4.16.0 |
| Локальная БД | Room 2.6.1 |
| Аутентификация | JWT в SharedPreferences + `AuthInterceptor` |
| Форматирование телефона | `PhoneFormatWatcher` (собственная реализация) |

### Node.js Backend
| Компонент | Технология |
|---|---|
| Среда | Node.js |
| Фреймворк | Express.js 5.1.0 |
| База данных | MySQL |
| Драйвер | mysql2 3.14.1 |
| Аутентификация | jsonwebtoken 9.0.2 (30 дней) |
| Хэширование паролей | bcryptjs 3.0.2 |
| Прочее | cors, dotenv |

### Tumar Market
| Компонент | Технология |
|---|---|
| Бэкенд | Go 1.21, Gin |
| БД | MySQL (собственная схема) |
| Аутентификация | JWT (HS256) |
| Фронтенд | HTML / CSS / JS (без фреймворков) |
| Порт | 8080 |

---

## Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                        Android App                          │
│                                                             │
│  PinEntryActivity (Launcher)                                │
│  LoginActivity / RegistrationActivity                       │
│                                                             │
│  MainActivity                                               │
│  ├── HomeFragment                                           │
│  │   ├── NotificationsFragment (колокольчик)               │
│  │   │   ├── Переводы (Node.js /api/transactions)          │
│  │   │   ├── Tumar Market (Go /api/notifications)          │
│  │   │   └── Tumar Credit (заглушка)                       │
│  │   ├── TransferFragment (телефон / карта / международный) │
│  │   ├── PaymentsFragment                                   │
│  │   ├── TopUpFragment                                      │
│  │   └── HistoryFragment                                    │
│  ├── CardFragment                                           │
│  ├── TravelFragment → AviationFragment / TourSearchFragment │
│  ├── PromotionsFragment                                     │
│  └── ProfileFragment                                        │
│                                                             │
│  TumarMarketFragment (WebView + TumarBridge)                │
│                                                             │
│  network/ ── Retrofit + OkHttp + AuthInterceptor            │
└────────────┬────────────────────────────┬───────────────────┘
             │ HTTP :3000                 │ HTTP :8080
             ▼                           ▼
┌────────────────────────┐  ┌────────────────────────────────┐
│  Node.js Backend       │  │       Tumar Market (Go)        │
│  Express + MySQL       │◄─┤  Gin + MySQL                   │
│  :3000                 │  │  :8080                         │
│                        │  │                                │
│  /api/register         │  │  /api/auth/login               │
│  /api/login            │  │  /api/auth/app-auto-login      │
│  /api/profile          │  │  /api/orders                   │
│  /api/transfer         │  │  /api/products                 │
│  /api/transactions     │  │  /api/cart                     │
│  /api/topup            │  │  /api/notifications            │
│  /api/pay              │  │  /api/returns                  │
│  /api/market/pay       │  │  /api/seller/*                 │
│  /api/market/cancel    │  │                                │
│  /api/market/return/   │  │  ← вызывает Node.js для       │
│    process-refund      │  │    возврата средств            │
└────────────┬───────────┘  └────────────────────────────────┘
             │ SQL                        │ SQL
             ▼                           ▼
      ┌─────────────┐            ┌───────────────┐
      │  MySQL      │            │  MySQL        │
      │  (Node.js   │            │  (Go Market   │
      │   схема)    │            │   схема)      │
      └─────────────┘            └───────────────┘
```

### Межсервисное взаимодействие
- **Покупка Tumar Pay**: Android → `POST /api/market/pay` (Node.js списывает кошелёк) → Android → `POST /api/orders` (Go создаёт заказ с `tumar_ref`)
- **Отмена**: Android → `POST /api/market/cancel` (Node.js возвращает деньги) + Android → `PUT /api/orders/:id/cancel` (Go меняет статус)
- **Возврат товара**: Go → `POST localhost:3000/api/market/return/process-refund` с `app_secret` (server-to-server)
- **Уведомления маркетплейса**: Android → `POST /api/auth/app-auto-login` (Go, с `app_secret`) → Android → `GET /api/notifications` (Go, с полученным JWT)

---

## Структура проекта

```
TumarSuperApp/
├── app/
│   └── src/main/
│       ├── java/com/digitalcompany/tumarsuperapp/
│       │   ├── MainActivity.java
│       │   ├── LoginActivity.java
│       │   ├── RegistrationActivity.java
│       │   ├── PinSetupActivity.java
│       │   ├── PinEntryActivity.java              # Launcher
│       │   ├── PhoneFormatWatcher.java            # Форматирование +7 (XXX) XXX XX XX
│       │   ├── HomeFragment.java
│       │   ├── NotificationsFragment.java         # Колокольчик: 3 вкладки
│       │   ├── CardFragment.java
│       │   ├── CardManagementFragment.java
│       │   ├── PaymentsFragment.java
│       │   ├── TopUpFragment.java
│       │   ├── TransferFragment.java
│       │   ├── HistoryFragment.java
│       │   ├── ProfileFragment.java
│       │   ├── PromotionsFragment.java
│       │   ├── TravelFragment.java
│       │   ├── AviationFragment.java
│       │   ├── TourSearchFragment.java
│       │   ├── TrainSearchFragment.java
│       │   ├── KazakhstanFragment.java
│       │   ├── FlightWebFragment.java
│       │   ├── TumarMarketFragment.java           # WebView + TumarBridge
│       │   ├── SecurityUtils.java
│       │   ├── adapter/
│       │   │   ├── TransactionAdapter.java        # MARKET_REFUND → «Возвращено»
│       │   │   ├── TourCardAdapter.java
│       │   │   ├── PaymentsAdapter.java
│       │   │   └── BannerAdapter.java
│       │   ├── network/
│       │   │   ├── ApiClient.java
│       │   │   ├── ApiService.java
│       │   │   ├── AuthInterceptor.java
│       │   │   └── models/
│       │   └── db/
│       └── res/
│           ├── layout/                            # XML-макеты
│           ├── drawable/                          # Иконки, фоны, векторы
│           ├── menu/top_app_bar_menu.xml          # Колокольчик в шапке
│           ├── values/                            # Цвета, строки, темы
│           └── anim/
│
├── tumar-super-app-backend/
│   ├── server.js                                  # Единственный файл сервера
│   ├── setup_db.py                                # Скрипт создания схемы
│   ├── package.json
│   └── .env
│
├── tumar-market/
│   ├── go/
│   │   ├── main.go                                # Маршруты Gin
│   │   ├── database.go                            # Миграции таблиц
│   │   ├── auth.go
│   │   ├── order.go                               # Заказы + Tumar Pay отмена
│   │   ├── returns.go                             # Возвраты товаров
│   │   ├── models.go
│   │   └── go.mod
│   ├── html/                                      # Страницы маркетплейса
│   ├── js/                                        # Клиентский JS
│   └── css/
│
├── tumar_super_app_db.sql                         # Схема Node.js БД
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Установка и запуск

### 1. База данных (MySQL)

```bash
# Создать схему для Node.js backend
mysql -u root -p < tumar_super_app_db.sql
```

Схему Go Market создаёт сам при первом запуске через `database.go`.

### 2. Node.js Backend

```bash
cd tumar-super-app-backend
npm install

# Создать .env:
cat > .env << 'EOF'
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=
DB_NAME=tumar_super_app_db
PORT=3000
JWT_SECRET=замените_на_надёжный_ключ
EOF

node server.js
# → http://localhost:3000
```

### 3. Tumar Market (Go)

```bash
cd tumar-market/go
go mod tidy
go run .
# → http://localhost:8080
```

Переменные среды для Go (или `.env` рядом с бинарником):
```
DB_DSN=root:@tcp(localhost:3306)/tumar_market_db
JWT_SECRET=market_secret
PORT=8080
```

### 4. Android-приложение

```bash
./gradlew assembleDebug
./gradlew installDebug
```

> **Эмулятор**: бэкенды доступны по адресам `http://10.0.2.2:3000` и `http://10.0.2.2:8080`.  
> **Физическое устройство**: замените `10.0.2.2` на IP компьютера в локальной сети в `ApiClient.java` и `TumarMarketFragment.java`.

---

## API

Все защищённые эндпоинты требуют заголовка `Authorization: Bearer <token>`.

### Аутентификация и профиль

| Метод | Эндпоинт | Описание | Авторизация |
|---|---|---|---|
| POST | `/api/register` | Регистрация + автовыдача JWT | — |
| POST | `/api/login` | Вход, получение токена (30 дней) | — |
| GET | `/api/profile` | Телефон, баланс, валюта | JWT |

### Кошелёк и транзакции

| Метод | Эндпоинт | Описание | Авторизация |
|---|---|---|---|
| POST | `/api/topup` | Пополнение баланса | JWT |
| POST | `/api/transfer` | Перевод по телефону с комментарием | JWT |
| GET | `/api/transactions` | История (TRANSFER, TOPUP, PAYMENT, MARKET_REFUND) | JWT |
| GET | `/api/lookup-phone` | Поиск пользователя по `?phone=` | JWT |

### Оплата

| Метод | Эндпоинт | Описание | Авторизация |
|---|---|---|---|
| POST | `/api/pay` | Оплата услуги (`service`, `accountNumber`, `amount`) | JWT |

### Виртуальная карта

| Метод | Эндпоинт | Описание | Авторизация |
|---|---|---|---|
| GET | `/api/card` | Данные карты (номер, срок, CVV зашифрован) | JWT |
| POST | `/api/card/issue` | Выпуск новой карты | JWT |

### Tumar Market (интеграция)

| Метод | Эндпоинт | Описание | Авторизация |
|---|---|---|---|
| POST | `/api/market/pay` | Списание с кошелька при покупке | JWT |
| GET | `/api/market/orders` | Список покупок через Tumar Pay | JWT |
| POST | `/api/market/cancel` | Возврат средств при отмене заказа | JWT |
| POST | `/api/market/return/process-refund` | Возврат средств (server-to-server от Go) | `app_secret` |

### Примеры запросов

**Регистрация**
```json
POST /api/register
{
  "firstName": "Асель",
  "lastName": "Нурова",
  "email": "asel@example.com",
  "phone": "+77001234567",
  "age": 28,
  "password": "securepassword"
}
```
Ответ: `{ "success": true, "userId": 42, "token": "<jwt>" }`

**Перевод с комментарием**
```json
POST /api/transfer
{
  "recipientPhone": "+77009876543",
  "amount": 5000,
  "description": "За обед"
}
```

---

## Tumar Market API

Базовый URL: `http://localhost:8080/api`  
Аутентификация: `Authorization: Bearer <market_jwt>`

### Покупатель

| Метод | Эндпоинт | Описание |
|---|---|---|
| POST | `/auth/login` | Вход |
| POST | `/auth/app-auto-login` | Автологин по `phone` + `app_secret` |
| GET | `/products` | Каталог товаров |
| GET/POST | `/cart` | Корзина |
| POST | `/orders` | Создание заказа |
| GET | `/orders` | Список заказов |
| PUT | `/orders/:id/cancel` | Отмена заказа |
| POST | `/returns` | Заявка на возврат (фото + причина) |
| GET | `/returns` | Мои возвраты |
| GET | `/notifications` | Уведомления (коды выдачи) |

### Продавец

| Метод | Эндпоинт | Описание |
|---|---|---|
| GET | `/seller/orders` | Заказы в магазине продавца |
| GET | `/seller/returns` | Заявки на возврат |
| PUT | `/seller/returns/:id/accept` | Принять возврат (→ курьер) |
| PUT | `/seller/returns/:id/refund` | Вернуть деньги покупателю |

---

## База данных

### Node.js (tumar_super_app_db)

```sql
users         (id, first_name, last_name, email, phone, age, password_hash)
balances      (id, user_id, balance, currency)
transactions  (id, sender_id, recipient_id, amount, currency,
               transaction_type ENUM('TRANSFER','TOPUP','PAYMENT','MARKET_REFUND'),
               description, timestamp)
cards         (id, user_id, card_number, cvv_encrypted, expiry_encrypted)
market_purchases (id, user_id, order_ref, amount, items_json, address, status)
```

Переводы выполняются в транзакциях БД с `SELECT ... FOR UPDATE` для защиты от гонки.

### Tumar Market (Go, создаётся автоматически)

```
users, stores, products, product_sellers, categories
cart_items, orders, order_items, favorites
return_requests, notifications
```

---

## Безопасность

| Механизм | Реализация |
|---|---|
| Пароли | bcrypt, 10 раундов |
| Сессии | JWT, 30 дней |
| PIN | SHA-256 хэш в `PinSecurityPrefs` |
| Запросы | `AuthInterceptor` добавляет Bearer-токен ко всем запросам |
| CVV | AES-256-CBC шифрование; отображается максимум 60 секунд |
| HTTP | `network_security_config.xml` разрешает cleartext только для `10.0.2.2` |
| Межсервис | Go Market → Node.js через `app_secret = "tumar_app_secret_2024"` |

> **Перед деплоем в продакшн:** замените `JWT_SECRET` и `app_secret` на криптографически стойкие ключи, переведите все соединения на HTTPS, ограничьте CORS конкретными доменами.
