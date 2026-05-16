package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class CardResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("card")
    private CardData card;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public CardData getCard() { return card; }
    public String getMessage() { return message; }

    public static class CardData {
        @SerializedName("cardNumber")
        private String cardNumber;

        @SerializedName("expiry")
        private String expiry;

        public String getCardNumber() { return cardNumber; }
        public String getExpiry() { return expiry; }

        public String getFormattedNumber() {
            if (cardNumber == null || cardNumber.length() != 16) return cardNumber;
            return cardNumber.substring(0, 4) + " " + cardNumber.substring(4, 8)
                    + " " + cardNumber.substring(8, 12) + " " + cardNumber.substring(12, 16);
        }
    }
}
