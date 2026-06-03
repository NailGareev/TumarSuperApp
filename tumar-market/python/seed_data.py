"""Seed script: creates demo categories, products, stores, and users."""

import os
import re
import hashlib
import urllib.request
import urllib.error
import mysql.connector

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "tumar_market"),
    "charset": "utf8mb4",
}

# Root of the project — one level above this script (python/)
ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
IMAGES_DIR = os.path.join(ROOT_DIR, "images")

# Format: (parent_slug_or_None, name, slug, icon)
CATEGORIES = [
    # ── Parent categories ──────────────────────────────────────────
    (None, "Телефоны и гаджеты", "phones-gadgets", "📱"),
    (None, "Бытовая техника", "home-appliances", "🏠"),
    (None, "ТВ, Аудио, Видео", "tv-audio-video", "📺"),
    (None, "Компьютеры", "computers", "💻"),
    (None, "Мебель и интерьер", "furniture", "🛋"),
    (None, "Красота и здоровье", "beauty-health", "💄"),
    (None, "Детские товары", "kids", "🧸"),
    (None, "Аптека", "pharmacy", "💊"),
    (None, "Строительство, ремонт", "construction", "🔨"),
    (None, "Спорт, туризм", "sport-tourism", "⚽"),
    (None, "Досуг, книги", "leisure-books", "📚"),
    (None, "Автотовары", "auto", "🚗"),
    (None, "Украшения, аксессуары", "jewelry-accessories", "💍"),
    (None, "Одежда и обувь", "clothing-shoes", "👔"),

    # ── Телефоны и гаджеты ────────────────────────────────────────
    ("phones-gadgets", "Смартфоны", "smartphones", ""),
    ("phones-gadgets", "Мобильные телефоны", "mobile-phones", ""),
    ("phones-gadgets", "Планшеты", "tablets", ""),
    ("phones-gadgets", "Умные часы", "smart-watches", ""),
    ("phones-gadgets", "Наушники", "headphones", ""),
    ("phones-gadgets", "Чехлы для телефонов", "phone-cases", ""),
    ("phones-gadgets", "Зарядные устройства", "chargers", ""),
    ("phones-gadgets", "Портативные аккумуляторы", "power-banks", ""),
    ("phones-gadgets", "Фотоаппараты", "cameras", ""),

    # ── Бытовая техника ───────────────────────────────────────────
    ("home-appliances", "Холодильники", "refrigerators", ""),
    ("home-appliances", "Стиральные машины", "washing-machines", ""),
    ("home-appliances", "Пылесосы", "vacuums", ""),
    ("home-appliances", "Микроволновые печи", "microwaves", ""),
    ("home-appliances", "Электрические плиты", "stoves", ""),
    ("home-appliances", "Утюги", "irons", ""),
    ("home-appliances", "Кондиционеры", "air-conditioners", ""),
    ("home-appliances", "Водонагреватели", "water-heaters", ""),
    ("home-appliances", "Посудомоечные машины", "dishwashers", ""),

    # ── ТВ, Аудио, Видео ──────────────────────────────────────────
    ("tv-audio-video", "Телевизоры", "tvs", ""),
    ("tv-audio-video", "Проекторы", "projectors", ""),
    ("tv-audio-video", "Аудиосистемы", "audio-systems", ""),
    ("tv-audio-video", "Саундбары", "soundbars", ""),
    ("tv-audio-video", "Игровые приставки", "gaming-consoles", ""),

    # ── Компьютеры ────────────────────────────────────────────────
    ("computers", "Ноутбуки", "laptops", ""),
    ("computers", "Настольные компьютеры", "desktops", ""),
    ("computers", "Мониторы", "monitors", ""),
    ("computers", "Принтеры", "printers", ""),
    ("computers", "Клавиатуры", "keyboards", ""),
    ("computers", "Мышки", "mice", ""),
    ("computers", "Роутеры", "routers", ""),

    # ── Мебель и интерьер ─────────────────────────────────────────
    ("furniture", "Диваны и кресла", "sofas", ""),
    ("furniture", "Кровати", "beds", ""),
    ("furniture", "Шкафы", "wardrobes", ""),
    ("furniture", "Столы", "tables", ""),
    ("furniture", "Матрасы", "mattresses", ""),
    ("furniture", "Освещение", "lighting", ""),
    ("furniture", "Ковры", "carpets", ""),

    # ── Красота и здоровье ────────────────────────────────────────
    ("beauty-health", "Уход за лицом", "face-care", ""),
    ("beauty-health", "Уход за телом", "body-care", ""),
    ("beauty-health", "Парфюмерия", "perfume", ""),
    ("beauty-health", "Декоративная косметика", "makeup", ""),
    ("beauty-health", "Фены и стайлеры", "hair-dryers", ""),
    ("beauty-health", "Электробритвы", "shavers", ""),

    # ── Детские товары ────────────────────────────────────────────
    ("kids", "Игрушки", "toys", ""),
    ("kids", "Коляски", "strollers", ""),
    ("kids", "Детская одежда", "kids-clothing", ""),
    ("kids", "Питание для детей", "baby-food", ""),
    ("kids", "Школьные товары", "school-supplies", ""),
    ("kids", "Детские велосипеды", "kids-bikes", ""),

    # ── Аптека ────────────────────────────────────────────────────
    ("pharmacy", "Витамины и БАД", "vitamins", ""),
    ("pharmacy", "Медицинские приборы", "medical-devices", ""),
    ("pharmacy", "Гигиена и уход", "hygiene", ""),

    # ── Строительство, ремонт ─────────────────────────────────────
    ("construction", "Строительные материалы", "building-materials", ""),
    ("construction", "Инструменты", "tools", ""),
    ("construction", "Сантехника", "plumbing", ""),
    ("construction", "Электрика", "electrical", ""),
    ("construction", "Краски и лаки", "paints", ""),

    # ── Спорт, туризм ─────────────────────────────────────────────
    ("sport-tourism", "Тренажёры", "fitness-equipment", ""),
    ("sport-tourism", "Велосипеды", "bikes", ""),
    ("sport-tourism", "Туристическое снаряжение", "camping", ""),
    ("sport-tourism", "Спортивная одежда", "sportswear", ""),
    ("sport-tourism", "Рыбалка", "fishing", ""),

    # ── Досуг, книги ──────────────────────────────────────────────
    ("leisure-books", "Книги", "books", ""),
    ("leisure-books", "Настольные игры", "board-games", ""),
    ("leisure-books", "Музыкальные инструменты", "musical-instruments", ""),
    ("leisure-books", "Хобби и творчество", "hobbies", ""),

    # ── Автотовары ────────────────────────────────────────────────
    ("auto", "Автошины", "tires", ""),
    ("auto", "Автозапчасти", "car-parts", ""),
    ("auto", "Автоаксессуары", "car-accessories", ""),
    ("auto", "Автохимия", "car-chemicals", ""),
    ("auto", "Видеорегистраторы", "dash-cams", ""),

    # ── Украшения, аксессуары ─────────────────────────────────────
    ("jewelry-accessories", "Ювелирные украшения", "jewelry", ""),
    ("jewelry-accessories", "Часы", "watches", ""),
    ("jewelry-accessories", "Сумки", "bags", ""),
    ("jewelry-accessories", "Очки", "glasses", ""),

    # ── Одежда и обувь ────────────────────────────────────────────
    ("clothing-shoes", "Мужская одежда", "mens-clothing", ""),
    ("clothing-shoes", "Женская одежда", "womens-clothing", ""),
    ("clothing-shoes", "Обувь мужская", "mens-shoes", ""),
    ("clothing-shoes", "Обувь женская", "womens-shoes", ""),
    ("clothing-shoes", "Спортивная обувь", "sport-shoes", ""),
]

