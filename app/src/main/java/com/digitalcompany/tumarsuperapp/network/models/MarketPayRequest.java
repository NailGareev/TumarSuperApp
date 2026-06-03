package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class MarketPayRequest {
    @SerializedName("amount")  public double amount;
    @SerializedName("address") public String address;
    @SerializedName("items")   public String items;

    public MarketPayRequest(double amount, String address, String items) {
        this.amount  = amount;
        this.address = address;
        this.items   = items;
    }
}
