package com.barangay.bdms.model;

import java.util.UUID;

public class Resident extends Profile {

    public Resident() {
        this.role = "resident";
    }

    public Resident(UUID id, String fullName, String email,
                    String mobileNumber, String purok, String address, boolean active) {
        super(id, fullName, email, mobileNumber, purok, "resident", active);
        this.address = address;
    }

    public boolean canSubmitComplaint() { return isActive(); }
    public boolean canSubmitEmergencyReport() { return isActive(); }
}
