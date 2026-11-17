# Member 2 Implementation Plan — Staff Role (S1–S7, S10)

**Owner:** Member 2 (Barangay Staff Back-Office Operations)  
**Screens Covered:** `S1` (Staff Dashboard), `S2` (Document Request Queue & Detail), `S3` (Complaint Queue), `S4` (Announcement Composer), `S5` (Emergency Response Dashboard), `S6` (Emergency Report Detail), `S7` (Relief Dispatch), `S10` (Walk-In Resident Service — 3 steps)  
**Authoritative References:** [BDMS_Mobile_Database_Structure.md](BDMS_Mobile_Database_Structure.md) (data), [BDMS_Mobile_Build_Plan.md](BDMS_Mobile_Build_Plan.md) (behavior/API), [BDMS_Mobile_Gemini_CLI_Prompts.md](BDMS_Mobile_Gemini_CLI_Prompts.md) (screen table)

---

## 1. Current State Audit (What Exists vs. What Is Broken)

| Screen / File | Current State in Repo | Problems to Fix |
| --- | --- | --- |
| **S1** `StaffMainActivity.java`, `StaffNavHelper.java`, `screen_staff_s1_staff_dashboard.xml` | `btnWalkIn` only shows a Toast; stat cards show static hardcoded numbers; bottom nav hardcodes 5 tabs including Admin/More | Convert `StaffMainActivity` to host `nav_graph_staff.xml` (coordinate 4-tab vs. 5-tab menu builder with Member 3); implement `StaffDashboardFragment` fetching real counts from list endpoints and wiring `"Walk-In"` → S10. |
| **S2** `StaffRequestQueueActivity.java`, `screen_staff_s2_request_queue.xml` | Fetches `getDocumentRequests(null)` and ignores the response; static mockup cards; **no detail screen exists** | Migrate to `StaffRequestQueueFragment` (`TabLayout` + `RecyclerView` + `StatusBadge`) and create `StaffRequestDetailFragment` + `screen_staff_request_detail.xml` (clearance lightbox + Verify/Reject, `"Exempted — Walk-In"` badge, payment panel, `staffNotes`, status progression). |
| **S3** `StaffComplaintQueueActivity.java`, `screen_staff_s3_complaint_queue.xml` | Fetches `getComplaints()` and discards response; static mockup XML | Migrate to `StaffComplaintQueueFragment` with `RecyclerView` (red left-edge bar for `high`/overdue), `SwipeRefreshLayout`, inline/sheet detail for `staffResponse` + `Reviewing` / `Resolve` / `Dismiss` (`PATCH /api/complaints/:id/resolve`). |
| **S4** `StaffAnnouncementActivity.java`, `screen_staff_s4_announcement_new.xml` | Only inflates XML and calls `StaffNavHelper` | Migrate to `AnnouncementComposerFragment`; load `targetPurok` `ChipGroup` live from `GET /api/puroks`; wire `POST /api/announcements`. |
| **S5** `StaffEmergencyDashboardActivity.java`, `screen_staff_s5_emergency_dashboard.xml` | Only inflates XML and calls `StaffNavHelper` | Migrate to `EmergencyResponseDashboardFragment`; fetch `GET /api/emergency-reports/grouped-by-purok`, build Purok filter chips + counts dynamically from response keys, support inline Verify (`PATCH /api/emergency-reports/:id/verify`), tap card → S6. |
| **S6** `StaffEmergencyDetailActivity.java`, `screen_staff_s6_emergency_detail.xml` | Local-only UI toggle on Verify; Dispatch button only shows a Toast | Migrate to `EmergencyReportDetailFragment` bound to `GET /api/emergency-reports/:id`; wire `PATCH /api/emergency-reports/:id/verify`; keep `"Dispatch Relief"` disabled until `status == "verified"`; tap → S7 with `reportId` & `purok`. |
| **S7** `StaffReliefDispatchActivity.java`, `screen_staff_s7_relief_dispatch.xml` | Static layout; Authorize button only shows Toast and calls `finish()` | Migrate to `ReliefDispatchFragment`; fetch `GET /api/relief-distributions?reportId=` first, grey out & disable already-served residents, enter `itemsGiven`, and submit `POST /api/relief-distributions`. |
| **S10** `WalkInSearchActivity.java`, `screen_staff_s10_walkin.xml`, `app/src/main/res/staff/layout/screen_staff_walkin_*.xml` | `WalkInSearchActivity` only shows a Toast; 3 walk-in XMLs sit in an invalid `res/staff/layout/` folder and have no view IDs or Java classes | Move/merge `screen_staff_walkin_search.xml`, `screen_staff_walkin_register.xml`, `screen_staff_walkin_request.xml` into `app/src/main/res/layout/`; build `WalkInSearchFragment`, `WalkInRegisterFragment`, `WalkInRequestFragment` wired to real walk-in endpoints + one-time Credential Slip. |

