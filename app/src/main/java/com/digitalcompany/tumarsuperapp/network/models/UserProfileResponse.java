package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class UserProfileResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("balance")
    private BigDecimal balance;

    @SerializedName("currency")
    private String currency;

    @SerializedName("operationCount")
    private int operationCount;

    public boolean isSuccess() { return success; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public int getOperationCount() { return operationCount; }
}
