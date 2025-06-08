package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("token") // Токен, который вернет бэкенд
    private String token;

    @SerializedName("userId")
    private Integer userId;

    // Геттеры
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
    public Integer getUserId() { return userId; }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", token='" + (token != null ? "present" : "null") + '\'' + // Не логируем сам токен
                ", userId=" + userId +
                '}';
    }
}