// server.js (ПОЛНАЯ ВЕРСИЯ с переводами и историей)

// Загружаем переменные окружения из файла .env в корне проекта
require('dotenv').config();

// Подключаем необходимые модули
const express = require('express');
const mysql = require('mysql2/promise'); // Используем mysql2 с поддержкой промисов
const bcrypt = require('bcryptjs');     // Для хэширования и сравнения паролей
const cors = require('cors');           // Для разрешения кросс-доменных запросов от Android
const jwt = require('jsonwebtoken');    // Для создания и проверки JWT токенов
const crypto = require('crypto');       // Для шифрования данных карты

// Создаем экземпляр Express приложения
const app = express();
// Определяем порт: из переменных окружения или 3000 по умолчанию
const port = process.env.PORT || 3000;

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET || JWT_SECRET.length < 32) {
    console.error('!!! FATAL: JWT_SECRET is missing or too short (< 32 chars). Set a strong secret in .env !!!');
    process.exit(1);
}

// --- AES-256-CBC encryption helpers for virtual card sensitive fields ---
const CARD_ENC_KEY = process.env.CARD_ENCRYPTION_KEY;
if (!CARD_ENC_KEY || CARD_ENC_KEY.length !== 64) {
    console.error('!!! FATAL: CARD_ENCRYPTION_KEY must be a 64-char hex string (32 bytes). Set it in .env !!!');
    process.exit(1);
}
const CARD_ENC_KEY_BUF = Buffer.from(CARD_ENC_KEY, 'hex');

function encryptField(plaintext) {
    const iv = crypto.randomBytes(16);
    const cipher = crypto.createCipheriv('aes-256-cbc', CARD_ENC_KEY_BUF, iv);
    let enc = cipher.update(plaintext, 'utf8', 'hex');
    enc += cipher.final('hex');
    return iv.toString('hex') + ':' + enc;
}

function decryptField(ciphertext) {
    const [ivHex, enc] = ciphertext.split(':');
    const iv = Buffer.from(ivHex, 'hex');
    const decipher = crypto.createDecipheriv('aes-256-cbc', CARD_ENC_KEY_BUF, iv);
    let dec = decipher.update(enc, 'hex', 'utf8');
    dec += decipher.final('utf8');
    return dec;
}

// --- Настройка Middleware ---
app.use(cors()); // Разрешаем запросы с других доменов (например, от Android приложения)
app.use(express.json()); // Позволяем Express разбирать тело запроса в формате JSON
app.use(express.urlencoded({ extended: true })); // Позволяем разбирать данные форм

// --- Настройка пула соединений к MySQL ---
const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost', // Хост БД
    user: process.env.DB_USER || 'root',      // Имя пользователя БД
    password: process.env.DB_PASSWORD || '',  // Пароль пользователя БД
    database: process.env.DB_NAME || 'tumar_super_app_db', // Имя БД
    waitForConnections: true,   // Ждать доступного соединения, если все заняты
    connectionLimit: 10,        // Максимальное количество соединений в пуле
    queueLimit: 0,              // Максимальное количество ожидающих запросов (0 - безлимитно)
    decimalNumbers: true        // ВАЖНО: Чтобы DECIMAL возвращался как number
});

// --- Middleware для проверки JWT токена ---
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    if (token == null) {
        console.warn('Auth middleware: Token missing');
        return res.status(401).json({ success: false, message: 'Authentication token required' });
    }
    jwt.verify(token, JWT_SECRET, (err, userPayload) => {
        if (err) {
            const isExpired = err.name === 'TokenExpiredError';
            console.warn(isExpired ? 'Auth middleware: Token expired - user must log in again' : `Auth middleware: Invalid token - ${err.message}`);
            return res.status(403).json({
                success: false,
                message: isExpired ? 'Session expired. Please log in again.' : 'Invalid or expired token',
                code: isExpired ? 'TOKEN_EXPIRED' : 'INVALID_TOKEN'
            });
        }
        req.user = userPayload;
        console.log('Auth middleware: Token verified for user ID:', req.user.userId);
        next();
    });
};

