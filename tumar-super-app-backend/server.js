// server.js (ПОЛНАЯ ВЕРСИЯ с переводами и историей)

// Загружаем переменные окружения из файла .env в корне проекта
require('dotenv').config();

// Подключаем необходимые модули
const express = require('express');
const mysql = require('mysql2/promise'); // Используем mysql2 с поддержкой промисов
const bcrypt = require('bcryptjs');     // Для хэширования и сравнения паролей
const cors = require('cors');           // Для разрешения кросс-доменных запросов от Android
const jwt = require('jsonwebtoken');    // Для создания и проверки JWT токенов

// Создаем экземпляр Express приложения
const app = express();
// Определяем порт: из переменных окружения или 3000 по умолчанию
const port = process.env.PORT || 3000;

// Секретный ключ для подписи JWT токенов
// ВАЖНО: Храните этот ключ безопасно! Лучше всего в переменной окружения (.env файле)
const JWT_SECRET = process.env.JWT_SECRET || '123'; // <<< ЗАМЕНИТЕ НА СВОЙ НАДЕЖНЫЙ КЛЮЧ В .env !!!
if (JWT_SECRET === 'your-default-super-secret-key-replace-me') {
    console.warn('!!! WARNING: Using default or weak JWT_SECRET. Please set a strong secret in your .env file! !!!');
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
            console.warn('Auth middleware: Invalid or expired token', err.message);
            return res.status(403).json({ success: false, message: 'Invalid or expired token' });
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
        const saltRounds = 10;
        const passwordHash = await bcrypt.hash(password, saltRounds);
        connection = await pool.getConnection();
        console.log("Database connection obtained for registration.");
        try {
            // await connection.beginTransaction();
            const userSql = 'INSERT INTO users (first_name, last_name, email, phone, age, password_hash) VALUES (?, ?, ?, ?, ?, ?)';
            const [userResult] = await connection.execute(userSql, [firstName, lastName, email, phone, age ? parseInt(age, 10) : null, passwordHash]);
            const newUserId = userResult.insertId;
            console.log('User inserted with ID:', newUserId);
            const balanceSql = 'INSERT INTO balances (user_id, balance, currency, updated_at) VALUES (?, ?, ?, NOW())';
            await connection.execute(balanceSql, [newUserId, 0.00, 'KZT']); // Валюта по умолчанию
            console.log('Initial balance created for user ID:', newUserId);
            // await connection.commit();
            res.status(201).json({ success: true, message: 'User registered successfully', userId: newUserId });
        } catch (insertError) {
            // if (connection) await connection.rollback();
            console.error('Error during user/balance insertion:', insertError);
            if (insertError.code === 'ER_DUP_ENTRY') {
                 return res.status(409).json({ success: false, message: 'Email or phone already exists' });
            }
            res.status(500).json({ success: false, message: 'Failed to save user data' });
        }
    } catch (error) {
        console.error('Registration process error (e.g., hashing):', error);
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
            console.log(`Login attempt failed: User not found for email ${email}`);
            return res.status(401).json({ success: false, message: 'Invalid email or password' });
        }
        const user = users[0];
        const isPasswordMatch = await bcrypt.compare(password, user.password_hash);
        if (!isPasswordMatch) {
            console.log(`Login attempt failed: Invalid password for email ${email}`);
            return res.status(401).json({ success: false, message: 'Invalid email or password' });
        }
        const payload = { userId: user.id, email: user.email };
        const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '1d' });
        console.log(`User ${user.email} (ID: ${user.id}) logged in successfully.`);
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
                t.id, t.sender_id, t.recipient_id, t.amount, t.currency, t.transaction_type, t.timestamp,
                sender.first_name AS sender_first_name,
                sender.last_name AS sender_last_name,
                sender.phone AS sender_phone,
                recipient.first_name AS recipient_first_name,
                recipient.last_name AS recipient_last_name,
                recipient.phone AS recipient_phone
            FROM transactions t
            LEFT JOIN users sender ON t.sender_id = sender.id
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