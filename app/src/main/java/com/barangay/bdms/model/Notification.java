package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Notification {
    @SerializedName("notificationId")
    private UUID notificationId;

    @SerializedName("userId")
    private UUID userId;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("type")
    private String type;

    @SerializedName("isRead")
    private boolean isRead;

    @SerializedName("relatedId")
    private UUID relatedId;

    @SerializedName("deliveryStatus")
    private String deliveryStatus;

    @SerializedName("deliveryAttempts")
    private int deliveryAttempts;

    @SerializedName("lastAttemptAt")
    private OffsetDateTime lastAttemptAt;

    @SerializedName("failureReason")
    private String failureReason;

    @SerializedName("createdAt")
    private OffsetDateTime createdAt;

    public Notification() {
    }

    public Notification(UUID notificationId, UUID userId, String title,
                        String message, String type) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
        this.deliveryStatus = "pending";
        this.createdAt = OffsetDateTime.now();
    }

    public void markAsRead() { this.isRead = true; }

    public UUID getNotificationId() { return notificationId; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public UUID getRelatedId() { return relatedId; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public int getDeliveryAttempts() { return deliveryAttempts; }
    public OffsetDateTime getLastAttemptAt() { return lastAttemptAt; }
    public String getFailureReason() { return failureReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