// --- Роуты API ---

// GET / - Проверка работы сервера
app.get('/', (req, res) => {
    res.send('Tumar Super App Backend API is running!');
});

// POST /api/register - Регистрация пользователя
app.post('/api/register', async (req, res) => {
    console.log("Received registration request:", req.body);
    const { firstName, lastName, email, phone, age, password } = req.body;
    if (!firstName || !lastName || !email || !phone || !password) {
        return res.status(400).json({ success: false, message: 'Missing required fields' });
    }
    let connection;
    try {
        const passwordHash = await bcrypt.hash(password, 10);
        connection = await pool.getConnection();
        await connection.beginTransaction();
        try {
            const userSql = 'INSERT INTO users (first_name, last_name, email, phone, age, password_hash) VALUES (?, ?, ?, ?, ?, ?)';
            const [userResult] = await connection.execute(userSql, [firstName, lastName, email, phone, age ? parseInt(age, 10) : null, passwordHash]);
            const newUserId = userResult.insertId;
            const balanceSql = 'INSERT INTO balances (user_id, balance, currency, updated_at) VALUES (?, ?, ?, NOW())';
            await connection.execute(balanceSql, [newUserId, 0.00, 'KZT']);
            await connection.commit();
            res.status(201).json({ success: true, message: 'User registered successfully', userId: newUserId });
        } catch (insertError) {
            await connection.rollback();
            console.error('Error during user/balance insertion:', insertError);
            if (insertError.code === 'ER_DUP_ENTRY') {
                return res.status(409).json({ success: false, message: 'Email or phone already exists' });
            }
            res.status(500).json({ success: false, message: 'Failed to save user data' });
        }
    } catch (error) {
        console.error('Registration process error:', error);
        res.status(500).json({ success: false, message: 'Internal server error during registration' });
    } finally {
        if (connection) connection.release();
    }
});

// POST /api/login - Вход пользователя
app.post('/api/login', async (req, res) => {
    console.log("Received login request:", req.body);
    const { email, password } = req.body;
    if (!email || !password) {
        return res.status(400).json({ success: false, message: 'Email and password are required' });
    }
    let connection;
    try {
        connection = await pool.getConnection();
        console.log("Database connection obtained for login.");
        const sql = 'SELECT id, email, password_hash FROM users WHERE email = ? LIMIT 1';
        const [users] = await connection.execute(sql, [email]);
        if (users.length === 0) {
            return res.status(401).json({ success: false, message: 'Invalid email or password' });
        }
        const user = users[0];
        const isPasswordMatch = await bcrypt.compare(password, user.password_hash);
        if (!isPasswordMatch) {
            return res.status(401).json({ success: false, message: 'Invalid email or password' });
        }
        const payload = { userId: user.id, email: user.email };
        const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '30d' });
        console.log(`User ID ${user.id} logged in successfully.`);
        res.status(200).json({ success: true, message: 'Login successful', token: token, userId: user.id });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ success: false, message: 'Internal server error during login' });
    } finally {
        if (connection) connection.release();
    }
});

// GET /api/profile - Получение данных профиля (защищено токеном)
app.get('/api/profile', authenticateToken, async (req, res) => {
    const userId = req.user.userId;
    console.log(`Received profile request for user ID: ${userId}`);
    if (!userId) { return res.status(403).json({ success: false, message: 'User ID not found in token' }); }
    let connection;
    try {
        connection = await pool.getConnection();
        const sql = `
            SELECT u.phone, b.balance, b.currency
            FROM users u
            LEFT JOIN balances b ON u.id = b.user_id
            WHERE u.id = ? LIMIT 1;`;
        const [results] = await connection.execute(sql, [userId]);
        if (results.length === 0) {
            console.warn(`Profile data not found for user ID: ${userId}`);
            return res.status(404).json({ success: false, message: 'User profile not found' });
        }
        const profileData = results[0];
        console.log(`Profile data retrieved for user ID ${userId}:`, profileData);
        res.status(200).json({
            success: true,
            phone: profileData.phone,
            balance: profileData.balance !== null ? profileData.balance : 0.00,
            currency: profileData.currency !== null ? profileData.currency : 'KZT'
        });
    } catch (error) {
        console.error(`Error fetching profile for user ID ${userId}:`, error);
        res.status(500).json({ success: false, message: 'Internal server error fetching profile' });
    } finally {
        if (connection) connection.release();
    }
});

