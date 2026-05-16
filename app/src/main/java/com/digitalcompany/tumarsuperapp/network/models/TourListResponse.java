package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TourListResponse {
    @SerializedName("success") public boolean success;
    @SerializedName("tours")   public List<Tour> tours;
}
