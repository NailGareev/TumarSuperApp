"""Order management utilities for Tumar Market."""

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

ORDER_STATUSES = ["pending", "confirmed", "processing", "shipped", "delivered", "cancelled"]
STATUS_LABELS = {
    "pending": "Ожидает подтверждения",
    "confirmed": "Подтверждён",
    "processing": "В обработке",
    "shipped": "Отправлен",
    "delivered": "Доставлен",
    "cancelled": "Отменён",
}


def get_connection():
    return mysql.connector.connect(**DB_CONFIG)


def get_order(order_id: int) -> dict | None:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("""
        SELECT o.*, u.name as customer_name, u.email as customer_email, u.phone as customer_phone
        FROM orders o
        JOIN users u ON u.id = o.user_id
        WHERE o.id = %s
    """, (order_id,))
    order = cursor.fetchone()
    if order:
        cursor.execute("""
            SELECT oi.*, ps.price as current_price
            FROM order_items oi
            JOIN product_sellers ps ON ps.id = oi.product_seller_id
            WHERE oi.order_id = %s
        """, (order_id,))
        order["items"] = cursor.fetchall()
        order["status_label"] = STATUS_LABELS.get(order["status"], order["status"])
    cursor.close()
    conn.close()
    return order


def list_orders(status: str = None, limit: int = 50, offset: int = 0) -> list:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    query = """
        SELECT o.id, o.total, o.status, o.created_at,
            u.name as customer_name, u.email as customer_email,
            COUNT(oi.id) as item_count
        FROM orders o
        JOIN users u ON u.id = o.user_id
        LEFT JOIN order_items oi ON oi.order_id = o.id
    """
    params = []
    if status:
        query += " WHERE o.status = %s"
        params.append(status)
    query += " GROUP BY o.id ORDER BY o.created_at DESC LIMIT %s OFFSET %s"
    params.extend([limit, offset])
    cursor.execute(query, params)
    orders = cursor.fetchall()
    for o in orders:
        o["status_label"] = STATUS_LABELS.get(o["status"], o["status"])
    cursor.close()
    conn.close()
    return orders


def update_order_status(order_id: int, status: str) -> bool:
    if status not in ORDER_STATUSES:
        raise ValueError(f"Invalid status: {status}. Valid: {ORDER_STATUSES}")
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("UPDATE orders SET status = %s WHERE id = %s", (status, order_id))
    conn.commit()
    affected = cursor.rowcount
    cursor.close()
    conn.close()
    return affected > 0


def get_store_orders(store_id: int, status: str = None) -> list:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    query = """
        SELECT DISTINCT o.id, o.total, o.status, o.delivery_address, o.created_at,
            u.name as customer_name, u.phone as customer_phone
        FROM orders o
        JOIN order_items oi ON oi.order_id = o.id
        JOIN product_sellers ps ON ps.id = oi.product_seller_id
        JOIN users u ON u.id = o.user_id
        WHERE ps.store_id = %s
    """
    params = [store_id]
    if status:
        query += " AND o.status = %s"
        params.append(status)
    query += " ORDER BY o.created_at DESC"
    cursor.execute(query, params)
    orders = cursor.fetchall()
    for o in orders:
        o["status_label"] = STATUS_LABELS.get(o["status"], o["status"])
    cursor.close()
    conn.close()
    return orders


def get_revenue_stats() -> dict:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    stats = {}

    cursor.execute("SELECT COUNT(*) as cnt, COALESCE(SUM(total), 0) as total FROM orders WHERE status != 'cancelled'")
    row = cursor.fetchone()
    stats["total_orders"] = row["cnt"]
    stats["total_revenue"] = float(row["total"])

    cursor.execute("""
        SELECT COUNT(*) as cnt, COALESCE(SUM(total), 0) as total FROM orders
        WHERE status != 'cancelled' AND DATE(created_at) = CURDATE()
    """)
    row = cursor.fetchone()
    stats["today_orders"] = row["cnt"]
    stats["today_revenue"] = float(row["total"])

    cursor.execute("SELECT COUNT(*) as cnt FROM orders WHERE status = 'pending'")
    stats["pending_orders"] = cursor.fetchone()["cnt"]

    cursor.close()
    conn.close()
    return stats


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("Usage: python orders.py <command>")
        print("Commands: list, list-pending, get <id>, update-status <id> <status>, stats")
        sys.exit(1)

    cmd = sys.argv[1]
    if cmd == "list":
        orders = list_orders()
        for o in orders:
            print(f"[#{o['id']}] {o['status_label']} | {float(o['total']):,.0f} тг | {o['customer_name']}")
    elif cmd == "list-pending":
        orders = list_orders(status="pending")
        for o in orders:
            print(f"[#{o['id']}] {o['customer_name']} | {float(o['total']):,.0f} тг | {o['created_at']}")
    elif cmd == "get" and len(sys.argv) > 2:
        order = get_order(int(sys.argv[2]))
        if order:
            print(f"Order #{order['id']}: {order['status_label']}")
            print(f"Customer: {order['customer_name']} ({order['customer_email']})")
            print(f"Total: {float(order['total']):,.0f} тг")
            for item in order.get("items", []):
                print(f"  - {item['product_name']} x{item['quantity']} = {float(item['price']):,.0f} тг")
        else:
            print("Order not found")
    elif cmd == "update-status" and len(sys.argv) > 3:
        ok = update_order_status(int(sys.argv[2]), sys.argv[3])
        print("Updated" if ok else "Not found")
    elif cmd == "stats":
        stats = get_revenue_stats()
        print(f"Total orders: {stats['total_orders']}")
        print(f"Total revenue: {stats['total_revenue']:,.0f} тг")
        print(f"Today orders: {stats['today_orders']}")
        print(f"Today revenue: {stats['today_revenue']:,.0f} тг")
        print(f"Pending: {stats['pending_orders']}")
