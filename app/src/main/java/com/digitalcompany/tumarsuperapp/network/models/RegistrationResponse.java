package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class RegistrationResponse {

    @SerializedName("success") // Имена полей как в JSON ответа бэкенда
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("userId")
    private Integer userId;

    @SerializedName("token")
    private String token;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Integer getUserId() { return userId; }
    public String getToken() { return token; }

    // toString() для удобства логирования
    @Override
    public String toString() {
        return "RegistrationResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", userId=" + userId +
                '}';
    }
}