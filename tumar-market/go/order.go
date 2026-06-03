package main

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

func createOrderHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	var req CreateOrderRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Get cart items
	rows, err := db.Query(`
		SELECT ci.product_seller_id, ci.quantity, ps.price, ps.stock, p.name, p.main_image, s.name
		FROM cart_items ci
		JOIN product_sellers ps ON ps.id = ci.product_seller_id
		JOIN products p ON p.id = ps.product_id
		JOIN stores s ON s.id = ps.store_id
		WHERE ci.user_id = ? AND ps.is_active = 1`, userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки корзины"})
		return
	}
	defer rows.Close()

	type cartRow struct {
		ProductSellerID int64
		Quantity        int
		Price           float64
		Stock           int
		ProductName     string
		ProductImage    string
		StoreName       string
	}

	var cartItems []cartRow
	var total float64
	for rows.Next() {
		var item cartRow
		rows.Scan(&item.ProductSellerID, &item.Quantity, &item.Price, &item.Stock, &item.ProductName, &item.ProductImage, &item.StoreName)
		if item.Stock < item.Quantity {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Недостаточно товара: " + item.ProductName})
			return
		}
		total += item.Price * float64(item.Quantity)
		cartItems = append(cartItems, item)
	}

	if len(cartItems) == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Корзина пуста"})
		return
	}

	tx, err := db.Begin()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка сервера"})
		return
	}
	defer tx.Rollback()

	result, err := tx.Exec(`
		INSERT INTO orders (user_id, total, delivery_address, payment_method, tumar_ref)
		VALUES (?, ?, ?, ?, ?)`,
		userID, total, req.DeliveryAddress, req.PaymentMethod, req.TumarRef)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка создания заказа"})
		return
	}
	orderID, _ := result.LastInsertId()

	for _, item := range cartItems {
		_, err = tx.Exec(`
			INSERT INTO order_items (order_id, product_seller_id, product_name, product_image, store_name, quantity, price)
			VALUES (?, ?, ?, ?, ?, ?, ?)`,
			orderID, item.ProductSellerID, item.ProductName, item.ProductImage, item.StoreName, item.Quantity, item.Price)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка сохранения товаров заказа"})
			return
		}
		// Reduce stock
		tx.Exec("UPDATE product_sellers SET stock = stock - ? WHERE id = ?", item.Quantity, item.ProductSellerID)
	}

	// Clear cart
	tx.Exec("DELETE FROM cart_items WHERE user_id = ?", userID)

	if err := tx.Commit(); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка оформления заказа"})
		return
	}

	c.JSON(http.StatusCreated, gin.H{
		"message":  "Заказ успешно оформлен",
		"order_id": orderID,
		"total":    total,
	})
}

func getOrdersHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")

	rows, err := db.Query(`
		SELECT id, total, status, delivery_address, payment_method, COALESCE(tumar_ref,''), created_at
		FROM orders WHERE user_id = ? ORDER BY created_at DESC`, userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки заказов"})
		return
	}
	defer rows.Close()

	var orders []Order
	for rows.Next() {
		var o Order
		rows.Scan(&o.ID, &o.Total, &o.Status, &o.DeliveryAddress, &o.PaymentMethod, &o.TumarRef, &o.CreatedAt)
		o.UserID = userID
		orders = append(orders, o)
	}
	if orders == nil {
		orders = []Order{}
	}
	c.JSON(http.StatusOK, orders)
}

func getOrderHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	orderID, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID"})
		return
	}

	var o Order
	err = db.QueryRow(`
		SELECT id, user_id, total, status, delivery_address, payment_method, COALESCE(tumar_ref,''), created_at
		FROM orders WHERE id = ? AND user_id = ?`, orderID, userID).
		Scan(&o.ID, &o.UserID, &o.Total, &o.Status, &o.DeliveryAddress, &o.PaymentMethod, &o.TumarRef, &o.CreatedAt)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Заказ не найден"})
		return
	}

	// Load items
	itemRows, _ := db.Query(`
		SELECT id, product_seller_id, product_name, product_image, store_name, quantity, price
		FROM order_items WHERE order_id = ?`, orderID)
	if itemRows != nil {
		defer itemRows.Close()
		for itemRows.Next() {
			var item OrderItem
			itemRows.Scan(&item.ID, &item.ProductSellerID, &item.ProductName, &item.ProductImage, &item.StoreName, &item.Quantity, &item.Price)
			item.OrderID = orderID
			o.Items = append(o.Items, item)
		}
	}
	c.JSON(http.StatusOK, o)
}

func cancelOrderHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	orderID, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID"})
		return
	}

	var status, paymentMethod string
	var total float64
	err = db.QueryRow(
		"SELECT status, payment_method, total FROM orders WHERE id = ? AND user_id = ?",
		orderID, userID).Scan(&status, &paymentMethod, &total)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Заказ не найден"})
		return
	}
	if status != "pending" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Можно отменить только ожидающий заказ"})
		return
	}

	db.Exec("UPDATE orders SET status = 'cancelled', updated_at = NOW() WHERE id = ?", orderID)

	// Refund to Tumar wallet if the order was paid via Tumar Pay
	if paymentMethod == "tumar_pay" {
		var phone string
		db.QueryRow("SELECT phone FROM users WHERE id = ?", userID).Scan(&phone)
		if phone != "" {
			go refundToTumarWallet(phone, total)
		}
	}

	c.JSON(http.StatusOK, gin.H{"message": "Заказ отменён"})
}

func refundToTumarWallet(phone string, amount float64) {
	payload, _ := json.Marshal(map[string]interface{}{
		"buyer_phone": phone,
		"amount":      fmt.Sprintf("%.2f", amount),
		"app_secret":  "tumar_app_secret_2024",
	})
	resp, err := http.Post(
		"http://localhost:3000/api/market/return/process-refund",
		"application/json",
		bytes.NewReader(payload),
	)
	if err == nil {
		io.Copy(io.Discard, resp.Body)
		resp.Body.Close()
	}
}