PRODUCTS_DATA = [
    {
        "name": "Samsung Galaxy S24 Ultra 256GB",
        "description": "Флагманский смартфон Samsung с мощным процессором Snapdragon 8 Gen 3 и улучшенной системой камер.",
        "category": "smartphones",
        "brand": "Samsung",
        "image": "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=400",
        "rating": 4.8,
        "reviews": 1245,
        "price": 549990,
        "orig_price": 649990,
    },
    {
        "name": "Apple iPhone 15 Pro 128GB",
        "description": "iPhone 15 Pro с чипом A17 Pro, новой системой камер и усовершенствованным дизайном из титана.",
        "category": "smartphones",
        "brand": "Apple",
        "image": "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?w=400",
        "rating": 4.9,
        "reviews": 2341,
        "price": 589990,
        "orig_price": 679990,
    },
    {
        "name": "MacBook Air M2 13\" 256GB",
        "description": "Ультратонкий ноутбук Apple с чипом M2, 8 ГБ RAM и до 18 часов работы от батареи.",
        "category": "laptops",
        "brand": "Apple",
        "image": "https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=400",
        "rating": 4.9,
        "reviews": 876,
        "price": 689990,
        "orig_price": 749990,
    },
    {
        "name": "Samsung 65\" QLED 4K Smart TV",
        "description": "65-дюймовый QLED телевизор с разрешением 4K, HDR и встроенным Smart TV.",
        "category": "tvs",
        "brand": "Samsung",
        "image": "https://images.unsplash.com/photo-1593359677879-a4bb92f829e1?w=400",
        "rating": 4.7,
        "reviews": 543,
        "price": 389990,
        "orig_price": 459990,
    },
    {
        "name": "LG Холодильник Side-by-Side 615л",
        "description": "Двухдверный холодильник LG с системой No Frost и функцией InstaView Door-in-Door.",
        "category": "refrigerators",
        "brand": "LG",
        "image": "https://images.unsplash.com/photo-1571175443880-49e1d25b2bc5?w=400",
        "rating": 4.6,
        "reviews": 321,
        "price": 299990,
        "orig_price": 349990,
    },
    {
        "name": "Sony PlayStation 5 Digital Edition",
        "description": "Игровая консоль нового поколения от Sony с поддержкой 4K, 120fps и трассировкой лучей.",
        "category": "gaming-consoles",
        "brand": "Sony",
        "image": "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=400",
        "rating": 4.8,
        "reviews": 1876,
        "price": 229990,
        "orig_price": 269990,
    },
    {
        "name": "Nike Air Max 270",
        "description": "Кроссовки Nike Air Max 270 с инновационной подошвой Air для максимального комфорта.",
        "category": "sport-shoes",
        "brand": "Nike",
        "image": "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400",
        "rating": 4.5,
        "reviews": 987,
        "price": 49990,
        "orig_price": 64990,
    },
    {
        "name": "Dyson V15 Detect Absolute",
        "description": "Беспроводной пылесос Dyson с лазерным обнаружением пыли и мощностью всасывания 240 АВт.",
        "category": "vacuums",
        "brand": "Dyson",
        "image": "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400",
        "rating": 4.7,
        "reviews": 654,
        "price": 189990,
        "orig_price": 219990,
    },
    {
        "name": 'Lenovo ThinkPad X1 Carbon 14"',
        "description": "Бизнес-ноутбук Lenovo ThinkPad X1 Carbon с процессором Intel Core i7, 16 ГБ RAM.",
        "category": "laptops",
        "brand": "Lenovo",
        "image": "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=400",
        "rating": 4.6,
        "reviews": 432,
        "price": 459990,
        "orig_price": 529990,
    },
    {
        "name": "AirPods Pro 2-го поколения",
        "description": "Беспроводные наушники Apple AirPods Pro с активным шумоподавлением и адаптивным звуком.",
        "category": "headphones",
        "brand": "Apple",
        "image": "https://images.unsplash.com/photo-1606741965429-02919b2e2a8f?w=400",
        "rating": 4.8,
        "reviews": 2156,
        "price": 109990,
        "orig_price": 129990,
    },
    {
        "name": "Adidas Ultraboost 22",
        "description": "Беговые кроссовки Adidas Ultraboost 22 с технологией Boost для максимального отклика.",
        "category": "sport-shoes",
        "brand": "Adidas",
        "image": "https://images.unsplash.com/photo-1608231387042-66d1773070a5?w=400",
        "rating": 4.4,
        "reviews": 765,
        "price": 54990,
        "orig_price": 69990,
    },
    {
        "name": "Samsung Galaxy Watch 6",
        "description": "Смарт-часы Samsung Galaxy Watch 6 с мониторингом здоровья и функцией GPS.",
        "category": "smart-watches",
        "brand": "Samsung",
        "image": "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400",
        "rating": 4.5,
        "reviews": 543,
        "price": 79990,
        "orig_price": 99990,
    },
]