// GET /api/lookup-phone - Поиск клиента Tumar по номеру телефона (защищено токеном)
app.get('/api/lookup-phone', authenticateToken, async (req, res) => {
    const { phone } = req.query;
    if (!phone || !phone.startsWith('+7') || phone.length !== 12) {
        return res.status(400).json({ success: false, message: 'Invalid phone format' });
    }
    let connection;
    try {
        connection = await pool.getConnection();
        const [rows] = await connection.execute(
            'SELECT first_name, last_name FROM users WHERE phone = ? LIMIT 1',
            [phone]
        );
        if (rows.length === 0) {
            return res.status(404).json({ success: false, message: 'User not found' });
        }
        const user = rows[0];
        res.status(200).json({
            success: true,
            firstName: user.first_name,
            lastNameInitial: user.last_name ? user.last_name.charAt(0).toUpperCase() + '.' : ''
        });
    } catch (error) {
        console.error('Error during phone lookup:', error);
        res.status(500).json({ success: false, message: 'Internal server error' });
    } finally {
        if (connection) connection.release();
    }
});

// === НАЧАЛО: Роут для перевода средств ===
app.post('/api/transfer', authenticateToken, async (req, res) => {
    const senderId = req.user.userId;
    const { recipientPhone, amount } = req.body;

    console.log(`Received transfer request from user ID: ${senderId} to phone: ${recipientPhone} for amount: ${amount}`);

    // Валидация
    let parsedAmount;
    try {
        parsedAmount = parseFloat(amount); // Пытаемся преобразовать в число
        if (isNaN(parsedAmount) || parsedAmount <= 0) {
            throw new Error('Invalid amount');
        }
    } catch (e) {
         console.log(`Transfer validation failed: Invalid amount value: ${amount}`);
        return res.status(400).json({ success: false, message: 'Invalid transfer amount' });
    }

    if (!recipientPhone || !recipientPhone.startsWith('+7') || recipientPhone.length !== 12) {
        console.log(`Transfer validation failed: Invalid recipient phone: ${recipientPhone}`);
        return res.status(400).json({ success: false, message: 'Invalid recipient phone number format (+7XXXXXXXXXX)' });
    }

    let connection;
    try {
        connection = await pool.getConnection();
        await connection.beginTransaction(); // Начинаем транзакцию
        console.log(`Transaction started for transfer from ${senderId}`);

        // 1. Находим получателя
        const [recipients] = await connection.execute('SELECT id FROM users WHERE phone = ? LIMIT 1', [recipientPhone]);
        if (recipients.length === 0) {
            await connection.rollback();
            console.log(`Transfer failed: Recipient not found for phone ${recipientPhone}`);
            return res.status(404).json({ success: false, message: 'Recipient user not found' });
        }
        const recipientId = recipients[0].id;

        // 2. Проверка на самоперевод
        if (senderId === recipientId) {
            await connection.rollback();
            console.log(`Transfer failed: Sender ${senderId} cannot transfer to self.`);
            return res.status(400).json({ success: false, message: 'Cannot transfer funds to yourself' });
        }

        // 3. Проверка баланса отправителя (блокируем строку)
        const [senderBalances] = await connection.execute('SELECT balance, currency FROM balances WHERE user_id = ? FOR UPDATE', [senderId]);
        if (senderBalances.length === 0 || senderBalances[0].balance < parsedAmount) {
            await connection.rollback();
            console.log(`Transfer failed: Insufficient funds for sender ${senderId}. Balance: ${senderBalances.length > 0 ? senderBalances[0].balance : 'N/A'}, Amount: ${parsedAmount}`);
            return res.status(400).json({ success: false, message: 'Insufficient funds' });
        }
        const senderBalance = senderBalances[0].balance;
        const senderCurrency = senderBalances[0].currency || 'KZT'; // Берем валюту отправителя

        // 4. Списываем средства у отправителя
        await connection.execute(
            'UPDATE balances SET balance = balance - ?, updated_at = NOW() WHERE user_id = ?',
            [parsedAmount, senderId]
        );
        console.log(`Debited ${parsedAmount} from sender ${senderId}. New potential balance: ${senderBalance - parsedAmount}`);

        // 5. Зачисляем средства получателю
        // Убедимся, что у получателя есть запись в balances (на случай, если она не создалась при регистрации)
        // Можно добавить ON DUPLICATE KEY UPDATE или отдельную проверку/вставку, если необходимо
        const [recipientBalanceResult] = await connection.execute(
             'UPDATE balances SET balance = balance + ?, updated_at = NOW() WHERE user_id = ?',
            [parsedAmount, recipientId]
        );
         // Если ни одна строка не была обновлена (например, нет записи для получателя), откатываем
         if (recipientBalanceResult.affectedRows === 0) {
             await connection.rollback();
             console.error(`Transfer failed: Could not update balance for recipient ID ${recipientId}. Balance record might be missing.`);
             // Можно попытаться создать запись баланса здесь, но это усложнит логику
             return res.status(500).json({ success: false, message: 'Failed to update recipient balance.' });
         }
        console.log(`Credited ${parsedAmount} to recipient ${recipientId}`);

        // 6. Записываем транзакцию в историю
        const transactionSql = 'INSERT INTO transactions (sender_id, recipient_id, amount, currency, transaction_type) VALUES (?, ?, ?, ?, ?)';
        await connection.execute(transactionSql, [senderId, recipientId, parsedAmount, senderCurrency, 'TRANSFER']);
        console.log(`Transaction recorded: ${senderId} -> ${recipientId}, Amount: ${parsedAmount} ${senderCurrency}`);

        // 7. Коммитим транзакцию
        await connection.commit();
        console.log(`Transaction committed for transfer from ${senderId}`);

        res.status(200).json({ success: true, message: 'Transfer successful' });

    } catch (error) {
        if (connection) {
            await connection.rollback();
            console.log("Transaction rolled back due to error during transfer.");
        }
        console.error(`Transfer error for sender ${senderId}:`, error);
        res.status(500).json({ success: false, message: 'Internal server error during transfer' });
    } finally {
        if (connection) connection.release();
    }
});
// === КОНЕЦ: Роут для перевода средств ===


