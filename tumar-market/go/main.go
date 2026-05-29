package main

import (
	"fmt"
	"log"
	"math/rand"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
)

func rootDir() string {
	exe, err := os.Executable()
	if err != nil {
		return "."
	}
	return filepath.Dir(exe)
}

func htmlFile(name string) string {
	return filepath.Join(rootDir(), "html", name)
}

func main() {
	initDB()

	r := gin.Default()

	r.Use(cors.New(cors.Config{
		AllowOrigins:     []string{"*"},
		AllowMethods:     []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowHeaders:     []string{"Origin", "Content-Type", "Authorization"},
		AllowCredentials: true,
	}))

	root := rootDir()

	// Ensure images directory exists
	imagesDir := filepath.Join(root, "images")
	os.MkdirAll(imagesDir, 0755)

	// Serve static files
	r.Static("/static", filepath.Join(root, "static"))
	r.Static("/images", imagesDir)
	r.Static("/html", filepath.Join(root, "html"))
	r.Static("/css", filepath.Join(root, "css"))
	r.Static("/js", filepath.Join(root, "js"))

	// Page routes
	r.GET("/", func(c *gin.Context) { c.File(htmlFile("index.html")) })
	r.GET("/catalog", func(c *gin.Context) { c.File(htmlFile("catalog.html")) })
	r.GET("/product/:id", func(c *gin.Context) { c.File(htmlFile("product.html")) })
	r.GET("/search", func(c *gin.Context) { c.File(htmlFile("search.html")) })
	r.GET("/login", func(c *gin.Context) { c.File(htmlFile("login.html")) })
	r.GET("/register", func(c *gin.Context) { c.File(htmlFile("register.html")) })
	r.GET("/profile", func(c *gin.Context) { c.File(htmlFile("profile.html")) })
	r.GET("/cart", func(c *gin.Context) { c.File(htmlFile("cart.html")) })
	r.GET("/checkout", func(c *gin.Context) { c.File(htmlFile("checkout.html")) })
	r.GET("/orders", func(c *gin.Context) { c.File(htmlFile("orders.html")) })
	r.GET("/favorites", func(c *gin.Context) { c.File(htmlFile("favorites.html")) })
	r.GET("/seller/dashboard", func(c *gin.Context) { c.File(htmlFile("seller-dashboard.html")) })
	r.GET("/seller/store-register", func(c *gin.Context) { c.File(htmlFile("store-register.html")) })
	r.GET("/seller/add-product", func(c *gin.Context) { c.File(htmlFile("add-product.html")) })

	// API routes
	api := r.Group("/api")

	// Auth
	auth := api.Group("/auth")
	auth.POST("/register", registerHandler)
	auth.POST("/login", loginHandler)
	auth.POST("/logout", logoutHandler)
	auth.GET("/me", authMiddleware(), getMeHandler)
	auth.PUT("/profile", authMiddleware(), updateProfileHandler)
	auth.POST("/app-auto-login", appAutoLoginHandler)

	// Catalog
	api.GET("/categories", getCategoriesHandler)
	api.GET("/products", getProductsHandler)
	api.GET("/products/featured", getFeaturedProductsHandler)
	api.GET("/products/:id", getProductHandler)
	api.GET("/products/:id/reviews", getProductReviewsHandler)
	api.GET("/search", searchProductsHandler)

	// Store
	storeGroup := api.Group("/store")
	storeGroup.Use(authMiddleware())
	storeGroup.POST("/register", registerStoreHandler)
	storeGroup.GET("/my", getMyStoreHandler)
	storeGroup.PUT("/my", updateStoreHandler)
	api.GET("/stores/:id", getStoreHandler)

	// Seller (requires seller role)
	seller := api.Group("/seller")
	seller.Use(authMiddleware(), sellerMiddleware())
	seller.GET("/dashboard", getSellerDashboardHandler)
	seller.GET("/products", getSellerProductsHandler)
	seller.POST("/products", createSellerProductHandler)
	seller.PUT("/products/:id", updateSellerProductHandler)
	seller.DELETE("/products/:id", deleteSellerProductHandler)
	seller.GET("/orders", getSellerOrdersHandler)
	seller.PUT("/orders/:id/status", updateOrderStatusHandler)
	seller.POST("/orders/:id/issue-code", issueOrderCodeHandler)
	seller.POST("/orders/:id/confirm-issue", confirmOrderIssueHandler)
	seller.POST("/products/:id/offer", addOfferToProductHandler)

	// Reviews
	api.POST("/reviews", authMiddleware(), createReviewHandler)

	// Notifications
	notifications := api.Group("/notifications")
	notifications.Use(authMiddleware())
	notifications.GET("", getNotificationsHandler)
	notifications.PUT("/read", markNotificationsReadHandler)

	// Favorites
	favGroup := api.Group("/favorites")
	favGroup.Use(authMiddleware())
	favGroup.GET("", getFavoritesHandler)
	favGroup.GET("/ids", getFavoriteIDsHandler)
	favGroup.POST("/toggle", toggleFavoriteHandler)
	favGroup.DELETE("/:product_id", removeFavoriteHandler)

	// Cart
	cart := api.Group("/cart")
	cart.Use(authMiddleware())
	cart.GET("", getCartHandler)
	cart.POST("", addToCartHandler)
	cart.PUT("/:id", updateCartItemHandler)
	cart.DELETE("/:id", removeCartItemHandler)
	cart.DELETE("", clearCartHandler)

	// Orders
	orders := api.Group("/orders")
	orders.Use(authMiddleware())
	orders.GET("", getOrdersHandler)
	orders.POST("", createOrderHandler)
	orders.GET("/:id", getOrderHandler)
	orders.PUT("/:id/cancel", cancelOrderHandler)

	// Upload endpoint
	api.POST("/upload", authMiddleware(), uploadHandler)

	// Health check
	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	log.Println("Server started on :8080")
	r.Run(":8080")
}

func uploadHandler(c *gin.Context) {
	file, err := c.FormFile("file")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Файл не найден"})
		return
	}

	// Validate content type
	ct := file.Header.Get("Content-Type")
	if !strings.HasPrefix(ct, "image/") {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Разрешены только изображения (jpg, png, webp, gif)"})
		return
	}

	// Max 10 MB
	if file.Size > 10*1024*1024 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Файл слишком большой (максимум 10 МБ)"})
		return
	}

	ext := strings.ToLower(filepath.Ext(file.Filename))
	allowed := map[string]bool{".jpg": true, ".jpeg": true, ".png": true, ".webp": true, ".gif": true}
	if !allowed[ext] {
		ext = ".jpg"
	}

	filename := fmt.Sprintf("%d_%s%s", time.Now().UnixNano(), randomStr(8), ext)
	imagesDir := filepath.Join(rootDir(), "images")
	os.MkdirAll(imagesDir, 0755)

	if err := c.SaveUploadedFile(file, filepath.Join(imagesDir, filename)); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка сохранения файла"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"url":      "/images/" + filename,
		"filename": filename,
	})
}

func randomStr(n int) string {
	const chars = "abcdefghijklmnopqrstuvwxyz0123456789"
	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	b := make([]byte, n)
	for i := range b {
		b[i] = chars[r.Intn(len(chars))]
	}
	return string(b)
}