---

## 2. Shared Models & API Contracts Owned by Member 2

Coordinate with Member 1 & Member 3 on `model/` additions (keep existing constructors and `@SerializedName` annotations intact):
- **`DocumentRequest.java`:** Add `staffNotes`, `purokClearanceVerifiedBy`, `processedBy`, `paymentStatus`, `paymentMethod`, `paymentOrNumber`, `rejectionReason`, `businessName`, `businessAddress`, `businessType`, `isWalkIn`, `purokClearanceUrl`, `purokClearanceVerified`, `purokClearanceVerifiedAt`, `certificateUrl`, `completedAt`, plus optional joined resident display fields (e.g., `residentName`, `residentPurok` if returned by staff queue query — never sensitive fields).
- **`ReliefDistribution.java`:** Ensure getters exist for `reportId`, `purok`, `itemsGiven`, `distributedBy`, `createdAt`.
- **`StaffMember.java`:** Keep `canProcessRequests()`, `canDispatchRelief()`, `isAdmin()` aligned with `SessionManager`.

> **Sensitive-Data Privacy Rule:** Never return or render `passwordHash`, `ctcNumber`, `ctcOrNumber`, or `disabilities` in `S2`, `S7`, or `S10` resident lists/search results.

---

## 3. Screen-by-Screen Implementation Specs (S1–S7, S10)

### S1 — Staff Dashboard (`StaffDashboardFragment` / `screen_staff_s1_staff_dashboard.xml`)
- **Endpoints:** Aggregated calls to `GET /api/document-requests`, `GET /api/complaints`, `GET /api/emergency-reports` (no dedicated stats endpoint).
- **Behavior:**
  - `SwipeRefreshLayout` wraps the dashboard.
  - Compute and bind live KPI card counts client-side:
    - Pending Document Requests (`status == "pending"` or unverified clearance) → tap jumps to S2 (`pending` tab).
    - Pending / Overdue Complaints (`status == "pending"` and/or `priority == "high"`) → tap jumps to S3.
    - Open Emergency Reports (`status != "resolved"`) → tap jumps to S5.
  - Top bar `"Walk-In"` quick-action button navigates to `WalkInSearchFragment` (`S10`).
  - Also include `NotificationBellView` in toolbar.

### S2 — Document Request Queue & Detail (`StaffRequestQueueFragment`, `StaffRequestDetailFragment`)
- **Layouts:** `screen_staff_s2_request_queue.xml` + **new** `screen_staff_request_detail.xml`.
- **Endpoints:** `GET /api/document-requests` (`?status=`), `GET /api/document-requests/:id`, `PATCH /api/document-requests/:id/verify-clearance`, `PATCH /api/document-requests/:id`.
- **Queue Behavior (`StaffRequestQueueFragment`):**
  - `TabLayout` tabs: `Pending`, `Processing`, `Ready`, `Completed`, `Rejected` (plus optional `All`).
  - `SwipeRefreshLayout` + `RecyclerView` + `StatusBadge` + `EmptyStateView` + `LoadingOverlay`.
  - Show a `"Walk-In"` badge on walk-in rows and `"Clearance Unverified"` indicator on unverified online rows.
  - Tapping a row navigates to `StaffRequestDetailFragment` passing `requestId`.
