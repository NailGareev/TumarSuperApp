package main

import (
	"database/sql"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

func registerStoreHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	var req StoreRegisterRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	var existing int
	db.QueryRow("SELECT COUNT(*) FROM stores WHERE owner_id = ?", userID).Scan(&existing)
	if existing > 0 {
		c.JSON(http.StatusConflict, gin.H{"error": "У вас уже зарегистрирован магазин"})
		return
	}

	result, err := db.Exec(`
		INSERT INTO stores (owner_id, name, description, legal_name, bin_number, address, phone, email, status)
		VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'active')`,
		userID, req.Name, req.Description, req.LegalName, req.BinNumber, req.Address, req.Phone, req.Email,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка регистрации магазина"})
		return
	}

	storeID, _ := result.LastInsertId()
	// Upgrade user role to seller
	db.Exec("UPDATE users SET role = 'seller' WHERE id = ?", userID)

	c.JSON(http.StatusCreated, gin.H{
		"message":  "Магазин успешно зарегистрирован",
		"store_id": storeID,
		"status":   "active",
	})
}

func getMyStoreHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	var store Store
	err := db.QueryRow(
		"SELECT id, owner_id, name, description, logo, legal_name, bin_number, address, phone, email, status, created_at FROM stores WHERE owner_id = ?",
		userID,
	).Scan(&store.ID, &store.OwnerID, &store.Name, &store.Description, &store.Logo, &store.LegalName, &store.BinNumber, &store.Address, &store.Phone, &store.Email, &store.Status, &store.CreatedAt)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Магазин не найден"})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки магазина"})
		return
	}
	c.JSON(http.StatusOK, store)
}

func updateStoreHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	var body struct {
		Name        string `json:"name"`
		Description string `json:"description"`
		Logo        string `json:"logo"`
		Address     string `json:"address"`
		Phone       string `json:"phone"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	_, err := db.Exec(`UPDATE stores SET name=?, description=?, logo=?, address=?, phone=?, updated_at=NOW() WHERE owner_id=?`,
		body.Name, body.Description, body.Logo, body.Address, body.Phone, userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка обновления магазина"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Магазин обновлён"})
}

func getStoreHandler(c *gin.Context) {
	id, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID"})
		return
	}
	var store Store
	err = db.QueryRow(
		"SELECT id, owner_id, name, description, logo, address, phone, email, status, created_at FROM stores WHERE id = ?",
		id,
	).Scan(&store.ID, &store.OwnerID, &store.Name, &store.Description, &store.Logo, &store.Address, &store.Phone, &store.Email, &store.Status, &store.CreatedAt)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Магазин не найден"})
		return
	}
	c.JSON(http.StatusOK, store)
}
