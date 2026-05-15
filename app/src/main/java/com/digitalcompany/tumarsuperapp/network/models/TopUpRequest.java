package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class TopUpRequest {

    @SerializedName("amount")
    private BigDecimal amount;

    public TopUpRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() { return amount; }
}
