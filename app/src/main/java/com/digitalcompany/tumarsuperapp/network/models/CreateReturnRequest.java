package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreateReturnRequest {
    @SerializedName("order_ref") public String     orderRef;
    @SerializedName("amount")    public double     amount;
    @SerializedName("reason")    public String     reason;
    @SerializedName("photos")    public List<String> photos;

    public CreateReturnRequest(String orderRef, double amount, String reason, List<String> photos) {
        this.orderRef = orderRef;
        this.amount   = amount;
        this.reason   = reason;
        this.photos   = photos;
    }
}
