package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MarketReturnsResponse {
    @SerializedName("success") public boolean         success;
    @SerializedName("returns") public List<MarketReturn> returns;
}
