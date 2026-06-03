package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class ReturnActionResponse {
    @SerializedName("success")  public boolean success;
    @SerializedName("message")  public String  message;
    @SerializedName("refunded") public double  refunded;
}
