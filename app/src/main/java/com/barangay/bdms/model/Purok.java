package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;

public class Purok {
    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("createdAt")
    private String createdAt;

    public Purok() {
    }

    public Purok(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCreatedAt() { return createdAt; }
}