// === НАЧАЛО: Роут для получения истории транзакций ===
app.get('/api/transactions', authenticateToken, async (req, res) => {
    const userId = req.user.userId;
    console.log(`Received transaction history request for user ID: ${userId}`);

    let connection;
    try {
        connection = await pool.getConnection();
        const sql = `
            SELECT
                t.id, t.sender_id, t.recipient_id, t.amount, t.currency,
                t.transaction_type, t.description, t.timestamp,
                sender.first_name    AS sender_first_name,
                sender.last_name     AS sender_last_name,
                sender.phone         AS sender_phone,
                recipient.first_name AS recipient_first_name,
                recipient.last_name  AS recipient_last_name,
                recipient.phone      AS recipient_phone
            FROM transactions t
            LEFT JOIN users sender    ON t.sender_id    = sender.id
            LEFT JOIN users recipient ON t.recipient_id = recipient.id
            WHERE t.sender_id = ? OR t.recipient_id = ?
            ORDER BY t.timestamp DESC;
        `;
        const [transactions] = await connection.execute(sql, [userId, userId]);

        console.log(`Found ${transactions.length} transactions for user ID: ${userId}`);
        res.status(200).json({ success: true, transactions: transactions });

    } catch (error) {
        console.error(`Error fetching transaction history for user ID ${userId}:`, error);
        res.status(500).json({ success: false, message: 'Internal server error fetching transactions' });
    } finally {
        if (connection) connection.release();
    }
});
// === КОНЕЦ: Роут для получения истории транзакций ===


