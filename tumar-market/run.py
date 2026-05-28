#!/usr/bin/env python3
"""
Tumar Market — единая точка запуска.
Использование: python3 run.py
Настройки БД: bd_settings.txt в корне проекта
"""

import os
import sys
import subprocess
import platform
import argparse
import shutil

BANNER = r"""
╔══════════════════════════════════════════╗
║         Tumar Market — Запуск            ║
║   Go + Python + HTML/CSS/JS + MySQL      ║
╚══════════════════════════════════════════╝
"""

ROOT_DIR    = os.path.dirname(os.path.abspath(__file__))
GO_DIR      = os.path.join(ROOT_DIR, "go")
PYTHON_DIR  = os.path.join(ROOT_DIR, "python")
SETTINGS_FILE = os.path.join(ROOT_DIR, "bd_settings.txt")
BINARY_NAME = "tumar-market-server" + (".exe" if platform.system() == "Windows" else "")
BINARY_PATH = os.path.join(ROOT_DIR, BINARY_NAME)

DEFAULTS = {
    "login":    "root",
    "password": "",
    "database": "tumar_market",
    "host":     "localhost",
    "port":     "3306",
}


# ── Утилиты ────────────────────────────────────────────────────

def info(msg):  print(f"  \033[32m✓\033[0m {msg}")
def warn(msg):  print(f"  \033[33m⚠\033[0m {msg}")
def error(msg): print(f"  \033[31m✗\033[0m {msg}")
def step(msg):  print(f"\n\033[1m▶ {msg}\033[0m")


def run(cmd, cwd=None, capture=False, env=None):
    merged_env = {**os.environ, **(env or {})}
    return subprocess.run(
        cmd, cwd=cwd, env=merged_env,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
    )


def check_command(name):
    return shutil.which(name) is not None


# ── Чтение bd_settings.txt ─────────────────────────────────────

def read_bd_settings():
    """Читает bd_settings.txt и возвращает dict с параметрами."""
    settings = dict(DEFAULTS)

    if not os.path.exists(SETTINGS_FILE):
        warn(f"Файл {SETTINGS_FILE} не найден — создаю с настройками по умолчанию")
        _create_default_settings()

    with open(SETTINGS_FILE, encoding="utf-8") as f:
        for raw_line in f:
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" not in line:
                continue
            key, _, value = line.partition("=")
            key = key.strip().lower()
            value = value.strip()
            if key in settings:
                settings[key] = value

    return settings


def _create_default_settings():
    with open(SETTINGS_FILE, "w", encoding="utf-8") as f:
        f.write("# Настройки подключения к базе данных MySQL\n")
        f.write("# Редактируйте этот файл перед запуском проекта\n\n")
        f.write("login=root\n")
        f.write("password=\n")
        f.write("database=tumar_market\n\n")
        f.write("# Дополнительные параметры (можно не менять)\n")
        f.write("host=localhost\n")
        f.write("port=3306\n")
    info(f"Создан файл настроек: {SETTINGS_FILE}")


def settings_to_env(s):
    """Переводит ключи из bd_settings.txt в переменные окружения для Go и Python."""
    return {
        "DB_HOST":     s["host"],
        "DB_PORT":     s["port"],
        "DB_USER":     s["login"],
        "DB_PASSWORD": s["password"],
        "DB_NAME":     s["database"],
        "JWT_SECRET":  "tumar-market-secret-2024",
    }


# ── Шаг 1: Проверка Go ─────────────────────────────────────────

def check_go():
    step("Проверка Go")
    if not check_command("go"):
        error("Go не установлен! Установите Go 1.21+ с https://go.dev/dl/")
        sys.exit(1)
    r = run(["go", "version"], capture=True)
    info(r.stdout.decode().strip())


# ── Шаг 2: Python-зависимости ──────────────────────────────────

def install_python_deps():
    step("Установка Python-зависимостей")
    req = os.path.join(ROOT_DIR, "requirements.txt")
    r = run([sys.executable, "-m", "pip", "install", "-r", req, "-q"])
    if r.returncode == 0:
        info("Зависимости установлены")
    else:
        warn("Не удалось установить зависимости — продолжаем без них")


# ── Шаг 3: База данных ─────────────────────────────────────────

