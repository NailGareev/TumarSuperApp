package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class AvatarUploadResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getMessage() { return message; }
}