DEMO_USERS = [
    {"email": "admin@tumar.kz", "password": "admin123", "name": "Администратор", "role": "admin"},
    {"email": "seller@tumar.kz", "password": "seller123", "name": "Продавец Demo", "role": "seller"},
    {"email": "user@tumar.kz", "password": "user123", "name": "Тестовый Пользователь", "role": "user"},
]

DEMO_STORES = [
    {
        "email": "seller@tumar.kz",
        "store_name": "TechStore KZ",
        "description": "Лучший магазин электроники в Казахстане",
        "legal_name": "ТОО TechStore",
        "bin": "123456789012",
        "address": "г. Алматы, пр. Абая, 52",
        "phone": "+7 701 123 4567",
        "store_email": "info@techstore.kz",
    }
]


def _slug(text: str) -> str:
    """Turn a product name into a safe filename stem."""
    s = text.lower()
    s = re.sub(r'[^a-z0-9]+', '_', s)
    return s[:40].strip('_')


def download_image(url: str, filename: str) -> str:
    """
    Download *url* into IMAGES_DIR/<filename>.
    Returns the local web path /images/<filename>, or the original URL on failure.
    """
    os.makedirs(IMAGES_DIR, exist_ok=True)
    dest = os.path.join(IMAGES_DIR, filename)
    if os.path.exists(dest):
        return "/images/" + filename
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=15) as resp, open(dest, "wb") as f:
            f.write(resp.read())
        print(f"    ↓ downloaded {filename}")
        return "/images/" + filename
    except Exception as e:
        print(f"    ⚠ could not download image ({e}), using URL as fallback")
        return url


