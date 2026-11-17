package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class Announcement {
    @SerializedName("announcementId")
    private UUID announcementId;

    @SerializedName("title")
    private String title;

    @SerializedName("body")
    private String body;

    @SerializedName("category")
    private String category;

    @SerializedName("targetPurok")
    private List<String> targetPurok;

    @SerializedName("postedBy")
    private UUID postedBy;

    @SerializedName("attachmentUrl")
    private String attachmentUrl;

    @SerializedName("createdAt")
    private OffsetDateTime createdAt;

    @SerializedName("expiresAt")
    private OffsetDateTime expiresAt;

    public Announcement() {
    }

    public Announcement(UUID announcementId, String title, String body,
                        String category, List<String> targetPurok, UUID postedBy) {
        this.announcementId = announcementId;
        this.title = title;
        this.body = body;
        this.category = category;
        this.targetPurok = targetPurok;
        this.postedBy = postedBy;
        this.createdAt = OffsetDateTime.now();
    }

    public boolean isBarangayWide() {
        return targetPurok == null || targetPurok.isEmpty();
    }

    public boolean isTargetedTo(String purok) {
        return isBarangayWide() || (purok != null && targetPurok.contains(purok));
    }

    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }

    public UUID getAnnouncementId() { return announcementId; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getCategory() { return category; }
    public List<String> getTargetPurok() { return targetPurok; }
    public UUID getPostedBy() { return postedBy; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
