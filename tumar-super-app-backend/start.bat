@echo off
chcp 65001 >nul
title Tumar SuperApp — Backend Server

:: Переходим в папку, где лежит сам bat-файл (ОБЯЗАТЕЛЬНО первой строкой)
cd /d "%~dp0"

echo.
echo  ╔══════════════════════════════════════╗
echo  ║      Tumar SuperApp  Backend         ║
echo  ╚══════════════════════════════════════╝
echo.

:: ── Проверяем Node.js ────────────────────────────────────────────────────────
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo  [ОШИБКА] Node.js не найден.
    echo  Скачай и установи: https://nodejs.org
    pause
    exit /b 1
)

:: ── Устанавливаем npm-зависимости если нужно ─────────────────────────────────
if not exist "node_modules\" (
    echo  [INFO] Устанавливаем npm-зависимости...
    npm install
    if %errorlevel% neq 0 (
        echo  [ОШИБКА] npm install завершился с ошибкой.
        pause
        exit /b 1
    )
    echo.
)

:: ── Проверяем .env ───────────────────────────────────────────────────────────
if not exist ".env" (
    echo  [ОШИБКА] Файл .env не найден.
    echo  Создай .env рядом со скриптом со следующим содержимым:
    echo.
    echo    DB_HOST=localhost
    echo    DB_USER=root
    echo    DB_PASSWORD=
    echo    DB_NAME=tumar_super_app_db
    echo    PORT=3000
    echo    JWT_SECRET=замени_на_случайный_ключ
    echo    CARD_ENCRYPTION_KEY=замени_на_64_символа_hex
    echo.
    pause
    exit /b 1
)

:: ── Инициализируем БД через Python setup_db.py ──────────────────────────────
where python >nul 2>&1
if %errorlevel% equ 0 (
    echo  [INFO] Проверяем и создаём таблицы в БД...
    python -c "import mysql.connector, dotenv" >nul 2>&1
    if %errorlevel% neq 0 (
        echo  [INFO] Устанавливаем Python-зависимости для setup_db...
        pip install mysql-connector-python python-dotenv >nul 2>&1
    )
    python setup_db.py
    if %errorlevel% neq 0 (
        echo  [ПРЕДУПРЕЖДЕНИЕ] setup_db.py завершился с ошибкой.
        echo  Проверь, что MySQL запущен и настройки в .env верны.
        echo  Сервер будет запущен, но БД может быть не инициализирована.
    )
    echo.
) else (
    echo  [ПРЕДУПРЕЖДЕНИЕ] Python не найден — пропускаем автоматическую инициализацию БД.
    echo  Запусти вручную: python setup_db.py
    echo.
)

:: ── Запускаем сервер ─────────────────────────────────────────────────────────
echo  [OK] Запускаем сервер...
echo  Остановить: Ctrl+C
echo.
node server.js

echo.
echo  Сервер остановлен.
pause
