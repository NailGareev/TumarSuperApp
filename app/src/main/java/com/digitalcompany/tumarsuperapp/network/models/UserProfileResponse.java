package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal; // Используем BigDecimal для точности баланса

public class UserProfileResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("phone")
    private String phone;

    @SerializedName("balance")
    private BigDecimal balance; // Используем BigDecimal или Double

    @SerializedName("currency")
    private String currency;

    // Геттеры
    public boolean isSuccess() { return success; }
    public String getPhone() { return phone; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }

    @Override
    public String toString() {
        return "UserProfileResponse{" +
                "success=" + success +
                ", phone='" + phone + '\'' +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                '}';
    }
}