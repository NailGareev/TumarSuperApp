#!/usr/bin/env python3
"""
Создаёт базу данных и все таблицы для Tumar Super App.
Читает параметры подключения из файла .env в той же папке.

Использование:
    python setup_db.py
    python setup_db.py --env /path/to/.env

Зависимости:
    pip install mysql-connector-python python-dotenv
"""

import argparse
import os
import sys

# --------------------------------------------------------------------------- #
# Зависимости                                                                   #
# --------------------------------------------------------------------------- #
try:
    import mysql.connector
    from mysql.connector import errorcode
except ImportError:
    sys.exit("Установите драйвер: pip install mysql-connector-python")

try:
    from dotenv import dotenv_values
except ImportError:
    sys.exit("Установите пакет: pip install python-dotenv")


# --------------------------------------------------------------------------- #
# SQL-определения таблиц (порядок важен из-за внешних ключей)                  #
# --------------------------------------------------------------------------- #

TABLES = {}

TABLES["users"] = """
CREATE TABLE IF NOT EXISTS users (
    id            INT          AUTO_INCREMENT PRIMARY KEY,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    phone         VARCHAR(20)  NOT NULL UNIQUE,
    age           INT          NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Пользователи приложения'
"""

TABLES["balances"] = """
CREATE TABLE IF NOT EXISTS balances (
    id         INT            AUTO_INCREMENT PRIMARY KEY,
    user_id    INT            NOT NULL UNIQUE,
    balance    DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency   VARCHAR(10)    NOT NULL DEFAULT 'KZT',
    updated_at DATETIME       NOT NULL,
    CONSTRAINT fk_balances_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Балансы пользователей'
"""

TABLES["transactions"] = """
CREATE TABLE IF NOT EXISTS transactions (
    id               INT             AUTO_INCREMENT PRIMARY KEY,
    sender_id        INT             NULL,
    recipient_id     INT             NULL,
    amount           DECIMAL(15, 2)  NOT NULL,
    currency         VARCHAR(10)     NOT NULL DEFAULT 'KZT',
    transaction_type ENUM('TRANSFER','TOPUP','PAYMENT') NOT NULL DEFAULT 'TRANSFER',
    description      VARCHAR(500)    NULL     COMMENT 'Описание платежа (для PAYMENT)',
    timestamp        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_sender    FOREIGN KEY (sender_id)
        REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_tx_recipient FOREIGN KEY (recipient_id)
        REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_tx_sender    (sender_id),
    INDEX idx_tx_recipient (recipient_id),
    INDEX idx_tx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='История транзакций: переводы, пополнения, платежи'
"""

TABLES["cards"] = """
CREATE TABLE IF NOT EXISTS cards (
    id               INT          AUTO_INCREMENT PRIMARY KEY,
    user_id          INT          NOT NULL UNIQUE,
    card_number      VARCHAR(16)  NOT NULL,
    cvv_encrypted    VARCHAR(255) NOT NULL  COMMENT 'AES-256-CBC: ivHex:encryptedHex',
    expiry_encrypted VARCHAR(255) NOT NULL  COMMENT 'AES-256-CBC: ivHex:encryptedHex',
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cards_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Виртуальные карты пользователей (CVV и срок действия зашифрованы)'
"""

TABLES["market_purchases"] = """
CREATE TABLE IF NOT EXISTS market_purchases (
    id         INT            AUTO_INCREMENT PRIMARY KEY,
    user_id    INT            NOT NULL,
    order_ref  VARCHAR(50)    NOT NULL    COMMENT 'Номер заказа, напр. TM12345678',
    amount     DECIMAL(10, 2) NOT NULL    COMMENT 'Сумма заказа (₸)',
    items_json TEXT           NOT NULL    COMMENT 'JSON-массив товаров из корзины',
    address    VARCHAR(500)   NOT NULL    COMMENT 'Адрес доставки',
    status     ENUM('processing','shipping','delivered','cancelled')
               NOT NULL DEFAULT 'shipping' COMMENT 'Статус заказа',
    created_at TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mp_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_mp_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Покупки пользователей в Tumar Market, оплаченные через баланс'
"""