def setup_database(env_cfg):
    step("Настройка базы данных MySQL")
    try:
        import mysql.connector
    except ImportError:
        warn("mysql-connector-python не установлен — БД будет создана Go-сервером")
        return

    host, port, user, password, dbname = (
        env_cfg["DB_HOST"], int(env_cfg["DB_PORT"]),
        env_cfg["DB_USER"], env_cfg["DB_PASSWORD"], env_cfg["DB_NAME"],
    )

    try:
        conn = mysql.connector.connect(host=host, port=port, user=user, password=password)
        cur = conn.cursor()
        cur.execute(
            f"CREATE DATABASE IF NOT EXISTS `{dbname}` "
            "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
        )
        conn.commit(); cur.close(); conn.close()
        info(f"База данных `{dbname}` готова")
    except Exception as e:
        warn(f"Не удалось подключиться к MySQL: {e}")
        warn("Убедитесь, что MySQL запущен. Go-сервер попробует создать БД сам.")
        return

    r = run([sys.executable, os.path.join(PYTHON_DIR, "setup_db.py")],
            env=env_cfg, cwd=PYTHON_DIR)
    if r.returncode == 0:
        info("Таблицы созданы")
    else:
        warn("setup_db.py завершился с ошибкой (таблицы могут уже существовать)")

    try:
        conn = mysql.connector.connect(
            host=host, port=port, user=user, password=password, database=dbname)
        cur = conn.cursor()
        cur.execute("SELECT COUNT(*) FROM products")
        count = cur.fetchone()[0]
        cur.close(); conn.close()

        # Check how many products still have external image URLs
        try:
            conn2 = mysql.connector.connect(
                host=host, port=port, user=user, password=password, database=dbname)
            cur2 = conn2.cursor()
            cur2.execute("SELECT COUNT(*) FROM products WHERE main_image LIKE 'http%'")
            bad_img_count = cur2.fetchone()[0]
            cur2.close(); conn2.close()
        except Exception:
            bad_img_count = 0

        if count == 0 or bad_img_count > 0:
            if count == 0:
                info("Загрузка демо-данных...")
            else:
                info(f"Найдено {bad_img_count} товаров с внешними ссылками на фото — исправляю...")
            r2 = run([sys.executable, os.path.join(PYTHON_DIR, "seed_data.py")],
                     env=env_cfg, cwd=PYTHON_DIR)
            if r2.returncode == 0:
                info("Демо-данные загружены / обновлены")
            else:
                warn("seed_data.py завершился с ошибкой")
        else:
            info(f"В базе {count} товаров, фото в порядке — seed пропущен")
    except Exception as e:
        warn(f"Не удалось проверить данные: {e}")


# ── Шаг 4: Сборка Go-сервера ───────────────────────────────────

def build_go():
    step("Сборка Go-сервера")
    r = run(["go", "mod", "tidy"], cwd=GO_DIR)
    if r.returncode != 0:
        error("go mod tidy завершился с ошибкой")
        sys.exit(1)
    info("go mod tidy...")

    r = run(["go", "build", "-o", BINARY_PATH, "."], cwd=GO_DIR)
    if r.returncode != 0:
        error("Сборка Go завершилась с ошибкой")
        sys.exit(1)
    info(f"Бинарный файл создан: {BINARY_NAME}")


# ── Шаг 5: Запуск сервера ──────────────────────────────────────

def run_server(env_cfg, port):
    step("Запуск сервера")
    print(f"""
  ┌─────────────────────────────────────────┐
  │  Tumar Market запущен!                  │
  │                                         │
  │  Открой в браузере:                     │
  │  http://localhost:{port:<25}│
  │                                         │
  │  Демо аккаунты:                         │
  │  • user@tumar.kz     / user123          │
  │  • seller@tumar.kz   / seller123        │
  │  • admin@tumar.kz    / admin123         │
  │                                         │
  │  Нажми Ctrl+C для остановки             │
  └─────────────────────────────────────────┘
""")
    env = {**os.environ, **env_cfg, "GIN_MODE": "release", "PORT": str(port)}
    try:
        proc = subprocess.Popen([BINARY_PATH], env=env, cwd=ROOT_DIR)
        proc.wait()
    except KeyboardInterrupt:
        print("\n\n  Сервер остановлен.")
        proc.terminate()
    except FileNotFoundError:
        error(f"Бинарный файл не найден: {BINARY_PATH}")
        sys.exit(1)


# ── CLI аргументы ──────────────────────────────────────────────

def parse_args():
    parser = argparse.ArgumentParser(
        description="Tumar Market — запуск проекта",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Настройки БД берутся из файла bd_settings.txt в корне проекта.

Примеры:
  python3 run.py
  python3 run.py --port 3000
  python3 run.py --skip-seed
  python3 run.py --build-only
        """,
    )
    parser.add_argument("--port",       default="8080", metavar="PORT",
                        help="Порт веб-сервера (default: 8080)")
    parser.add_argument("--skip-seed",  action="store_true",
                        help="Не загружать демо-данные")
    parser.add_argument("--build-only", action="store_true",
                        help="Только собрать бинарный файл, не запускать")
    return parser.parse_args()


# ── Главная функция ────────────────────────────────────────────

def main():
    print(BANNER)
    args = parse_args()

    step("Чтение настроек БД из bd_settings.txt")
    bd = read_bd_settings()
    env_cfg = settings_to_env(bd)

    print(f"  Файл настроек : {SETTINGS_FILE}")
    print(f"  Подключение   : {bd['login']}@{bd['host']}:{bd['port']}/{bd['database']}")
    print(f"  Порт сервера  : {args.port}")

    check_go()
    install_python_deps()

    if not args.skip_seed:
        setup_database(env_cfg)
    else:
        step("Seed данные пропущены (--skip-seed)")

    build_go()

    if args.build_only:
        info(f"Готово! Запусти сервер: ./{BINARY_NAME}")
        return

    run_server(env_cfg, args.port)


if __name__ == "__main__":
    main()
