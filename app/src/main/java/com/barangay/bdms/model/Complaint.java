package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class Complaint {
    @SerializedName("complaintId")
    private UUID complaintId;

    @SerializedName("userId")
    private UUID userId;

    @SerializedName("purok")
    private String purok;

    @SerializedName("category")
    private String category;

    @SerializedName("description")
    private String description;

    @SerializedName("photoUrls")
    private List<String> photoUrls;

    @SerializedName("status")
    private String status;

    @SerializedName("priority")
    private String priority;

    @SerializedName("slaDueAt")
    private OffsetDateTime slaDueAt;

    @SerializedName("escalatedAt")
    private OffsetDateTime escalatedAt;

    @SerializedName("handledBy")
    private UUID handledBy;

    @SerializedName("staffResponse")
    private String staffResponse;

    @SerializedName("createdAt")
    private OffsetDateTime createdAt;

    @SerializedName("resolvedAt")
    private OffsetDateTime resolvedAt;

    public Complaint() {
    }

    public Complaint(UUID complaintId, UUID userId, String purok,
                     String category, String description, OffsetDateTime slaDueAt) {
        this.complaintId = complaintId;
        this.userId = userId;
        this.purok = purok;
        this.category = category;
        this.description = description;
        this.status = "pending";
        this.priority = "normal";
        this.slaDueAt = slaDueAt;
        this.createdAt = OffsetDateTime.now();
    }

    public boolean isOverdue() {
        return "pending".equals(status) && slaDueAt != null && OffsetDateTime.now().isAfter(slaDueAt);
    }

    public void escalate() {
        this.priority = "high";
        this.escalatedAt = OffsetDateTime.now();
    }

    public void resolve(String response, UUID staffId) {
        this.status = "resolved";
        this.handledBy = staffId;
        this.staffResponse = response;
        this.resolvedAt = OffsetDateTime.now();
    }

    public UUID getComplaintId() { return complaintId; }
    public UUID getUserId() { return userId; }
    public String getPurok() { return purok; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public List<String> getPhotoUrls() { return photoUrls; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public OffsetDateTime getSlaDueAt() { return slaDueAt; }
    public OffsetDateTime getEscalatedAt() { return escalatedAt; }
    public UUID getHandledBy() { return handledBy; }
    public String getStaffResponse() { return staffResponse; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }
}
