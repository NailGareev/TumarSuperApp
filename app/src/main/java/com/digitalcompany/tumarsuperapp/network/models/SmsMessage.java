package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class SmsMessage {
    @SerializedName("id")          public int    id;
    @SerializedName("sender_id")   public int    senderId;
    @SerializedName("receiver_id") public int    receiverId;
    @SerializedName("message")     public String message;
    @SerializedName("created_at")  public String createdAt;
}
