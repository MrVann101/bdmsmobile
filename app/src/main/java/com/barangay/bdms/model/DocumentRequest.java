package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DocumentRequest {
    @SerializedName("requestId")
    private UUID requestId;

    @SerializedName("userId")
    private UUID userId;

    @SerializedName("documentType")
    private String documentType;

    @SerializedName("controlNumber")
    private String controlNumber;

    @SerializedName("purpose")
    private String purpose;

    @SerializedName("deliveryMethod")
    private String deliveryMethod;

    @SerializedName("fee")
    private BigDecimal fee;

    @SerializedName("status")
    private String status;

    @SerializedName("paymentStatus")
    private String paymentStatus;

    @SerializedName("paymentMethod")
    private String paymentMethod;

    @SerializedName("paymentOrNumber")
    private String paymentOrNumber;

    @SerializedName("rejectionReason")
    private String rejectionReason;

    @SerializedName("staffNotes")
    private String staffNotes;

    @SerializedName("businessName")
    private String businessName;

    @SerializedName("businessAddress")
    private String businessAddress;

    @SerializedName("businessType")
    private String businessType;

    @SerializedName("isWalkIn")
    private boolean isWalkIn;

    @SerializedName("purokClearanceUrl")
    private String purokClearanceUrl;

    @SerializedName("purokClearanceVerified")
    private boolean purokClearanceVerified;

    @SerializedName("purokClearanceVerifiedBy")
    private UUID purokClearanceVerifiedBy;

    @SerializedName("purokClearanceVerifiedAt")
    private OffsetDateTime purokClearanceVerifiedAt;

    @SerializedName("certificateUrl")
    private String certificateUrl;

    @SerializedName("processedBy")
    private UUID processedBy;

    @SerializedName("createdAt")
    private OffsetDateTime createdAt;

    @SerializedName("completedAt")
    private OffsetDateTime completedAt;

    public DocumentRequest() {
    }

    public DocumentRequest(UUID requestId, UUID userId, String documentType,
                           String purpose, String deliveryMethod, BigDecimal fee) {
        this.requestId = requestId;
        this.userId = userId;
        this.documentType = documentType;
        this.purpose = purpose;
        this.deliveryMethod = deliveryMethod;
        this.fee = fee;
        this.status = "pending";
        this.paymentStatus = "pending";
        this.createdAt = OffsetDateTime.now();
    }

    public String generateControlNumber() {
        String clean = documentType != null ? documentType.replaceAll("[^A-Za-z0-9]", "") : "DOC";
        String prefix = clean.substring(0, Math.min(4, clean.length())).toUpperCase();
        this.controlNumber = prefix + "-" + System.currentTimeMillis();
        return controlNumber;
    }

    public void updateStatus(String newStatus) { this.status = newStatus; }
    public boolean isPending() { return "pending".equals(status); }

    public UUID getRequestId() { return requestId; }
    public UUID getUserId() { return userId; }
    public String getDocumentType() { return documentType; }
    public String getControlNumber() { return controlNumber; }
    public String getPurpose() { return purpose; }
    public String getDeliveryMethod() { return deliveryMethod; }
    public BigDecimal getFee() { return fee; }
    public String getStatus() { return status; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentOrNumber() { return paymentOrNumber; }
    public String getRejectionReason() { return rejectionReason; }
    public String getStaffNotes() { return staffNotes; }
    public String getBusinessName() { return businessName; }
    public String getBusinessAddress() { return businessAddress; }
    public String getBusinessType() { return businessType; }
    public boolean isWalkIn() { return isWalkIn; }
    public String getPurokClearanceUrl() { return purokClearanceUrl; }
    public boolean isPurokClearanceVerified() { return purokClearanceVerified; }
    public UUID getPurokClearanceVerifiedBy() { return purokClearanceVerifiedBy; }
    public OffsetDateTime getPurokClearanceVerifiedAt() { return purokClearanceVerifiedAt; }
    public String getCertificateUrl() { return certificateUrl; }
    public UUID getProcessedBy() { return processedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
}
