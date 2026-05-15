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

:: Проверяем mysql в PATH
where mysql >nul 2>&1
if %errorlevel% neq 0 (
    echo  [ПРЕДУПРЕЖДЕНИЕ] mysql.exe не найден в PATH.
    echo  Добавь папку bin MySQL в переменную PATH, например:
    echo    C:\Program Files\MySQL\MySQL Server 8.0\bin
    echo  Или установи таблицы вручную через phpMyAdmin / MySQL Workbench.
    echo.
    set SKIP_DB=1
) else (
    set SKIP_DB=0
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

:: Читаем DB_USER, DB_PASSWORD, DB_NAME из .env
set DB_USER=root
set DB_PASSWORD=
set DB_NAME=tumar_super_app_db

for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
    if "%%A"=="DB_USER"     set DB_USER=%%B
    if "%%A"=="DB_PASSWORD" set DB_PASSWORD=%%B
    if "%%A"=="DB_NAME"     set DB_NAME=%%B
)

:: Инициализируем БД если mysql доступен
if "%SKIP_DB%"=="0" (
    echo  [INFO] Проверяем базу данных...

    :: Путь к SQL-схеме (на уровень выше, в корне проекта)
    set SQL_FILE=..\tumar_super_app_db.sql

    if not exist "%SQL_FILE%" (
        echo  [ПРЕДУПРЕЖДЕНИЕ] Файл схемы не найден: %SQL_FILE%
        echo  Таблицы нужно создать вручную.
        echo.
        goto :start_server
    )

    :: Создаём БД и импортируем схему
    if "%DB_PASSWORD%"=="" (
        mysql -u %DB_USER% --connect-timeout=5 -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
        if %errorlevel% equ 0 (
            mysql -u %DB_USER% %DB_NAME% < "%SQL_FILE%" 2>nul
            echo  [OK] База данных инициализирована.
        ) else (
            echo  [ПРЕДУПРЕЖДЕНИЕ] Не удалось подключиться к MySQL. Проверь, что MySQL запущен.
        )
    ) else (
        mysql -u %DB_USER% -p%DB_PASSWORD% --connect-timeout=5 -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
        if %errorlevel% equ 0 (
            mysql -u %DB_USER% -p%DB_PASSWORD% %DB_NAME% < "%SQL_FILE%" 2>nul
            echo  [OK] База данных инициализирована.
        ) else (
            echo  [ПРЕДУПРЕЖДЕНИЕ] Не удалось подключиться к MySQL. Проверь, что MySQL запущен.
        )
    )
    echo.
)

:start_server
echo  [OK] Запускаем сервер...
echo  Остановить: Ctrl+C
echo.
node server.js

echo.
echo  Сервер остановлен.
pause
