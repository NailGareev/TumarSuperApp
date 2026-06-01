# TumarSuperApp

**TumarSuperApp** — казахстанский финтех-суперапп: цифровой кошелёк, денежные переводы, бронирование авиабилетов и туров, маркетплейс, оплата услуг. Целевая аудитория — рынок Казахстана и Центральной Азии (валюта KZT, номера +7).

---

## Содержание

- [Экраны и функции](#экраны-и-функции)
- [Стек технологий](#стек-технологий)
- [Архитектура](#архитектура)
- [Структура проекта](#структура-проекта)
- [Установка и запуск](#установка-и-запуск)
- [API](#api)
- [База данных](#база-данных)
- [Безопасность](#безопасность)

---

## Экраны и функции

### Вход и безопасность
- **PIN-код** — 4-значный PIN при каждом запуске (PinEntryActivity — точка входа)
- **Регистрация / Вход** — email + пароль, JWT-токен сохраняется в SharedPreferences
- **Смена PIN** — доступна из профиля в любой момент

### Главный экран (Home)
- Отображение баланса в KZT и номера телефона (загружается с API)
- Быстрые действия: **Пополнить**, **История**, **Перевести**, **Оплата**
- Навигационные карточки в разделы **Путешествия** и **Tumar Market**

### Виртуальная карта (Card)
- Генерация виртуальной Visa-карты (номер, срок действия, CVV)
- Просмотр CVV с автоскрытием через 60 секунд
- Блокировка / разблокировка карты
- Переименование карты

### Переводы (Transfer)
Три режима в одном экране:
| Режим | Описание |
|---|---|
| По номеру телефона | Поиск получателя по номеру +7, подтверждение, отправка |
| По номеру карты | Ввод 16-значного номера карты |
| Международный | Страна, имя, IBAN, SWIFT, цель платежа; валюты: USD, EUR, RUB, GBP |

### Оплата услуг (Payments)
30+ провайдеров в 6 категориях:
- **Мобильная связь** — Activ/Kcell, Beeline KZ, Tele2, Алтел и др.
- **ЖКХ** — АЛСЕКО, водоканал, теплоснабжение, газ
- **Интернет** — Beeline, АЛМА, MEGANET, Казахтелеком
- **Электронные кошельки** — YURTA, QPLUS, Kaspi
- **Игры и стриминг** — Steam, PlayStation, Xbox, Netflix
- **Транспорт** — Air Astana, FlyArystan, Yandex Такси

### История транзакций (History)
- Полный список операций из API
- Отображение суммы, типа и даты каждой транзакции

### Путешествия (Travel)
Хаб с вращающимся баннером (смена каждые 3 сек.) и 4 разделами:

#### Авиабилеты (Aviation → Aviasales)
- Выбор городов вылета/назначения (150+ городов с IATA-кодами)
- Дата туда / обратно
- Класс обслуживания (Эконом / Комфорт / Бизнес / Первый)
- Открывает Aviasales с заполненными параметрами поиска

#### Туры (TourSearch → ht.kz)
- Выбор города вылета (Алматы, Астана, Шымкент и др.)
- Выбор направления (100+ городов и стран)
- Даты заезда и выезда, количество ночей
- Открывает **ht.kz** с фильтрами `country`, `region`, `departCity`, `adult`, `daysFrom`, `daysTo`, `dateFrom`

#### Карточки туров (на главном экране Travel)
10 горящих и рекомендованных туров:
- Анталья, Дубай, Пхукет, Мальдивы, Хургада, Бали, Барселона, Паттайя, Амальфи, Шарм-эш-Шейх
- Фото с Unsplash, название отеля, звёзды, цена, рассрочка на 12 месяцев, скидка
- Нажатие → ht.kz с поиском туров по конкретной стране/курорту
- Вкладки **Рекомендуем** / **Горящие**

#### Ж/Д билеты (TrainSearch)
- Выбор городов (20+ городов ЦА и России)
- Дата отправления, количество пассажиров
- UI готов, внешняя интеграция в разработке

#### Казахстан (Kazakhstan)
8 внутренних направлений: Алматы, Астана, Шымкент, Боровое, Туркестан, Актау, Чарын, Шымбулак

### Tumar Market
- Встроенный маркетплейс на WebView (`http://10.0.2.2:8080`)
- JavaScript-мост (TumarBridge) для нативного взаимодействия с Android
- Автологин через инъекцию JWT-токена
- 5 вкладок: Магазин, Каталог, Избранное, Корзина, Заказы
- Оплата через Tumar Pay прямо из приложения

### Акции (Promotions)
- Вертикальный список промо-баннеров

### Профиль (Profile)
- Смена PIN-кода
- Выход из аккаунта (очистка SharedPreferences)

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
| Веб-интеграции | WebView (Aviasales, ht.kz, Tumar Market) |
| Аутентификация | JWT в SharedPreferences + AuthInterceptor |

### Backend (Node.js)
| Компонент | Технология |
|---|---|
| Среда | Node.js |
| Фреймворк | Express.js 5.1.0 |
| База данных | MySQL |
| ORM / Драйвер | mysql2 3.14.1 |
| Аутентификация | jsonwebtoken 9.0.2 |
| Хэширование | bcryptjs 3.0.2 |
| Прочее | cors 2.8.5, dotenv 16.5.0 |

### Tumar Market (фронтенд маркетплейса)
- Отдельный веб-модуль в папке `tumar-market/`
- Запускается на порту 8080
- Интегрируется с Android через JavaScript-мост

---

## Архитектура

```
┌──────────────────────────────────────────────────────┐
│                    Android App                       │
│                                                      │
│  PinEntryActivity (Launcher)                         │
│         │                                            │
│  LoginActivity / RegistrationActivity                │
│         │                                            │
│  MainActivity ──── BottomNavigationView              │
│    │  │  │  │  │                                     │
│    │  │  │  │  └── PromotionsFragment                │
│    │  │  │  └───── ProfileFragment                   │
│    │  │  └──────── TravelFragment                    │
│    │  │               ├── AviationFragment           │
│    │  │               ├── TourSearchFragment         │
│    │  │               ├── TrainSearchFragment        │
│    │  │               ├── KazakhstanFragment         │
│    │  │               └── FlightWebFragment (WebView)│
│    │  └─────────── CardFragment                      │
│    │                  └── CardManagementFragment      │
│    └────────────── HomeFragment                      │
│                       ├── PaymentsFragment           │
│                       ├── TopUpFragment              │
│                       ├── TransferFragment           │
│                       ├── HistoryFragment            │
│                       └── TumarMarketFragment (WebView)│
│                                                      │
│  network/ ──── Retrofit + OkHttp + AuthInterceptor   │
│  adapter/ ──── RecyclerView адаптеры                 │
│  db/      ──── Room (локальное хранение User)         │
└──────────────────────────┬───────────────────────────┘
                           │ HTTP/JSON (порт 3000)
┌──────────────────────────▼───────────────────────────┐
│              Node.js Backend (Express)               │
│  /api/register  /api/login  /api/profile             │
│  /api/transfer  /api/transactions  /api/topup        │
│  /api/pay  /api/card  /api/tours  /api/market/*      │
└──────────────────────────┬───────────────────────────┘
                           │ SQL
┌──────────────────────────▼───────────────────────────┐
│                     MySQL                            │
│       users | balances | transactions                │
└──────────────────────────────────────────────────────┘
                           
┌─────────────────────────────────────────────────────┐
│           Tumar Market (порт 8080)                  │
│    Веб-маркетплейс + JavaScript-мост к Android      │
└─────────────────────────────────────────────────────┘
```

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
│       │   ├── PinEntryActivity.java          # Launcher
│       │   ├── HomeFragment.java
│       │   ├── CardFragment.java
│       │   ├── CardManagementFragment.java
│       │   ├── PaymentsFragment.java
│       │   ├── TopUpFragment.java
│       │   ├── TransferFragment.java
│       │   ├── HistoryFragment.java
│       │   ├── ProfileFragment.java
│       │   ├── PromotionsFragment.java
│       │   ├── TravelFragment.java            # Хаб путешествий
│       │   ├── AviationFragment.java          # Авиабилеты → Aviasales
│       │   ├── TourSearchFragment.java        # Туры → ht.kz
│       │   ├── TrainSearchFragment.java       # Ж/Д билеты
│       │   ├── KazakhstanFragment.java        # Внутренний туризм
│       │   ├── FlightWebFragment.java         # WebView для внешних сайтов
│       │   ├── TumarMarketFragment.java       # Маркетплейс WebView
│       │   ├── SecurityUtils.java
│       │   ├── adapter/
│       │   │   ├── TourCardAdapter.java
│       │   │   ├── TransactionAdapter.java
│       │   │   ├── PaymentsAdapter.java
│       │   │   └── BannerAdapter.java
│       │   ├── network/
│       │   │   ├── ApiClient.java             # Retrofit singleton
│       │   │   ├── ApiService.java            # 14 эндпоинтов
│       │   │   ├── AuthInterceptor.java       # Авто-добавление токена
│       │   │   └── models/                   # POJO для запросов/ответов
│       │   └── db/
│       │       ├── AppDatabase.java
│       │       ├── entity/User.java
│       │       └── dao/UserDao.java
│       └── res/
│           ├── layout/                        # 30 XML-макетов
│           ├── drawable/                      # Иконки, баннеры, фоны
│           ├── values/                        # Цвета, строки, темы
│           ├── anim/                          # Анимации переходов
│           └── xml/network_security_config.xml
│
├── tumar-super-app-backend/
│   ├── server.js                              # Основной файл сервера
│   ├── package.json
│   └── .env                                   # Конфигурация (DB, JWT)
│
├── tumar-market/                              # Веб-маркетплейс
├── tumar_super_app_db.sql                     # Схема базы данных
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## Установка и запуск

### 1. База данных (MySQL)

```bash
mysql -u root -p < tumar_super_app_db.sql
```

### 2. Бэкенд

```bash
cd tumar-super-app-backend

npm install

# Настроить .env:
# DB_HOST=localhost
# DB_USER=root
# DB_PASSWORD=
# DB_NAME=tumar_super_app_db
# PORT=3000
# JWT_SECRET=замените_на_надёжный_ключ

node server.js
```

### 3. Tumar Market (маркетплейс)

```bash
cd tumar-market
npm install
npm start   # запускается на порту 8080
```

### 4. Android-приложение

```bash
# Сборка
./gradlew build

# Установка на эмулятор/устройство
./gradlew installDebug
```

> **Важно:** Бэкенд слушает по адресу `http://10.0.2.2:3000/` (стандартный адрес хоста для эмулятора Android). При запуске на физическом устройстве замените адрес в `ApiClient.java` на IP вашего компьютера в локальной сети.

---

## API

Все защищённые эндпоинты требуют заголовка `Authorization: Bearer <token>`.

| Метод | Эндпоинт | Описание | Авторизация |
|---|---|---|---|
| POST | `/api/register` | Регистрация | — |
| POST | `/api/login` | Вход, получение токена | — |
| GET | `/api/profile` | Данные профиля (телефон, баланс) | JWT |
| POST | `/api/transfer` | Перевод средств | JWT |
| GET | `/api/transactions` | История транзакций | JWT |
| POST | `/api/topup` | Пополнение баланса | JWT |
| POST | `/api/pay` | Оплата услуг | JWT |
| GET | `/api/lookup-phone` | Поиск пользователя по телефону | JWT |
| GET | `/api/card` | Данные виртуальной карты | JWT |
| POST | `/api/card/issue` | Выпуск новой карты | JWT |
| GET | `/api/tours` | Список туров | JWT |
| GET | `/api/tours/search` | Поиск туров | JWT |
| POST | `/api/market/pay` | Оплата в маркетплейсе | JWT |
| GET | `/api/market/orders` | Заказы в маркетплейсе | JWT |

### Примеры

**Регистрация**
```json
POST /api/register
{
  "name": "Асель Нурова",
  "email": "asel@example.com",
  "phone": "+77001234567",
  "age": 28,
  "password": "securepassword"
}
```

**Перевод по телефону**
```json
POST /api/transfer
{
  "recipient_phone": "+77009876543",
  "amount": 5000
}
```

---

## База данных

Схема определена в `tumar_super_app_db.sql`:

```sql
users        (id, name, email, phone, age, password_hash)
balances     (id, user_id, balance, currency)
transactions (id, sender_id, recipient_id, amount, type, created_at)
```

Переводы выполняются в рамках транзакций БД с блокировкой строк (`SELECT ... FOR UPDATE`) для защиты от состояния гонки.

---

## Безопасность

| Механизм | Реализация |
|---|---|
| Пароли | bcrypt, 10 раундов соли |
| Сессии | JWT, срок действия 24 часа |
| Устройство | 4-значный PIN (хэш в SharedPreferences) |
| HTTP-запросы | AuthInterceptor автоматически добавляет Bearer-токен |
| CVV карты | Отображается максимум 60 секунд, затем скрывается |
| Сеть | `network_security_config.xml` разрешает HTTP только для localhost |

> **Перед деплоем в продакшн:** замените `JWT_SECRET` в `.env` на криптографически стойкий ключ и переведите все соединения на HTTPS.
