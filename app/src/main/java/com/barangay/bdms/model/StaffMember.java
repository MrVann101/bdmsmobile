package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;
import java.util.UUID;

public class StaffMember extends Profile {
    @SerializedName("position")
    private String position;

    public StaffMember() {
    }

    public StaffMember(UUID id, String fullName, String email,
                       String mobileNumber, String purok, String role,
                       String position, boolean active) {
        super(id, fullName, email, mobileNumber, purok, role, active);
        this.position = position;
    }

    public boolean canProcessRequests() { return isActive(); }
    public boolean canDispatchRelief() { return isActive(); }
    public boolean isAdmin() { return "admin".equals(role); }
    public String getPosition() { return position; }
}
