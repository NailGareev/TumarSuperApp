package main

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

func getCartHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")

	rows, err := db.Query(`
		SELECT ci.id, ci.product_seller_id, ci.quantity,
			ps.price, ps.original_price, ps.stock, ps.delivery_days,
			p.id, p.name, p.main_image,
			s.id, s.name
		FROM cart_items ci
		JOIN product_sellers ps ON ps.id = ci.product_seller_id
		JOIN products p ON p.id = ps.product_id
		JOIN stores s ON s.id = ps.store_id
		WHERE ci.user_id = ?
		ORDER BY ci.created_at DESC`, userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки корзины"})
		return
	}
	defer rows.Close()

	type CartItemFull struct {
		ID              int64   `json:"id"`
		ProductSellerID int64   `json:"product_seller_id"`
		Quantity        int     `json:"quantity"`
		Price           float64 `json:"price"`
		OriginalPrice   float64 `json:"original_price"`
		Stock           int     `json:"stock"`
		DeliveryDays    int     `json:"delivery_days"`
		ProductID       int64   `json:"product_id"`
		ProductName     string  `json:"product_name"`
		ProductImage    string  `json:"product_image"`
		StoreID         int64   `json:"store_id"`
		StoreName       string  `json:"store_name"`
	}

	var items []CartItemFull
	var total float64
	for rows.Next() {
		var item CartItemFull
		rows.Scan(&item.ID, &item.ProductSellerID, &item.Quantity,
			&item.Price, &item.OriginalPrice, &item.Stock, &item.DeliveryDays,
			&item.ProductID, &item.ProductName, &item.ProductImage,
			&item.StoreID, &item.StoreName)
		total += item.Price * float64(item.Quantity)
		items = append(items, item)
	}
	if items == nil {
		items = []CartItemFull{}
	}

	c.JSON(http.StatusOK, gin.H{
		"items": items,
		"total": total,
		"count": len(items),
	})
}

func addToCartHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	var req AddToCartRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Check stock
	var stock int
	err := db.QueryRow("SELECT stock FROM product_sellers WHERE id = ? AND is_active = 1", req.ProductSellerID).Scan(&stock)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Товар не найден"})
		return
	}
	if stock < req.Quantity {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Недостаточно товара на складе"})
		return
	}

	_, err = db.Exec(`
		INSERT INTO cart_items (user_id, product_seller_id, quantity)
		VALUES (?, ?, ?)
		ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)`,
		userID, req.ProductSellerID, req.Quantity)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка добавления в корзину"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Товар добавлен в корзину"})
}

func updateCartItemHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	itemID, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID"})
		return
	}
	var body struct {
		Quantity int `json:"quantity" binding:"required,min=1"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	db.Exec("UPDATE cart_items SET quantity = ? WHERE id = ? AND user_id = ?", body.Quantity, itemID, userID)
	c.JSON(http.StatusOK, gin.H{"message": "Количество обновлено"})
}

func removeCartItemHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	itemID, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID"})
		return
	}
	db.Exec("DELETE FROM cart_items WHERE id = ? AND user_id = ?", itemID, userID)
	c.JSON(http.StatusOK, gin.H{"message": "Товар удалён из корзины"})
}

func clearCartHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	db.Exec("DELETE FROM cart_items WHERE user_id = ?", userID)
	c.JSON(http.StatusOK, gin.H{"message": "Корзина очищена"})
}
