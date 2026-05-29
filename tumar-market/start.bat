@echo off
chcp 65001 >nul
title Tumar Market — Сервер

:: Переходим в папку скрипта
cd /d "%~dp0"

set "PYTHON_CMD="

echo.
echo  ╔══════════════════════════════════════════╗
echo  ║         Tumar Market — Запуск            ║
echo  ║   Go + Python + HTML/CSS/JS + MySQL      ║
echo  ╚══════════════════════════════════════════╝
echo.

:: Проверяем Python. Сначала пробуем launcher py -3, чтобы не попасть в Windows Store alias "python".
where py >nul 2>&1
if %errorlevel% equ 0 (
    py -3 --version >nul 2>&1
    if %errorlevel% equ 0 set "PYTHON_CMD=py -3"
)

if not defined PYTHON_CMD (
    where python >nul 2>&1
    if %errorlevel% equ 0 (
        python --version >nul 2>&1
        if %errorlevel% equ 0 set "PYTHON_CMD=python"
    )
)

if not defined PYTHON_CMD (
    echo  [ОШИБКА] Рабочий Python 3 не найден.
    echo  Если вместо запуска появляется только "Python", отключи alias Microsoft Store:
    echo  Settings ^> Apps ^> Advanced app settings ^> App execution aliases ^> python.exe.
    echo  Затем установи Python 3: https://www.python.org/downloads/
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
%PYTHON_CMD% run.py %*
if %errorlevel% neq 0 (
    echo.
    echo  [ОШИБКА] run.py завершился с кодом %errorlevel%.
)

echo.
pause