// === POST /api/pay - Оплата услуг (мобильная связь, ЖКХ, интернет и др.) ===
app.post('/api/pay', authenticateToken, async (req, res) => {
    const userId = req.user.userId;
    const { service, accountNumber, amount } = req.body;

    if (!service || !accountNumber) {
        return res.status(400).json({ success: false, message: 'Service and account number are required' });
    }

    let parsedAmount;
    try {
        parsedAmount = parseFloat(amount);
        if (isNaN(parsedAmount) || parsedAmount < 1 || parsedAmount > 500000) throw new Error();
    } catch (e) {
        return res.status(400).json({ success: false, message: 'Invalid amount (1–500,000 KZT)' });
    }

    let connection;
    try {
        connection = await pool.getConnection();
        await connection.beginTransaction();

        const [balances] = await connection.execute(
            'SELECT balance, currency FROM balances WHERE user_id = ? FOR UPDATE',
            [userId]
        );

        if (balances.length === 0 || balances[0].balance < parsedAmount) {
            await connection.rollback();
            return res.status(400).json({ success: false, message: 'Insufficient funds' });
        }

        await connection.execute(
            'UPDATE balances SET balance = balance - ?, updated_at = NOW() WHERE user_id = ?',
            [parsedAmount, userId]
        );

        const description = `${service}: ${accountNumber}`;
        await connection.execute(
            'INSERT INTO transactions (sender_id, recipient_id, amount, currency, transaction_type, description) VALUES (?, NULL, ?, ?, ?, ?)',
            [userId, parsedAmount, balances[0].currency || 'KZT', 'PAYMENT', description]
        );

        await connection.commit();

        const [rows] = await connection.execute('SELECT balance FROM balances WHERE user_id = ?', [userId]);
        res.status(200).json({ success: true, message: 'Payment successful', newBalance: rows[0].balance });

    } catch (error) {
        if (connection) await connection.rollback();
        console.error(`Payment error for user ID ${userId}:`, error);
        res.status(500).json({ success: false, message: 'Internal server error during payment' });
    } finally {
        if (connection) connection.release();
    }
});

// === POST /api/topup - Пополнение баланса ===
app.post('/api/topup', authenticateToken, async (req, res) => {
    const userId = req.user.userId;
    const { amount } = req.body;

    let parsedAmount;
    try {
        parsedAmount = parseFloat(amount);
        if (isNaN(parsedAmount) || parsedAmount < 1 || parsedAmount > 1000000) {
            throw new Error('Invalid amount');
        }
    } catch (e) {
        return res.status(400).json({ success: false, message: 'Invalid top-up amount (must be 1 – 1,000,000)' });
    }

    let connection;
    try {
        connection = await pool.getConnection();
        await connection.beginTransaction();

        const [result] = await connection.execute(
            'UPDATE balances SET balance = balance + ?, updated_at = NOW() WHERE user_id = ?',
            [parsedAmount, userId]
        );

        if (result.affectedRows === 0) {
            await connection.rollback();
            return res.status(404).json({ success: false, message: 'Balance record not found' });
        }

        const [rows] = await connection.execute(
            'SELECT balance FROM balances WHERE user_id = ?',
            [userId]
        );

        await connection.commit();
        res.status(200).json({ success: true, message: 'Top-up successful', newBalance: rows[0].balance });
    } catch (error) {
        if (connection) await connection.rollback();
        console.error(`Top-up error for user ID ${userId}:`, error);
        res.status(500).json({ success: false, message: 'Internal server error during top-up' });
    } finally {
        if (connection) connection.release();
    }
});

