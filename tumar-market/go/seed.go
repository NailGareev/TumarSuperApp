package main

import (
	"log"

	"golang.org/x/crypto/bcrypt"
)

// dbInsert executes an INSERT and returns the new row ID (0 on error).
func dbInsert(query string, args ...interface{}) int64 {
	res, err := db.Exec(query, args...)
	if err != nil {
		log.Printf("dbInsert: %v", err)
		return 0
	}
	id, _ := res.LastInsertId()
	return id
}

// catID returns the DB id for a category slug, 0 if not found.
func catID(slug string) int64 {
	var id int64
	db.QueryRow("SELECT id FROM categories WHERE slug=?", slug).Scan(&id)
	return id
}

type seedReview struct {
	userID  int64
	rating  int
	comment string
}

type seedProduct struct {
	name      string
	desc      string
	catSlug   string
	brand     string
	mainImg   string
	extraImgs []string
	attrs     [][2]string
	price     float64
	origPrice float64
	storeID   int64
	stock     int
	delivery  int
	reviews   []seedReview
}

func seedDemoData() {
	var existing int
	db.QueryRow("SELECT COUNT(*) FROM users WHERE email='seller1@tumar.kz'").Scan(&existing)
	if existing > 0 {
		return
	}
	log.Println("Заполнение демо-данных...")

	hash, _ := bcrypt.GenerateFromPassword([]byte("Demo1234"), bcrypt.DefaultCost)
	ph := string(hash)

	// ── Users ──────────────────────────────────────────────────────────────────
	dbInsert("INSERT INTO users (email,password_hash,name,phone,role) VALUES(?,?,?,?,?)",
		"admin@tumar.kz", ph, "Администратор", "+77001000000", "admin")
	s1 := dbInsert("INSERT INTO users (email,password_hash,name,phone,role) VALUES(?,?,?,?,?)",
		"seller1@tumar.kz", ph, "Арман Сейткали", "+77011234567", "seller")
	s2 := dbInsert("INSERT INTO users (email,password_hash,name,phone,role) VALUES(?,?,?,?,?)",
		"seller2@tumar.kz", ph, "Гүлнар Бекова", "+77017654321", "seller")
	s3 := dbInsert("INSERT INTO users (email,password_hash,name,phone,role) VALUES(?,?,?,?,?)",
		"seller3@tumar.kz", ph, "Нурлан Жанов", "+77019876543", "seller")
	b1 := dbInsert("INSERT INTO users (email,password_hash,name,phone,role) VALUES(?,?,?,?,?)",
		"buyer1@tumar.kz", ph, "Дамир Ахметов", "+77021112233", "user")
	b2 := dbInsert("INSERT INTO users (email,password_hash,name,phone,role) VALUES(?,?,?,?,?)",
		"buyer2@tumar.kz", ph, "Айгерим Касымова", "+77022233445", "user")
	b3 := dbInsert("INSERT INTO users (email,password_hash,name,phone,role) VALUES(?,?,?,?,?)",
		"buyer3@tumar.kz", ph, "Берик Нуров", "+77023344556", "user")

	// ── Stores ─────────────────────────────────────────────────────────────────
	st1 := dbInsert(`INSERT INTO stores
		(owner_id,name,description,logo,legal_name,bin_number,address,phone,email,status)
		VALUES(?,?,?,?,?,?,?,?,?,?)`,
		s1, "ТехноМаркет",
		"Официальный дилер Samsung, Apple, Xiaomi, ASUS, Sony. Широкий ассортимент электроники и гаджетов с гарантией производителя.",
		"https://picsum.photos/seed/technomarket-logo/100/100",
		"ТОО «ТехноМаркет»", "123456789012",
		"г. Алматы, пр. Абая, 10", "+77011234567", "info@technomarket.kz", "active")

	st2 := dbInsert(`INSERT INTO stores
		(owner_id,name,description,logo,legal_name,bin_number,address,phone,email,status)
		VALUES(?,?,?,?,?,?,?,?,?,?)`,
		s2, "МодаKZ",
		"Модная одежда и обувь ведущих мировых брендов: Nike, Adidas, Levi's. Оригинальная продукция с доставкой по Казахстану.",
		"https://picsum.photos/seed/modakz-logo/100/100",
		"ТОО «МодаKZ»", "234567890123",
		"г. Алматы, ул. Толе Би, 25", "+77017654321", "info@modakz.kz", "active")

	st3 := dbInsert(`INSERT INTO stores
		(owner_id,name,description,logo,legal_name,bin_number,address,phone,email,status)
		VALUES(?,?,?,?,?,?,?,?,?,?)`,
		s3, "ДомоМаркет",
		"Бытовая техника и товары для дома: Dyson, LG, Samsung. Доставка и установка по всему Алматы.",
		"https://picsum.photos/seed/domomarkt-logo/100/100",
		"ТОО «ДомоМаркет»", "345678901234",
		"г. Алматы, пр. Аль-Фараби, 5", "+77019876543", "info@domomarket.kz", "active")

	// ── Category images ────────────────────────────────────────────────────────
	catImgs := map[string]string{
		"phones-gadgets":      "https://picsum.photos/seed/phones-gadgets/400/400",
		"home-appliances":     "https://picsum.photos/seed/home-appliances/400/400",
		"tv-audio-video":      "https://picsum.photos/seed/tv-audio/400/400",
		"computers":           "https://picsum.photos/seed/computers/400/400",
		"furniture":           "https://picsum.photos/seed/furniture/400/400",
		"beauty-health":       "https://picsum.photos/seed/beauty/400/400",
		"kids":                "https://picsum.photos/seed/kids/400/400",
		"pharmacy":            "https://picsum.photos/seed/pharmacy/400/400",
		"construction":        "https://picsum.photos/seed/construction/400/400",
		"sport-tourism":       "https://picsum.photos/seed/sport/400/400",
		"leisure-books":       "https://picsum.photos/seed/books/400/400",
		"auto":                "https://picsum.photos/seed/automobile/400/400",
		"jewelry-accessories": "https://picsum.photos/seed/jewelry/400/400",
		"clothing-shoes":      "https://picsum.photos/seed/clothing/400/400",
	}
	for slug, img := range catImgs {
		db.Exec("UPDATE categories SET image=? WHERE slug=?", img, slug)
	}

	// ── Products ───────────────────────────────────────────────────────────────
	products := []seedProduct{
		// ── Smartphones ─────────────────────────────────────────────────────────
		{
			name:    "Samsung Galaxy S24 Ultra",
			desc:    "Флагманский смартфон с революционным стилусом S Pen встроенным в корпус. Основная камера 200 МП с оптическим зумом 10x позволяет снимать как профессионал. Процессор Snapdragon 8 Gen 3, дисплей Dynamic AMOLED 6.8\" 120Гц QHD+ и аккумулятор 5000 мАч с поддержкой 45W зарядки. Galaxy AI — искусственный интеллект прямо в вашем телефоне. 7 лет обновлений Android.",
			catSlug: "smartphones", brand: "Samsung",
			mainImg:   "https://picsum.photos/seed/samsung-s24-ultra/600/600",
			extraImgs: []string{"https://picsum.photos/seed/samsung-s24-ultra-2/600/600", "https://picsum.photos/seed/samsung-s24-ultra-3/600/600"},
			attrs: [][2]string{
				{"Дисплей", "6.8\" Dynamic AMOLED 120Гц QHD+"},
				{"Камера", "200+12+10+10 МП, зум 10x"},
				{"Процессор", "Snapdragon 8 Gen 3"},
				{"ОЗУ", "12 ГБ"},
				{"Память", "256 ГБ"},
				{"Аккумулятор", "5000 мАч, 45W"},
				{"ОС", "Android 14 / One UI 6.1"},
			},
			price: 549990, origPrice: 599990, storeID: st1, stock: 15, delivery: 2,
			reviews: []seedReview{
				{b1, 5, "Лучший Android-телефон на рынке! Камера снимает невероятно чётко, S Pen очень удобен для заметок и набросков. Аккумулятор держит полтора дня при активном использовании."},
				{b2, 5, "Купила в подарок мужу — в полном восторге! Фото получаются профессиональные. Зум 10x работает отлично даже ночью. Рекомендую!"},
				{b3, 4, "Отличный аппарат, но цена высоковата. Камера и производительность на высшем уровне. Немного тяжёлый, но привыкаешь."},
			},
		},
		{
			name:    "iPhone 15 Pro Max 256 ГБ",
			desc:    "Apple iPhone 15 Pro Max с революционным чипом A17 Pro из 3нм техпроцесса — самым мощным в истории смартфонов. Первый iPhone с титановым корпусом Grade 5 — прочным и лёгким. Камера с 5-кратным оптическим зумом и системой Tetraprism. USB-C с поддержкой USB 3 для передачи данных. Динамический остров и дисплей Super Retina XDR 6.7\" Always-On с ProMotion 120Гц.",
			catSlug: "smartphones", brand: "Apple",
			mainImg:   "https://picsum.photos/seed/iphone15pro-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/iphone15pro-back/600/600", "https://picsum.photos/seed/iphone15pro-side/600/600"},
			attrs: [][2]string{
				{"Дисплей", "6.7\" Super Retina XDR 120Гц"},
				{"Камера", "48+12+12 МП, оптический зум 5x"},
				{"Процессор", "Apple A17 Pro"},
				{"Память", "256 ГБ NVMe"},
				{"Аккумулятор", "4422 мАч"},
				{"Корпус", "Титан Grade 5 + стекло Ceramic Shield"},
				{"ОС", "iOS 17"},
			},
			price: 679990, origPrice: 729990, storeID: st1, stock: 10, delivery: 1,
			reviews: []seedReview{
				{b2, 5, "Перешла с Android — и не жалею! Зум 5x просто фантастика для съёмки концертов и природы. A17 Pro не тормозит ни в играх, ни в видеомонтаже."},
				{b3, 5, "Качество сборки на высоте — титан чувствуется в руках. Камера намного лучше предыдущего iPhone. Dynamic Island стал привычным быстро."},
			},
		},
		{
			name:    "Xiaomi 14 Pro",
			desc:    "Xiaomi 14 Pro — флагман с профессиональной камерой Leica и тройным объективом по 50 МП в каждом. Уникальная система Summilux обеспечивает богатые, живые цвета в стиле легендарных объективов Leica. Процессор Snapdragon 8 Gen 3, керамический корпус и AMOLED дисплей 6.73\" 120Гц. Супербыстрая зарядка 120W — полный заряд за 23 минуты. Беспроводная зарядка 50W.",
			catSlug: "smartphones", brand: "Xiaomi",
			mainImg:   "https://picsum.photos/seed/xiaomi14pro-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/xiaomi14pro-camera/600/600"},
			attrs: [][2]string{
				{"Дисплей", "6.73\" AMOLED 120Гц, 3200×1440"},
				{"Камера", "50+50+50 МП Leica Summilux"},
				{"Процессор", "Snapdragon 8 Gen 3"},
				{"ОЗУ / Память", "12 ГБ / 256 ГБ"},
				{"Аккумулятор", "4880 мАч"},
				{"Зарядка", "120W (кабель) + 50W (WC)"},
				{"Корпус", "Керамика / Стекло"},
			},
			price: 399990, origPrice: 449990, storeID: st1, stock: 20, delivery: 3,
			reviews: []seedReview{
				{b1, 4, "Камера Leica действительно отличается — цвета живые, портреты с боке выглядят дорого. Зарядка 120W это вообще чудо — за завтрак заряжается полностью."},
				{b3, 5, "Лучшее соотношение цены и качества среди флагманов. Быстрее айфона заряжается, снимает не хуже. Рекомендую всем!"},
			},
		},
		// ── Laptops ─────────────────────────────────────────────────────────────
		{
			name:    "MacBook Pro 14\" M3 (2023)",
			desc:    "MacBook Pro 14\" с чипом Apple M3 — революционная производительность для профессионалов. Дисплей Liquid Retina XDR с яркостью до 1600 нит в SDR и 1000 нит в HDR, ProMotion 120Гц. До 18 часов автономной работы. Порты: MagSafe 3, три Thunderbolt 4, HDMI 2.1, SD-кард ридер, разъём для наушников. Звук акустической системы с шестью динамиками и Spatial Audio.",
			catSlug: "laptops", brand: "Apple",
			mainImg:   "https://picsum.photos/seed/macbook-m3-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/macbook-m3-open/600/600", "https://picsum.photos/seed/macbook-m3-side/600/600"},
			attrs: [][2]string{
				{"Процессор", "Apple M3 (8-ядерный CPU, 10-ядерный GPU)"},
				{"Оперативная память", "18 ГБ Unified Memory"},
				{"Накопитель", "512 ГБ SSD"},
				{"Дисплей", "14.2\" Liquid Retina XDR 120Гц"},
				{"Автономность", "До 18 часов"},
				{"Вес", "1.55 кг"},
				{"Цвет", "Серебристый / «Космический чёрный»"},
			},
			price: 849990, origPrice: 899990, storeID: st1, stock: 8, delivery: 2,
			reviews: []seedReview{
				{b2, 5, "Работаю дизайнером — MacBook Pro M3 просто летает! Render в Blender занимает вдвое меньше времени чем на прошлом Intel-маке. Дисплей XDR вообще шедевр."},
				{b1, 5, "Аккумулятор держит весь рабочий день с запасом — реально 15–16 часов при средней нагрузке. Лучший ноутбук из всех что у меня были."},
			},
		},
		{
			name:    "ASUS ROG Zephyrus G14 (2024)",
			desc:    "Игровой ноутбук с AMD Ryzen 9 8945HS и NVIDIA GeForce RTX 4070. Дисплей OLED 14\" 165Гц — 100% DCI-P3, яркость 600 нит, толщина 1мм. 32 ГБ DDR5 и 1 ТБ SSD PCIe 4.0. Вес всего 1.65 кг. Технология MUX Switch для максимальной производительности в играх. Система охлаждения Tri-Fan с 87 лопастями.",
			catSlug: "laptops", brand: "ASUS",
			mainImg:   "https://picsum.photos/seed/asus-rog-g14-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/asus-rog-g14-lid/600/600"},
			attrs: [][2]string{
				{"Процессор", "AMD Ryzen 9 8945HS (8 ядер, до 5.2 ГГц)"},
				{"Видеокарта", "NVIDIA RTX 4070 8 ГБ GDDR6"},
				{"ОЗУ", "32 ГБ DDR5-7500"},
				{"Накопитель", "1 ТБ NVMe SSD PCIe 4.0"},
				{"Дисплей", "14\" OLED 165Гц, 100% DCI-P3"},
				{"Вес", "1.65 кг"},
				{"ОС", "Windows 11 Home"},
			},
			price: 649990, origPrice: 699990, storeID: st1, stock: 6, delivery: 3,
			reviews: []seedReview{
				{b3, 5, "Играю в Cyberpunk, Helldivers 2 — всё на максималках плавно. Дисплей OLED с 165Гц выглядит потрясающе, не сравнить с IPS. Для игрового ноута ещё и лёгкий!"},
				{b1, 4, "Отличный баланс мощности и мобильности. Нагревается под нагрузкой, но это ожидаемо. Главное — производительность не страдает."},
			},
		},
		// ── Headphones ──────────────────────────────────────────────────────────
		{
			name:    "Apple AirPods Pro 2 (USB-C)",
			desc:    "AirPods Pro 2 с чипом H2 — лучшее активное шумоподавление среди вкладышей. Адаптивное аудио автоматически регулирует степень шумоподавления и прозрачности. Персонализированный пространственный звук с отслеживанием движений головы. Жест Double Tap для управления воспроизведением. Чехол MagSafe с динамиком, точным поиском и петлёй. До 6 часов воспроизведения / 30 часов с кейсом.",
			catSlug: "headphones", brand: "Apple",
			mainImg:   "https://picsum.photos/seed/airpodspro2-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/airpodspro2-case/600/600"},
			attrs: [][2]string{
				{"Тип", "TWS вкладыши с ANC"},
				{"Чип", "Apple H2"},
				{"Шумоподавление", "Активное адаптивное"},
				{"Автономность", "6 ч / 30 ч с кейсом"},
				{"Соединение", "Bluetooth 5.3"},
				{"Влагозащита", "IPX4 (наушники + кейс)"},
			},
			price: 129990, origPrice: 149990, storeID: st1, stock: 25, delivery: 1,
			reviews: []seedReview{
				{b2, 5, "Шумоподавление волшебное — в метро в час пик полная тишина, слышна только музыка. Звук чистый, бас приятный. Рекомендую всем без исключения!"},
				{b3, 5, "Использую для работы и тренировок. Сидят плотно, не вываливаются даже при беге. Батареи хватает на весь рабочий день и ещё остаётся."},
				{b1, 4, "Отличные наушники, но цена высоковата. Шумоподавление лучшее из всего, что я пробовал. Адаптивный режим очень удобен в городе."},
			},
		},
		{
			name:    "Sony WH-1000XM5",
			desc:    "Накладные наушники Sony с лучшим в классе шумоподавлением через 8 микрофонов и два фирменных чипа QN1 + V1. Поддержка LDAC для высококачественного беспроводного звука 990 кбит/с. Multipoint — одновременное соединение с двумя устройствами. 30 часов работы, быстрая зарядка: 3 минуты = 3 часа. Speak-to-Chat — распознаёт речь и включает режим прозрачности.",
			catSlug: "headphones", brand: "Sony",
			mainImg:   "https://picsum.photos/seed/sony-xm5-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/sony-xm5-folded/600/600"},
			attrs: [][2]string{
				{"Тип", "Полноразмерные накладные"},
				{"Шумоподавление", "ANC (8 микрофонов, QN1+V1)"},
				{"Автономность", "30 ч (ANC вкл)"},
				{"Соединение", "Bluetooth 5.2 Multipoint"},
				{"Кодеки", "LDAC, aptX HD, AAC, SBC"},
				{"Вес", "250 г"},
			},
			price: 119990, origPrice: 139990, storeID: st1, stock: 18, delivery: 2,
			reviews: []seedReview{
				{b1, 5, "Использую для удалённой работы — шумоподавление изолирует от домашних полностью. Звук детальный, сцена широкая. Через 3 часа непрерывного ношения не давят."},
				{b3, 4, "Хорошие наушники, звук понравился. Через 2–3 часа немного давят на уши, но это субъективно. Качество за свои деньги отличное."},
			},
		},
		// ── TVs ─────────────────────────────────────────────────────────────────
		{
			name:    "Samsung QLED 55\" Q80C (2023)",
			desc:    "Телевизор QLED 4K с квантовыми точками и процессором Quantum 4K AI Neural. Частота 120Гц с Motion Xcelerator Turbo+ обеспечивает чёткость при спорте и играх. HDR10+ с яркостью 2000 нит. Anti-Reflection покрытие убирает блики. Smart TV Tizen с голосовыми помощниками Bixby и Alexa, Gaming Hub для облачных игр.",
			catSlug: "tvs", brand: "Samsung",
			mainImg:   "https://picsum.photos/seed/samsung-q80c-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/samsung-q80c-back/600/600"},
			attrs: [][2]string{
				{"Диагональ", "55\""},
				{"Матрица", "QLED с квантовыми точками"},
				{"Разрешение", "4K UHD 3840×2160"},
				{"Частота обновления", "120 Гц"},
				{"HDR", "HDR10+ Adaptive"},
				{"ОС", "Tizen Smart TV"},
				{"HDMI 2.1", "2 порта"},
			},
			price: 319990, origPrice: 369990, storeID: st3, stock: 12, delivery: 5,
			reviews: []seedReview{
				{b2, 5, "Картинка потрясающая! QLED с HDR10+ — краски яркие и сочные, чёрный цвет глубокий. Smart TV работает быстро, интерфейс удобный. Очень доволен!"},
				{b1, 4, "Отличный телевизор за свои деньги. Gaming Hub удобный — играю в облачные игры без консоли. 120Гц заметно даже при просмотре спорта."},
			},
		},
		{
			name:    "LG OLED 55\" C3 (2023)",
			desc:    "LG OLED C3 — легендарная серия с самосветящимися пикселями OLED evo. Идеальный чёрный (пиксели отключаются), бесконечный контраст и яркость до 2100 нит в HDR. 4 порта HDMI 2.1, G-Sync Compatible и FreeSync Premium Pro — идеал для игр. Dolby Vision IQ и Dolby Atmos. ОС webOS 23 с ThinQ AI. Процессор α9 Gen6 AI.",
			catSlug: "tvs", brand: "LG",
			mainImg:   "https://picsum.photos/seed/lg-oled-c3-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/lg-oled-c3-side/600/600"},
			attrs: [][2]string{
				{"Диагональ", "55\""},
				{"Матрица", "OLED evo (самосветящиеся пиксели)"},
				{"Разрешение", "4K UHD"},
				{"Частота обновления", "120 Гц"},
				{"HDR", "Dolby Vision IQ, HDR10, HLG"},
				{"Игровые функции", "G-Sync, FreeSync, 4×HDMI 2.1"},
				{"ОС", "webOS 23 + ThinQ AI"},
			},
			price: 489990, origPrice: 549990, storeID: st3, stock: 7, delivery: 5,
			reviews: []seedReview{
				{b3, 5, "OLED — это совсем другой уровень! Смотрел 5 телевизоров в магазине, взял C3 — не пожалел ни разу. Ради такой картинки деньги не жаль."},
				{b1, 5, "Dolby Vision IQ работает идеально — картинка автоматически адаптируется под освещение. Для кино — однозначно лучший выбор."},
				{b2, 5, "Купили для игровой комнаты. 4 порта HDMI 2.1 — подключили Xbox, PS5, ПК и AV-ресивер. G-Sync работает, всё плавно!"},
			},
		},
		// ── Shoes ───────────────────────────────────────────────────────────────
		{
			name:    "Nike Air Max 270",
			desc:    "Nike Air Max 270 с самой большой воздушной камерой в истории Air Max — высота 32мм. Верх из инженерного сетчатого материала с армированными накладками обеспечивает дышащий комфорт. Подошва BRS 1000 из углеродистой резины в зоне пятки. Максимальный комфорт для повседневной носки в городе.",
			catSlug: "mens-shoes", brand: "Nike",
			mainImg:   "https://picsum.photos/seed/nike-am270-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/nike-am270-sole/600/600"},
			attrs: [][2]string{
				{"Подошва", "Air Max 270 (32мм) + BRS 1000"},
				{"Верх", "Инженерная сетка + накладки"},
				{"Тип", "Lifestyle / Повседневные"},
				{"Размерный ряд", "40–47"},
				{"Вес (US 10)", "309 г"},
			},
			price: 59990, origPrice: 74990, storeID: st2, stock: 30, delivery: 3,
			reviews: []seedReview{
				{b1, 5, "Ношу каждый день уже 4 месяца. Очень удобные, нога совсем не устаёт. Подошва воздушная — ощущение как идёшь по облаку. Советую!"},
				{b3, 4, "Хорошие кроссовки, но размер немного большемерит — брал 44, надо было брать 43. В остальном без нареканий."},
			},
		},
		{
			name:    "Adidas Ultraboost 22",
			desc:    "Adidas Ultraboost 22 с технологией BOOST — самый отзывчивый материал подошвы, возвращающий энергию при каждом шаге. Верх Primeknit+ плотно облегает стопу и адаптируется к движению. TORSION SYSTEM — жёсткая вставка для поддержки средней части стопы. Резиновая подошва STRETCHWEB для сцепления с поверхностью. Идеальны для бега и активного образа жизни.",
			catSlug: "sport-shoes", brand: "Adidas",
			mainImg:   "https://picsum.photos/seed/adidas-ub22-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/adidas-ub22-top/600/600"},
			attrs: [][2]string{
				{"Подошва", "BOOST + STRETCHWEB"},
				{"Верх", "Primeknit+"},
				{"Технология", "TORSION SYSTEM"},
				{"Тип", "Нейтральные беговые"},
				{"Дроп", "10 мм"},
				{"Размерный ряд", "38–48"},
			},
			price: 69990, origPrice: 89990, storeID: st2, stock: 22, delivery: 3,
			reviews: []seedReview{
				{b2, 5, "Бегаю в них каждое утро уже 3 месяца — больше 300км пробег. Амортизация BOOST не деградирует, всё так же пружинит. Лучшие кроссовки для бега!"},
				{b1, 4, "Отличные беговые, Primeknit хорошо дышит в жару. Единственное — цена кусается. Но качество соответствует."},
			},
		},
		// ── Clothing ────────────────────────────────────────────────────────────
		{
			name:    "Худи Nike Tech Fleece Full-Zip",
			desc:    "Худи из революционного материала Nike Tech Fleece с двухслойной конструкцией — лёгкое, тёплое, без лишнего объёма. Инновационная технология создаёт изолирующий воздушный карман между слоями. Полноразмерная молния YKK, два боковых кармана на молнии, регулируемый капюшон со шнурком. Плоские швы не натирают кожу.",
			catSlug: "mens-clothing", brand: "Nike",
			mainImg:   "https://picsum.photos/seed/nike-tech-fleece-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/nike-tech-fleece-detail/600/600"},
			attrs: [][2]string{
				{"Материал", "Nike Tech Fleece (66% хлопок, 34% полиэстер)"},
				{"Крой", "Облегающий (Slim Fit)"},
				{"Застёжка", "YKK Full-Zip"},
				{"Размерный ряд", "XS / S / M / L / XL / XXL"},
				{"Уход", "Стирка при 30°C, без отбеливателя"},
			},
			price: 39990, origPrice: 49990, storeID: st2, stock: 35, delivery: 3,
			reviews: []seedReview{
				{b3, 5, "Ношу уже год — не растянулась, не скаталась, не потеряла форму. Очень тёплая и при этом не жарко. Качество оправдывает цену. Рекомендую!"},
				{b2, 4, "Красивое худи, материал очень мягкий и приятный. Единственное — размер чуть меньше стандарта, советую брать на 1 больше обычного."},
			},
		},
		// ── Smart Watches ────────────────────────────────────────────────────────
		{
			name:    "Apple Watch Series 9 GPS 45mm",
			desc:    "Apple Watch Series 9 с новым двухъядерным чипом S9 SiP и функцией Double Tap — управление жестом двойного касания большого и указательного пальцев без прикосновения к экрану. Яркий Always-On дисплей Retina до 2000 нит — виден даже на ярком солнце. Мониторинг здоровья: ЧСС, ЭКГ, SpO2, температура тела. Crash Detection, Emergency SOS и звонки без iPhone. Водозащита WR50.",
			catSlug: "smart-watches", brand: "Apple",
			mainImg:   "https://picsum.photos/seed/apple-watch-s9-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/apple-watch-s9-band/600/600"},
			attrs: [][2]string{
				{"Корпус", "45мм алюминий (Midnight/Starlight/Pink)"},
				{"Дисплей", "Always-On Retina LTPO OLED, 2000 нит"},
				{"Чип", "Apple S9 SiP (двухъядерный)"},
				{"Датчики", "ЧСС, ЭКГ, SpO2, температура тела"},
				{"Автономность", "18 часов / 36ч режим экономии"},
				{"Влагозащита", "WR50 (50 метров)"},
			},
			price: 189990, origPrice: 219990, storeID: st1, stock: 20, delivery: 2,
			reviews: []seedReview{
				{b1, 5, "Слежу за здоровьем каждый день. ЭКГ очень полезно — врач оценил качество снимков. Double Tap невероятно удобен за рулём. Рекомендую!"},
				{b2, 5, "Купила вместо фитнес-браслета — разница огромная. Дисплей яркий на солнце, Always-On не разряжает батарею сильно. Отслеживание тренировок точное."},
			},
		},
		// ── Home Appliances ──────────────────────────────────────────────────────
		{
			name:    "Пылесос Dyson V15 Detect Absolute",
			desc:    "Беспроводной пылесос Dyson V15 Detect с лазерной подсветкой Laser Slim Fluffy — делает мелкую пыль видимой на любой поверхности. HEPA фильтрация в 5 этапов задерживает 99.99% частиц размером 0.3 микрона. Датчик частиц в реальном времени автоматически регулирует мощность. Дисплей LCD показывает оставшееся время работы. До 60 минут в режиме Eco.",
			catSlug: "vacuums", brand: "Dyson",
			mainImg:   "https://picsum.photos/seed/dyson-v15-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/dyson-v15-attach/600/600"},
			attrs: [][2]string{
				{"Тип", "Беспроводной вертикальный"},
				{"Мощность всасывания", "240 АВт"},
				{"Ёмкость пылесборника", "0.77 л"},
				{"Фильтрация", "5-ступенчатая HEPA (99.99%)"},
				{"Автономность", "До 60 мин (Eco) / 10 мин (MAX)"},
				{"Насадки в комплекте", "7 насадок + мягкий пол Laser"},
				{"Вес", "3.1 кг"},
			},
			price: 349990, origPrice: 399990, storeID: st3, stock: 10, delivery: 4,
			reviews: []seedReview{
				{b3, 5, "Лазерная подсветка — это не маркетинг! Реально видно пыль, которую никогда раньше не замечал на светлых полах. Убирает идеально, HEPA держит всё внутри."},
				{b2, 4, "Мощный и удобный пылесос. Единственный нюанс — дорогие сменные фильтры (раз в год). Но в целом очень доволен, рекомендую."},
			},
		},
		{
			name:    "Холодильник LG InstaView 419 л",
			desc:    "Холодильник LG с технологией Total No Frost и уникальным InstaView — постучите дважды по стеклянной панели и посмотрите содержимое без открытия двери, сохраняя холод. Линейный инверторный компрессор снижает расход электроэнергии на 32%. Smart Diagnosis через NFC — диагностика через приложение. Класс A++, 10 лет гарантии на компрессор.",
			catSlug: "refrigerators", brand: "LG",
			mainImg:   "https://picsum.photos/seed/lg-fridge-main/600/600",
			extraImgs: []string{"https://picsum.photos/seed/lg-fridge-open/600/600"},
			attrs: [][2]string{
				{"Объём", "419 л (303 л холодильник + 116 л морозильник)"},
				{"Технология", "Total No Frost + InstaView"},
				{"Класс энергопотребления", "A++"},
				{"Компрессор", "Линейный инверторный"},
				{"Размеры (В×Ш×Г)", "179×70×74 см"},
				{"Гарантия компрессора", "10 лет"},
			},
			price: 349990, origPrice: 389990, storeID: st3, stock: 8, delivery: 7,
			reviews: []seedReview{
				{b1, 5, "InstaView — очень удобная функция, дверь открываю в 2 раза реже. Тихий как мышь. Энергопотребление низкое. Доставили и подключили быстро. Рекомендую!"},
				{b3, 4, "Хороший холодильник с богатым функционалом. Total No Frost работает без нареканий. Занимает чуть больше места чем ожидал, но это мелочи."},
			},
		},
	}

	for _, p := range products {
		cid := catID(p.catSlug)
		if cid == 0 {
			log.Printf("seed: категория не найдена: %s", p.catSlug)
			continue
		}
		pid := dbInsert(
			"INSERT INTO products (name,description,category_id,brand,main_image) VALUES(?,?,?,?,?)",
			p.name, p.desc, cid, p.brand, p.mainImg,
		)
		if pid == 0 {
			continue
		}
		for i, img := range p.extraImgs {
			db.Exec("INSERT INTO product_images (product_id,image_url,sort_order) VALUES(?,?,?)", pid, img, i+1)
		}
		for _, attr := range p.attrs {
			db.Exec("INSERT INTO product_attributes (product_id,name,value) VALUES(?,?,?)", pid, attr[0], attr[1])
		}
		db.Exec(`INSERT INTO product_sellers (product_id,store_id,price,original_price,stock,delivery_days)
			VALUES(?,?,?,?,?,?)`, pid, p.storeID, p.price, p.origPrice, p.stock, p.delivery)

		// Second seller (competitor) for electronics
		switch p.catSlug {
		case "smartphones", "laptops", "headphones", "smart-watches":
			altStore := st3
			if p.storeID == st3 {
				altStore = st1
			}
			altPrice := p.price * 1.04 // 4% more expensive
			db.Exec(`INSERT IGNORE INTO product_sellers (product_id,store_id,price,original_price,stock,delivery_days)
				VALUES(?,?,?,?,?,?)`, pid, altStore, altPrice, 0, 4, p.delivery+2)
		}

		for _, r := range p.reviews {
			db.Exec("INSERT INTO reviews (product_id,user_id,rating,comment) VALUES(?,?,?,?)",
				pid, r.userID, r.rating, r.comment)
		}
	}

	// Rebuild all ratings from real reviews
	recalculateProductRatings()

	log.Println("✅ Демо-данные созданы!")
	log.Println("   Аккаунты (пароль: Demo1234):")
	log.Println("   admin@tumar.kz — Администратор")
	log.Println("   seller1@tumar.kz — ТехноМаркет (продавец)")
	log.Println("   seller2@tumar.kz — МодаKZ (продавец)")
	log.Println("   seller3@tumar.kz — ДомоМаркет (продавец)")
	log.Println("   buyer1@tumar.kz — Покупатель 1")
	log.Println("   buyer2@tumar.kz — Покупатель 2")
	log.Println("   buyer3@tumar.kz — Покупатель 3")
}
