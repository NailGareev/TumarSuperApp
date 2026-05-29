package main

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

func getFavoritesHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	rows, err := db.Query(`
		SELECT f.id, f.product_id, p.name, p.main_image, p.rating, p.review_count,
		       COALESCE(MIN(ps.price), 0) as min_price
		FROM favorites f
		JOIN products p ON p.id = f.product_id
		LEFT JOIN product_sellers ps ON ps.product_id = p.id AND ps.is_active = 1
		WHERE f.user_id = ?
		GROUP BY f.id, f.product_id, p.name, p.main_image, p.rating, p.review_count
		ORDER BY f.created_at DESC
	`, userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "DB error"})
		return
	}
	defer rows.Close()

	var favs []FavProduct
	for rows.Next() {
		var f FavProduct
		rows.Scan(&f.ID, &f.ProductID, &f.Name, &f.MainImage, &f.Rating, &f.ReviewCount, &f.MinPrice)
		favs = append(favs, f)
	}
	if favs == nil {
		favs = []FavProduct{}
	}
	c.JSON(http.StatusOK, favs)
}

func getFavoriteIDsHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	rows, err := db.Query("SELECT product_id FROM favorites WHERE user_id = ?", userID)
	if err != nil {
		c.JSON(http.StatusOK, []int64{})
		return
	}
	defer rows.Close()
	var ids []int64
	for rows.Next() {
		var id int64
		rows.Scan(&id)
		ids = append(ids, id)
	}
	if ids == nil {
		ids = []int64{}
	}
	c.JSON(http.StatusOK, ids)
}

func toggleFavoriteHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	var req struct {
		ProductID int64 `json:"product_id" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	var count int
	db.QueryRow("SELECT COUNT(*) FROM favorites WHERE user_id = ? AND product_id = ?",
		userID, req.ProductID).Scan(&count)

	if count > 0 {
		db.Exec("DELETE FROM favorites WHERE user_id = ? AND product_id = ?", userID, req.ProductID)
		c.JSON(http.StatusOK, gin.H{"favorited": false})
	} else {
		_, err := db.Exec("INSERT INTO favorites (user_id, product_id) VALUES (?, ?)",
			userID, req.ProductID)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "DB error"})
			return
		}
		c.JSON(http.StatusOK, gin.H{"favorited": true})
	}
}

func removeFavoriteHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	productID, _ := strconv.ParseInt(c.Param("product_id"), 10, 64)
	db.Exec("DELETE FROM favorites WHERE user_id = ? AND product_id = ?", userID, productID)
	c.JSON(http.StatusOK, gin.H{"message": "Removed"})
}