// Helper: ensure cards table exists with encrypted columns (idempotent migration)
async function ensureCardsTable(connection) {
    await connection.execute(`
        CREATE TABLE IF NOT EXISTS cards (
            id              INT AUTO_INCREMENT PRIMARY KEY,
            user_id         INT NOT NULL UNIQUE,
            card_number     VARCHAR(16)  NOT NULL,
            cvv_encrypted   VARCHAR(255) NOT NULL,
            expiry_encrypted VARCHAR(255) NOT NULL,
            created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    `);
    // Migration: add columns that may be missing in older schema
    for (const col of [
        "ADD COLUMN IF NOT EXISTS cvv_encrypted VARCHAR(255) NOT NULL DEFAULT ''",
        "ADD COLUMN IF NOT EXISTS expiry_encrypted VARCHAR(255) NOT NULL DEFAULT ''"
    ]) {
        try { await connection.execute(`ALTER TABLE cards ${col}`); } catch (e) { /* already exists */ }
    }
}

// === GET /api/card - Получить виртуальную карту пользователя ===
app.get('/api/card', authenticateToken, async (req, res) => {
    const userId = req.user.userId;
    let connection;
    try {
        connection = await pool.getConnection();
        await ensureCardsTable(connection);
        const [rows] = await connection.execute(
            'SELECT card_number, cvv_encrypted, expiry_encrypted FROM cards WHERE user_id = ?',
            [userId]
        );
        if (rows.length === 0) {
            return res.json({ success: true, card: null });
        }
        const c = rows[0];
        const expiry = decryptField(c.expiry_encrypted);
        const cvv    = decryptField(c.cvv_encrypted);
        res.json({ success: true, card: { cardNumber: c.card_number, expiry, cvv } });
    } catch (err) {
        console.error('Get card error:', err);
        res.status(500).json({ success: false, message: 'Internal server error' });
    } finally {
        if (connection) connection.release();
    }
});

// === POST /api/card/issue - Выпустить виртуальную карту ===
app.post('/api/card/issue', authenticateToken, async (req, res) => {
    const userId = req.user.userId;
    let connection;
    try {
        connection = await pool.getConnection();
        await ensureCardsTable(connection);

        const [existing] = await connection.execute(
            'SELECT card_number, cvv_encrypted, expiry_encrypted FROM cards WHERE user_id = ?', [userId]
        );
        if (existing.length > 0) {
            const c = existing[0];
            return res.json({
                success: true,
                card: {
                    cardNumber: c.card_number,
                    expiry: decryptField(c.expiry_encrypted),
                    cvv:    decryptField(c.cvv_encrypted)
                }
            });
        }

        // Generate card: 4279 + 12 random digits
        const suffix = Math.floor(Math.random() * 1e12).toString().padStart(12, '0');
        const cardNumber = '4279' + suffix;

        // Generate CVV: 3 random digits
        const cvv = String(Math.floor(Math.random() * 900) + 100);

        // Expiry: 5 years from now
        const now = new Date();
        const expiryMonth = String(now.getMonth() + 1).padStart(2, '0');
        const expiryYear  = String(now.getFullYear() + 5).slice(-2);
        const expiry = `${expiryMonth}/${expiryYear}`;

        const cvvEncrypted    = encryptField(cvv);
        const expiryEncrypted = encryptField(expiry);

        await connection.execute(
            'INSERT INTO cards (user_id, card_number, cvv_encrypted, expiry_encrypted) VALUES (?, ?, ?, ?)',
            [userId, cardNumber, cvvEncrypted, expiryEncrypted]
        );

        res.json({ success: true, card: { cardNumber, expiry, cvv } });
    } catch (err) {
        console.error('Issue card error:', err);
        res.status(500).json({ success: false, message: 'Internal server error' });
    } finally {
        if (connection) connection.release();
    }
});

