package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class Tour {
    @SerializedName("id")           public int id;
    @SerializedName("location")     public String location;
    @SerializedName("hotel_name")   public String hotelName;
    @SerializedName("stars")        public int stars;
    @SerializedName("price")        public long price;
    @SerializedName("months")       public int months;
    @SerializedName("discount_percent") public int discountPercent;
    @SerializedName("original_price")   public long originalPrice;
    @SerializedName("image_url")    public String imageUrl;
    @SerializedName("is_hot")       public int isHot;

    public long getMonthlyPrice() {
        return months > 0 ? price / months : price;
    }
}
