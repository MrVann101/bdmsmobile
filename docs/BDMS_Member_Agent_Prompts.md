# BDMS Mobile — AI Coding Agent Prompts by Team Member

Use this file to instruct an AI coding agent (Antigravity, Gemini CLI, etc.) for each role in the 3-member split. Each prompt is self-contained, grounded in the repository's current state, and enforces strict file-ownership boundaries so Member 1, Member 2, and Member 3 can work in parallel without overwriting each other.

---

## How to Run These Prompts (Recommended Workflow)

1. **Step 0 (Once, before parallel branches):** Have one person (or the tech lead) run **Prompt 0 (Shared Foundation)** on the main branch and merge it. This prevents all 3 members from editing `ApiClient.java`, `app/build.gradle.kts`, `SessionManager`, and `PhotoPickerHelper` differently.
2. **Parallel Role Work:** Each teammate creates their own branch from the updated `main` and runs their respective prompt:
   - Member 1 runs **Prompt 1 (User / Resident Role)**
   - Member 2 runs **Prompt 2 (Staff Role)**
   - Member 3 runs **Prompt 3 (Admin Role & Role Guards)**
3. **Final Verification:** After merging all 3 branches, the lead runs **Prompt 4 (Output Verification & QA)** to verify everything against [BDMS_Implementation_Overview.md](BDMS_Implementation_Overview.md).

---

## Prompt 0 — Shared Foundation Setup (Run Once Before Parallel Member Work)

Copy and paste this prompt for the person setting up the shared base:

```text
You are setting up the Shared Foundation (Step 0) for BDMS Mobile before Member 1 (Resident), Member 2 (Staff), and Member 3 (Admin) implement their role features in parallel.

FIRST, read these files carefully:
1. GEMINI.md
2. docs/BDMS_Implementation_Overview.md
3. docs/BDMS_Mobile_Database_Structure.md
4. docs/BDMS_Mobile_Build_Plan.md

CONSTRAINTS:
- Native Android in Java 11 only (no Kotlin, no Compose). Keep minSdk = 23 with core library desugaring enabled.
- Never hardcode hex colors in layouts (use res/values/colors.xml tokens) or hardcode API URLs in Java (use BuildConfig).
- Never invent endpoints, fields, or status values outside docs/BDMS_Mobile_Database_Structure.md and docs/BDMS_Mobile_Build_Plan.md.
- Stop and ask the user if any real secret or human-only asset is missing (.env secrets, DATABASE_URL, RESEND_API_KEY, JWT_SECRET, real barangay seal/signature). Do not fake production secrets.

TASKS (Shared Infrastructure Only — Do NOT implement R1-R13 or S1-S14 screen logic yet):
1. Android Build & Networking Config:
   - In app/build.gradle.kts, enable `buildFeatures { buildConfig = true }` and define `buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5000/api/\"")` for debug (and a configurable release value).
   - Add a debug-only `network_security_config.xml` permitting cleartext HTTP to `10.0.2.2` and local dev hosts, referenced safely in AndroidManifest.xml (keep release HTTPS-only).
   - In app/src/main/java/com/barangay/bdms/network/ApiClient.java, replace the hardcoded BASE_URL with `BuildConfig.API_BASE_URL` and register a custom Gson JsonSerializer/JsonDeserializer for `java.time.OffsetDateTime` so ISO-8601 timestamps serialize/deserialize cleanly on API 23+.
   - Create a shared `SessionManager.java` (using EncryptedSharedPreferences) storing JWT token, userId, role, purok, fullName, and email, and update ApiClient token reads/clears to stay synchronized.
2. Shared UI Components & Resources:
   - Move the valid layout XML files currently sitting in `app/src/main/res/staff/layout/` (`screen_staff_user_create.xml`, `screen_staff_walkin_search.xml`, `screen_staff_walkin_register.xml`, `screen_staff_walkin_request.xml`) into `app/src/main/res/layout/`, and delete `app/src/main/res/staff/layout/task.artifact.md` + the stray `res/staff/` folder.
   - Implement `PhotoPickerHelper.java` under `ui/common/` (system photo picker for gallery + camera capture using `FileProvider` declared in AndroidManifest.xml, creating `MultipartBody.Part` under 5 MB).
   - Implement `NotificationBellView.java` under `ui/common/` (bell icon with unread count indicator).
   - Review `StatusBadge.java` and add support for `"cancelled"` (muted grey) plus clean pill shape background without losing existing status mappings.
3. Shared Backend Core (`server/src/`):
   - Verify `server/src/middleware/auth.js` checks active user status or properly validates `{ id, role, purok }` JWT claims.
   - Create `server/src/middleware/uploadMiddleware.js` (Multer: jpeg/png/webp only, max 5 MB, stored under `uploads/<type>/`) and replace open `/uploads` static serving in `server/src/index.js` with authenticated route handling where required (clearances, complaint photos, emergency photos).
   - Create `server/src/middleware/rateLimitMiddleware.js` (default 5/hour DB count returning HTTP 429 for complaints and emergency reports).
   - Create `server/src/services/auditLogService.js` exporting `writeAuditLog(req, action, targetId, details, metadata, client)` inserting into `"auditLogs"` (capturing `req.ip` and `req.headers['user-agent']`).
   - Create `server/src/services/emailService.js` (Resend wrapper, reading RESEND_API_KEY and RESEND_FROM_EMAIL from .env) and `server/src/services/notificationService.js` (`pushInApp` + Socket.io emit to `notifications:{userId}`, email delivery status tracking: pending/sent/failed/cancelled).
   - Fix `server/src/routes/puroks.js` GET query to use `"createdAt"` (matching `server/src/database/schema.sql`), and ensure `server/src/index.js` starts HTTP + Socket.io properly.

DELIVERABLE:
- Run `./gradlew assembleDebug` and syntax-check all edited JS files (`node -c ...`).
- List every file created, moved, or modified, and flag any missing `.env` keys or Gradle sync issues clearly.
```

