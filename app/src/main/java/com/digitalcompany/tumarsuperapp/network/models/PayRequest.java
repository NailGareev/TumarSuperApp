package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class PayRequest {

    @SerializedName("service")
    private String service;

    @SerializedName("accountNumber")
    private String accountNumber;

    @SerializedName("amount")
    private BigDecimal amount;

    public PayRequest(String service, String accountNumber, BigDecimal amount) {
        this.service = service;
        this.accountNumber = accountNumber;
        this.amount = amount;
    }

    public String getService() { return service; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getAmount() { return amount; }
}
