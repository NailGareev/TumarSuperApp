"""Product management utilities for Tumar Market."""

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


def get_product(product_id: int) -> dict | None:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("""
        SELECT p.*, c.name as category_name, c.slug as category_slug,
            MIN(ps.price) as min_price
        FROM products p
        LEFT JOIN categories c ON c.id = p.category_id
        LEFT JOIN product_sellers ps ON ps.product_id = p.id AND ps.is_active = 1
        WHERE p.id = %s
        GROUP BY p.id
    """, (product_id,))
    product = cursor.fetchone()
    if product:
        cursor.execute("SELECT image_url FROM product_images WHERE product_id = %s ORDER BY sort_order", (product_id,))
        product["images"] = [row["image_url"] for row in cursor.fetchall()]
        cursor.execute("SELECT name, value FROM product_attributes WHERE product_id = %s", (product_id,))
        product["attributes"] = cursor.fetchall()
    cursor.close()
    conn.close()
    return product


def list_products(category_slug: str = None, limit: int = 20, offset: int = 0) -> list:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    query = """
        SELECT p.id, p.name, p.brand, p.main_image, p.rating, p.review_count,
            c.name as category_name, MIN(ps.price) as min_price
        FROM products p
        LEFT JOIN categories c ON c.id = p.category_id
        LEFT JOIN product_sellers ps ON ps.product_id = p.id AND ps.is_active = 1
    """
    params = []
    if category_slug:
        query += " WHERE c.slug = %s"
        params.append(category_slug)
    query += " GROUP BY p.id ORDER BY p.created_at DESC LIMIT %s OFFSET %s"
    params.extend([limit, offset])
    cursor.execute(query, params)
    products = cursor.fetchall()
    cursor.close()
    conn.close()
    return products


def search_products(query: str, limit: int = 20) -> list:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    search = f"%{query}%"
    cursor.execute("""
        SELECT p.id, p.name, p.brand, p.main_image, p.rating, MIN(ps.price) as min_price
        FROM products p
        LEFT JOIN product_sellers ps ON ps.product_id = p.id AND ps.is_active = 1
        WHERE p.name LIKE %s OR p.description LIKE %s OR p.brand LIKE %s
        GROUP BY p.id
        ORDER BY p.rating DESC
        LIMIT %s
    """, (search, search, search, limit))
    results = cursor.fetchall()
    cursor.close()
    conn.close()
    return results


def get_product_sellers(product_id: int) -> list:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("""
        SELECT ps.id, ps.price, ps.original_price, ps.stock, ps.delivery_days,
            s.id as store_id, s.name as store_name, s.logo as store_logo
        FROM product_sellers ps
        JOIN stores s ON s.id = ps.store_id
        WHERE ps.product_id = %s AND ps.is_active = 1 AND s.status = 'active'
        ORDER BY ps.price ASC
    """, (product_id,))
    sellers = cursor.fetchall()
    cursor.close()
    conn.close()
    return sellers


def get_categories() -> list:
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT id, parent_id, name, slug, icon FROM categories ORDER BY id")
    cats = cursor.fetchall()
    cursor.close()
    conn.close()
    return cats


def update_product_rating(product_id: int):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
        UPDATE products SET
            rating = (SELECT AVG(rating) FROM reviews WHERE product_id = %s),
            review_count = (SELECT COUNT(*) FROM reviews WHERE product_id = %s)
        WHERE id = %s
    """, (product_id, product_id, product_id))
    conn.commit()
    cursor.close()
    conn.close()


def delete_product(product_id: int, store_id: int) -> bool:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute(
        "UPDATE product_sellers SET is_active = 0 WHERE product_id = %s AND store_id = %s",
        (product_id, store_id),
    )
    conn.commit()
    affected = cursor.rowcount
    cursor.close()
    conn.close()
    return affected > 0


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("Usage: python products.py <command>")
        print("Commands: list, search <query>, categories")
        sys.exit(1)

    cmd = sys.argv[1]
    if cmd == "list":
        products = list_products()
        for p in products:
            price = f"{p['min_price']:,.0f} тг" if p['min_price'] else "Нет в продаже"
            print(f"[{p['id']}] {p['name']} | {p['brand']} | {price} | ★{p['rating']}")
    elif cmd == "search" and len(sys.argv) > 2:
        results = search_products(" ".join(sys.argv[2:]))
        for p in results:
            print(f"[{p['id']}] {p['name']} | {p['brand']}")
    elif cmd == "categories":
        cats = get_categories()
        for c in cats:
            print(f"[{c['id']}] {c['icon']} {c['name']} ({c['slug']})")
