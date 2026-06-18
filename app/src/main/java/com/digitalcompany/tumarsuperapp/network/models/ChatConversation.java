package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class ChatConversation {
    @SerializedName("other_user_id")    public int     otherUserId;
    @SerializedName("other_first_name") public String  otherFirstName;
    @SerializedName("other_last_name")  public String  otherLastName;
    @SerializedName("other_phone")      public String  otherPhone;
    @SerializedName("last_message")     public String  lastMessage;
    @SerializedName("last_time")        public String  lastTime;
    @SerializedName("last_amount")      public Double  lastAmount;
    @SerializedName("unread_count")     public int     unreadCount;
    @SerializedName("is_incoming")      public boolean isIncoming;
}
