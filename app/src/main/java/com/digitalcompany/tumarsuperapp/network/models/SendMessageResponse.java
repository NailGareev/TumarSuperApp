package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class SendMessageResponse {
    @SerializedName("success") public boolean success;
    @SerializedName("message") public String  message;
    @SerializedName("id")      public int     id;
}
