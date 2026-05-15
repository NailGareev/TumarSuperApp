@echo off
chcp 65001 >nul
title Tumar SuperApp — Backend Server

echo.
echo  ╔══════════════════════════════════════╗
echo  ║      Tumar SuperApp  Backend         ║
echo  ╚══════════════════════════════════════╝
echo.

:: Проверяем Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo  [ОШИБКА] Node.js не найден.
    echo  Скачай и установи: https://nodejs.org
    pause
    exit /b 1
)

:: Проверяем node_modules
if not exist "node_modules\" (
    echo  [INFO] Устанавливаем зависимости...
    npm install
    if %errorlevel% neq 0 (
        echo  [ОШИБКА] npm install завершился с ошибкой.
        pause
        exit /b 1
    )
    echo.
)

:: Проверяем .env
if not exist ".env" (
    echo  [ОШИБКА] Файл .env не найден рядом со скриптом.
    echo  Создай .env на основе примера ниже:
    echo.
    echo    DB_HOST=localhost
    echo    DB_USER=root
    echo    DB_PASSWORD=
    echo    DB_NAME=tumar_super_app_db
    echo    PORT=3000
    echo    JWT_SECRET=замени_на_случайный_ключ_64_символа
    echo.
    pause
    exit /b 1
)

echo  [OK] Запускаем сервер...
echo  Остановить: Ctrl+C
echo.
node server.js

echo.
echo  Сервер остановлен.
pause
