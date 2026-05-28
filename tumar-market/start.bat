@echo off
chcp 65001 >nul
title Tumar Market — Сервер

:: Переходим в папку скрипта
cd /d "%~dp0"

echo.
echo  ╔══════════════════════════════════════════╗
echo  ║         Tumar Market — Запуск            ║
echo  ║   Go + Python + HTML/CSS/JS + MySQL      ║
echo  ╚══════════════════════════════════════════╝
echo.

:: Проверяем Python
where python >nul 2>&1
if %errorlevel% neq 0 (
    echo  [ОШИБКА] Python не найден.
    echo  Скачай: https://www.python.org/downloads/
    pause
    exit /b 1
)

:: Проверяем Go
where go >nul 2>&1
if %errorlevel% neq 0 (
    echo  [ОШИБКА] Go не найден.
    echo  Скачай: https://go.dev/dl/
    pause
    exit /b 1
)

:: Запускаем через run.py (установит зависимости, создаст БД, соберёт и запустит)
echo  [INFO] Запускаем через run.py...
echo.
python run.py %*

echo.
pause
