package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class PayResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("newBalance")
    private BigDecimal newBalance;

    @SerializedName("transactionId")
    private int transactionId;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public BigDecimal getNewBalance() { return newBalance; }
    public int getTransactionId() { return transactionId; }
}
