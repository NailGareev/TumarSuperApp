package main

import (
	"bufio"
	"database/sql"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"

	_ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

// readBdSettings читает bd_settings.txt из корня проекта.
// Файл ищется рядом с бинарным файлом, затем на уровень выше (при go run ./go/).
func readBdSettings() {
	candidates := []string{
		filepath.Join(execDir(), "bd_settings.txt"),
		filepath.Join(execDir(), "..", "bd_settings.txt"),
		"bd_settings.txt",
	}

	var settingsPath string
	for _, p := range candidates {
		if _, err := os.Stat(p); err == nil {
			settingsPath = p
			break
		}
	}
	if settingsPath == "" {
		log.Println("bd_settings.txt не найден — используются переменные окружения")
		return
	}

	f, err := os.Open(settingsPath)
	if err != nil {
		log.Printf("Не удалось открыть bd_settings.txt: %v", err)
		return
	}
	defer f.Close()

	keyMap := map[string]string{
		"login":    "DB_USER",
		"password": "DB_PASSWORD",
		"database": "DB_NAME",
		"host":     "DB_HOST",
		"port":     "DB_PORT",
	}

	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		idx := strings.Index(line, "=")
		if idx < 0 {
			continue
		}
		key := strings.TrimSpace(strings.ToLower(line[:idx]))
		value := strings.TrimSpace(line[idx+1:])

		if envKey, ok := keyMap[key]; ok {
			// Переменная окружения имеет приоритет, если уже задана явно
			if os.Getenv(envKey) == "" {
				os.Setenv(envKey, value)
			}
		}
	}
	log.Printf("Настройки БД загружены из: %s", settingsPath)
}

func execDir() string {
	exe, err := os.Executable()
	if err != nil {
		return "."
	}
	return filepath.Dir(exe)
}

func initDB() {
	readBdSettings()

	host := getEnv("DB_HOST", "localhost")
	port := getEnv("DB_PORT", "3306")
	user := getEnv("DB_USER", "root")
	password := getEnv("DB_PASSWORD", "")
	dbname := getEnv("DB_NAME", "tumar_market")

	// Создать БД если не существует
	dsn := fmt.Sprintf("%s:%s@tcp(%s:%s)/", user, password, host, port)
	tmpDB, err := sql.Open("mysql", dsn)
	if err != nil {
		log.Fatal("Не удалось подключиться к MySQL:", err)
	}
	_, err = tmpDB.Exec(fmt.Sprintf(
		"CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
		dbname,
	))
	if err != nil {
		log.Fatal("Не удалось создать базу данных:", err)
	}
	tmpDB.Close()

	dsn = fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?charset=utf8mb4&parseTime=True&loc=Asia%%2FAlmaty",
		user, password, host, port, dbname)
	db, err = sql.Open("mysql", dsn)
	if err != nil {
		log.Fatal("Ошибка открытия БД:", err)
	}
	if err = db.Ping(); err != nil {
		log.Fatal("Не удалось подключиться к БД:", err)
	}

	log.Printf("БД подключена: %s@%s:%s/%s", user, host, port, dbname)
	runMigrations()
}

