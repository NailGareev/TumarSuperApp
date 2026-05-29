"""Database initialization and migration script for Tumar Market."""

import os
import sys
import mysql.connector
from mysql.connector import errorcode


DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", ""),
}
DB_NAME = os.getenv("DB_NAME", "tumar_market")


TABLES = {
    "users": """
        CREATE TABLE IF NOT EXISTS users (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            email VARCHAR(191) UNIQUE NOT NULL,
            password_hash VARCHAR(255) NOT NULL,
            name VARCHAR(255) NOT NULL,
            phone VARCHAR(20) DEFAULT '',
            role ENUM('user','seller','admin') DEFAULT 'user',
            avatar VARCHAR(500) DEFAULT '',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NULL DEFAULT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
    "stores": """
        CREATE TABLE IF NOT EXISTS stores (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            owner_id BIGINT NOT NULL,
            name VARCHAR(255) NOT NULL,
            description TEXT,
            logo VARCHAR(500) DEFAULT '',
            legal_name VARCHAR(255) NOT NULL,
            bin_number VARCHAR(20) NOT NULL,
            address TEXT,
            phone VARCHAR(20) DEFAULT '',
            email VARCHAR(255) DEFAULT '',
            status ENUM('pending','active','suspended') DEFAULT 'pending',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NULL DEFAULT NULL,
            FOREIGN KEY (owner_id) REFERENCES users(id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
    "categories": """
        CREATE TABLE IF NOT EXISTS categories (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            parent_id BIGINT DEFAULT NULL,
            name VARCHAR(255) NOT NULL,
            slug VARCHAR(191) UNIQUE NOT NULL,
            icon VARCHAR(255) DEFAULT '',
            image VARCHAR(500) DEFAULT '',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
    "products": """
        CREATE TABLE IF NOT EXISTS products (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(500) NOT NULL,
            description TEXT,
            category_id BIGINT,
            brand VARCHAR(255) DEFAULT '',
            main_image VARCHAR(500) DEFAULT '',
            rating DECIMAL(3,2) DEFAULT 0.00,
            review_count INT DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NULL DEFAULT NULL,
            FOREIGN KEY (category_id) REFERENCES categories(id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
    "product_sellers": """
        CREATE TABLE IF NOT EXISTS product_sellers (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            product_id BIGINT NOT NULL,
            store_id BIGINT NOT NULL,
            price DECIMAL(10,2) NOT NULL,
            original_price DECIMAL(10,2) DEFAULT 0,
            stock INT DEFAULT 0,
            delivery_days INT DEFAULT 3,
            is_active TINYINT(1) DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NULL DEFAULT NULL,
            FOREIGN KEY (product_id) REFERENCES products(id),
            FOREIGN KEY (store_id) REFERENCES stores(id),
            UNIQUE KEY unique_product_store (product_id, store_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
    "product_images": """
        CREATE TABLE IF NOT EXISTS product_images (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            product_id BIGINT NOT NULL,
            image_url VARCHAR(500) NOT NULL,
            sort_order INT DEFAULT 0,
            FOREIGN KEY (product_id) REFERENCES products(id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
    "product_attributes": """
        CREATE TABLE IF NOT EXISTS product_attributes (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            product_id BIGINT NOT NULL,
            name VARCHAR(255) NOT NULL,
            value TEXT NOT NULL,
            FOREIGN KEY (product_id) REFERENCES products(id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
    "reviews": """
        CREATE TABLE IF NOT EXISTS reviews (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            product_id BIGINT NOT NULL,
            user_id BIGINT NOT NULL,
            store_id BIGINT DEFAULT NULL,
            rating INT NOT NULL,
            comment TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (product_id) REFERENCES products(id),
            FOREIGN KEY (user_id) REFERENCES users(id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
    "cart_items": """
        CREATE TABLE IF NOT EXISTS cart_items (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            user_id BIGINT NOT NULL,
            product_seller_id BIGINT NOT NULL,
            quantity INT DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id),
            FOREIGN KEY (product_seller_id) REFERENCES product_sellers(id),
            UNIQUE KEY unique_cart_item (user_id, product_seller_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
    "orders": """
        CREATE TABLE IF NOT EXISTS orders (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            user_id BIGINT NOT NULL,
            total DECIMAL(10,2) NOT NULL,
            status ENUM('pending','confirmed','processing','shipped','delivered','cancelled') DEFAULT 'pending',
            issue_code CHAR(4) DEFAULT NULL,
            issue_code_sent_at TIMESTAMP NULL DEFAULT NULL,
            delivery_address TEXT,
            payment_method VARCHAR(50) DEFAULT '',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NULL DEFAULT NULL,
            FOREIGN KEY (user_id) REFERENCES users(id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
    "order_items": """
        CREATE TABLE IF NOT EXISTS order_items (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            order_id BIGINT NOT NULL,
            product_seller_id BIGINT NOT NULL,
            product_name VARCHAR(500) NOT NULL,
            product_image VARCHAR(500) DEFAULT '',
            store_name VARCHAR(255) NOT NULL,
            quantity INT NOT NULL,
            price DECIMAL(10,2) NOT NULL,
            FOREIGN KEY (order_id) REFERENCES orders(id),
            FOREIGN KEY (product_seller_id) REFERENCES product_sellers(id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
    "notifications": """
        CREATE TABLE IF NOT EXISTS notifications (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            user_id BIGINT NOT NULL,
            order_id BIGINT DEFAULT NULL,
            title VARCHAR(255) NOT NULL,
            message TEXT NOT NULL,
            is_read TINYINT(1) DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id),
            FOREIGN KEY (order_id) REFERENCES orders(id),
            INDEX idx_notifications_user (user_id, is_read, created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """,
}


def create_database(cursor):
    try:
        cursor.execute(
            f"CREATE DATABASE IF NOT EXISTS `{DB_NAME}` "
            "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
        )
        print(f"Database '{DB_NAME}' created or already exists.")
    except mysql.connector.Error as err:
        print(f"Failed creating database: {err}")
        sys.exit(1)


def create_tables(cursor):
    cursor.execute(f"USE `{DB_NAME}`")
    for table_name, ddl in TABLES.items():
        try:
            cursor.execute(ddl)
            print(f"Table '{table_name}' created or already exists.")
        except mysql.connector.Error as err:
            if err.errno == errorcode.ER_TABLE_EXISTS_ERROR:
                print(f"Table '{table_name}' already exists.")
            else:
                print(f"Error creating table '{table_name}': {err}")


def main():
    print("=" * 50)
    print("Tumar Market - Database Setup")
    print("=" * 50)

    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        create_database(cursor)
        create_tables(cursor)
        conn.commit()

        print("\nDatabase setup complete!")
        print(f"Database: {DB_NAME}")
        print(f"Host: {DB_CONFIG['host']}:{DB_CONFIG['port']}")
        print("\nNow run: python seed_data.py")

        cursor.close()
        conn.close()
    except mysql.connector.Error as err:
        if err.errno == errorcode.ER_ACCESS_DENIED_ERROR:
            print("Error: Wrong username or password")
        elif err.errno == errorcode.ER_BAD_DB_ERROR:
            print("Error: Database does not exist")
        else:
            print(f"Error: {err}")
        sys.exit(1)


if __name__ == "__main__":
    main()