TABLES["tours"] = """
CREATE TABLE IF NOT EXISTS tours (
    id               INT            AUTO_INCREMENT PRIMARY KEY,
    location         VARCHAR(255)   NOT NULL    COMMENT 'Локация, напр. «Вьетнам, Нячанг»',
    hotel_name       VARCHAR(255)   NOT NULL    COMMENT 'Название отеля',
    stars            TINYINT        NOT NULL DEFAULT 5  COMMENT 'Звёзды 1–5',
    price            DECIMAL(12, 2) NOT NULL    COMMENT 'Итоговая цена тура (₸)',
    months           INT            NOT NULL DEFAULT 12 COMMENT 'Количество месяцев рассрочки',
    discount_percent INT            NOT NULL DEFAULT 0  COMMENT 'Скидка в процентах',
    original_price   DECIMAL(12, 2) NULL        COMMENT 'Цена до скидки (₸)',
    image_url        VARCHAR(500)   NULL        COMMENT 'URL картинки тура',
    is_hot           TINYINT(1)     NOT NULL DEFAULT 0  COMMENT '1 = горящий тур',
    is_active        TINYINT(1)     NOT NULL DEFAULT 1  COMMENT '1 = показывать в приложении',
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Туры, добавляемые администратором'
"""


# --------------------------------------------------------------------------- #
# Вспомогательные функции                                                       #
# --------------------------------------------------------------------------- #

def load_env(env_path: str) -> dict:
    if not os.path.exists(env_path):
        sys.exit(f"Файл .env не найден: {env_path}")
    return dotenv_values(env_path)


def get_conn_params(env: dict) -> dict:
    return {
        "host":     env.get("DB_HOST", "localhost"),
        "user":     env.get("DB_USER", "root"),
        "password": env.get("DB_PASSWORD", ""),
        "charset":  "utf8mb4",
    }


def ensure_database(cursor, db_name: str):
    cursor.execute(
        f"CREATE DATABASE IF NOT EXISTS `{db_name}` "
        f"CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )
    print(f"  База данных «{db_name}»: OK")


def create_tables(cursor):
    for name, ddl in TABLES.items():
        try:
            cursor.execute(ddl)
            print(f"  Таблица «{name}»: OK")
        except mysql.connector.Error as err:
            print(f"  [ОШИБКА] Таблица «{name}»: {err}")
            raise


# --------------------------------------------------------------------------- #
# Точка входа                                                                   #
# --------------------------------------------------------------------------- #

def main():
    parser = argparse.ArgumentParser(description="Инициализация БД Tumar Super App")
    parser.add_argument(
        "--env",
        default=os.path.join(os.path.dirname(__file__), ".env"),
        help="Путь к файлу .env (по умолчанию: .env рядом со скриптом)",
    )
    args = parser.parse_args()

    print(f"Загружаю настройки из: {args.env}")
    env = load_env(args.env)

    db_name = env.get("DB_NAME", "tumar_super_app_db")
    params  = get_conn_params(env)

    print(f"Подключаюсь к MySQL: {params['user']}@{params['host']} ...")
    try:
        conn = mysql.connector.connect(**params)
    except mysql.connector.Error as err:
        if err.errno == errorcode.ER_ACCESS_DENIED_ERROR:
            sys.exit("Неверный логин или пароль MySQL.")
        sys.exit(f"Ошибка подключения: {err}")

    cursor = conn.cursor()

    print("\n[1/3] Создаю базу данных...")
    ensure_database(cursor, db_name)

    print(f"\n[2/3] Переключаюсь на БД «{db_name}»...")
    cursor.execute(f"USE `{db_name}`")

    print("\n[3/3] Создаю таблицы...")
    create_tables(cursor)

    conn.commit()
    cursor.close()
    conn.close()

    print(f"\nГотово! База данных «{db_name}» и все таблицы успешно созданы.")
    print("\nТаблицы:")
    for name in TABLES:
        print(f"  ✓ {name}")


if __name__ == "__main__":
    main()
