package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AuditLog {
    @SerializedName("auditLogId")
    private UUID auditLogId;

    @SerializedName("performedBy")
    private UUID performedBy;

    @SerializedName("action")
    private String action;

    @SerializedName("targetId")
    private UUID targetId;

    @SerializedName("details")
    private String details;

    @SerializedName("createdAt")
    private OffsetDateTime createdAt;

    public AuditLog(UUID auditLogId, UUID performedBy, String action,
                    UUID targetId, String details) {
        this.auditLogId = auditLogId;
        this.performedBy = performedBy;
        this.action = action;
        this.targetId = targetId;
        this.details = details;
        this.createdAt = OffsetDateTime.now();
    }

    public String describeAction() {
        return action + " on " + targetId + " by " + performedBy;
    }

    public UUID getAuditLogId() { return auditLogId; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
}
