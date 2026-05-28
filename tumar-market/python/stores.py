"""Store management utilities for Tumar Market."""

import os
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


def get_store(store_id: int) -> dict | None:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("""
        SELECT s.*, u.name as owner_name, u.email as owner_email,
            COUNT(DISTINCT ps.product_id) as product_count
        FROM stores s
        JOIN users u ON u.id = s.owner_id
        LEFT JOIN product_sellers ps ON ps.store_id = s.id AND ps.is_active = 1
        WHERE s.id = %s
        GROUP BY s.id
    """, (store_id,))
    store = cursor.fetchone()
    cursor.close()
    conn.close()
    return store


def get_store_by_owner(owner_id: int) -> dict | None:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM stores WHERE owner_id = %s", (owner_id,))
    store = cursor.fetchone()
    cursor.close()
    conn.close()
    return store


def list_stores(status: str = None, limit: int = 50) -> list:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    query = """
        SELECT s.id, s.name, s.status, s.email, s.phone, s.created_at,
            u.name as owner_name, COUNT(DISTINCT ps.id) as product_count
        FROM stores s
        JOIN users u ON u.id = s.owner_id
        LEFT JOIN product_sellers ps ON ps.store_id = s.id AND ps.is_active = 1
    """
    params = []
    if status:
        query += " WHERE s.status = %s"
        params.append(status)
    query += " GROUP BY s.id ORDER BY s.created_at DESC LIMIT %s"
    params.append(limit)
    cursor.execute(query, params)
    stores = cursor.fetchall()
    cursor.close()
    conn.close()
    return stores


def approve_store(store_id: int) -> bool:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("UPDATE stores SET status = 'active' WHERE id = %s", (store_id,))
    cursor.execute("""
        UPDATE users SET role = 'seller'
        WHERE id = (SELECT owner_id FROM stores WHERE id = %s)
    """, (store_id,))
    conn.commit()
    affected = cursor.rowcount
    cursor.close()
    conn.close()
    return affected > 0


def suspend_store(store_id: int) -> bool:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("UPDATE stores SET status = 'suspended' WHERE id = %s", (store_id,))
    conn.commit()
    affected = cursor.rowcount
    cursor.close()
    conn.close()
    return affected > 0


def get_store_stats(store_id: int) -> dict:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    stats = {}

    cursor.execute("SELECT COUNT(*) as cnt FROM product_sellers WHERE store_id = %s AND is_active = 1", (store_id,))
    stats["total_products"] = cursor.fetchone()["cnt"]

    cursor.execute("""
        SELECT COUNT(DISTINCT oi.order_id) as cnt FROM order_items oi
        JOIN product_sellers ps ON ps.id = oi.product_seller_id
        WHERE ps.store_id = %s
    """, (store_id,))
    stats["total_orders"] = cursor.fetchone()["cnt"]

    cursor.execute("""
        SELECT COALESCE(SUM(oi.price * oi.quantity), 0) as revenue FROM order_items oi
        JOIN product_sellers ps ON ps.id = oi.product_seller_id
        WHERE ps.store_id = %s
    """, (store_id,))
    stats["total_revenue"] = float(cursor.fetchone()["revenue"])

    cursor.close()
    conn.close()
    return stats


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("Usage: python stores.py <command>")
        print("Commands: list, list-pending, approve <id>, suspend <id>, stats <id>")
        sys.exit(1)

    cmd = sys.argv[1]
    if cmd == "list":
        stores = list_stores()
        for s in stores:
            print(f"[{s['id']}] {s['name']} | {s['status']} | {s['owner_name']} | {s['product_count']} товаров")
    elif cmd == "list-pending":
        stores = list_stores(status="pending")
        for s in stores:
            print(f"[{s['id']}] {s['name']} | {s['owner_name']} | {s['email']}")
    elif cmd == "approve" and len(sys.argv) > 2:
        ok = approve_store(int(sys.argv[2]))
        print("Approved" if ok else "Not found")
    elif cmd == "suspend" and len(sys.argv) > 2:
        ok = suspend_store(int(sys.argv[2]))
        print("Suspended" if ok else "Not found")
    elif cmd == "stats" and len(sys.argv) > 2:
        stats = get_store_stats(int(sys.argv[2]))
        print(f"Products: {stats['total_products']}")
        print(f"Orders: {stats['total_orders']}")
        print(f"Revenue: {stats['total_revenue']:,.0f} тг")
