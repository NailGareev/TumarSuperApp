package main

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
)

func registerHandler(c *gin.Context) {
	var req RegisterRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	var existing int
	db.QueryRow("SELECT COUNT(*) FROM users WHERE email = ?", req.Email).Scan(&existing)
	if existing > 0 {
		c.JSON(http.StatusConflict, gin.H{"error": "Email уже используется"})
		return
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка сервера"})
		return
	}

	result, err := db.Exec(
		"INSERT INTO users (email, password_hash, name, phone) VALUES (?, ?, ?, ?)",
		req.Email, string(hash), req.Name, req.Phone,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка создания пользователя"})
		return
	}

	userID, _ := result.LastInsertId()
	token, err := generateToken(userID, req.Email, "user")
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка генерации токена"})
		return
	}

	c.SetCookie("token", token, 7*24*3600, "/", "", false, true)
	c.JSON(http.StatusCreated, gin.H{
		"token": token,
		"user": gin.H{
			"id":    userID,
			"email": req.Email,
			"name":  req.Name,
			"role":  "user",
		},
	})
}

func loginHandler(c *gin.Context) {
	var req LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	var user User
	err := db.QueryRow(
		"SELECT id, email, password_hash, name, phone, role, avatar FROM users WHERE email = ?",
		req.Email,
	).Scan(&user.ID, &user.Email, &user.PasswordHash, &user.Name, &user.Phone, &user.Role, &user.Avatar)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Неверный email или пароль"})
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)); err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Неверный email или пароль"})
		return
	}

	token, err := generateToken(user.ID, user.Email, user.Role)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка генерации токена"})
		return
	}

	c.SetCookie("token", token, 7*24*3600, "/", "", false, true)
	c.JSON(http.StatusOK, gin.H{
		"token": token,
		"user": gin.H{
			"id":     user.ID,
			"email":  user.Email,
			"name":   user.Name,
			"phone":  user.Phone,
			"role":   user.Role,
			"avatar": user.Avatar,
		},
	})
}

func getMeHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	var user User
	err := db.QueryRow(
		"SELECT id, email, name, phone, role, avatar, created_at FROM users WHERE id = ?",
		userID,
	).Scan(&user.ID, &user.Email, &user.Name, &user.Phone, &user.Role, &user.Avatar, &user.CreatedAt)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Пользователь не найден"})
		return
	}
	c.JSON(http.StatusOK, user)
}

func updateProfileHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")
	var body struct {
		Name   string `json:"name"`
		Phone  string `json:"phone"`
		Avatar string `json:"avatar"`
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	_, err := db.Exec("UPDATE users SET name=?, phone=?, avatar=?, updated_at=NOW() WHERE id=?",
		body.Name, body.Phone, body.Avatar, userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка обновления профиля"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Профиль обновлён"})
}

func logoutHandler(c *gin.Context) {
	c.SetCookie("token", "", -1, "/", "", false, true)
	c.JSON(http.StatusOK, gin.H{"message": "Выход выполнен"})
}

func generateToken(userID int64, email, role string) (string, error) {
	claims := &Claims{
		UserID: userID,
		Email:  email,
		Role:   role,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(7 * 24 * time.Hour)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(jwtSecret)
}
