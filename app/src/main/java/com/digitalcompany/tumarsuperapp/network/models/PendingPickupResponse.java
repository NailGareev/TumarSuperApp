package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class PendingPickupResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("pickup_code")
    private String pickupCode;

    @SerializedName("order_ref")
    private String orderRef;

    @SerializedName("amount")
    private double amount;

    public boolean isSuccess() { return success; }
    public String getPickupCode() { return pickupCode; }
    public String getOrderRef() { return orderRef; }
    public double getAmount() { return amount; }
}
