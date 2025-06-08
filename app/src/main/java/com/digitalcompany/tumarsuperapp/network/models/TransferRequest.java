package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal; // Используем BigDecimal для суммы

public class TransferRequest {

    @SerializedName("recipientPhone")
    private String recipientPhone;

    @SerializedName("amount")
    private BigDecimal amount; // Сумму передаем как BigDecimal или Double

    public TransferRequest(String recipientPhone, BigDecimal amount) {
        this.recipientPhone = recipientPhone;
        this.amount = amount;
    }

    // Геттеры (опционально)
    public String getRecipientPhone() { return recipientPhone; }
    public BigDecimal getAmount() { return amount; }
}