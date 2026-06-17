package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class UserLookupResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastNameInitial")
    private String lastNameInitial;

    @SerializedName("message")
    private String message;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    public boolean isSuccess() { return success; }
    public String getFirstName() { return firstName; }
    public String getLastNameInitial() { return lastNameInitial; }
    public String getMessage() { return message; }
    public String getAvatarUrl() { return avatarUrl; }

    public String getDisplayName() {
        if (firstName == null) return "";
        return firstName + " " + (lastNameInitial != null ? lastNameInitial : "");
    }
}
