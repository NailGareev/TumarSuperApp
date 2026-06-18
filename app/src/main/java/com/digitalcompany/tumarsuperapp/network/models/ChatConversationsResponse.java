package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatConversationsResponse {
    @SerializedName("success")       public boolean             success;
    @SerializedName("conversations") public List<ChatConversation> conversations;
}
