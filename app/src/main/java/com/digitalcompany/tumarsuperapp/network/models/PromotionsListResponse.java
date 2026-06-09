package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PromotionsListResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("promotions")
    private List<Promotion> promotions;

    public boolean isSuccess()            { return success; }
    public List<Promotion> getPromotions(){ return promotions; }
}