def hash_password(password: str) -> str:
    import bcrypt
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()


def seed(cursor, conn):
    print("\n--- Seeding categories ---")
    # Pass 1: insert root categories
    for parent_slug, name, slug, icon in CATEGORIES:
        if parent_slug is None:
            try:
                cursor.execute(
                    "INSERT IGNORE INTO categories (name, slug, icon) VALUES (%s, %s, %s)",
                    (name, slug, icon),
                )
            except Exception as e:
                print(f"  Category error: {e}")
    conn.commit()

    # Build slug→id map
    cursor.execute("SELECT id, slug FROM categories")
    slug_to_id = {row[1]: row[0] for row in cursor.fetchall()}

    # Pass 2: insert child categories
    for parent_slug, name, slug, icon in CATEGORIES:
        if parent_slug is None:
            continue
        parent_id = slug_to_id.get(parent_slug)
        if not parent_id:
            print(f"  Warning: parent slug '{parent_slug}' not found, skipping '{name}'")
            continue
        try:
            cursor.execute(
                "INSERT IGNORE INTO categories (parent_id, name, slug, icon) VALUES (%s, %s, %s, %s)",
                (parent_id, name, slug, icon),
            )
        except Exception as e:
            print(f"  Category error: {e}")
    conn.commit()
    print(f"  {len(CATEGORIES)} categories seeded.")

    print("\n--- Seeding demo users ---")
    try:
        import bcrypt
        has_bcrypt = True
    except ImportError:
        has_bcrypt = False
        print("  Warning: bcrypt not installed, using sha256 for demo passwords")

    user_ids = {}
    for u in DEMO_USERS:
        cursor.execute("SELECT id FROM users WHERE email = %s", (u["email"],))
        existing = cursor.fetchone()
        if existing:
            user_ids[u["email"]] = existing[0]
            print(f"  User {u['email']} already exists.")
            continue

        if has_bcrypt:
            import bcrypt
            pwd_hash = bcrypt.hashpw(u["password"].encode(), bcrypt.gensalt()).decode()
        else:
            pwd_hash = "$2b$12$" + hashlib.sha256(u["password"].encode()).hexdigest()[:53]

        cursor.execute(
            "INSERT INTO users (email, password_hash, name, role) VALUES (%s, %s, %s, %s)",
            (u["email"], pwd_hash, u["name"], u["role"]),
        )
        conn.commit()
        user_ids[u["email"]] = cursor.lastrowid
        print(f"  Created user: {u['email']}")

    print("\n--- Seeding demo stores ---")
    for s in DEMO_STORES:
        owner_id = user_ids.get(s["email"])
        if not owner_id:
            continue
        cursor.execute("SELECT id FROM stores WHERE owner_id = %s", (owner_id,))
        if cursor.fetchone():
            print(f"  Store for {s['email']} already exists.")
            continue
        cursor.execute(
            """INSERT INTO stores (owner_id, name, description, legal_name, bin_number, address, phone, email, status)
               VALUES (%s, %s, %s, %s, %s, %s, %s, %s, 'active')""",
            (owner_id, s["store_name"], s["description"], s["legal_name"],
             s["bin"], s["address"], s["phone"], s["store_email"]),
        )
        conn.commit()
        print(f"  Created store: {s['store_name']}")

    print("\n--- Seeding products ---")
    cursor.execute("SELECT id FROM stores WHERE status = 'active' LIMIT 1")
    store_row = cursor.fetchone()
    if not store_row:
        print("  No active store found, skipping products.")
        return
    store_id = store_row[0]

    category_map = {}
    cursor.execute("SELECT slug, id FROM categories")
    for row in cursor.fetchall():
        category_map[row[0]] = row[1]

    for p in PRODUCTS_DATA:
        cursor.execute("SELECT id FROM products WHERE name = %s", (p["name"],))
        if cursor.fetchone():
            print(f"  Product '{p['name']}' already exists.")
            continue

        # Download image to images/ so it works without internet
        img_filename = f"demo_{_slug(p['name'])}.jpg"
        local_image = download_image(p["image"], img_filename)

        cat_id = category_map.get(p["category"], 1)
        cursor.execute(
            """INSERT INTO products (name, description, category_id, brand, main_image, rating, review_count)
               VALUES (%s, %s, %s, %s, %s, %s, %s)""",
            (p["name"], p["description"], cat_id, p["brand"], local_image, p["rating"], p["reviews"]),
        )
        conn.commit()
        product_id = cursor.lastrowid

        cursor.execute(
            """INSERT IGNORE INTO product_sellers (product_id, store_id, price, original_price, stock, delivery_days)
               VALUES (%s, %s, %s, %s, %s, %s)""",
            (product_id, store_id, p["price"], p["orig_price"], 50, 2),
        )
        conn.commit()
        print(f"  Created product: {p['name']}")

    # Fix already-seeded products that still use external URLs
    print("\n--- Fixing existing products with external image URLs ---")
    for p in PRODUCTS_DATA:
        cursor.execute("SELECT id, main_image FROM products WHERE name = %s", (p["name"],))
        row = cursor.fetchone()
        if row and row[1] and row[1].startswith("http"):
            img_filename = f"demo_{_slug(p['name'])}.jpg"
            local_image = download_image(p["image"], img_filename)
            if not local_image.startswith("http"):
                cursor.execute("UPDATE products SET main_image = %s WHERE id = %s", (local_image, row[0]))
                conn.commit()
                print(f"  Updated image for: {p['name']}")

    print("\nSeed complete!")
    print("\nDemo accounts:")
    for u in DEMO_USERS:
        print(f"  {u['role']}: {u['email']} / {u['password']}")


def main():
    print("=" * 50)
    print("Tumar Market - Seed Data")
    print("=" * 50)
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()
        seed(cursor, conn)
        cursor.close()
        conn.close()
    except mysql.connector.Error as err:
        print(f"DB Error: {err}")


if __name__ == "__main__":
    main()
