package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class ChatItem {
    @SerializedName("type")        public String type; // "TRANSFER" or "SMS"
    @SerializedName("id")          public int    id;
    @SerializedName("sender_id")   public int    senderId;
    @SerializedName("receiver_id") public int    receiverId;
    @SerializedName("amount")      public Double amount;
    @SerializedName("description") public String description;
    @SerializedName("message")     public String message;
    @SerializedName("timestamp")   public String timestamp;
}