// Helper: ensure market_purchases table exists
async function ensureMarketPurchasesTable(connection) {
    await connection.execute(`
        CREATE TABLE IF NOT EXISTS market_purchases (
            id         INT           AUTO_INCREMENT PRIMARY KEY,
            user_id    INT           NOT NULL,
            order_ref  VARCHAR(50)   NOT NULL,
            amount     DECIMAL(10,2) NOT NULL,
            items_json TEXT          NOT NULL,
            address    VARCHAR(500)  NOT NULL,
            status     ENUM('processing','shipping','delivered','cancelled') DEFAULT 'shipping',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id)
        )
    `);
}

// === POST /api/market/pay - Оплата покупки в Tumar Market через баланс ===
app.post('/api/market/pay', authenticateToken, async (req, res) => {
    const userId = req.user.userId;
    const { amount, address, items } = req.body;

    const parsedAmount = parseFloat(amount);
    if (!parsedAmount || parsedAmount <= 0) {
        return res.status(400).json({ success: false, message: 'Некорректная сумма' });
    }
    if (!address || !address.trim()) {
        return res.status(400).json({ success: false, message: 'Адрес доставки обязателен' });
    }

    let connection;
    try {
        connection = await pool.getConnection();
        await connection.beginTransaction();

        const [balRows] = await connection.execute(
            'SELECT balance, currency FROM balances WHERE user_id = ? FOR UPDATE', [userId]);
        if (!balRows.length || balRows[0].balance < parsedAmount) {
            await connection.rollback();
            return res.status(400).json({ success: false, message: 'Недостаточно средств на счёте Tumar' });
        }

        await connection.execute(
            'UPDATE balances SET balance = balance - ?, updated_at = NOW() WHERE user_id = ?',
            [parsedAmount, userId]);

        await connection.execute(
            'INSERT INTO transactions (sender_id, recipient_id, amount, currency, transaction_type, description) VALUES (?, NULL, ?, ?, ?, ?)',
            [userId, parsedAmount, balRows[0].currency || 'KZT', 'PAYMENT', 'Покупка в Tumar Market']);

        await ensureMarketPurchasesTable(connection);
        const orderRef = 'TM' + Date.now().toString().slice(-8);
        await connection.execute(
            'INSERT INTO market_purchases (user_id, order_ref, amount, items_json, address) VALUES (?, ?, ?, ?, ?)',
            [userId, orderRef, parsedAmount, items || '[]', address.trim()]);

        await connection.commit();
        res.json({ success: true, orderRef });
    } catch (err) {
        if (connection) await connection.rollback();
        console.error('Market pay error:', err);
        res.status(500).json({ success: false, message: 'Ошибка при оплате' });
    } finally {
        if (connection) connection.release();
    }
});

// === GET /api/market/orders - История покупок в Tumar Market ===
app.get('/api/market/orders', authenticateToken, async (req, res) => {
    const userId = req.user.userId;
    let connection;
    try {
        connection = await pool.getConnection();
        await ensureMarketPurchasesTable(connection);
        const [rows] = await connection.execute(
            `SELECT id, order_ref, amount, items_json, address, status, created_at
             FROM market_purchases WHERE user_id = ? ORDER BY created_at DESC`,
            [userId]);
        res.json({ success: true, orders: rows });
    } catch (err) {
        console.error('Market orders error:', err);
        res.status(500).json({ success: false, message: 'Ошибка загрузки заказов' });
    } finally {
        if (connection) connection.release();
    }
});

