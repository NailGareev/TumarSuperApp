package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class MarketNotification {
    @SerializedName("id")         public long    id;
    @SerializedName("user_id")    public long    userId;
    @SerializedName("order_id")   public Long    orderId;
    @SerializedName("title")      public String  title;
    @SerializedName("message")    public String  message;
    @SerializedName("is_read")    public boolean isRead;
    @SerializedName("created_at") public String  createdAt;
}