- **Detail Behavior (`StaffRequestDetailFragment`):**
  - **Hard Rule — Walk-In Exemption:**
    - If `isWalkIn == true`: hide the clearance photo card and Verify/Reject buttons entirely; display an `"Exempted (Walk-In)"` badge instead.
    - If `isWalkIn == false`: display the uploaded `purokClearanceUrl` image (tappable to expand in a fullscreen/lightbox dialog) plus `"Verify Clearance"` and `"Reject Clearance"` buttons (`PATCH /api/document-requests/:id/verify-clearance`).
  - **Status Controls:** Allow advancing `pending → processing → ready → completed`, or `rejected` (require a non-empty `rejectionReason` dialog before submitting `rejected`). Disable moving past `pending` on online requests until `purokClearanceVerified == true`.
  - **Payment Panel:** Dropdown for `paymentMethod` (`cash`, `debit_card`, `credit_card`, `waived`, `demo`), `paymentOrNumber` input, and `"Confirm Payment"` button calling `PATCH /api/document-requests/:id` (sets `paymentStatus = "confirmed"`).
  - **Internal Notes:** Multiline `staffNotes` input saved via `PATCH /api/document-requests/:id`.
  - When status moves to `ready`, show certificate readiness / allow previewing or downloading generated PDF (`GET /api/document-requests/:id/certificate`).

### S3 — Complaint Queue (`StaffComplaintQueueFragment` / `screen_staff_s3_complaint_queue.xml`)
- **Endpoints:** `GET /api/complaints`, `PATCH /api/complaints/:id/resolve`.
- **Behavior:**
  - Server returns complaints sorted by `ORDER BY (priority = 'high') DESC, "slaDueAt" ASC` (client can also re-sort via `PriorityQueue` / comparator after local edits).
  - `SwipeRefreshLayout` + `RecyclerView` + `StatusBadge` + `EmptyStateView` + `LoadingOverlay`.
  - **Visual Rule:** Render a solid red left-edge accent bar (`@color/statusRejectedText`) on rows where `priority == "high"` or `complaint.isOverdue()`.
  - Tapping a card opens an inline expansion or bottom sheet/detail dialog showing full `description`, attached `photoUrls`, `purok`, `slaDueAt`, multiline `staffResponse` input, and `"Mark Reviewing"`, `"Resolve"`, and `"Dismiss"` buttons:
    - `"Mark Reviewing"` calls `PATCH /api/complaints/:id/resolve` with `status: "reviewing"`.
    - `"Resolve"` and `"Dismiss"` require a non-empty `staffResponse` before submitting.

### S4 — Announcement Composer (`AnnouncementComposerFragment` / `screen_staff_s4_announcement_new.xml`)
- **Endpoints:** `GET /api/puroks`, `POST /api/announcements`.
- **Behavior:**
  - Inputs: `title`, `body`, `category` dropdown (`general`, `event`, `emergency`), optional `expiresAt` date picker, and `"Barangay-Wide"` toggle / Purok multi-select `ChipGroup`.
  - Populate the Purok `ChipGroup` live from `GET /api/puroks` (never hardcode Purok names). Empty selection = barangay-wide.
  - Validate non-empty `title` and `body`; on `POST /api/announcements` success, confirm broadcast & fan-out notification count and navigate back or reset form.

### S5 — Emergency Response Dashboard (`EmergencyResponseDashboardFragment` / `screen_staff_s5_emergency_dashboard.xml`)
- **Endpoints:** `GET /api/emergency-reports/grouped-by-purok`, `PATCH /api/emergency-reports/:id/verify`.
- **Behavior:**
  - Server returns a map keyed by Purok name (`Map<String, List<EmergencyReport>>`). **Do not re-group client-side.**
  - Dynamically render Purok filter chips from the response's keys, displaying per-Purok counts (e.g., `"All (12)"`, `"Purok 3 (5)"`).
  - `RecyclerView` cards show `disasterType`, `purok`, `householdMembersAffected`, `needs`, `StatusBadge`, and an inline `"Verify"` button on cards where `status == "reported"` (`PATCH /api/emergency-reports/:id/verify`).
  - Tapping any report card navigates to `S6` (`EmergencyReportDetailFragment`) passing `reportId`.

