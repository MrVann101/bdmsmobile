package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class EmergencyReport {
    @SerializedName("reportId")
    private UUID reportId;

    @SerializedName("userId")
    private UUID userId;

    @SerializedName("purok")
    private String purok;

    @SerializedName("disasterType")
    private String disasterType;

    @SerializedName("householdMembersAffected")
    private int householdMembersAffected;

    @SerializedName("needs")
    private List<String> needs;

    @SerializedName("photoUrl")
    private String photoUrl;

    @SerializedName("status")
    private String status;

    @SerializedName("createdAt")
    private OffsetDateTime createdAt;

    public EmergencyReport() {
    }

    public EmergencyReport(UUID reportId, UUID userId, String purok,
                           String disasterType, int householdMembersAffected, List<String> needs) {
        this.reportId = reportId;
        this.userId = userId;
        this.purok = purok;
        this.disasterType = disasterType;
        this.householdMembersAffected = householdMembersAffected;
        this.needs = needs;
        this.status = "reported";
        this.createdAt = OffsetDateTime.now();
    }

    public void verify() { this.status = "verified"; }
    public void markReliefDispatched() { this.status = "relief_dispatched"; }
    public boolean isOpen() { return !"resolved".equals(status); }

    public UUID getReportId() { return reportId; }
    public UUID getUserId() { return userId; }
    public String getPurok() { return purok; }
    public String getDisasterType() { return disasterType; }
    public int getHouseholdMembersAffected() { return householdMembersAffected; }
    public List<String> getNeeds() { return needs; }
    public String getPhotoUrl() { return photoUrl; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