// === POST /api/market/cancel - Отмена покупки в Tumar Market и возврат средств ===
app.post('/api/market/cancel', authenticateToken, async (req, res) => {
    const userId = req.user.userId;
    const { order_ref } = req.body;

    if (!order_ref) {
        return res.status(400).json({ success: false, message: 'Укажите order_ref' });
    }

    let connection;
    try {
        connection = await pool.getConnection();
        await connection.beginTransaction();

        await ensureMarketPurchasesTable(connection);
        const [rows] = await connection.execute(
            'SELECT id, amount, status FROM market_purchases WHERE user_id = ? AND order_ref = ? FOR UPDATE',
            [userId, order_ref]);

        if (!rows.length) {
            await connection.rollback();
            return res.status(404).json({ success: false, message: 'Заказ не найден' });
        }
        if (rows[0].status === 'cancelled') {
            await connection.rollback();
            return res.status(400).json({ success: false, message: 'Заказ уже отменён' });
        }

        const amount = parseFloat(rows[0].amount);

        await connection.execute(
            'UPDATE market_purchases SET status = "cancelled" WHERE id = ?', [rows[0].id]);

        const [balRows] = await connection.execute(
            'SELECT currency FROM balances WHERE user_id = ?', [userId]);
        const currency = balRows.length ? (balRows[0].currency || 'KZT') : 'KZT';

        await connection.execute(
            'UPDATE balances SET balance = balance + ?, updated_at = NOW() WHERE user_id = ?',
            [amount, userId]);

        await connection.execute(
            'INSERT INTO transactions (sender_id, recipient_id, amount, currency, transaction_type, description) VALUES (NULL, ?, ?, ?, ?, ?)',
            [userId, amount, currency, 'MARKET_REFUND', 'Возврат — Tumar Market']);

        await connection.commit();
        res.json({ success: true, refunded: amount });
    } catch (err) {
        if (connection) await connection.rollback();
        console.error('Market cancel error:', err);
        res.status(500).json({ success: false, message: 'Ошибка при отмене' });
    } finally {
        if (connection) connection.release();
    }
});

// GET /api/tours - Список активных туров (публичный)
app.get('/api/tours', async (req, res) => {
    let connection;
    try {
        connection = await pool.getConnection();
        const [rows] = await connection.execute(
            `SELECT id, location, hotel_name, stars, price, months,
                    discount_percent, original_price, image_url, is_hot
             FROM tours
             WHERE is_active = 1
             ORDER BY created_at DESC`
        );
        res.json({ success: true, tours: rows });
    } catch (err) {
        console.error('Get tours error:', err);
        res.status(500).json({ success: false, message: 'Internal server error' });
    } finally {
        if (connection) connection.release();
    }
});

// GET /api/tours/search - Поиск туров по направлению и параметрам (публичный)
app.get('/api/tours/search', async (req, res) => {
    let connection;
    try {
        connection = await pool.getConnection();
        const { destination, adults, children } = req.query;

        let query = `SELECT id, location, hotel_name, stars, price, months,
                            discount_percent, original_price, image_url, is_hot
                     FROM tours
                     WHERE is_active = 1`;
        const params = [];

        if (destination && destination.trim()) {
            query += ` AND location LIKE ?`;
            params.push(`%${destination.trim()}%`);
        }

        query += ` ORDER BY created_at DESC`;

        const [rows] = await connection.execute(query, params);
        res.json({ success: true, tours: rows });
    } catch (err) {
        console.error('Search tours error:', err);
        res.status(500).json({ success: false, message: 'Internal server error' });
    } finally {
        if (connection) connection.release();
    }
});

// --- Запуск сервера ---
app.listen(port, async () => {
    try {
        const connection = await pool.getConnection();
        console.log('Successfully connected to the database.');
        connection.release();
        console.log(`Server listening at http://localhost:${port}`);
    } catch (err) {
        console.error('!!! FAILED TO CONNECT TO DATABASE on startup: !!!', err);
    }
});

// --- Обработчики необработанных ошибок ---
process.on('uncaughtException', (error) => {
  console.error('UNCAUGHT EXCEPTION:', error);
  // process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('UNHANDLED REJECTION at:', promise, 'reason:', reason);
  // process.exit(1);
});