func runMigrations() {
	migrations := []string{
		`CREATE TABLE IF NOT EXISTS users (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			email VARCHAR(191) UNIQUE NOT NULL,
			password_hash VARCHAR(255) NOT NULL,
			name VARCHAR(255) NOT NULL,
			phone VARCHAR(20) DEFAULT '',
			role ENUM('user','seller','admin') DEFAULT 'user',
			avatar VARCHAR(500) DEFAULT '',
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			updated_at TIMESTAMP NULL DEFAULT NULL
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS stores (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			owner_id BIGINT NOT NULL,
			name VARCHAR(255) NOT NULL,
			description TEXT,
			logo VARCHAR(500) DEFAULT '',
			legal_name VARCHAR(255) NOT NULL,
			bin_number VARCHAR(20) NOT NULL,
			address TEXT,
			phone VARCHAR(20) DEFAULT '',
			email VARCHAR(255) DEFAULT '',
			status ENUM('pending','active','suspended') DEFAULT 'pending',
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			updated_at TIMESTAMP NULL DEFAULT NULL,
			FOREIGN KEY (owner_id) REFERENCES users(id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS categories (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			parent_id BIGINT DEFAULT NULL,
			name VARCHAR(255) NOT NULL,
			slug VARCHAR(191) UNIQUE NOT NULL,
			icon VARCHAR(255) DEFAULT '',
			image VARCHAR(500) DEFAULT '',
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS products (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			name VARCHAR(500) NOT NULL,
			description TEXT,
			category_id BIGINT,
			brand VARCHAR(255) DEFAULT '',
			main_image VARCHAR(500) DEFAULT '',
			rating DECIMAL(3,2) DEFAULT 0.00,
			review_count INT DEFAULT 0,
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			updated_at TIMESTAMP NULL DEFAULT NULL,
			FOREIGN KEY (category_id) REFERENCES categories(id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS product_sellers (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			product_id BIGINT NOT NULL,
			store_id BIGINT NOT NULL,
			price DECIMAL(10,2) NOT NULL,
			original_price DECIMAL(10,2) DEFAULT 0,
			stock INT DEFAULT 0,
			delivery_days INT DEFAULT 3,
			is_active TINYINT(1) DEFAULT 1,
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			updated_at TIMESTAMP NULL DEFAULT NULL,
			FOREIGN KEY (product_id) REFERENCES products(id),
			FOREIGN KEY (store_id) REFERENCES stores(id),
			UNIQUE KEY unique_product_store (product_id, store_id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS product_images (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			product_id BIGINT NOT NULL,
			image_url VARCHAR(500) NOT NULL,
			sort_order INT DEFAULT 0,
			FOREIGN KEY (product_id) REFERENCES products(id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS product_attributes (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			product_id BIGINT NOT NULL,
			name VARCHAR(255) NOT NULL,
			value TEXT NOT NULL,
			FOREIGN KEY (product_id) REFERENCES products(id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS reviews (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			product_id BIGINT NOT NULL,
			user_id BIGINT NOT NULL,
			store_id BIGINT DEFAULT NULL,
			rating INT NOT NULL,
			comment TEXT,
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			FOREIGN KEY (product_id) REFERENCES products(id),
			FOREIGN KEY (user_id) REFERENCES users(id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS cart_items (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			user_id BIGINT NOT NULL,
			product_seller_id BIGINT NOT NULL,
			quantity INT DEFAULT 1,
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			FOREIGN KEY (user_id) REFERENCES users(id),
			FOREIGN KEY (product_seller_id) REFERENCES product_sellers(id),
			UNIQUE KEY unique_cart_item (user_id, product_seller_id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS orders (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			user_id BIGINT NOT NULL,
			total DECIMAL(10,2) NOT NULL,
			status ENUM('pending','confirmed','processing','shipped','delivered','cancelled') DEFAULT 'pending',
			issue_code CHAR(4) DEFAULT NULL,
			issue_code_sent_at TIMESTAMP NULL DEFAULT NULL,
			delivery_address TEXT,
			payment_method VARCHAR(50) DEFAULT '',
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			updated_at TIMESTAMP NULL DEFAULT NULL,
			FOREIGN KEY (user_id) REFERENCES users(id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS order_items (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			order_id BIGINT NOT NULL,
			product_seller_id BIGINT NOT NULL,
			product_name VARCHAR(500) NOT NULL,
			product_image VARCHAR(500) DEFAULT '',
			store_name VARCHAR(255) NOT NULL,
			quantity INT NOT NULL,
			price DECIMAL(10,2) NOT NULL,
			FOREIGN KEY (order_id) REFERENCES orders(id),
			FOREIGN KEY (product_seller_id) REFERENCES product_sellers(id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS notifications (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			user_id BIGINT NOT NULL,
			order_id BIGINT DEFAULT NULL,
			title VARCHAR(255) NOT NULL,
			message TEXT NOT NULL,
			is_read TINYINT(1) DEFAULT 0,
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			FOREIGN KEY (user_id) REFERENCES users(id),
			FOREIGN KEY (order_id) REFERENCES orders(id),
			INDEX idx_notifications_user (user_id, is_read, created_at)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,

		`CREATE TABLE IF NOT EXISTS favorites (
			id BIGINT AUTO_INCREMENT PRIMARY KEY,
			user_id BIGINT NOT NULL,
			product_id BIGINT NOT NULL,
			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
			UNIQUE KEY unique_fav (user_id, product_id),
			FOREIGN KEY (user_id) REFERENCES users(id),
			FOREIGN KEY (product_id) REFERENCES products(id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,
	}

	for _, m := range migrations {
		if _, err := db.Exec(m); err != nil {
			log.Printf("Ошибка миграции: %v", err)
		}
	}

	ensureOrderColumns()
	recalculateProductRatings()
	seedCategories()
	log.Println("Миграции выполнены")
}

func recalculateProductRatings() {
	_, err := db.Exec(`
		UPDATE products p SET
			rating = COALESCE((SELECT AVG(r.rating) FROM reviews r WHERE r.product_id = p.id), 0),
			review_count = COALESCE((SELECT COUNT(*) FROM reviews r WHERE r.product_id = p.id), 0)
	`)
	if err != nil {
		log.Printf("Ошибка пересчёта рейтингов: %v", err)
	}
}

func ensureOrderColumns() {
	addOrderColumnIfMissing("issue_code", "ALTER TABLE orders ADD COLUMN issue_code CHAR(4) DEFAULT NULL")
	addOrderColumnIfMissing("issue_code_sent_at", "ALTER TABLE orders ADD COLUMN issue_code_sent_at TIMESTAMP NULL DEFAULT NULL")
	addOrderColumnIfMissing("tumar_ref", "ALTER TABLE orders ADD COLUMN tumar_ref VARCHAR(50) DEFAULT NULL")
}

func addOrderColumnIfMissing(column, ddl string) {
	switch column {
	case "issue_code", "issue_code_sent_at", "tumar_ref":
	default:
		log.Printf("Неподдерживаемый столбец orders.%s", column)
		return
	}
	var count int
	err := db.QueryRow(`
		SELECT COUNT(*)
		FROM information_schema.COLUMNS
		WHERE TABLE_SCHEMA = DATABASE()
			AND TABLE_NAME = 'orders'
			AND COLUMN_NAME = ?`, column).Scan(&count)
	if err != nil {
		log.Printf("Ошибка проверки столбца orders.%s: %v", column, err)
		return
	}
	if count > 0 {
		return
	}
	if _, err := db.Exec(ddl); err != nil {
		log.Printf("Ошибка добавления столбца orders.%s: %v", column, err)
	}
}

func seedCategories() {
	type catDef struct {
		parentSlug string
		name       string
		slug       string
		icon       string
	}

	all := []catDef{
		// ── Parent categories ──────────────────────────────────────
		{"", "Телефоны и гаджеты", "phones-gadgets", "📱"},
		{"", "Бытовая техника", "home-appliances", "🏠"},
		{"", "ТВ, Аудио, Видео", "tv-audio-video", "📺"},
		{"", "Компьютеры", "computers", "💻"},
		{"", "Мебель и интерьер", "furniture", "🛋"},
		{"", "Красота и здоровье", "beauty-health", "💄"},
		{"", "Детские товары", "kids", "🧸"},
		{"", "Аптека", "pharmacy", "💊"},
		{"", "Строительство, ремонт", "construction", "🔨"},
		{"", "Спорт, туризм", "sport-tourism", "⚽"},
		{"", "Досуг, книги", "leisure-books", "📚"},
		{"", "Автотовары", "auto", "🚗"},
		{"", "Украшения, аксессуары", "jewelry-accessories", "💍"},
		{"", "Одежда и обувь", "clothing-shoes", "👔"},

		// ── Телефоны и гаджеты ────────────────────────────────────
		{"phones-gadgets", "Смартфоны", "smartphones", ""},
		{"phones-gadgets", "Мобильные телефоны", "mobile-phones", ""},
		{"phones-gadgets", "Планшеты", "tablets", ""},
		{"phones-gadgets", "Умные часы", "smart-watches", ""},
		{"phones-gadgets", "Наушники", "headphones", ""},
		{"phones-gadgets", "Чехлы для телефонов", "phone-cases", ""},
		{"phones-gadgets", "Зарядные устройства", "chargers", ""},
		{"phones-gadgets", "Портативные аккумуляторы", "power-banks", ""},
		{"phones-gadgets", "Фотоаппараты", "cameras", ""},

		// ── Бытовая техника ───────────────────────────────────────
		{"home-appliances", "Холодильники", "refrigerators", ""},
		{"home-appliances", "Стиральные машины", "washing-machines", ""},
		{"home-appliances", "Пылесосы", "vacuums", ""},
		{"home-appliances", "Микроволновые печи", "microwaves", ""},
		{"home-appliances", "Электрические плиты", "stoves", ""},
		{"home-appliances", "Утюги", "irons", ""},
		{"home-appliances", "Кондиционеры", "air-conditioners", ""},
		{"home-appliances", "Водонагреватели", "water-heaters", ""},
		{"home-appliances", "Посудомоечные машины", "dishwashers", ""},

		// ── ТВ, Аудио, Видео ──────────────────────────────────────
		{"tv-audio-video", "Телевизоры", "tvs", ""},
		{"tv-audio-video", "Проекторы", "projectors", ""},
		{"tv-audio-video", "Аудиосистемы", "audio-systems", ""},
		{"tv-audio-video", "Саундбары", "soundbars", ""},
		{"tv-audio-video", "Игровые приставки", "gaming-consoles", ""},

		// ── Компьютеры ────────────────────────────────────────────
		{"computers", "Ноутбуки", "laptops", ""},
		{"computers", "Настольные компьютеры", "desktops", ""},
		{"computers", "Мониторы", "monitors", ""},
		{"computers", "Принтеры", "printers", ""},
		{"computers", "Клавиатуры", "keyboards", ""},
		{"computers", "Мышки", "mice", ""},
		{"computers", "Роутеры", "routers", ""},

		// ── Мебель и интерьер ─────────────────────────────────────
		{"furniture", "Диваны и кресла", "sofas", ""},
		{"furniture", "Кровати", "beds", ""},
		{"furniture", "Шкафы", "wardrobes", ""},
		{"furniture", "Столы", "tables", ""},
		{"furniture", "Матрасы", "mattresses", ""},
		{"furniture", "Освещение", "lighting", ""},
		{"furniture", "Ковры", "carpets", ""},

		// ── Красота и здоровье ────────────────────────────────────
		{"beauty-health", "Уход за лицом", "face-care", ""},
		{"beauty-health", "Уход за телом", "body-care", ""},
		{"beauty-health", "Парфюмерия", "perfume", ""},
		{"beauty-health", "Декоративная косметика", "makeup", ""},
		{"beauty-health", "Фены и стайлеры", "hair-dryers", ""},
		{"beauty-health", "Электробритвы", "shavers", ""},

		// ── Детские товары ────────────────────────────────────────
		{"kids", "Игрушки", "toys", ""},
		{"kids", "Коляски", "strollers", ""},
		{"kids", "Детская одежда", "kids-clothing", ""},
		{"kids", "Питание для детей", "baby-food", ""},
		{"kids", "Школьные товары", "school-supplies", ""},
		{"kids", "Детские велосипеды", "kids-bikes", ""},

		// ── Аптека ────────────────────────────────────────────────
		{"pharmacy", "Витамины и БАД", "vitamins", ""},
		{"pharmacy", "Медицинские приборы", "medical-devices", ""},
		{"pharmacy", "Гигиена и уход", "hygiene", ""},

		// ── Строительство, ремонт ─────────────────────────────────
		{"construction", "Строительные материалы", "building-materials", ""},
		{"construction", "Инструменты", "tools", ""},
		{"construction", "Сантехника", "plumbing", ""},
		{"construction", "Электрика", "electrical", ""},
		{"construction", "Краски и лаки", "paints", ""},

		// ── Спорт, туризм ─────────────────────────────────────────
		{"sport-tourism", "Тренажёры", "fitness-equipment", ""},
		{"sport-tourism", "Велосипеды", "bikes", ""},
		{"sport-tourism", "Туристическое снаряжение", "camping", ""},
		{"sport-tourism", "Спортивная одежда", "sportswear", ""},
		{"sport-tourism", "Рыбалка", "fishing", ""},

		// ── Досуг, книги ──────────────────────────────────────────
		{"leisure-books", "Книги", "books", ""},
		{"leisure-books", "Настольные игры", "board-games", ""},
		{"leisure-books", "Музыкальные инструменты", "musical-instruments", ""},
		{"leisure-books", "Хобби и творчество", "hobbies", ""},

		// ── Автотовары ────────────────────────────────────────────
		{"auto", "Автошины", "tires", ""},
		{"auto", "Автозапчасти", "car-parts", ""},
		{"auto", "Автоаксессуары", "car-accessories", ""},
		{"auto", "Автохимия", "car-chemicals", ""},
		{"auto", "Видеорегистраторы", "dash-cams", ""},

		// ── Украшения, аксессуары ─────────────────────────────────
		{"jewelry-accessories", "Ювелирные украшения", "jewelry", ""},
		{"jewelry-accessories", "Часы", "watches", ""},
		{"jewelry-accessories", "Сумки", "bags", ""},
		{"jewelry-accessories", "Очки", "glasses", ""},

		// ── Одежда и обувь ────────────────────────────────────────
		{"clothing-shoes", "Мужская одежда", "mens-clothing", ""},
		{"clothing-shoes", "Женская одежда", "womens-clothing", ""},
		{"clothing-shoes", "Обувь мужская", "mens-shoes", ""},
		{"clothing-shoes", "Обувь женская", "womens-shoes", ""},
		{"clothing-shoes", "Спортивная обувь", "sport-shoes", ""},
	}

	// Pass 1: insert parent categories
	for _, c := range all {
		if c.parentSlug == "" {
			db.Exec("INSERT IGNORE INTO categories (name, slug, icon) VALUES (?, ?, ?)", c.name, c.slug, c.icon)
		}
	}

	// Build slug→id map
	slugToID := map[string]int64{}
	rows, err := db.Query("SELECT id, slug FROM categories")
	if err == nil {
		for rows.Next() {
			var id int64
			var slug string
			rows.Scan(&id, &slug)
			slugToID[slug] = id
		}
		rows.Close()
	}

	// Pass 2: insert child categories
	for _, c := range all {
		if c.parentSlug == "" {
			continue
		}
		parentID, ok := slugToID[c.parentSlug]
		if !ok {
			continue
		}
		db.Exec("INSERT IGNORE INTO categories (parent_id, name, slug, icon) VALUES (?, ?, ?, ?)",
			parentID, c.name, c.slug, c.icon)
	}
}

func getEnv(key, defaultVal string) string {
	if val := os.Getenv(key); val != "" {
		return val
	}
	return defaultVal
}
