package main

import (
	"crypto/rand"
	"database/sql"
	"fmt"
	"math/big"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

func getSellerDashboardHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")

	var storeID int64
	err := db.QueryRow("SELECT id FROM stores WHERE owner_id = ?", userID).Scan(&storeID)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Магазин не найден. Зарегистрируйте магазин."})
		return
	}

	var stats struct {
		TotalProducts int     `json:"total_products"`
		TotalOrders   int     `json:"total_orders"`
		TotalRevenue  float64 `json:"total_revenue"`
		PendingOrders int     `json:"pending_orders"`
	}

	db.QueryRow("SELECT COUNT(*) FROM product_sellers WHERE store_id = ? AND is_active = 1", storeID).Scan(&stats.TotalProducts)
	db.QueryRow(`SELECT COUNT(DISTINCT oi.order_id) FROM order_items oi
		JOIN product_sellers ps ON ps.id = oi.product_seller_id WHERE ps.store_id = ?`, storeID).Scan(&stats.TotalOrders)
	db.QueryRow(`SELECT COALESCE(SUM(oi.price * oi.quantity), 0) FROM order_items oi
		JOIN product_sellers ps ON ps.id = oi.product_seller_id WHERE ps.store_id = ?`, storeID).Scan(&stats.TotalRevenue)
	db.QueryRow(`SELECT COUNT(DISTINCT oi.order_id) FROM order_items oi
		JOIN product_sellers ps ON ps.id = oi.product_seller_id
		JOIN orders o ON o.id = oi.order_id
		WHERE ps.store_id = ? AND o.status = 'pending'`, storeID).Scan(&stats.PendingOrders)

	c.JSON(http.StatusOK, gin.H{
		"store_id": storeID,
		"stats":    stats,
	})
}

func getSellerProductsHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")

	var storeID int64
	err := db.QueryRow("SELECT id FROM stores WHERE owner_id = ?", userID).Scan(&storeID)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Магазин не найден"})
		return
	}

	rows, err := db.Query(`
		SELECT ps.id, ps.product_id, ps.price, ps.original_price, ps.stock, ps.delivery_days, ps.is_active, ps.created_at,
			p.name, p.main_image, p.rating, p.review_count
		FROM product_sellers ps
		JOIN products p ON p.id = ps.product_id
		WHERE ps.store_id = ?
		ORDER BY ps.created_at DESC`, storeID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки товаров"})
		return
	}
	defer rows.Close()

	type SellerProduct struct {
		ProductSellerID int64   `json:"product_seller_id"`
		ProductID       int64   `json:"product_id"`
		Price           float64 `json:"price"`
		OriginalPrice   float64 `json:"original_price"`
		Stock           int     `json:"stock"`
		DeliveryDays    int     `json:"delivery_days"`
		IsActive        bool    `json:"is_active"`
		ProductName     string  `json:"product_name"`
		MainImage       string  `json:"main_image"`
		Rating          float64 `json:"rating"`
		ReviewCount     int     `json:"review_count"`
	}

	var products []SellerProduct
	for rows.Next() {
		var sp SellerProduct
		rows.Scan(&sp.ProductSellerID, &sp.ProductID, &sp.Price, &sp.OriginalPrice, &sp.Stock, &sp.DeliveryDays, &sp.IsActive, nil,
			&sp.ProductName, &sp.MainImage, &sp.Rating, &sp.ReviewCount)
		products = append(products, sp)
	}
	if products == nil {
		products = []SellerProduct{}
	}
	c.JSON(http.StatusOK, products)
}

func createSellerProductHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")

	var storeID int64
	err := db.QueryRow("SELECT id FROM stores WHERE owner_id = ? AND status = 'active'", userID).Scan(&storeID)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusForbidden, gin.H{"error": "Активный магазин не найден"})
		return
	}

	var req CreateProductRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	if req.DeliveryDays == 0 {
		req.DeliveryDays = 3
	}

	tx, err := db.Begin()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка сервера"})
		return
	}
	defer tx.Rollback()

	result, err := tx.Exec(`
		INSERT INTO products (name, description, category_id, brand, main_image)
		VALUES (?, ?, ?, ?, ?)`,
		req.Name, req.Description, req.CategoryID, req.Brand, req.MainImage,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка создания товара"})
		return
	}
	productID, _ := result.LastInsertId()

	_, err = tx.Exec(`
		INSERT INTO product_sellers (product_id, store_id, price, original_price, stock, delivery_days)
		VALUES (?, ?, ?, ?, ?, ?)`,
		productID, storeID, req.Price, req.OriginalPrice, req.Stock, req.DeliveryDays,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка создания предложения продавца"})
		return
	}

	for i, img := range req.Images {
		tx.Exec("INSERT INTO product_images (product_id, image_url, sort_order) VALUES (?, ?, ?)", productID, img, i)
	}

	for _, attr := range req.Attributes {
		tx.Exec("INSERT INTO product_attributes (product_id, name, value) VALUES (?, ?, ?)", productID, attr.Name, attr.Value)
	}

	if err := tx.Commit(); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка сохранения товара"})
		return
	}

	c.JSON(http.StatusCreated, gin.H{
		"message":    "Товар успешно добавлен",
		"product_id": productID,
	})
}

func updateSellerProductHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	psID, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID"})
		return
	}

	var storeID int64
	db.QueryRow("SELECT id FROM stores WHERE owner_id = ?", userID).Scan(&storeID)

	var body struct {
		Price         float64 `json:"price"`
		OriginalPrice float64 `json:"original_price"`
		Stock         int     `json:"stock"`
		DeliveryDays  int     `json:"delivery_days"`
		IsActive      bool    `json:"is_active"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	result, err := db.Exec(`
		UPDATE product_sellers SET price=?, original_price=?, stock=?, delivery_days=?, is_active=?, updated_at=NOW()
		WHERE id=? AND store_id=?`,
		body.Price, body.OriginalPrice, body.Stock, body.DeliveryDays, body.IsActive, psID, storeID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка обновления"})
		return
	}
	rows, _ := result.RowsAffected()
	if rows == 0 {
		c.JSON(http.StatusNotFound, gin.H{"error": "Предложение не найдено"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Товар обновлён"})
}

func deleteSellerProductHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	psID, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID"})
		return
	}

	var storeID int64
	db.QueryRow("SELECT id FROM stores WHERE owner_id = ?", userID).Scan(&storeID)

	_, err = db.Exec("UPDATE product_sellers SET is_active = 0 WHERE id = ? AND store_id = ?", psID, storeID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка удаления"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Товар снят с продажи"})
}

func getSellerOrdersHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")

	var storeID int64
	db.QueryRow("SELECT id FROM stores WHERE owner_id = ?", userID).Scan(&storeID)

	rows, err := db.Query(`
		SELECT DISTINCT o.id, o.user_id, o.total, o.status, o.delivery_address, o.payment_method, o.created_at, u.name, u.phone
		FROM orders o
		JOIN order_items oi ON oi.order_id = o.id
		JOIN product_sellers ps ON ps.id = oi.product_seller_id
		JOIN users u ON u.id = o.user_id
		WHERE ps.store_id = ?
		ORDER BY o.created_at DESC`, storeID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки заказов"})
		return
	}
	defer rows.Close()

	type SellerOrder struct {
		Order
		CustomerName  string `json:"customer_name"`
		CustomerPhone string `json:"customer_phone"`
	}

	var orders []SellerOrder
	for rows.Next() {
		var o SellerOrder
		rows.Scan(&o.ID, &o.UserID, &o.Total, &o.Status, &o.DeliveryAddress, &o.PaymentMethod, &o.CreatedAt, &o.CustomerName, &o.CustomerPhone)
		orders = append(orders, o)
	}
	if orders == nil {
		orders = []SellerOrder{}
	}
	c.JSON(http.StatusOK, orders)
}

func updateOrderStatusHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	orderID, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID"})
		return
	}

	var storeID int64
	db.QueryRow("SELECT id FROM stores WHERE owner_id = ?", userID).Scan(&storeID)

	var body struct {
		Status string `json:"status" binding:"required"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Verify this order belongs to this seller
	var count int
	db.QueryRow(`SELECT COUNT(*) FROM order_items oi
		JOIN product_sellers ps ON ps.id = oi.product_seller_id
		WHERE oi.order_id = ? AND ps.store_id = ?`, orderID, storeID).Scan(&count)
	if count == 0 {
		c.JSON(http.StatusForbidden, gin.H{"error": "Нет доступа к этому заказу"})
		return
	}

	db.Exec("UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?", body.Status, orderID)
	c.JSON(http.StatusOK, gin.H{"message": "Статус заказа обновлён"})
}

func issueOrderCodeHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	orderID, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID"})
		return
	}

	var storeID int64
	if err := db.QueryRow("SELECT id FROM stores WHERE owner_id = ?", userID).Scan(&storeID); err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Магазин не найден"})
		return
	} else if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки магазина"})
		return
	}

	var customerID int64
	var status string
	var issueCode sql.NullString
	err = db.QueryRow(`
		SELECT o.user_id, o.status, o.issue_code
		FROM orders o
		JOIN order_items oi ON oi.order_id = o.id
		JOIN product_sellers ps ON ps.id = oi.product_seller_id
		WHERE o.id = ? AND ps.store_id = ?
		LIMIT 1`, orderID, storeID).Scan(&customerID, &status, &issueCode)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Заказ не найден"})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки заказа"})
		return
	}
	if status == "cancelled" || status == "delivered" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Нельзя выдать отменённый или доставленный заказ"})
		return
	}

	code := issueCode.String
	if !issueCode.Valid {
		code, err = generateIssueCode()
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка генерации кода"})
			return
		}
		_, err = db.Exec("UPDATE orders SET issue_code = ?, issue_code_sent_at = NOW(), updated_at = NOW() WHERE id = ?", code, orderID)
	} else if issueCode.String == "" {
		code, err = generateIssueCode()
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка генерации кода"})
			return
		}
		_, err = db.Exec("UPDATE orders SET issue_code = ?, issue_code_sent_at = NOW(), updated_at = NOW() WHERE id = ?", code, orderID)
	} else {
		_, err = db.Exec("UPDATE orders SET issue_code_sent_at = NOW(), updated_at = NOW() WHERE id = ?", orderID)
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка сохранения кода"})
		return
	}

	title := "Код выдачи заказа"
	message := fmt.Sprintf("Ваш заказ #%d готов к выдаче. Код: %s", orderID, code)
	if err := createNotification(customerID, &orderID, title, message); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка отправки уведомления"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Код отправлен клиенту"})
}

func confirmOrderIssueHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	orderID, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID"})
		return
	}

	var storeID int64
	if err := db.QueryRow("SELECT id FROM stores WHERE owner_id = ?", userID).Scan(&storeID); err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Магазин не найден"})
		return
	} else if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки магазина"})
		return
	}

	var body struct {
		Code string `json:"code" binding:"required"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Код обязателен"})
		return
	}

	var customerID int64
	var issueCode sql.NullString
	var status string
	err = db.QueryRow(`
		SELECT o.user_id, o.issue_code, o.status
		FROM orders o
		JOIN order_items oi ON oi.order_id = o.id
		JOIN product_sellers ps ON ps.id = oi.product_seller_id
		WHERE o.id = ? AND ps.store_id = ?
		LIMIT 1`, orderID, storeID).Scan(&customerID, &issueCode, &status)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Заказ не найден"})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки заказа"})
		return
	}
	if status == "delivered" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Заказ уже выдан"})
		return
	}
	if !issueCode.Valid || issueCode.String == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Код ещё не был отправлен клиенту"})
		return
	}
	if issueCode.String != body.Code {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный код выдачи"})
		return
	}

	_, err = db.Exec("UPDATE orders SET status='delivered', issue_code=NULL, updated_at=NOW() WHERE id=?", orderID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка обновления заказа"})
		return
	}

	db.Exec("DELETE FROM notifications WHERE user_id=? AND title='Код выдачи заказа'", customerID)

	c.JSON(http.StatusOK, gin.H{"message": "Заказ успешно выдан"})
}

func generateIssueCode() (string, error) {
	n, err := rand.Int(rand.Reader, big.NewInt(10000))
	if err != nil {
		return "", err
	}
	return fmt.Sprintf("%04d", n.Int64()), nil
}

// Seller can add their offer to existing product
func addOfferToProductHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	productID, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID товара"})
		return
	}

	var storeID int64
	err = db.QueryRow("SELECT id FROM stores WHERE owner_id = ? AND status = 'active'", userID).Scan(&storeID)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusForbidden, gin.H{"error": "Активный магазин не найден"})
		return
	}

	var body struct {
		Price         float64 `json:"price" binding:"required"`
		OriginalPrice float64 `json:"original_price"`
		Stock         int     `json:"stock" binding:"required"`
		DeliveryDays  int     `json:"delivery_days"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if body.DeliveryDays == 0 {
		body.DeliveryDays = 3
	}

	_, err = db.Exec(`
		INSERT INTO product_sellers (product_id, store_id, price, original_price, stock, delivery_days)
		VALUES (?, ?, ?, ?, ?, ?)
		ON DUPLICATE KEY UPDATE price=VALUES(price), original_price=VALUES(original_price),
		stock=VALUES(stock), delivery_days=VALUES(delivery_days), is_active=1`,
		productID, storeID, body.Price, body.OriginalPrice, body.Stock, body.DeliveryDays,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка добавления предложения"})
		return
	}
	c.JSON(http.StatusCreated, gin.H{"message": "Предложение добавлено"})
}
