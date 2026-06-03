package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class ReturnStatusUpdateRequest {
    @SerializedName("return_id") public int    returnId;
    @SerializedName("status")    public String status;

    public ReturnStatusUpdateRequest(int returnId, String status) {
        this.returnId = returnId;
        this.status   = status;
    }
}
