package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class MarketPayResponse {
    @SerializedName("success")  public boolean success;
    @SerializedName("orderRef") public String  orderRef;
    @SerializedName("message")  public String  message;
}
