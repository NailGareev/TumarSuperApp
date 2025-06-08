package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class TransferResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    // Геттеры
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "TransferResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}