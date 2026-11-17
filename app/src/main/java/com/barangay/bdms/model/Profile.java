package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class Profile {
    @SerializedName("id")
    protected UUID id;

    @SerializedName("username")
    protected String username;

    @SerializedName("fullName")
    protected String fullName;

    @SerializedName("email")
    protected String email;

    @SerializedName("mobileNumber")
    protected String mobileNumber;

    @SerializedName("sex")
    protected String sex;

    @SerializedName("birthdate")
    protected String birthdate;

    @SerializedName("civilStatus")
    protected String civilStatus;

    @SerializedName("citizenship")
    protected String citizenship;

    @SerializedName("birthplace")
    protected String birthplace;

    @SerializedName("purok")
    protected String purok;

    @SerializedName("address")
    protected String address;

    @SerializedName("residencyType")
    protected String residencyType;

    @SerializedName("residencyPeriod")
    protected String residencyPeriod;

    @SerializedName("sector")
    protected List<String> sector;

    @SerializedName("ctcNumber")
    protected String ctcNumber;

    @SerializedName("ctcOrNumber")
    protected String ctcOrNumber;

    @SerializedName("disabilities")
    protected List<String> disabilities;

    @SerializedName("avatarUrl")
    protected String avatarUrl;

    @SerializedName("consentAcceptedAt")
    protected OffsetDateTime consentAcceptedAt;

    @SerializedName("role")
    protected String role;

    @SerializedName("registrationMethod")
    protected String registrationMethod;

    @SerializedName("registeredBy")
    protected UUID registeredBy;

    @SerializedName("isActive")
    protected boolean active = true;

    @SerializedName("createdAt")
    protected OffsetDateTime createdAt;

    public Profile() {
    }

    public Profile(UUID id, String fullName, String email,
                   String mobileNumber, String purok, String role, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.purok = purok;
        this.role = role;
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getMobileNumber() { return mobileNumber; }
    public String getSex() { return sex; }
    public String getBirthdate() { return birthdate; }
    public String getCivilStatus() { return civilStatus; }
    public String getCitizenship() { return citizenship; }
    public String getBirthplace() { return birthplace; }
    public String getPurok() { return purok; }
    public String getAddress() { return address; }
    public String getResidencyType() { return residencyType; }
    public String getResidencyPeriod() { return residencyPeriod; }
    public List<String> getSector() { return sector; }
    public String getCtcNumber() { return ctcNumber; }
    public String getCtcOrNumber() { return ctcOrNumber; }
    public List<String> getDisabilities() { return disabilities; }
    public String getAvatarUrl() { return avatarUrl; }
    public OffsetDateTime getConsentAcceptedAt() { return consentAcceptedAt; }
    public String getRole() { return role; }
    public String getRegistrationMethod() { return registrationMethod; }
    public UUID getRegisteredBy() { return registeredBy; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public boolean isStaffOrAdmin() {
        return "staff".equals(role) || "admin".equals(role);
    }
}
