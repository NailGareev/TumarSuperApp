package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class TransferRequest {

    @SerializedName("recipientPhone")
    private String recipientPhone;

    @SerializedName("amount")
    private BigDecimal amount;

    @SerializedName("description")
    private String description;

    public TransferRequest(String recipientPhone, BigDecimal amount, String description) {
        this.recipientPhone = recipientPhone;
        this.amount = amount;
        this.description = (description != null && !description.trim().isEmpty())
                ? description.trim() : null;
    }

    public String getRecipientPhone() { return recipientPhone; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
}
