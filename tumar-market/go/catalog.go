package main

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

func getCategoriesHandler(c *gin.Context) {
	rows, err := db.Query("SELECT id, parent_id, name, slug, icon, image FROM categories ORDER BY id")
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки категорий"})
		return
	}
	defer rows.Close()

	var categories []Category
	for rows.Next() {
		var cat Category
		rows.Scan(&cat.ID, &cat.ParentID, &cat.Name, &cat.Slug, &cat.Icon, &cat.Image)
		categories = append(categories, cat)
	}
	if categories == nil {
		categories = []Category{}
	}
	c.JSON(http.StatusOK, categories)
}

func getProductsHandler(c *gin.Context) {
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "20"))
	categorySlug := c.Query("category")
	sortBy := c.DefaultQuery("sort", "newest")
	minPrice := c.Query("min_price")
	maxPrice := c.Query("max_price")

	if page < 1 {
		page = 1
	}
	if limit > 50 {
		limit = 50
	}
	offset := (page - 1) * limit

	query := `SELECT DISTINCT p.id, p.name, p.description, p.category_id, p.brand, p.main_image, p.rating, p.review_count, p.created_at,
		MIN(ps.price) as min_price
		FROM products p
		LEFT JOIN product_sellers ps ON ps.product_id = p.id AND ps.is_active = 1
		LEFT JOIN categories c ON c.id = p.category_id`

	var args []interface{}
	where := " WHERE 1=1"

	if categorySlug != "" {
		where += " AND (c.slug = ? OR (SELECT slug FROM categories WHERE id = c.parent_id) = ?)"
		args = append(args, categorySlug, categorySlug)
	}
	if minPrice != "" {
		where += " AND ps.price >= ?"
		args = append(args, minPrice)
	}
	if maxPrice != "" {
		where += " AND ps.price <= ?"
		args = append(args, maxPrice)
	}

	query += where + " GROUP BY p.id"

	switch sortBy {
	case "price_asc":
		query += " ORDER BY min_price ASC"
	case "price_desc":
		query += " ORDER BY min_price DESC"
	case "rating":
		query += " ORDER BY p.rating DESC"
	default:
		query += " ORDER BY p.created_at DESC"
	}

	query += " LIMIT ? OFFSET ?"
	args = append(args, limit, offset)

	rows, err := db.Query(query, args...)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки товаров"})
		return
	}
	defer rows.Close()

	var products []Product
	for rows.Next() {
		var p Product
		var minP *float64
		rows.Scan(&p.ID, &p.Name, &p.Description, &p.CategoryID, &p.Brand, &p.MainImage, &p.Rating, &p.ReviewCount, &p.CreatedAt, &minP)
		if minP != nil {
			p.MinPrice = *minP
		}
		products = append(products, p)
	}
	if products == nil {
		products = []Product{}
	}

	var total int
	countQuery := `SELECT COUNT(DISTINCT p.id) FROM products p LEFT JOIN categories c ON c.id = p.category_id` + where
	db.QueryRow(countQuery, args[:len(args)-2]...).Scan(&total)

	c.JSON(http.StatusOK, gin.H{
		"products": products,
		"total":    total,
		"page":     page,
		"limit":    limit,
		"pages":    (total + limit - 1) / limit,
	})
}

func searchProductsHandler(c *gin.Context) {
	q := c.Query("q")
	if q == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Введите поисковый запрос"})
		return
	}

	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "20"))
	if page < 1 {
		page = 1
	}
	offset := (page - 1) * limit
	search := "%" + q + "%"

	rows, err := db.Query(`
		SELECT DISTINCT p.id, p.name, p.description, p.category_id, p.brand, p.main_image, p.rating, p.review_count, p.created_at,
			MIN(ps.price) as min_price
		FROM products p
		LEFT JOIN product_sellers ps ON ps.product_id = p.id AND ps.is_active = 1
		WHERE p.name LIKE ? OR p.description LIKE ? OR p.brand LIKE ?
		GROUP BY p.id
		ORDER BY p.rating DESC, p.review_count DESC
		LIMIT ? OFFSET ?`,
		search, search, search, limit, offset,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка поиска"})
		return
	}
	defer rows.Close()

	var products []Product
	for rows.Next() {
		var p Product
		var minP *float64
		rows.Scan(&p.ID, &p.Name, &p.Description, &p.CategoryID, &p.Brand, &p.MainImage, &p.Rating, &p.ReviewCount, &p.CreatedAt, &minP)
		if minP != nil {
			p.MinPrice = *minP
		}
		products = append(products, p)
	}
	if products == nil {
		products = []Product{}
	}

	var total int
	db.QueryRow(`SELECT COUNT(DISTINCT p.id) FROM products p WHERE p.name LIKE ? OR p.description LIKE ? OR p.brand LIKE ?`,
		search, search, search).Scan(&total)

	c.JSON(http.StatusOK, gin.H{
		"products": products,
		"total":    total,
		"query":    q,
		"page":     page,
		"limit":    limit,
	})
}

func getFeaturedProductsHandler(c *gin.Context) {
	rows, err := db.Query(`
		SELECT DISTINCT p.id, p.name, p.category_id, p.brand, p.main_image, p.rating, p.review_count, p.created_at,
			MIN(ps.price) as min_price
		FROM products p
		LEFT JOIN product_sellers ps ON ps.product_id = p.id AND ps.is_active = 1
		GROUP BY p.id
		ORDER BY p.rating DESC, p.review_count DESC
		LIMIT 12`)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки"})
		return
	}
	defer rows.Close()

	var products []Product
	for rows.Next() {
		var p Product
		var minP *float64
		rows.Scan(&p.ID, &p.Name, &p.CategoryID, &p.Brand, &p.MainImage, &p.Rating, &p.ReviewCount, &p.CreatedAt, &minP)
		if minP != nil {
			p.MinPrice = *minP
		}
		products = append(products, p)
	}
	if products == nil {
		products = []Product{}
	}
	c.JSON(http.StatusOK, products)
}
