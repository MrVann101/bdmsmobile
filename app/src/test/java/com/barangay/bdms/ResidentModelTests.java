package com.barangay.bdms;

import com.barangay.bdms.model.Announcement;
import com.barangay.bdms.model.Complaint;
import com.barangay.bdms.model.EmergencyReport;
import com.barangay.bdms.ui.auth.OnboardingViewModel;

import org.junit.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Member 1 resident domain models and onboarding password validation.
 */
public class ResidentModelTests {

    @Test
    public void complaint_isOverdue_trueOnlyWhenPendingAndPastSla() {
        UUID complaintId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        Complaint overduePending = new Complaint(
                complaintId,
                userId,
                "Purok 1",
                "noise",
                "Loud karaoke past midnight",
                OffsetDateTime.now().minusHours(2)
        );
        assertTrue(overduePending.isOverdue());

        Complaint withinSla = new Complaint(
                UUID.randomUUID(),
                userId,
                "Purok 1",
                "infrastructure",
                "Broken street light",
                OffsetDateTime.now().plusHours(24)
        );
        assertFalse(withinSla.isOverdue());

        overduePending.resolve("Addressed by barangay tanod.", staffId);
        assertFalse(overduePending.isOverdue());
    }

    @Test
    public void complaint_escalateAndResolve_updatesPriorityAndResolutionFields() {
        UUID staffId = UUID.randomUUID();
        Complaint complaint = new Complaint(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Purok 2",
                "sanitation",
                "Uncollected garbage near canal",
                OffsetDateTime.now().plusHours(48)
        );

        assertEquals("pending", complaint.getStatus());
        assertEquals("normal", complaint.getPriority());

        complaint.escalate();
        assertEquals("high", complaint.getPriority());
        assertNotNull(complaint.getEscalatedAt());

        complaint.resolve("Scheduled pickup completed.", staffId);
        assertEquals("resolved", complaint.getStatus());
        assertEquals("Scheduled pickup completed.", complaint.getStaffResponse());
        assertEquals(staffId, complaint.getHandledBy());
        assertNotNull(complaint.getResolvedAt());
    }

    @Test
    public void announcement_targeting_handlesBarangayWideAndSpecificPuroks() {
        UUID posterId = UUID.randomUUID();
        Announcement wide = new Announcement(
                UUID.randomUUID(),
                "Typhoon Alert",
                "Prepare emergency kits.",
                "emergency",
                Collections.emptyList(),
                posterId
        );
        assertTrue(wide.isBarangayWide());
        assertTrue(wide.isTargetedTo("Purok 1"));
        assertTrue(wide.isTargetedTo(null));
        assertFalse(wide.isExpired());

        Announcement targeted = new Announcement(
                UUID.randomUUID(),
                "Water Interruption",
                "Maintenance work.",
                "general",
                Arrays.asList("Purok 2", "Purok 3"),
                posterId
        );
        assertFalse(targeted.isBarangayWide());
        assertTrue(targeted.isTargetedTo("Purok 2"));
        assertFalse(targeted.isTargetedTo("Purok 1"));
        assertFalse(targeted.isTargetedTo(null));
    }

    @Test
    public void emergencyReport_statusTransitions_updateStatusAndOpenState() {
        EmergencyReport report = new EmergencyReport(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Purok 4",
                "flood",
                5,
                Arrays.asList("food", "water")
        );

        assertEquals("reported", report.getStatus());
        assertTrue(report.isOpen());

        report.verify();
        assertEquals("verified", report.getStatus());
        assertTrue(report.isOpen());

        report.markReliefDispatched();
        assertEquals("relief_dispatched", report.getStatus());
        assertTrue(report.isOpen());
    }

    @Test
    public void onboardingViewModel_passwordPolicy_enforcesLengthUpperDigitSpecial() {
        assertTrue(OnboardingViewModel.isValidPassword("Barangay1!"));
        assertFalse(OnboardingViewModel.isValidPassword(null));
        assertFalse(OnboardingViewModel.isValidPassword("Short1!"));
        assertFalse(OnboardingViewModel.isValidPassword("barangay1!"));
        assertFalse(OnboardingViewModel.isValidPassword("Barangay!!"));
        assertFalse(OnboardingViewModel.isValidPassword("Barangay12"));
    }
}
