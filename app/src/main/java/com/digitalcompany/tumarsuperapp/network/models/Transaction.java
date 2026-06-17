package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.util.Date; // Или используйте String, если timestamp приходит как строка

public class Transaction implements java.io.Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("sender_id")
    private int senderId;

    @SerializedName("recipient_id")
    private int recipientId;

    @SerializedName("amount")
    private BigDecimal amount; // Используем BigDecimal для точности

    @SerializedName("currency")
    private String currency;

    @SerializedName("transaction_type")
    private String transactionType; // 'TRANSFER', 'PAYMENT', 'TOPUP'

    @SerializedName("description")
    private String description;

    @SerializedName("timestamp")
    private Date timestamp;

    // Поля из JOIN с users (имена/телефоны могут быть null, если JOIN не удался или user удален)
    @SerializedName("sender_first_name")
    private String senderFirstName;

    @SerializedName("sender_last_name")
    private String senderLastName;

    @SerializedName("sender_phone")
    private String senderPhone;

    @SerializedName("recipient_first_name")
    private String recipientFirstName;

    @SerializedName("recipient_last_name")
    private String recipientLastName;

    @SerializedName("recipient_phone")
    private String recipientPhone;

    @SerializedName("sender_avatar_url")
    private String senderAvatarUrl;

    @SerializedName("recipient_avatar_url")
    private String recipientAvatarUrl;

    // Геттеры для доступа к полям (обязательны или сделайте поля public)
    public int getId() { return id; }
    public int getSenderId() { return senderId; }
    public int getRecipientId() { return recipientId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getTransactionType() { return transactionType; }
    public String getDescription() { return description; }
    public Date getTimestamp() { return timestamp; }
    public String getSenderFirstName() { return senderFirstName; }
    public String getSenderLastName() { return senderLastName; }
    public String getSenderPhone() { return senderPhone; }
    public String getRecipientFirstName() { return recipientFirstName; }
    public String getRecipientLastName() { return recipientLastName; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getSenderAvatarUrl() { return senderAvatarUrl; }
    public String getRecipientAvatarUrl() { return recipientAvatarUrl; }

    // toString() для отладки (опционально)
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", senderId=" + senderId +
                ", recipientId=" + recipientId +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", timestamp=" + timestamp +
                // Добавим имена для наглядности
                ", senderName='" + senderFirstName + " " + senderLastName + '\'' +
                ", recipientName='" + recipientFirstName + " " + recipientLastName + '\'' +
                '}';
    }
}