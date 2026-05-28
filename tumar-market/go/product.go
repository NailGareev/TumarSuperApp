package main

import (
	"database/sql"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

func getProductHandler(c *gin.Context) {
	id, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID товара"})
		return
	}

	var p Product
	err = db.QueryRow(
		"SELECT id, name, description, category_id, brand, main_image, rating, review_count, created_at FROM products WHERE id = ?",
		id,
	).Scan(&p.ID, &p.Name, &p.Description, &p.CategoryID, &p.Brand, &p.MainImage, &p.Rating, &p.ReviewCount, &p.CreatedAt)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Товар не найден"})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки товара"})
		return
	}

	// Category
	var cat Category
	if err := db.QueryRow("SELECT id, name, slug FROM categories WHERE id = ?", p.CategoryID).
		Scan(&cat.ID, &cat.Name, &cat.Slug); err == nil {
		p.Category = &cat
	}

	// Images
	imgRows, _ := db.Query("SELECT image_url FROM product_images WHERE product_id = ? ORDER BY sort_order", id)
	if imgRows != nil {
		defer imgRows.Close()
		for imgRows.Next() {
			var img string
			imgRows.Scan(&img)
			p.Images = append(p.Images, img)
		}
	}

	// Attributes
	attrRows, _ := db.Query("SELECT id, name, value FROM product_attributes WHERE product_id = ?", id)
	if attrRows != nil {
		defer attrRows.Close()
		for attrRows.Next() {
			var a Attribute
			attrRows.Scan(&a.ID, &a.Name, &a.Value)
			p.Attributes = append(p.Attributes, a)
		}
	}

	// Sellers
	sellerRows, _ := db.Query(`
		SELECT ps.id, ps.product_id, ps.store_id, ps.price, ps.original_price, ps.stock, ps.delivery_days, ps.is_active,
			s.id, s.name, s.logo, s.status
		FROM product_sellers ps
		JOIN stores s ON s.id = ps.store_id
		WHERE ps.product_id = ? AND ps.is_active = 1 AND s.status = 'active'
		ORDER BY ps.price ASC`, id)
	if sellerRows != nil {
		defer sellerRows.Close()
		for sellerRows.Next() {
			var ps ProductSeller
			var store Store
			sellerRows.Scan(
				&ps.ID, &ps.ProductID, &ps.StoreID, &ps.Price, &ps.OriginalPrice, &ps.Stock, &ps.DeliveryDays, &ps.IsActive,
				&store.ID, &store.Name, &store.Logo, &store.Status,
			)
			ps.Store = &store
			p.Sellers = append(p.Sellers, ps)
		}
	}

	if len(p.Sellers) > 0 {
		p.MinPrice = p.Sellers[0].Price
	}

	c.JSON(http.StatusOK, p)
}

func getProductReviewsHandler(c *gin.Context) {
	id, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Неверный ID"})
		return
	}

	rows, err := db.Query(`
		SELECT r.id, r.product_id, r.user_id, r.rating, r.comment, r.created_at, u.name
		FROM reviews r JOIN users u ON u.id = r.user_id
		WHERE r.product_id = ? ORDER BY r.created_at DESC`, id)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки отзывов"})
		return
	}
	defer rows.Close()

	var reviews []Review
	for rows.Next() {
		var r Review
		rows.Scan(&r.ID, &r.ProductID, &r.UserID, &r.Rating, &r.Comment, &r.CreatedAt, &r.UserName)
		reviews = append(reviews, r)
	}
	if reviews == nil {
		reviews = []Review{}
	}
	c.JSON(http.StatusOK, reviews)
}

func createReviewHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	var req CreateReviewRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	var existing int
	db.QueryRow("SELECT COUNT(*) FROM reviews WHERE product_id = ? AND user_id = ?", req.ProductID, userID).Scan(&existing)
	if existing > 0 {
		c.JSON(http.StatusConflict, gin.H{"error": "Вы уже оставили отзыв на этот товар"})
		return
	}

	_, err := db.Exec("INSERT INTO reviews (product_id, user_id, rating, comment) VALUES (?, ?, ?, ?)",
		req.ProductID, userID, req.Rating, req.Comment)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка создания отзыва"})
		return
	}

	// Update product rating
	db.Exec(`UPDATE products p SET
		rating = (SELECT AVG(rating) FROM reviews WHERE product_id = p.id),
		review_count = (SELECT COUNT(*) FROM reviews WHERE product_id = p.id)
		WHERE id = ?`, req.ProductID)

	c.JSON(http.StatusCreated, gin.H{"message": "Отзыв добавлен"})
}
