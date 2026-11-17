package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class DocumentTypeInfo {
    @SerializedName("documentType")
    private String documentType;

    @SerializedName("fee")
    private BigDecimal fee;

    public DocumentTypeInfo() {
    }

    public DocumentTypeInfo(String documentType, BigDecimal fee) {
        this.documentType = documentType;
        this.fee = fee;
    }

    public String getDocumentType() { return documentType; }
    public BigDecimal getFee() { return fee; }

    @Override
    public String toString() {
        return documentType != null ? documentType : "";
    }
}
