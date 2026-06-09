package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CurrencyRatesResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("rates")
    private List<CurrencyRate> rates;

    public boolean isSuccess() { return success; }
    public List<CurrencyRate> getRates() { return rates; }
}