### S6 — Emergency Report Detail (`EmergencyReportDetailFragment` / `screen_staff_s6_emergency_detail.xml`)
- **Endpoints:** `GET /api/emergency-reports/:id`, `PATCH /api/emergency-reports/:id/verify`.
- **Behavior:**
  - Display full report details (`disasterType`, `purok`, `householdMembersAffected`, `needs` chips, `photoUrl` preview, `StatusBadge`, `createdAt`).
  - If `status == "reported"`, `"Verify Report"` is enabled and `"Dispatch Relief"` is **disabled**.
  - **Hard Rule:** `"Dispatch Relief"` becomes enabled **only** when `status == "verified"` (or `"relief_dispatched"` to dispatch to additional affected residents in the Purok). Tapping it navigates to `S7` with `reportId` and `purok`.

### S7 — Relief Dispatch (`ReliefDispatchFragment` / `screen_staff_s7_relief_dispatch.xml`)
- **Endpoints:** `GET /api/relief-distributions?reportId=...`, `GET /api/residents/search` (or Purok resident lookup for that report's Purok), `POST /api/relief-distributions`.
- **Behavior:**
  - Display report summary header (`purok`, `disasterType`, `reportId`).
  - Fetch `GET /api/relief-distributions?reportId=...` **first** to build a `Set<UUID>` of already-served `residentId`s.
  - Render the checklist of residents in that Purok (name, mobile, household info — **never** `ctcNumber`/`ctcOrNumber`/`disabilities`/`passwordHash`):
    - **Hard Rule:** Already-served residents must render greyed-out with `"Already Served"` badge and **non-interactive** (`checkbox.setEnabled(false)`, row unclickable) so they cannot be checked again.
  - Staff enters `itemsGiven` (e.g., `"Rice, canned goods, bottled water"`) and selects unserved resident(s), then submits `POST /api/relief-distributions`.
  - Handle `409 Conflict` duplicate-dispatch response cleanly with a human-readable alert if another staff member dispatched concurrently.

### S10 — Walk-In Resident Service (`WalkInSearchFragment`, `WalkInRegisterFragment`, `WalkInRequestFragment`)
Move the 3 layout files from `app/src/main/res/staff/layout/` into `app/src/main/res/layout/` (`screen_staff_walkin_search.xml`, `screen_staff_walkin_register.xml`, `screen_staff_walkin_request.xml`) and add proper `@+id` bindings:

1. **Step 1 — `WalkInSearchFragment` (`screen_staff_walkin_search.xml`):**
   - Search bar calling `GET /api/residents/search?q=...` (filters by name/email/mobile).
   - `RecyclerView` showing matched residents (`fullName`, `email`, `mobileNumber`, `purok` — **no sensitive fields**). Tapping a resident proceeds directly to **Step 3 (`WalkInRequestFragment`)** with `residentId` & resident summary.
   - `"Register New Walk-In Resident"` button opens **Step 2 (`WalkInRegisterFragment`)** when no match exists.
2. **Step 2 — `WalkInRegisterFragment` (`screen_staff_walkin_register.xml`):**
   - Form collecting core resident onboarding fields (username/fullName, email, mobileNumber, sex, birthdate, civilStatus, citizenship, birthplace, **live `purok` dropdown from `GET /api/puroks`**, address, residencyType, residencyPeriod, sector — **minus OTP**).
   - Submit calls `POST /api/residents/walk-in`.
   - **Hard Rule — Credential Slip:** Display a one-time **Credential Slip card** with the resident's email and server-generated temporary password + one-click Copy button. **Never store this password locally or allow re-fetching it.** Provide `"Proceed to Document Request →"` button to pass `residentId` to Step 3.
3. **Step 3 — `WalkInRequestFragment` (`screen_staff_walkin_request.xml`):**
   - Load document types & fees from `GET /api/document-types`, `purpose`, `deliveryMethod`, Business Clearance conditional fields, plus optional immediate payment recording (`paymentMethod`, `paymentOrNumber`).
   - **No Purok Clearance upload field.** Display a visible `"Exempted — Walk-In"` badge.
   - Submit calls `POST /api/document-requests/walk-in` (`{ userId, documentType, purpose, deliveryMethod, businessName, businessAddress, businessType }`), shows confirmation with generated `controlNumber`, and returns to `S1`.

---

## 4. Backend Fixes & Additions Required for Member 2 (`server/src/`)

Ensure all queries match `schema.sql` quoted camelCase identifiers and use `writeAuditLog(req, action, targetId, details)` on sensitive staff actions:

1. **`routes/documents.js`:**
   - `GET /api/document-requests`: for `staff`/`admin`, join resident `fullName` & `purok` (never `passwordHash`, `ctcNumber`, `ctcOrNumber`, `disabilities`), support `?status=`, order by `"createdAt" DESC`.
   - `PATCH /api/document-requests/:id/verify-clearance`: accept `{ verified: boolean, rejectionReason?: string }`. If `verified === true`, set `"purokClearanceVerified" = true`, `"purokClearanceVerifiedBy" = req.user.id`, `"purokClearanceVerifiedAt" = now()`, audit log `CLEARANCE_VERIFIED`. If `false`, notify resident to re-upload (and optionally record `rejectionReason`).
   - `PATCH /api/document-requests/:id`:
     - Enforce lifecycle: block leaving `pending` when `!isWalkIn && !purokClearanceVerified` (`400`).
     - Require `rejectionReason` when setting `status = 'rejected'` (`400`).
     - Support recording payment (`paymentMethod`, `paymentOrNumber` → set `"paymentStatus" = 'confirmed'`), internal `"staffNotes"`, `"processedBy" = req.user.id`, and `"completedAt" = now()` when `completed`.
     - When moving to `ready`: invoke `services/certificateService.js` (`pdfkit`) reading the matching `certificateTemplates` row (`sealUrl`, `signatureUrl`, `officialName`, `officialTitle`), save PDF under `uploads/certificates/`, store `"certificateUrl"`, and send in-app + Resend email notification (attaching PDF if `deliveryMethod` is `email` or `both`).
     - Write audit log (`STATUS_CHANGE`, `PAYMENT_CONFIRMED`).
   - `POST /api/document-requests/walk-in`: set `"isWalkIn" = true`, `"purokClearanceVerified" = true`, `"purokClearanceVerifiedBy" = req.user.id`, `"purokClearanceVerifiedAt" = now()`, generate control number, write audit log `CLEARANCE_EXEMPTED_WALKIN`.
2. **New `routes/residents.js` (mount at `/api/residents` in `index.js`, gated `requireRole('staff', 'admin')`):**
   - `GET /api/residents/search?q=&purok=`: search active residents by `fullName`, `email`, `mobileNumber` (or filter by `purok` for S7); select **only** safe fields (`id`, `username`, `"fullName"`, `email`, `"mobileNumber"`, `purok`, `address`, `"isActive"`, `"createdAt"`).
   - `POST /api/residents/walk-in`: generate random strong temp password, bcrypt hash it, insert `profiles` with `role = 'resident'`, `"registrationMethod" = 'walk_in'`, `"registeredBy" = req.user.id`, write audit log `WALKIN_REGISTERED`, and return profile + plaintext `temporaryPassword` **once**.
3. **`routes/complaints.js` + `jobs/slaEscalationJob.js`:**
   - Fix `GET /api/complaints` SQL to `ORDER BY (priority = 'high') DESC, "slaDueAt" ASC`.
   - `PATCH /api/complaints/:id/resolve`: accept `status` (`reviewing` | `resolved` | `dismissed`). Require non-empty `staffResponse` for `resolved` and `dismissed`. Set `"handledBy" = req.user.id`, `"resolvedAt" = now()` for resolved/dismissed. Notify resident and write audit log (`COMPLAINT_RESOLVED` / `COMPLAINT_DISMISSED`).
   - Implement `jobs/slaEscalationJob.js` (export standalone `runSlaEscalationScan()` + hourly `node-cron` started in `server/src/index.js`) updating overdue `pending` complaints to `priority = 'high', "escalatedAt" = now()`, notifying all `staff`/`admin` users, and writing audit log.
4. **`routes/announcements.js`:**
   - Validate every element of `targetPurok` against `puroks(name)` before insert (`400` if unknown Purok).
   - Fan out notifications (`notifications` rows + queued email via `notificationService`) to targeted active residents (`purok = ANY($1)` or all active residents if empty) and audit log `ANNOUNCEMENT_PUBLISHED`.
5. **`routes/emergencies.js`:**
   - Fix `GET /api/emergency-reports/grouped-by-purok` SQL to use `"emergencyReports"` and `"createdAt"` DESC.
   - Add `GET /api/emergency-reports/:id`.
   - Fix `PATCH /api/emergency-reports/:id/verify` (`"reportId"`, `"emergencyReports"`), notify reporting resident, and audit log `EMERGENCY_VERIFIED`.
6. **New `routes/reliefDistributions.js` (mount at `/api/relief-distributions`, gated `requireRole('staff', 'admin')`):**
   - `GET /api/relief-distributions?reportId=`: return distribution rows for that `reportId`.
   - `POST /api/relief-distributions`: run inside a single PostgreSQL transaction (`BEGIN ... COMMIT`, rollback on error):
     1. Verify report exists & is `verified` or `relief_dispatched`.
     2. Check `(reportId, residentId)` duplicate (catching PG `23505` unique violation gracefully with `409`).
     3. Insert into `"reliefDistributions"`.
     4. Update `"emergencyReports"` status to `'relief_dispatched'` (or `'resolved'` when all affected residents/target count served).
     5. Insert `"auditLogs"` (`RELIEF_DISPATCHED`).
     6. Insert `"notifications"` row + trigger email/Socket.io for recipient resident.

---

## 5. Conflict Boundaries (Do Not Touch Without Syncing)

- Do **not** implement or modify Admin screens (`S8`, `S9`, `S11`, `S12`, `S13`, `S14`) or add the Admin tab to `staff` accounts — Member 3 owns `S8`/`S9`/`S11`–`S14` and the role-conditional menu logic in `StaffMainActivity`.
- Do **not** modify Resident screens (`R1–R13`).
- Keep `nav_graph_staff.xml` destination IDs for S1–S7 and S10 clearly documented so Member 3 can hook S13/Admin destinations into the same graph without conflicts.

---

## 6. Member 2 Self-Test Checklist

- [ ] Staff login displays **4 tabs** in `StaffMainActivity` (no Admin tab), and top-bar Walk-In shortcut opens S10.
- [ ] S2 blocks advancing an online request past `pending` until Purok Clearance is verified; walk-in requests show `"Exempted (Walk-In)"` with no clearance buttons; confirming payment updates `paymentStatus = confirmed`; setting `ready` generates a downloadable PDF.
- [ ] S3 loads complaints ordered high/overdue first with red left-edge bar; Resolve/Dismiss rejects empty `staffResponse`; SLA scan escalates backdated pending complaints.
- [ ] S4 loads Purok chips from `GET /api/puroks`, rejects invalid Puroks server-side, and fans out notifications to targeted residents.
- [ ] S5 builds Purok chips directly from `grouped-by-purok` keys; S6 disables `"Dispatch Relief"` until `status == "verified"`.
- [ ] S7 disables already-served residents and executes `POST /api/relief-distributions` transactionally (duplicate attempt returns friendly `409`, never `500`).
- [ ] S10 search returns zero sensitive fields; walk-in registration displays temp password **once**; walk-in request creates an exempted request with audit log.
