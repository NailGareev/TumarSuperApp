package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class SendMessageRequest {
    @SerializedName("receiverId") public int    receiverId;
    @SerializedName("message")    public String message;

    public SendMessageRequest(int receiverId, String message) {
        this.receiverId = receiverId;
        this.message    = message;
    }
}