---

## Prompt 1 — Member 1 Agent Prompt (User / Resident Role: R1–R13)

Copy and paste this prompt for Member 1's coding agent:

```text
You are Member 1 on the BDMS Mobile team, responsible for the **User / Resident Role (Screens R1–R13)** end-to-end (Android Java client + matching Express API routes).

FIRST, read these files completely:
1. GEMINI.md
2. docs/BDMS_Implementation_Overview.md
3. docs/BDMS_Member1_User_Implementation.md
4. docs/BDMS_Mobile_Database_Structure.md
5. docs/BDMS_Mobile_Build_Plan.md (focus on Phase 3, Phase 5.3, Phase 7–13)
6. docs/BDMS_Mobile_Gemini_CLI_Prompts.md (Resident Screens table R1–R13)

PRE-FLIGHT CHECK:
Inspect `ApiClient.java`, `SessionManager` (if created), `PhotoPickerHelper` (if created), and `server/src/` before editing. If Step 0 Shared Foundation files (`OffsetDateTime` Gson adapter, `SessionManager`, `PhotoPickerHelper`, `auditLogService`, `rateLimitMiddleware`, `emailService`) are missing, inform me first or implement only what you directly depend on while clearly flagging shared-file edits.

STRICT RULES & BOUNDARIES:
- You OWN: `ui/auth/*`, `ui/resident/*`, `res/navigation/nav_graph_resident.xml`, resident layouts `screen_resident_*` / `screen_onboarding_*`, and resident endpoints (`auth`, `profiles`, resident paths of `document-requests`, `document-types`, `complaints`, `announcements`, `emergency-reports`, `notifications/me`).
- DO NOT touch Staff screens (`S1–S7`, `S10`), Admin screens (`S8`, `S9`, `S11–S14`), `StaffMainActivity`, or `StaffEvacCenterActivity`.
- Native Java 11 + XML Views + Material 3 only (no Kotlin). Keep minSdk 23 and core library desugaring compatible.
- Never invent endpoints, JSON fields, or status values. Match `server/src/database/schema.sql` quoted camelCase identifiers in EVERY SQL query (`"documentRequests"`, `"userId"`, `"createdAt"`, etc. — fix all existing unquoted snake_case SQL bugs in your routes).
- Never hardcode hex colors, Purok lists, fees, or base URLs. Never include `passwordHash` in any Android model or API response.

WHAT YOU MUST FIX & IMPLEMENT:
1. Models (`app/src/main/java/com/barangay/bdms/model/`):
   - Expand `Profile.java`, `Resident.java`, `DocumentRequest.java`, `Complaint.java`, `Announcement.java`, `EmergencyReport.java`, and `Notification.java` to match all fields in `schema.sql` using `@SerializedName` camelCase keys (never add `passwordHash`; never show `staffNotes` on resident UI).
2. Remove All Demo/Mock Bypasses:
   - In `LoginActivity.java` (R1): delete the `"admin"`/`"staff"`/`"clerk"` email bypass and `"offline_demo_token"` error fallbacks. Call `POST /api/auth/login`, save session (`token`, `id`, `role`, `purok`, `fullName`) to `SessionManager`, and route by server `role` (`resident` -> `ResidentMainActivity`, `staff`/`admin` -> `StaffMainActivity`). Handle 401/403 cleanly.
   - In `OnboardingActivity` (R2): consolidate duplicate onboarding layouts (`screen_onboarding_step*` vs `screen_resident_r2_step*`), build a real 6-step `ViewPager2` flow (`OnboardingStep1Fragment`..`Step6Fragment`) + `OnboardingViewModel` collecting all `profiles` fields:
     * Step 1: password policy (>=8 chars, uppercase, digit, special char) + confirm match.
     * Step 3: Purok dropdown MUST be the required first field and populated live from `GET /api/puroks`.
     * Step 5: RA 10173 consent checkbox required.
     * Step 6: real `POST /api/auth/send-otp` and `POST /api/auth/verify-otp` against PostgreSQL `"otpCodes"`, then `POST /api/auth/register` (plus optional avatar upload).
   - Retire or redirect `RegisterActivity.java` into `OnboardingActivity`.
3. Migrate Resident Screens (`R3–R13`) to `ResidentMainActivity` + `BottomNavigationView` + `NavHostFragment` (`nav_graph_resident.xml`):
   - 4 tabs: Home (`R3`), Requests (`R4 -> R5/R6`), Community (sub-tabbed `R10` Announcements, `R7/R8` Complaints, `R12/R11` Emergency Reports), Profile (`R13`) + NotificationBellView -> `R9`. Use `popUpTo`/`singleTop` to avoid stacking duplicate tabs.
   - Replace ALL static XML mockup cards in list screens (`R4`, `R7`, `R9`, `R10`, `R12`) with `SwipeRefreshLayout` + `RecyclerView` + `EmptyStateView` + `LoadingOverlay` + `StatusBadge`.
   - R5 (`DocumentRequestFormFragment`): load types & fees from `GET /api/document-types` (ensure PHP 0.00 for Indigency works), show Business Clearance fields conditionally, keep Submit DISABLED until a real image is attached via `PhotoPickerHelper`, upload multipart `POST /api/document-requests` (remove `"mock_image_bytes"`).
   - R6 (`DocumentRequestDetailFragment`): bind `GET /api/document-requests/:id`, render Purok Clearance status badge (`Pending Verification` / `Verified` / `Rejected`), dynamic status timeline (`pending -> processing -> ready -> completed` / `rejected`), rejection reason, and real PDF download (`GET /api/document-requests/:id/certificate`) when `ready`/`completed`.
   - R7 (`ComplaintListFragment`): show Overdue badge strictly via `Complaint.isOverdue()` and tap-to-expand inline to read `staffResponse` (no separate complaint detail screen).
   - R8 (`ComplaintFormFragment`): use exact categories (`noise`, `sanitation`, `dispute`, `infrastructure`, `other`), NO Purok input field, optional multipart photo attachment, and clean HTTP 429 rate-limit feedback.
   - R9 (`NotificationListFragment`): mark read (`PATCH /api/notifications/:id/read`), deep-link via `relatedId`, and connect Socket.io `notifications:{userId}` with 15-30s polling fallback.
   - R10 (`AnnouncementFeedFragment`): pin the first `category == "emergency"` announcement to top with alert styling; expandable body.
   - R11 (`EmergencyReportFormFragment`): show read-only Purok chip from `SessionManager`, `disasterType` enum (`flood`, `fire`, `typhoon`, `earthquake`, `landslide`), affected count, `needs` ChipGroup, optional photo multipart, handle HTTP 409 duplicate warning ("Submit again?") and HTTP 429.
   - R12 (`EmergencyReportListFragment`): create the missing Java Fragment + Adapter bound to `screen_resident_r12_emergency_report_list.xml` and `GET /api/emergency-reports`.
   - R13 (`ProfileFragment`): `GET /api/profiles/me`, editable `mobileNumber` (`PATCH /api/profiles/me`), avatar upload (`POST /api/profiles/me/avatar`), Purok strictly read-only with lock icon, and logout clearing `SessionManager`.
4. Backend Resident Routes (`server/src/`):
   - Fix `routes/auth.js` (full onboarding fields, password policy, real `"otpCodes"` persistence + email OTP, 1h JWT).
   - Create `routes/profiles.js` (`/api/profiles/me`, `/avatar`, never return `passwordHash` or allow changing `role`/`purok`).
   - Fix `routes/documents.js` & mount `/api/document-types` in `server/src/index.js` (enforce required Purok Clearance on online `POST`, fix `fees[documentType] || 50.00` falsy-zero bug for `0.00`, add `GET /:id` and `GET /:id/certificate` PDF stream scoped to owner/staff).
   - Fix `routes/complaints.js` (camelCase SQL, 48h/72h SLA calculation by category, owner-scoped GET, rate limiting).
   - Fix `routes/announcements.js` (camelCase SQL, resident Purok + expiry filtering).
   - Fix `routes/emergencies.js` (camelCase SQL, scope `GET /` to `req.user.id` for residents, add `GET /:id`, multipart photo, rate limiting + 10-min duplicate warning).
   - Fix `routes/notifications.js` (`GET /me`, `PATCH /:id/read` with camelCase SQL).

VERIFICATION & OUTPUT:
- Run `./gradlew assembleDebug` and verify server syntax/tests for all modified files.
- Verify every item in Section 7 of `docs/BDMS_Member1_User_Implementation.md`.
- Report every created/modified file and list any open items (such as missing `.env` credentials) without inventing placeholders.
```

---

## Prompt 2 — Member 2 Agent Prompt (Staff Role: S1–S7, S10)

Copy and paste this prompt for Member 2's coding agent:

```text
You are Member 2 on the BDMS Mobile team, responsible for the **Staff Role (Screens S1–S7 and S10)** end-to-end (Android Java client + matching Express API routes/services).

FIRST, read these files completely:
1. GEMINI.md
2. docs/BDMS_Implementation_Overview.md
3. docs/BDMS_Member2_Staff_Implementation.md
4. docs/BDMS_Mobile_Database_Structure.md
5. docs/BDMS_Mobile_Build_Plan.md (focus on Phase 3, Phase 5.4, Phase 8–11, Phase 14)
6. docs/BDMS_Mobile_Gemini_CLI_Prompts.md (Staff/Admin Screens table S1–S7, S10)

PRE-FLIGHT CHECK:
Inspect `ApiClient.java`, `ApiService.java`, `BdmsRepository.java`, `StaffMainActivity.java`, `app/src/main/res/staff/layout/`, and `server/src/` before editing. Coordinate with Member 3 on `StaffMainActivity` and `res/navigation/nav_graph_staff.xml` (you implement S1–S7 & S10 destinations; Member 3 owns the 4-tab vs 5-tab role menu builder and S8/S9/S11–S14).

STRICT RULES & BOUNDARIES:
- You OWN: Staff operational screens `S1` (`StaffDashboardFragment`), `S2` (`StaffRequestQueueFragment` + `StaffRequestDetailFragment`), `S3` (`StaffComplaintQueueFragment`), `S4` (`AnnouncementComposerFragment`), `S5` (`EmergencyResponseDashboardFragment`), `S6` (`EmergencyReportDetailFragment`), `S7` (`ReliefDispatchFragment`), and `S10` (`WalkInSearchFragment`, `WalkInRegisterFragment`, `WalkInRequestFragment`), plus their adapters, layouts, and staff backend routes/services.
- DO NOT implement Admin screens (`S8`, `S9`, `S11`, `S12`, `S13`, `S14`) or add an Admin tab to `staff` accounts. DO NOT touch Resident screens (`R1–R13`).
- Never expose `passwordHash`, `ctcNumber`, `ctcOrNumber`, or `disabilities` in ANY staff list or resident search response/UI (`S2`, `S7`, `S10`).
- Never hardcode hex colors, Purok names (always read `GET /api/puroks` or server keys), fees, or base URLs.
- Match `server/src/database/schema.sql` quoted camelCase identifiers in EVERY SQL query (`"documentRequests"`, `"emergencyReports"`, `"reliefDistributions"`, `"slaDueAt"`, etc.).

WHAT YOU MUST FIX & IMPLEMENT:
1. Models (`app/src/main/java/com/barangay/bdms/model/`):
   - Ensure `DocumentRequest.java`, `ReliefDistribution.java`, and `StaffMember.java` include all staff fields (`staffNotes`, `purokClearanceVerifiedBy`, `processedBy`, `paymentStatus`, `paymentMethod`, `paymentOrNumber`, `rejectionReason`, `isWalkIn`, `purokClearanceUrl`, `purokClearanceVerified`, `certificateUrl`, `completedAt`, plus safe resident display name/purok if joined).
2. Staff Screens (`S1–S7`, `S10`) — Replace All Static Mockups & Toast Stubs:
   - Convert all staff list screens (`S2`, `S3`, `S5`, `S7`, `S10` search) to `SwipeRefreshLayout` + `RecyclerView` + `EmptyStateView` + `LoadingOverlay` + `StatusBadge`.
   - S1 (`StaffDashboardFragment`): fetch live lists from `GET /api/document-requests`, `GET /api/complaints`, `GET /api/emergency-reports`; compute and display live counts; card taps jump to S2/S3/S5; top-bar `"Walk-In"` button navigates to S10.
   - S2 (`StaffRequestQueueFragment` + `StaffRequestDetailFragment` + new `screen_staff_request_detail.xml`):
     * `TabLayout` filter (`pending`, `processing`, `ready`, `completed`, `rejected`).
     * Detail screen: if `isWalkIn == true`, HIDE clearance image & Verify/Reject buttons and show `"Exempted (Walk-In)"` badge. If online, show zoomable Purok Clearance image + Verify/Reject (`PATCH /api/document-requests/:id/verify-clearance`).
     * Enforce status lifecycle (`pending -> processing -> ready -> completed` or `rejected` with required `rejectionReason`; disable leaving `pending` if online clearance unverified).
     * Payment panel: `paymentMethod` dropdown (`cash`, `debit_card`, `credit_card`, `waived`, `demo`), `paymentOrNumber`, internal `staffNotes`, Confirm Payment (`PATCH /api/document-requests/:id`).
   - S3 (`StaffComplaintQueueFragment`): fetch `GET /api/complaints` (server-sorted `priority DESC, slaDueAt ASC`), render solid red left-edge bar on `high` priority or `isOverdue()` rows, inline/sheet detail with multiline `staffResponse` + `Mark Reviewing` / `Resolve` / `Dismiss` (`PATCH /api/complaints/:id/resolve`, enforcing non-empty `staffResponse` for resolved/dismissed).
   - S4 (`AnnouncementComposerFragment`): load Purok multi-select `ChipGroup` live from `GET /api/puroks` (empty = barangay-wide), category dropdown (`general`, `event`, `emergency`), optional `expiresAt`, submit `POST /api/announcements`.
   - S5 (`EmergencyResponseDashboardFragment`): fetch `GET /api/emergency-reports/grouped-by-purok`, build filter chips + per-Purok counts directly from response keys (do not re-group client-side), inline Verify (`PATCH /api/emergency-reports/:id/verify`), tap card -> S6.
   - S6 (`EmergencyReportDetailFragment`): fetch `GET /api/emergency-reports/:id`, wire Verify (`PATCH /api/emergency-reports/:id/verify`), keep `"Dispatch Relief"` DISABLED until `status == "verified"`, tap -> S7 with `reportId` & `purok`.
   - S7 (`ReliefDispatchFragment`): fetch `GET /api/relief-distributions?reportId=...` FIRST, render residents in that Purok, and grey out + disable interaction (`setEnabled(false)`, unclickable) on already-served residents. Submit `itemsGiven` + selected unserved residents via `POST /api/relief-distributions`; handle HTTP 409 duplicate conflict cleanly.
   - S10 (`WalkInSearchFragment`, `WalkInRegisterFragment`, `WalkInRequestFragment`):
     * Move `screen_staff_walkin_search.xml`, `screen_staff_walkin_register.xml`, `screen_staff_walkin_request.xml` out of `app/src/main/res/staff/layout/` into `app/src/main/res/layout/` (if not already moved by Step 0) and give all inputs/buttons proper `@+id`s.
     * Step 1: `GET /api/residents/search?q=` (exclude sensitive fields).
     * Step 2: `POST /api/residents/walk-in` (live `GET /api/puroks` dropdown, no OTP) -> show one-time **Credential Slip** card with email + temporary password + copy button (NEVER persist or re-fetch this password).
     * Step 3: `POST /api/document-requests/walk-in` (`GET /api/document-types` dropdown/fees, NO clearance upload, display `"Exempted — Walk-In"` badge).
3. Staff Backend Routes & Services (`server/src/`):
   - `routes/documents.js` + new `services/certificateService.js` (`pdfkit`): fix camelCase SQL, enforce clearance verification before leaving `pending`, require `rejectionReason` on `rejected`, record payment (`paymentStatus = 'confirmed'`), generate PDF using matching `certificateTemplates` row when moving to `ready`, notify resident (in-app + Resend email), handle `POST /api/document-requests/walk-in` (`isWalkIn = true`, `purokClearanceVerified = true`, audit log `CLEARANCE_EXEMPTED_WALKIN`).
   - New `routes/residents.js` (`/api/residents/search` excluding sensitive columns, `/api/residents/walk-in` generating random password once + audit log `WALKIN_REGISTERED`).
   - `routes/complaints.js` + `jobs/slaEscalationJob.js`: fix SQL sorting `(priority = 'high') DESC, "slaDueAt" ASC`, implement `reviewing`/`resolved`/`dismissed` with required `staffResponse` + resident notification + audit log, and create hourly `node-cron` + testable `runSlaEscalationScan()` notifying staff/admin on overdue escalation.
   - `routes/announcements.js`: validate every `targetPurok` element against `puroks(name)` before insert, fan out notifications to targeted residents, write audit log.
   - `routes/emergencies.js` + new `routes/reliefDistributions.js`: fix camelCase SQL on `/grouped-by-purok`, `/:id`, `/:id/verify`; implement `GET /api/relief-distributions?reportId=` and transactional `POST /api/relief-distributions` (`BEGIN ... COMMIT`: duplicate check + insert distribution + update emergency status + audit log `RELIEF_DISPATCHED` + resident notification, catching PG `23505` with HTTP 409).

VERIFICATION & OUTPUT:
- Run `./gradlew assembleDebug` and syntax-check/test your server routes & SLA scan function.
- Verify every item in Section 6 of `docs/BDMS_Member2_Staff_Implementation.md`.
- List every created/modified file and note any integration dependencies with Member 3 (`StaffMainActivity` / `nav_graph_staff.xml` / `certificateTemplates` / `auditLogs`).
```

---

## Prompt 3 — Member 3 Agent Prompt (Admin Role: S8, S9, S11–S14 & Role Guards)

Copy and paste this prompt for Member 3's coding agent:

```text
You are Member 3 on the BDMS Mobile team, responsible for the **Admin Role (Screens S8, S9, S11, S12, S13, S14)** and **Role-Based Navigation & Access Guards** end-to-end (Android Java client + matching Express API routes/services).

FIRST, read these files completely:
1. GEMINI.md
2. docs/BDMS_Implementation_Overview.md
3. docs/BDMS_Member3_Admin_Implementation.md
4. docs/BDMS_Mobile_Database_Structure.md
5. docs/BDMS_Mobile_Build_Plan.md (focus on Phase 3, Phase 5.2, Phase 5.4, Phase 12, Phase 14)
6. docs/BDMS_Mobile_Gemini_CLI_Prompts.md (Staff/Admin table S8, S9, S11–S14 & Navigation Wiring Prompt)

PRE-FLIGHT CHECK:
Inspect `StaffMainActivity.java`, `StaffNavHelper.java`, `AdminHubActivity.java`, `StaffEvacCenterActivity.java`, `screen_staff_s8_audit_log.xml`, `screen_staff_s9_certificate_template.xml`, `app/src/main/res/staff/layout/`, and `server/src/` before editing.

CRITICAL DESIGN DRIFT TO FIX FIRST:
In the current repository, `StaffEvacCenterActivity.java`, `screen_staff_s8_audit_log.xml`, and `screen_staff_s9_certificate_template.xml` contain **Evacuation Center Operations** and **Evacuee Intake Registration** mockups (from `stitch_bdms_mobile_ui2/s8_*` and `s9_*`), which are NOT part of the authoritative spec. You MUST retire `StaffEvacCenterActivity.java` and replace `screen_staff_s8_audit_log.xml` with **Audit Log Viewer (S8)** and `screen_staff_s9_certificate_template.xml` with **Certificate Template Settings (S9)**.

STRICT RULES & BOUNDARIES:
- You OWN: `StaffMainActivity` role-based bottom navigation (`staff` = 4 tabs, `admin` = 5 tabs), `res/navigation/nav_graph_staff.xml` (coordinating S1–S7/S10 IDs with Member 2), `AdminHubFragment (S13)`, `AuditLogFragment (S8)`, `CertificateTemplateFragment (S9)`, `NotificationCenterFragment (S11)`, `UserManagementFragment` + `UserCreateFragment` (`S12`), `PurokManagementFragment (S14)`, and Admin backend routes (`audit-logs`, `certificate-templates`, admin `notifications`, `admin/users`, `puroks` POST, `auditLogService`).
- DO NOT break or rewrite Member 1 (`R1–R13`) or Member 2 (`S1–S7`, `S10`) business logic.
- Never hardcode hex colors, Purok names, fees, or base URLs. Never expose `passwordHash`, `ctcNumber`, `ctcOrNumber`, or `disabilities` in `S12` user lists.
- Match `server/src/database/schema.sql` quoted camelCase identifiers in EVERY SQL query.

WHAT YOU MUST FIX & IMPLEMENT:
1. Role-Based Navigation & Defense-in-Depth Guards:
   - Refactor `StaffMainActivity` to host a `NavHostFragment` (`nav_graph_staff.xml`) and `BottomNavigationView`:
     * Read `SessionManager.getRole()`.
     * If `"staff"`: populate ONLY 4 bottom tabs (`Dashboard`, `Complaints`, `Emergency`, `Announcements`). Leave the 5th `"Admin"` tab OUT of the menu completely (never inflate-and-hide).
     * If `"admin"`: populate the same 4 tabs PLUS a 5th `"Admin"` tab whose start destination is `AdminHubFragment (S13)`.
     * Configure `popUpTo`/`singleTop` so bottom tabs do not stack duplicate destinations.
   - Create an `AdminGuard.requireAdmin(Fragment)` check and invoke it in `S8`, `S9`, `S11`, `S12` (both list & create), `S13`, and `S14`: if role != `"admin"`, show `"Not authorized"` and `popBackStack()` immediately.
2. Models (`app/src/main/java/com/barangay/bdms/model/`):
   - Update `AuditLog.java` (`ipAddress`, `userAgent`, `metadata`, `createdAt`, optional safe actor name/email), `CertificateTemplate.java` (`updatedAt`, getters for URLs), `Notification.java` (`deliveryStatus`, `deliveryAttempts`, `lastAttemptAt`, `failureReason`, `type`, `relatedId`), and `Purok.java`.
3. Admin Screens (`S13`, `S8`, `S9`, `S11`, `S12`, `S14`):
   - Replace static XML mockups with `SwipeRefreshLayout` + `RecyclerView` + `EmptyStateView` + `LoadingOverlay` + `StatusBadge` using `@color/*` tokens and `@layout/card_container`.
   - S13 (`AdminHubFragment` / `screen_staff_s13_admin_hub.xml`): apply `AdminGuard`; bind live admin fullName + `"ADMIN"` chip from `SessionManager`; fetch `GET /api/notifications?deliveryStatus=failed` to show failed-count badge; wire the 5 tool rows in exact order (`User Management` -> S12, `Audit Log` -> S8, `Certificate Templates` -> S9, `Notification Center` -> S11, `Manage Puroks` -> S14) with back arrows returning to S13. Remove static hardcoded demo names and non-existent Data Vault Sync claims.
   - S8 (`AuditLogFragment` / `screen_staff_s8_audit_log.xml`): remove `StaffEvacCenterActivity.java` + its manifest declaration; replace XML with Audit Log Viewer UI; bind to `GET /api/audit-logs` showing `action`, actor, `details`, `targetId`, `ipAddress`, `userAgent`, `createdAt`. Strictly read-only (zero edit/delete controls).
   - S9 (`CertificateTemplateFragment` / `screen_staff_s9_certificate_template.xml`): replace Evacuee Intake XML with Certificate Template Settings UI; load `documentType` options from `GET /api/document-types`; fetch `GET /api/certificate-templates/:documentType`; wire seal & signature pickers via `PhotoPickerHelper`, `officialName`, `officialTitle`, and Save via `PUT /api/certificate-templates/:documentType`.
   - S11 (`NotificationCenterFragment` / `screen_staff_s11_notification_center.xml`): `TabLayout` filter (`All`, `Pending`, `Sent`, `Failed`, `Cancelled`) calling `GET /api/notifications` (`?deliveryStatus=`); each row shows `StatusBadge`, `deliveryAttempts`, `lastAttemptAt`, and `failureReason` (for `failed`). Bind `"Retry"` enabled ONLY when `deliveryStatus == "failed"` (`POST /api/notifications/:id/retry`) and `"Cancel"` enabled ONLY when `deliveryStatus == "pending"` (`POST /api/notifications/:id/cancel`).
   - S12 (`UserManagementFragment` + `UserCreateFragment`): move `screen_staff_user_create.xml` from `app/src/main/res/staff/layout/` to `app/src/main/res/layout/` (if not already moved); bind `GET /api/admin/users?q=` search (excluding sensitive fields); per-row active toggle (`PATCH /api/admin/users/:id/active`); role change requires a confirmation `AlertDialog` before `PATCH /api/admin/users/:id/role`; `UserCreateFragment` loads `GET /api/puroks`, calls `POST /api/admin/users`, and shows the returned temporary password ONCE on a Credential Slip with copy button (never stored/re-fetched).
   - S14 (`PurokManagementFragment` / `screen_staff_s14_purok_management.xml`): bind `GET /api/puroks`; keep rows **add-only** (ZERO edit or delete buttons, plus info note explaining why); `"+ Add Purok"` opens bottom sheet (`name`, `description`) calling `POST /api/puroks`, displaying an inline error on HTTP `409 Conflict` duplicate name.
4. Admin Backend Routes & Services (`server/src/`):
   - Ensure `services/auditLogService.js` (`writeAuditLog`) exists and is called across all admin actions.
   - Create `routes/auditLogs.js` (`/api/audit-logs`, `requireRole('admin')`, GET only — NEVER add PATCH/PUT/DELETE routes).
   - Create `routes/certificateTemplates.js` (`GET /:documentType` authenticated; `PUT /:documentType` `requireRole('admin')` with Multer seal/signature uploads + audit log `TEMPLATE_UPDATED`).
   - Update `routes/notifications.js`: fix `"deliveryStatus"` / `"createdAt"` SQL on `GET /api/notifications` (`requireRole('admin')`), add `POST /:id/retry` (`requireRole('admin')` re-sending via Resend `emailService` and updating attempts/status/failureReason + audit log), and `POST /:id/cancel` (`requireRole('admin')` setting `deliveryStatus = 'cancelled'` without deleting + audit log).
   - Update `routes/admin.js`: generate random strong temp password on `POST /api/admin/users` and return `{ user, temporaryPassword }` once; validate allowed roles on `PATCH /users/:id/role`; write audit logs on create, role change, and active toggle; verify sensitive columns stay excluded on `GET /users`.
   - Update `routes/puroks.js`: fix `"createdAt"` SQL, catch PG `23505` duplicate key on `POST /api/puroks` returning HTTP `409`, write audit log `PUROK_ADDED`, and keep no edit/delete routes.

VERIFICATION & OUTPUT:
- Run `./gradlew assembleDebug` and syntax-check/test all server admin routes.
- Verify every item in Section 7 of `docs/BDMS_Member3_Admin_Implementation.md`.
- List every created/modified/deleted file and flag any dependencies or `.env` configuration needed.
```

---

## Prompt 4 — Team Lead Output Verification & QA Prompt (Run After Merging All 3 Members)

Copy and paste this prompt when reviewing the integrated codebase from Member 1, Member 2, and Member 3:

```text
You are reviewing the merged BDMS Mobile repository (Android app + Node.js/Express server) produced by Member 1 (Resident), Member 2 (Staff), and Member 3 (Admin).

DO NOT SILENTLY FIX ANYTHING YET. Inspect the code and run verification checks, then produce a clear Pass/Fail report first and wait for my confirmation before editing code.

FIRST, read:
1. GEMINI.md
2. docs/BDMS_Implementation_Overview.md
3. docs/BDMS_Mobile_Database_Structure.md
4. docs/BDMS_Mobile_Build_Plan.md

CHECKLIST TO AUDIT:
1. Build & Schema Integrity:
   - Does `./gradlew assembleDebug` pass cleanly with minSdk 23 and core library desugaring enabled?
   - Does `ApiClient.java` read `BuildConfig.API_BASE_URL` and register a working `OffsetDateTime` Gson adapter?
   - Do ALL SQL queries in `server/src/` match `server/src/database/schema.sql` quoted camelCase identifiers (no leftover `document_requests`, `user_id`, `created_at`, `sla_due_at`)?
   - Is `app/src/main/res/staff/layout/` gone, and are `StaffEvacCenterActivity` / Evacuation Center mockups completely removed from `S8`/`S9`?
2. Member 1 Output (`R1–R13` — Section 5.2 of Overview):
   - Are offline/email-bypass hacks in `LoginActivity`, hardcoded user data in `OnboardingActivity`, and `"mock_image_bytes"` in `DocumentRequestForm` completely gone?
   - Does `R2` load Puroks from `GET /api/puroks` and run real DB-backed OTP?
   - Does `R5` keep Submit disabled until a real Purok Clearance image is attached, and preserve `0.00` fee for Indigency?
   - Does `R6` show clearance status, timeline including `ready`, and real PDF download?
   - Does `R7` call `Complaint.isOverdue()` and expand inline for `staffResponse`?
   - Do `R4`, `R7`, `R9`, `R10`, `R12` use `RecyclerView`, `SwipeRefreshLayout`, `EmptyStateView`, `LoadingOverlay`, and `StatusBadge`?
3. Member 2 Output (`S1–S7`, `S10` — Section 5.3 of Overview):
   - Does `S2` show `"Exempted (Walk-In)"` and hide clearance verify/reject controls when `isWalkIn == true`, and block advancing online requests past `pending` while unverified?
   - Does `S3` sort `(priority DESC, slaDueAt ASC)` on the server, show red left-edge bar on overdue/high rows, and require `staffResponse` for Resolve/Dismiss?
   - Does `S4` load Puroks from `GET /api/puroks` and validate `targetPurok` server-side?
   - Does `S5` render filter chips from `grouped-by-purok` response keys; does `S6` disable `"Dispatch Relief"` until `status == "verified"`; does `S7` render already-served residents disabled/non-interactive and dispatch inside one DB transaction?
   - Does `S10` show the one-time temporary password on the Credential Slip without storing/re-fetching it, and exclude sensitive fields from `GET /api/residents/search`?
4. Member 3 Output (`S8`, `S9`, `S11–S14` & Role Guards — Section 5.4 of Overview):
   - Does logging in as `staff` show EXACTLY 4 bottom tabs with NO Admin tab in the menu, while `admin` shows 5 tabs opening `AdminHubFragment (S13)`?
   - Do `S8`, `S9`, `S11`, `S12`, `S13`, `S14` each enforce an in-Fragment admin role check AND `requireRole('admin')` (403) on the server?
   - Are `S8` (Audit Log) and `S14` (Purok Management) strictly read-only / add-only respectively (no edit/delete controls, no PATCH/DELETE routes)?
   - Does `S11` bind Retry (`failed` only) and Cancel (`pending` only) to `deliveryStatus`?
   - Does `S12` exclude `passwordHash`, `ctcNumber`, `ctcOrNumber`, `disabilities` from multi-user lists, confirm before role change, and show temp password once?

OUTPUT FORMAT:
Return:
1. A Markdown table: `Area / Screen ID | Check | Status (PASS/FAIL/PARTIAL) | Exact File & Line Reference | What Needs Fixing`.
2. Any cross-member integration mismatches found between Android models/endpoints and Express routes.
3. Ask for my approval before applying any fixes.
```
