package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatMessagesResponse {
    @SerializedName("success") public boolean        success;
    @SerializedName("items")   public List<ChatItem> items;
}
