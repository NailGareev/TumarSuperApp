package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class CurrencyRate {

    @SerializedName("currency_code")
    private String currencyCode;

    @SerializedName("rate")
    private double rate;

    @SerializedName("date")
    private String date;

    public String getCurrencyCode() { return currencyCode; }
    public double getRate() { return rate; }
    public String getDate() { return date; }
}
