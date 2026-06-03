package main

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
)

type ReturnRequest struct {
	ID         int64      `json:"id"`
	OrderID    int64      `json:"order_id"`
	UserID     int64      `json:"user_id"`
	Reason     string     `json:"reason"`
	PhotosJSON string     `json:"photos_json"`
	Status     string     `json:"status"`
	CreatedAt  time.Time  `json:"created_at"`
	UpdatedAt  *time.Time `json:"updated_at"`
	UserPhone  string     `json:"user_phone,omitempty"`
	OrderTotal float64    `json:"order_total,omitempty"`
}

// POST /api/returns
func createReturnHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")

	var req struct {
		OrderID int64    `json:"order_id" binding:"required"`
		Reason  string   `json:"reason"   binding:"required"`
		Photos  []string `json:"photos"   binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if len(req.Photos) < 2 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Минимум 2 фотографии обязательны"})
		return
	}
	if len(req.Photos) > 10 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Максимум 10 фотографий"})
		return
	}

	var orderStatus string
	err := db.QueryRow("SELECT status FROM orders WHERE id = ? AND user_id = ?", req.OrderID, userID).Scan(&orderStatus)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Заказ не найден"})
		return
	}
	if orderStatus != "delivered" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Возврат возможен только для доставленных заказов"})
		return
	}

	var existingID int64
	if err = db.QueryRow("SELECT id FROM return_requests WHERE order_id = ? AND user_id = ?", req.OrderID, userID).Scan(&existingID); err == nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Заявка на возврат для этого заказа уже существует"})
		return
	}

	photosJSON, _ := json.Marshal(req.Photos)
	_, err = db.Exec(
		"INSERT INTO return_requests (order_id, user_id, reason, photos_json) VALUES (?, ?, ?, ?)",
		req.OrderID, userID, req.Reason, string(photosJSON))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка создания заявки"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"ok": true})
}

// GET /api/returns
func getMyReturnsHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")

	rows, err := db.Query(`
		SELECT r.id, r.order_id, r.user_id, r.reason, r.photos_json, r.status, r.created_at, r.updated_at
		FROM return_requests r
		WHERE r.user_id = ?
		ORDER BY r.created_at DESC`, userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки"})
		return
	}
	defer rows.Close()

	var returns []ReturnRequest
	for rows.Next() {
		var r ReturnRequest
		rows.Scan(&r.ID, &r.OrderID, &r.UserID, &r.Reason, &r.PhotosJSON, &r.Status, &r.CreatedAt, &r.UpdatedAt)
		returns = append(returns, r)
	}
	if returns == nil {
		returns = []ReturnRequest{}
	}
	c.JSON(http.StatusOK, gin.H{"ok": true, "returns": returns})
}

// GET /api/seller/returns
func getSellerReturnsHandler(c *gin.Context) {
	rows, err := db.Query(`
		SELECT r.id, r.order_id, r.user_id, r.reason, r.photos_json, r.status, r.created_at, r.updated_at,
		       u.phone, o.total
		FROM return_requests r
		JOIN users u ON u.id = r.user_id
		JOIN orders o ON o.id = r.order_id
		ORDER BY r.created_at DESC`)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки"})
		return
	}
	defer rows.Close()

	var returns []ReturnRequest
	for rows.Next() {
		var r ReturnRequest
		rows.Scan(&r.ID, &r.OrderID, &r.UserID, &r.Reason, &r.PhotosJSON, &r.Status, &r.CreatedAt, &r.UpdatedAt,
			&r.UserPhone, &r.OrderTotal)
		returns = append(returns, r)
	}
	if returns == nil {
		returns = []ReturnRequest{}
	}
	c.JSON(http.StatusOK, gin.H{"ok": true, "returns": returns})
}

// PUT /api/seller/returns/:id/accept  →  COURIER_PENDING
func acceptReturnHandler(c *gin.Context) {
	id, _ := strconv.ParseInt(c.Param("id"), 10, 64)

	var status string
	if err := db.QueryRow("SELECT status FROM return_requests WHERE id = ?", id).Scan(&status); err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Заявка не найдена"})
		return
	}
	if status != "CREATED" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Нельзя принять заявку в текущем статусе"})
		return
	}

	db.Exec("UPDATE return_requests SET status='COURIER_PENDING', updated_at=NOW() WHERE id=?", id)
	c.JSON(http.StatusOK, gin.H{"ok": true})
}

// PUT /api/seller/returns/:id/refund  →  REFUNDED + debit Node.js wallet
func refundReturnHandler(c *gin.Context) {
	id, _ := strconv.ParseInt(c.Param("id"), 10, 64)

	var (
		userPhone  string
		orderTotal float64
		status     string
	)
	err := db.QueryRow(`
		SELECT u.phone, o.total, r.status
		FROM return_requests r
		JOIN users u ON u.id = r.user_id
		JOIN orders o ON o.id = r.order_id
		WHERE r.id = ?`, id).Scan(&userPhone, &orderTotal, &status)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Заявка не найдена"})
		return
	}
	if status == "REFUNDED" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Деньги уже возвращены"})
		return
	}

	// Server-to-server call to Node.js wallet
	payload, _ := json.Marshal(map[string]interface{}{
		"buyer_phone": userPhone,
		"amount":      orderTotal,
		"app_secret":  "tumar_app_secret_2024",
	})
	resp, err := http.Post("http://localhost:3000/api/market/return/process-refund",
		"application/json", bytes.NewReader(payload))
	if err != nil || resp.StatusCode != http.StatusOK {
		c.JSON(http.StatusBadGateway, gin.H{"error": "Ошибка перевода средств. Попробуйте позже."})
		if resp != nil {
			resp.Body.Close()
		}
		return
	}
	resp.Body.Close()

	db.Exec("UPDATE return_requests SET status='REFUNDED', updated_at=NOW() WHERE id=?", id)
	c.JSON(http.StatusOK, gin.H{"ok": true})
}

// PUT /api/returns/:id/courier-sent  →  IN_TRANSIT (buyer action)
func markCourierSentHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	id, _ := strconv.ParseInt(c.Param("id"), 10, 64)

	var (
		status  string
		ownerID int64
	)
	if err := db.QueryRow("SELECT status, user_id FROM return_requests WHERE id=?", id).Scan(&status, &ownerID); err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Заявка не найдена"})
		return
	}
	if ownerID != userID {
		c.JSON(http.StatusForbidden, gin.H{"error": "Нет доступа"})
		return
	}
	if status != "COURIER_PENDING" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Нельзя обновить статус"})
		return
	}

	db.Exec("UPDATE return_requests SET status='IN_TRANSIT', updated_at=NOW() WHERE id=?", id)
	c.JSON(http.StatusOK, gin.H{"ok": true})
}
