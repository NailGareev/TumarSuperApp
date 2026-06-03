package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class ReturnIdRequest {
    @SerializedName("return_id") public int returnId;

    public ReturnIdRequest(int returnId) {
        this.returnId = returnId;
    }
}
