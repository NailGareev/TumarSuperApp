package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class MarketReturn {
    @SerializedName("id")          public int    id;
    @SerializedName("order_ref")   public String orderRef;
    @SerializedName("amount")      public double amount;
    @SerializedName("reason")      public String reason;
    @SerializedName("photos_json") public String photosJson;
    @SerializedName("status")      public String status;
    @SerializedName("created_at")  public String createdAt;
    @SerializedName("phone_number") public String phoneNumber;
}
