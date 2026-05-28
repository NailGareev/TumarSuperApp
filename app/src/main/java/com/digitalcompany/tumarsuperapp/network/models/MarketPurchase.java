package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class MarketPurchase {
    @SerializedName("id")         public int    id;
    @SerializedName("order_ref")  public String orderRef;
    @SerializedName("amount")     public double amount;
    @SerializedName("items_json") public String itemsJson;
    @SerializedName("address")    public String address;
    @SerializedName("status")     public String status;
    @SerializedName("created_at") public String createdAt;
}
