package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;
import java.util.UUID;

public class CertificateTemplate {
    @SerializedName("templateId")
    private UUID templateId;

    @SerializedName("documentType")
    private String documentType;

    @SerializedName("sealUrl")
    private String sealUrl;

    @SerializedName("signatureUrl")
    private String signatureUrl;

    @SerializedName("officialName")
    private String officialName;

    @SerializedName("officialTitle")
    private String officialTitle;

    public CertificateTemplate(UUID templateId, String documentType,
                               String sealUrl, String signatureUrl,
                               String officialName, String officialTitle) {
        this.templateId = templateId;
        this.documentType = documentType;
        this.sealUrl = sealUrl;
        this.signatureUrl = signatureUrl;
        this.officialName = officialName;
        this.officialTitle = officialTitle;
    }

    public boolean isConfigured() {
        return sealUrl != null && signatureUrl != null;
    }

    public UUID getTemplateId() { return templateId; }
    public String getDocumentType() { return documentType; }
    public String getOfficialName() { return officialName; }
    public String getOfficialTitle() { return officialTitle; }
}
