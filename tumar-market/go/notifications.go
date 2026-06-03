package main

import (
	"database/sql"
	"net/http"

	"github.com/gin-gonic/gin"
)

func createNotification(userID int64, orderID *int64, title, message string) error {
	var orderValue interface{}
	if orderID != nil {
		orderValue = *orderID
	}
	_, err := db.Exec(
		"INSERT INTO notifications (user_id, order_id, title, message) VALUES (?, ?, ?, ?)",
		userID, orderValue, title, message,
	)
	return err
}

func getNotificationsHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")

	rows, err := db.Query(`
		SELECT id, user_id, order_id, title, message, is_read, created_at
		FROM notifications
		WHERE user_id = ?
		ORDER BY created_at DESC
		LIMIT 50`, userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка загрузки уведомлений"})
		return
	}
	defer rows.Close()

	var notifications []Notification
	for rows.Next() {
		var n Notification
		var orderID sql.NullInt64
		var isRead int
		if err := rows.Scan(&n.ID, &n.UserID, &orderID, &n.Title, &n.Message, &isRead, &n.CreatedAt); err != nil {
			continue
		}
		if orderID.Valid {
			n.OrderID = &orderID.Int64
		}
		n.IsRead = isRead == 1
		notifications = append(notifications, n)
	}
	if notifications == nil {
		notifications = []Notification{}
	}
	c.JSON(http.StatusOK, notifications)
}

func markNotificationsReadHandler(c *gin.Context) {
	userID := c.GetInt64("user_id")

	_, err := db.Exec("UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0", userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Ошибка обновления уведомлений"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Уведомления обновлены"})
}
