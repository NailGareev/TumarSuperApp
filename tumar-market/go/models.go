package main

import "time"

type User struct {
	ID           int64     `json:"id"`
	Email        string    `json:"email"`
	PasswordHash string    `json:"-"`
	Name         string    `json:"name"`
	Phone        string    `json:"phone"`
	Role         string    `json:"role"`
	Avatar       string    `json:"avatar"`
	CreatedAt    time.Time `json:"created_at"`
}

type Store struct {
	ID          int64     `json:"id"`
	OwnerID     int64     `json:"owner_id"`
	Name        string    `json:"name"`
	Description string    `json:"description"`
	Logo        string    `json:"logo"`
	LegalName   string    `json:"legal_name"`
	BinNumber   string    `json:"bin_number"`
	Address     string    `json:"address"`
	Phone       string    `json:"phone"`
	Email       string    `json:"email"`
	Status      string    `json:"status"`
	CreatedAt   time.Time `json:"created_at"`
}

type Category struct {
	ID       int64  `json:"id"`
	ParentID *int64 `json:"parent_id"`
	Name     string `json:"name"`
	Slug     string `json:"slug"`
	Icon     string `json:"icon"`
	Image    string `json:"image"`
}

type Product struct {
	ID          int64           `json:"id"`
	Name        string          `json:"name"`
	Description string          `json:"description"`
	CategoryID  int64           `json:"category_id"`
	Brand       string          `json:"brand"`
	MainImage   string          `json:"main_image"`
	Rating      float64         `json:"rating"`
	ReviewCount int             `json:"review_count"`
	CreatedAt   time.Time       `json:"created_at"`
	Category    *Category       `json:"category,omitempty"`
	Images      []string        `json:"images,omitempty"`
	Attributes  []Attribute     `json:"attributes,omitempty"`
	Sellers     []ProductSeller `json:"sellers,omitempty"`
	MinPrice    float64         `json:"min_price,omitempty"`
}

type ProductSeller struct {
	ID            int64     `json:"id"`
	ProductID     int64     `json:"product_id"`
	StoreID       int64     `json:"store_id"`
	Price         float64   `json:"price"`
	OriginalPrice float64   `json:"original_price"`
	Stock         int       `json:"stock"`
	DeliveryDays  int       `json:"delivery_days"`
	IsActive      bool      `json:"is_active"`
	CreatedAt     time.Time `json:"created_at"`
	Store         *Store    `json:"store,omitempty"`
}

type Attribute struct {
	ID        int64  `json:"id"`
	ProductID int64  `json:"product_id"`
	Name      string `json:"name"`
	Value     string `json:"value"`
}

type Review struct {
	ID        int64     `json:"id"`
	ProductID int64     `json:"product_id"`
	UserID    int64     `json:"user_id"`
	StoreID   *int64    `json:"store_id"`
	Rating    int       `json:"rating"`
	Comment   string    `json:"comment"`
	CreatedAt time.Time `json:"created_at"`
	UserName  string    `json:"user_name,omitempty"`
}

type CartItem struct {
	ID              int64          `json:"id"`
	UserID          int64          `json:"user_id"`
	ProductSellerID int64          `json:"product_seller_id"`
	Quantity        int            `json:"quantity"`
	ProductSeller   *ProductSeller `json:"product_seller,omitempty"`
	Product         *Product       `json:"product,omitempty"`
}

type Order struct {
	ID              int64       `json:"id"`
	UserID          int64       `json:"user_id"`
	Total           float64     `json:"total"`
	Status          string      `json:"status"`
	DeliveryAddress string      `json:"delivery_address"`
	PaymentMethod   string      `json:"payment_method"`
	CreatedAt       time.Time   `json:"created_at"`
	Items           []OrderItem `json:"items,omitempty"`
}

type OrderItem struct {
	ID              int64   `json:"id"`
	OrderID         int64   `json:"order_id"`
	ProductSellerID int64   `json:"product_seller_id"`
	ProductName     string  `json:"product_name"`
	StoreName       string  `json:"store_name"`
	Quantity        int     `json:"quantity"`
	Price           float64 `json:"price"`
	ProductImage    string  `json:"product_image,omitempty"`
}

type Notification struct {
	ID        int64     `json:"id"`
	UserID    int64     `json:"user_id"`
	OrderID   *int64    `json:"order_id,omitempty"`
	Title     string    `json:"title"`
	Message   string    `json:"message"`
	IsRead    bool      `json:"is_read"`
	CreatedAt time.Time `json:"created_at"`
}

type RegisterRequest struct {
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required,min=6"`
	Name     string `json:"name" binding:"required"`
	Phone    string `json:"phone"`
}

type LoginRequest struct {
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required"`
}

type StoreRegisterRequest struct {
	Name        string `json:"name" binding:"required"`
	Description string `json:"description"`
	LegalName   string `json:"legal_name" binding:"required"`
	BinNumber   string `json:"bin_number" binding:"required"`
	Address     string `json:"address" binding:"required"`
	Phone       string `json:"phone" binding:"required"`
	Email       string `json:"email" binding:"required,email"`
}

type CreateProductRequest struct {
	Name          string      `json:"name" binding:"required"`
	Description   string      `json:"description"`
	CategoryID    int64       `json:"category_id" binding:"required"`
	Brand         string      `json:"brand"`
	MainImage     string      `json:"main_image"`
	Images        []string    `json:"images"`
	Attributes    []Attribute `json:"attributes"`
	Price         float64     `json:"price" binding:"required"`
	OriginalPrice float64     `json:"original_price"`
	Stock         int         `json:"stock" binding:"required"`
	DeliveryDays  int         `json:"delivery_days"`
}

type AddToCartRequest struct {
	ProductSellerID int64 `json:"product_seller_id" binding:"required"`
	Quantity        int   `json:"quantity" binding:"required,min=1"`
}

type CreateOrderRequest struct {
	DeliveryAddress string `json:"delivery_address" binding:"required"`
	PaymentMethod   string `json:"payment_method" binding:"required"`
}

type CreateReviewRequest struct {
	ProductID int64  `json:"product_id" binding:"required"`
	Rating    int    `json:"rating" binding:"required,min=1,max=5"`
	Comment   string `json:"comment"`
}
