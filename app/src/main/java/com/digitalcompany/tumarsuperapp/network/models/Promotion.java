package com.digitalcompany.tumarsuperapp.network.models;

import com.google.gson.annotations.SerializedName;

public class Promotion {
    @SerializedName("id")       private int id;
    @SerializedName("tag")      private String tag;
    @SerializedName("title")    private String title;
    @SerializedName("subtitle") private String subtitle;
    @SerializedName("badge")    private String badge;
    @SerializedName("hot")      private int hot;
    @SerializedName("stat1_value") private String stat1Value;
    @SerializedName("stat1_label") private String stat1Label;
    @SerializedName("stat2_value") private String stat2Value;
    @SerializedName("stat2_label") private String stat2Label;
    @SerializedName("stat3_value") private String stat3Value;
    @SerializedName("stat3_label") private String stat3Label;
    @SerializedName("description") private String description;
    @SerializedName("terms")       private String terms;

    public int getId()           { return id; }
    public String getTag()       { return tag; }
    public String getTitle()     { return title; }
    public String getSubtitle()  { return subtitle; }
    public String getBadge()     { return badge; }
    public boolean isHot()       { return hot == 1; }
    public String getStat1Value(){ return stat1Value; }
    public String getStat1Label(){ return stat1Label; }
    public String getStat2Value(){ return stat2Value; }
    public String getStat2Label(){ return stat2Label; }
    public String getStat3Value(){ return stat3Value; }
    public String getStat3Label(){ return stat3Label; }
    public String getDescription(){ return description; }
    public String getTerms()     { return terms; }
}
