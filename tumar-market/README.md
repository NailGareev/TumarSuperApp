# Tumar Market Clone

Полноценная копия Tumar Market — интернет-магазин на Go + Python + HTML/CSS/JS + MySQL.

## Структура проекта

```
tumar-market/
├── html/           # HTML страницы
│   ├── index.html          - Главная страница
│   ├── catalog.html        - Каталог товаров
│   ├── product.html        - Карточка товара (несколько продавцов)
│   ├── search.html         - Поиск
│   ├── login.html          - Вход
│   ├── register.html       - Регистрация
│   ├── profile.html        - Профиль пользователя
│   ├── cart.html           - Корзина
│   ├── checkout.html       - Оформление заказа
│   ├── orders.html         - История заказов
│   ├── seller-dashboard.html - Кабинет продавца
│   ├── store-register.html - Регистрация магазина
│   └── add-product.html    - Добавление товара
├── css/            # Стили (отдельный файл для каждой страницы)
│   ├── main.css            - Общие стили (header, footer, cards)
│   ├── index.css           - Главная страница
│   ├── catalog.css         - Каталог
│   ├── product.css         - Карточка товара
│   ├── auth.css            - Авторизация
│   ├── seller-dashboard.css
│   ├── store-register.css
│   ├── add-product.css
│   ├── cart.css
│   ├── checkout.css
│   ├── orders.css
│   └── profile.css
├── js/             # JavaScript (отдельный файл для каждой страницы)
│   ├── main.js             - Общий: API, auth, toast, header
│   ├── index.js            - Главная страница
│   ├── catalog.js          - Каталог
│   ├── product.js          - Карточка товара
│   ├── auth.js             - Авторизация
│   ├── seller-dashboard.js - Кабинет продавца
│   ├── store-register.js   - Регистрация магазина
│   ├── add-product.js      - Добавление товара
│   ├── cart.js             - Корзина
│   ├── checkout.js         - Оформление заказа
│   ├── orders.js           - Заказы
│   ├── profile.js          - Профиль
│   └── search.js           - Поиск
├── go/             # Go backend (REST API)
│   ├── main.go             - Точка входа, роутер
│   ├── database.go         - Подключение MySQL, автомиграция
│   ├── models.go           - Структуры данных
│   ├── middleware.go       - JWT, CORS
│   ├── auth.go             - Регистрация, вход, профиль
│   ├── catalog.go          - Каталог, категории, поиск
│   ├── product.go          - Карточка товара, отзывы
│   ├── store.go            - Регистрация и управление магазином
│   ├── seller.go           - Кабинет продавца
│   ├── cart.go             - Корзина
│   └── order.go            - Заказы
├── python/         # Python утилиты
│   ├── setup_db.py         - Инициализация базы данных
│   ├── seed_data.py        - Тестовые данные
│   ├── auth.py             - Утилиты авторизации
│   ├── products.py         - Утилиты товаров
│   ├── stores.py           - Утилиты магазинов
│   ├── orders.py           - Утилиты заказов
│   └── requirements.txt
├── static/uploads/ # Загруженные файлы
├── start.sh        # Скрипт запуска
└── README.md
```

## Запуск

### Требования
- Go 1.21+
- MySQL 8.0+
- Python 3.10+ (опционально, для seed данных)

### 1. Настройка базы данных

```bash
# База создаётся автоматически при первом запуске Go сервера
# Или через Python:
cd python
pip install -r requirements.txt
python3 setup_db.py
python3 seed_data.py   # Загрузить тестовые данные
```

### 2. Запуск сервера

```bash
# Через стартовый скрипт:
chmod +x start.sh
./start.sh

# Или вручную:
export DB_PASSWORD=your_password
cd go
go mod tidy
go run .
```

### 3. Переменные окружения

| Переменная | По умолчанию | Описание |
|---|---|---|
| `DB_HOST` | `localhost` | Хост MySQL |
| `DB_PORT` | `3306` | Порт MySQL |
| `DB_USER` | `root` | Пользователь MySQL |
| `DB_PASSWORD` | `` | Пароль MySQL |
| `DB_NAME` | `tumar_market` | Имя базы данных |
| `JWT_SECRET` | `tumar-market-secret-2024` | Секрет JWT |

### 4. Доступ

Откройте браузер: **http://localhost:8080**

## Демо аккаунты (после seed_data.py)

| Роль | Email | Пароль |
|---|---|---|
| Покупатель | user@tumar.kz | user123 |
| Продавец | seller@tumar.kz | seller123 |
| Администратор | admin@tumar.kz | admin123 |

## Функционал

### Покупатель
- Просмотр каталога с фильтрами по категории, цене, сортировке
- Поиск товаров
- Карточка товара с несколькими продавцами (как у Tumar)
- Корзина с управлением количеством
- Оформление заказа с выбором адреса и способа оплаты
- История заказов с детализацией
- Профиль пользователя
- Система отзывов

### Продавец
- Регистрация магазина (с юр. данными: название, БИН, адрес)
- Кабинет продавца с дашбордом (статистика: товары, заказы, выручка)
- Добавление новых товаров с фото, характеристиками, ценой
- Управление ценой и остатками
- Просмотр и управление заказами (статусы)
- Несколько продавцов могут предлагать один товар

## API Endpoints

```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/logout
GET    /api/auth/me
PUT    /api/auth/profile

GET    /api/categories
GET    /api/products
GET    /api/products/featured
GET    /api/products/:id
GET    /api/products/:id/reviews
GET    /api/search?q=...

POST   /api/store/register
GET    /api/store/my
PUT    /api/store/my
GET    /api/stores/:id

GET    /api/seller/dashboard
GET    /api/seller/products
POST   /api/seller/products
PUT    /api/seller/products/:id
DELETE /api/seller/products/:id
GET    /api/seller/orders
PUT    /api/seller/orders/:id/status
POST   /api/seller/products/:id/offer

POST   /api/reviews

GET    /api/cart
POST   /api/cart
PUT    /api/cart/:id
DELETE /api/cart/:id

GET    /api/orders
POST   /api/orders
GET    /api/orders/:id
PUT    /api/orders/:id/cancel

POST   /api/upload
```

## Python утилиты (CLI)

```bash
# Управление пользователями
python3 python/auth.py list-users
python3 python/auth.py create-admin

# Управление товарами
python3 python/products.py list
python3 python/products.py search Samsung
python3 python/products.py categories

# Управление магазинами
python3 python/stores.py list
python3 python/stores.py list-pending
python3 python/stores.py approve 1
python3 python/stores.py stats 1

# Управление заказами
python3 python/orders.py list
python3 python/orders.py get 1
python3 python/orders.py update-status 1 shipped
python3 python/orders.py stats
```
