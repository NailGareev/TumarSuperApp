package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MarketPurchasesResponse {
    @SerializedName("success") public boolean           success;
    @SerializedName("orders")  public List<MarketPurchase> orders;
}
