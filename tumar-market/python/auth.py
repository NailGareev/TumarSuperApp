"""Authentication utilities for Tumar Market Python tools."""

import os
import hashlib
import secrets
import mysql.connector

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "tumar_market"),
    "charset": "utf8mb4",
}


def get_connection():
    return mysql.connector.connect(**DB_CONFIG)


def hash_password(password: str) -> str:
    try:
        import bcrypt
        return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()
    except ImportError:
        salt = secrets.token_hex(16)
        hashed = hashlib.sha256((password + salt).encode()).hexdigest()
        return f"sha256${salt}${hashed}"


def verify_password(password: str, password_hash: str) -> bool:
    try:
        import bcrypt
        if password_hash.startswith("$2b$") or password_hash.startswith("$2a$"):
            return bcrypt.checkpw(password.encode(), password_hash.encode())
    except ImportError:
        pass

    if password_hash.startswith("sha256$"):
        _, salt, stored_hash = password_hash.split("$")
        computed = hashlib.sha256((password + salt).encode()).hexdigest()
        return computed == stored_hash

    return False


def get_user_by_email(email: str) -> dict | None:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM users WHERE email = %s", (email,))
    user = cursor.fetchone()
    cursor.close()
    conn.close()
    return user


def get_user_by_id(user_id: int) -> dict | None:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT id, email, name, phone, role, avatar, created_at FROM users WHERE id = %s", (user_id,))
    user = cursor.fetchone()
    cursor.close()
    conn.close()
    return user


def create_user(email: str, password: str, name: str, phone: str = "") -> int:
    conn = get_connection()
    cursor = conn.cursor()
    pwd_hash = hash_password(password)
    cursor.execute(
        "INSERT INTO users (email, password_hash, name, phone) VALUES (%s, %s, %s, %s)",
        (email, pwd_hash, name, phone),
    )
    conn.commit()
    user_id = cursor.lastrowid
    cursor.close()
    conn.close()
    return user_id


def authenticate_user(email: str, password: str) -> dict | None:
    user = get_user_by_email(email)
    if not user:
        return None
    if verify_password(password, user["password_hash"]):
        user.pop("password_hash", None)
        return user
    return None


def upgrade_to_seller(user_id: int) -> bool:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("UPDATE users SET role = 'seller' WHERE id = %s", (user_id,))
    conn.commit()
    affected = cursor.rowcount
    cursor.close()
    conn.close()
    return affected > 0


def list_users(limit: int = 50, offset: int = 0) -> list:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(
        "SELECT id, email, name, phone, role, created_at FROM users ORDER BY created_at DESC LIMIT %s OFFSET %s",
        (limit, offset),
    )
    users = cursor.fetchall()
    cursor.close()
    conn.close()
    return users


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("Usage: python auth.py <command>")
        print("Commands: list-users, create-admin")
        sys.exit(1)

    cmd = sys.argv[1]
    if cmd == "list-users":
        users = list_users()
        for u in users:
            print(f"[{u['role']}] {u['email']} - {u['name']} (id={u['id']})")
    elif cmd == "create-admin":
        email = input("Email: ")
        password = input("Password: ")
        name = input("Name: ")
        uid = create_user(email, password, name)
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("UPDATE users SET role = 'admin' WHERE id = %s", (uid,))
        conn.commit()
        conn.close()
        print(f"Admin created with id={uid}")
