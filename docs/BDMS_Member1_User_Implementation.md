# Member 1 Implementation Plan — User / Resident Role (R1–R13)

**Owner:** Member 1 (Resident / Citizen Portal)  
**Screens Covered:** `R1` through `R13` (Auth, 6-Step Onboarding, Home Dashboard, Document Requests, Complaints, Notifications, Announcements, Emergency Reports, Profile)  
**Authoritative References:** [BDMS_Mobile_Database_Structure.md](BDMS_Mobile_Database_Structure.md) (data), [BDMS_Mobile_Build_Plan.md](BDMS_Mobile_Build_Plan.md) (behavior/API), [BDMS_Mobile_Gemini_CLI_Prompts.md](BDMS_Mobile_Gemini_CLI_Prompts.md) (screen table)

---

## 1. Current State Audit (What Exists vs. What Is Broken)

Before coding, review what is currently in the repository for Member 1's scope:

| Screen / File | Current State in Repo | Problems to Fix |
| --- | --- | --- |
| **R1** `LoginActivity.java` + `screen_resident_r1_login.xml` | Calls `repository.login(...)` only for non-staff emails; otherwise bypasses backend | **Remove** `"admin"`/`"staff"`/`"clerk"` email shortcut and `"offline_demo_token"` error fallbacks; store `SessionManager` fields (`token`, `id`, `role`, `purok`, `fullName`) and route strictly from server `role`. |
| **R2** `RegisterActivity.java`, `OnboardingWizardActivity.java`, `screen_resident_r1_register.xml`, `screen_onboarding_step1..6.xml`, `screen_resident_r2_step1..6.xml` | Duplicate onboarding XML sets exist; `OnboardingWizardActivity` hardcodes `"maria_santos92"` and ignores inputs/OTP; no `ViewPager2` | Consolidate into one canonical 6-step `OnboardingActivity` + `ViewPager2` (`OnboardingStep1Fragment`..`Step6Fragment`) with real form binding, live Purok dropdown (`GET /api/puroks`), password validation, RA 10173 consent, and real `send-otp` / `verify-otp`. |
| **R3** `ResidentMainActivity.java`, `ResidentDashboardFragment.java`, `screen_resident_r3_resident_dashboard.xml` | `ResidentMainActivity` uses manual `LinearLayout` bottom nav and launches separate Activities; `ResidentDashboardFragment` fetches pending requests and discards the response | Convert `ResidentMainActivity` into the single host Activity with `BottomNavigationView` + `NavHostFragment` (`nav_graph_resident.xml`). Bind live counts, recent items, announcements, and `NotificationBellView`. |
| **R4** `DocumentRequestListActivity.java`, `screen_resident_r4_document_request_list.xml` | Static XML cards; no `RecyclerView`, no `SwipeRefreshLayout`, no API fetch | Migrate to `DocumentRequestListFragment` with `SwipeRefreshLayout`, `RecyclerView` adapter, `StatusBadge`, `EmptyStateView`, `LoadingOverlay`, status filter chips, FAB → R5, row tap → R6. |
| **R5** `DocumentRequestFormActivity.java`, `DocumentRequestFormFragment.java`, `screen_resident_r5_document_request_new.xml` | Hardcodes 5 document types; clicking Upload flips boolean and uploads `"mock_image_bytes"`; ignores `deliveryMethod`, fee preview, and Business Clearance fields | Load types/fees from `GET /api/document-types`; wire `PhotoPickerHelper` for real camera/gallery file; keep Submit disabled until file is attached; support `deliveryMethod` + Business Clearance fields (`businessName`, `businessAddress`, `businessType`). |
| **R6** `DocumentRequestDetailActivity.java`, `screen_resident_r6_document_request_detail.xml` | Hardcoded `"BC-2024-0892"`, `"CLAIM-892-QR"`, Toast-only PDF download | Migrate to `DocumentRequestDetailFragment` bound to `GET /api/document-requests/:id`; render clearance status chip, dynamic timeline, `rejectionReason`, and real PDF download (`GET /api/document-requests/:id/certificate`). |
| **R7** `ComplaintListActivity.java`, `screen_resident_r7_complaint_list.xml` | Static mockup XML; only wires FAB | Migrate to `ComplaintListFragment` with `RecyclerView` + `SwipeRefreshLayout`; render `StatusBadge`, Overdue badge using `Complaint.isOverdue()`, and inline card expansion for `staffResponse`. |
| **R8** `ComplaintFormActivity.java`, `ComplaintFormFragment.java`, `screen_resident_r8_complaint_new.xml` | Sends empty `new ArrayList<>()` for photos; mismatches category vocabulary; closes screen with fake "Saved locally" Toast on HTTP errors | Wire `PhotoPickerHelper` (optional up to 3 photos), align categories (`noise`, `sanitation`, `dispute`, `infrastructure`, `other`), remove fake offline save, handle `429` rate limit gracefully. |
| **R9** `NotificationListActivity.java`, `NotificationListFragment.java`, `screen_resident_r9_notifications.xml` | Empty fragment / Toast-only Activity; static XML rows | Migrate to `NotificationListFragment` bound to `GET /api/notifications/me`, mark-as-read (`PATCH /api/notifications/:id/read`), deep-link via `relatedId`, Socket.io listener + polling fallback. |
| **R10** `AnnouncementActivity.java`, `screen_resident_r10_announcement_feed.xml` | Static XML cards; only wires hotline button | Migrate to `AnnouncementFeedFragment` bound to `GET /api/announcements`, pinning the first `category == "emergency"` announcement to top with alert styling and expandable body. |
| **R11** `EmergencyReportFormActivity.java`, `screen_resident_r11_emergency_report_new.xml` | Sends `"Flood (Baha)"` display string, empty `needs`, no read-only Purok chip, no photo upload, no duplicate warning dialog | Migrate to `EmergencyReportFormFragment`; display read-only Purok chip from `SessionManager`; bind `needs` `ChipGroup` + optional photo multipart; handle `409` duplicate warning ("Submit again?") & `429` rate limit. |
| **R12** `screen_resident_r12_emergency_report_list.xml` | XML exists with static cards; **no Java Activity or Fragment exists** | Create `EmergencyReportListFragment` + adapter bound to `GET /api/emergency-reports` with `StatusBadge`, `SwipeRefreshLayout`, `EmptyStateView`, `LoadingOverlay`. |
| **R13** `ProfileActivity.java`, `screen_resident_r13_profile.xml` | Static UI; edit mobile & rows only show Toasts | Migrate to `ProfileFragment` bound to `GET /api/profiles/me`, `PATCH /api/profiles/me` (editable mobile/fields, Purok locked), `POST /api/profiles/me/avatar` via `PhotoPickerHelper`, and logout clearing `SessionManager`. |

---

## 2. Shared Prerequisites & Models to Update

Coordinate with the Shared Foundation setup (see [BDMS_Implementation_Overview.md](BDMS_Implementation_Overview.md) §4) before modifying shared files:

### 2.1 Java Model Updates (`app/src/main/java/com/barangay/bdms/model/`)
Update only the resident-needed fields cleanly using exact `@SerializedName` camelCase keys from `schema.sql`:
- **`Profile.java` & `Resident.java`:** Add onboarding/profile fields: `username`, `sex`, `birthdate`, `civilStatus`, `citizenship`, `birthplace`, `address`, `residencyType`, `residencyPeriod`, `sector` (`List<String>`), `ctcNumber`, `ctcOrNumber`, `disabilities` (`List<String>`), `avatarUrl`, `consentAcceptedAt`, `registrationMethod`, `createdAt`. Never add `passwordHash`.
- **`DocumentRequest.java`:** Add `paymentStatus`, `paymentMethod`, `paymentOrNumber`, `rejectionReason`, `businessName`, `businessAddress`, `businessType`, `isWalkIn`, `purokClearanceUrl`, `purokClearanceVerified`, `purokClearanceVerifiedAt`, `certificateUrl`, `completedAt` (plus getters). Do **not** expose `staffNotes` in resident UI.
- **`Complaint.java`:** Add `photoUrls` (`List<String>`), `createdAt` (`OffsetDateTime`), `resolvedAt` (`OffsetDateTime`), and getters for `category`, `description`, `photoUrls`, `slaDueAt`, `createdAt`, `resolvedAt`. Keep `isOverdue()` intact.
- **`Announcement.java`:** Add `attachmentUrl`, `createdAt` (`OffsetDateTime`), and getters for `targetPurok`, `expiresAt`, `createdAt`.
- **`EmergencyReport.java`:** Add `photoUrl`, `createdAt` (`OffsetDateTime`), and getters for `householdMembersAffected`, `needs`, `photoUrl`, `createdAt`.
- **`Notification.java`:** Add `relatedId` (`UUID`), `deliveryAttempts`, `lastAttemptAt`, `failureReason`, and getters for `type`, `relatedId`, `createdAt`, `deliveryStatus`.

### 2.2 Common Helpers (`ui/common/`)
- **`PhotoPickerHelper.java`:** Uses `ActivityResultContracts.GetContent` / `PickVisualMedia` (system photo picker, no storage permission needed) and `ActivityResultContracts.TakePicture` via `FileProvider`. Compresses/prepares JPEG/PNG under 5 MB into `MultipartBody.Part`.
- **`NotificationBellView.java`:** Toolbar bell icon with unread count dot/badge, used on `ResidentDashboardFragment` and top bars to open R9.

---

## 3. Navigation Architecture (`nav_graph_resident.xml`)

Replace the multi-Activity launching pattern with a single host Activity after login:

- **Outside the graph (auth Activities):**
  - `LoginActivity` (`R1`)
  - `OnboardingActivity` (`R2`, with `ViewPager2` hosting 6 step Fragments)
- **Inside `ResidentMainActivity` (`BottomNavigationView` + `NavHostFragment`):**
  - **Tab 1 — Home:** `ResidentDashboardFragment` (`R3`) → actions to `DocumentRequestFormFragment` (`R5`), `ComplaintFormFragment` (`R8`), `EmergencyReportFormFragment` (`R11`), `NotificationListFragment` (`R9`).
  - **Tab 2 — Requests:** `DocumentRequestListFragment` (`R4`) → FAB to `DocumentRequestFormFragment` (`R5`), item click to `DocumentRequestDetailFragment` (`R6`, passing `requestId`).
  - **Tab 3 — Community:** `CommunityHostFragment` (sub-tabbed `TabLayout` + `ViewPager2` or chip switcher) hosting:
    - Sub-tab 1: `AnnouncementFeedFragment` (`R10`)
    - Sub-tab 2: `ComplaintListFragment` (`R7`) → FAB to `ComplaintFormFragment` (`R8`)
    - Sub-tab 3: `EmergencyReportListFragment` (`R12`) → FAB to `EmergencyReportFormFragment` (`R11`)
  - **Tab 4 — Profile:** `ProfileFragment` (`R13`) → Logout returns to `LoginActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`.
- Configure `popUpTo` / `launchSingleTop` on `BottomNavigationView` tabs so tapping tabs does not stack duplicate destinations.

---

## 4. Screen-by-Screen Implementation Specs (R1–R13)

### R1 — Login (`LoginActivity` / `screen_resident_r1_login.xml`)
- **Endpoint:** `POST /api/auth/login` (`{ email, password }`).
- **Behavior:**
  1. Disable login button + show loading state during call.
  2. Validate non-empty email and password locally.
  3. On `200 OK`: save `token` and `user` (`id`, `role`, `purok`, `fullName`, `email`) into `SessionManager`. Route `resident` → `ResidentMainActivity`, `staff` or `admin` → `StaffMainActivity`, then `finish()`.
  4. On `401` / `403` / network failure: show clear server/network error message (`"Invalid credentials"`, `"Account is deactivated"`, or `"Cannot reach server"`). **Never** fall back to an offline token or route by email substring.
  5. `"Create Account"` link launches `OnboardingActivity` (retire `RegisterActivity` or keep it as a thin redirect to `OnboardingActivity`).

### R2 — 6-Step Onboarding (`OnboardingActivity` + `OnboardingStep1..6Fragment`)
- **Layouts:** Consolidate `screen_onboarding_step1_account.xml`..`step6_verify.xml` (and remove unused duplicate `screen_resident_r2_step*` files once merged) inside a `ViewPager2` (`userInputEnabled = false` so users advance via validated buttons).
- **Endpoints:** `GET /api/puroks`, `POST /api/auth/register`, `POST /api/auth/send-otp`, `POST /api/auth/verify-otp`.
- **Shared Onboarding ViewModel (`OnboardingViewModel`):** Holds collected fields across steps:
  - **Step 1 (Account):** `username`, `email`, `password`, `confirmPassword`. Enforce password policy (`>= 8` chars, at least 1 uppercase, 1 digit, 1 special character) + match check.
  - **Step 2 (Personal Info):** `fullName`, `sex`, `birthdate` (DatePicker), `civilStatus`, `citizenship`, `birthplace`.
  - **Step 3 (Residency):** **First field required:** `purok` dropdown populated live from `GET /api/puroks` (never hardcoded). Then `address`, `residencyType`, `residencyPeriod`, and `sector` multi-select `ChipGroup`.
  - **Step 4 (Additional):** `ctcNumber`, `ctcOrNumber`, `disabilities` `ChipGroup`, and optional profile avatar picker (`PhotoPickerHelper`).
  - **Step 5 (Privacy Consent):** Display RA 10173 notice + mandatory checkbox; blocks Next until checked; records `consentAccepted = true` / timestamp.
  - **Step 6 (OTP Verification):** On entering Step 6 (or tapping "Send Code"), call `POST /api/auth/send-otp` with `email`. User enters 6-digit OTP → `"Verify & Complete"` calls `POST /api/auth/verify-otp`, then `POST /api/auth/register` with all collected fields (and avatar upload via `POST /api/profiles/me/avatar` if picked). Save returned JWT/session and route to `ResidentMainActivity`.

### R3 — Resident Home Dashboard (`ResidentDashboardFragment` / `screen_resident_r3_resident_dashboard.xml`)
- **Endpoints:** `GET /api/document-requests`, `GET /api/complaints`, `GET /api/notifications/me`, `GET /api/announcements`.
- **Behavior:**
  - Wrap root content in `SwipeRefreshLayout`.
  - Derive stat card numbers client-side from the returned lists (e.g., active/pending requests count, open complaints count, unread notifications count for `NotificationBellView`).
  - Show latest announcements preview (prioritizing `emergency` category).
  - Quick-action cards navigate via `NavController` to `R5`, `R8`, `R11`, `R10`; bell navigates to `R9`.

### R4 — My Document Requests (`DocumentRequestListFragment` / `screen_resident_r4_document_request_list.xml`)
- **Endpoint:** `GET /api/document-requests` (optionally `?status=`).
- **Behavior:**
  - Replace hardcoded XML cards with `SwipeRefreshLayout` + `RecyclerView` (`DocumentRequestAdapter`), `EmptyStateView`, and `LoadingOverlay`.
  - Filter `ChipGroup`/`TabLayout`: `All`, `Pending`, `Processing`, `Ready`, `Completed`, `Rejected`.
  - Each card shows `documentType`, `controlNumber`, `createdAt` (formatted cleanly), `fee`, and `StatusBadge`.
  - FAB (`+`) navigates to `R5`; tapping a row passes `requestId` to `R6`.

### R5 — New Document Request (`DocumentRequestFormFragment` / `screen_resident_r5_document_request_new.xml`)
- **Endpoints:** `GET /api/document-types`, `POST /api/document-requests` (multipart).
- **Behavior:**
  - Load `documentType` options and live PHP fees from `GET /api/document-types`; show live fee preview when user selects a type.
  - Conditionally show `businessName`, `businessAddress`, `businessType` fields **only** when `"Business Clearance"` is selected.
  - Collect `purpose` and `deliveryMethod` `RadioGroup` (`pickup` | `email` | `both`).
  - **Hard Rule:** `submitRequestBtn` starts **disabled** and stays disabled until `PhotoPickerHelper` returns a valid local image URI/file for `purokClearance` (camera or gallery), showing a thumbnail preview + filename.
  - Submit multipart `POST /api/document-requests` with real file bytes (`purokClearance`) + form fields, show `LoadingOverlay`, navigate back to `R4` on `201 Created`.

### R6 — Document Request Detail (`DocumentRequestDetailFragment` / `screen_resident_r6_document_request_detail.xml`)
- **Endpoints:** `GET /api/document-requests/:id`, `GET /api/document-requests/:id/certificate`.
- **Behavior:**
  - Bind real `controlNumber` (with copy-to-clipboard button), `documentType`, `purpose`, `deliveryMethod`, `fee`, `StatusBadge`, and `paymentStatus` / `paymentOrNumber` if confirmed.
  - Render **Purok Clearance status chip**: `"Verified"` (`purokClearanceVerified == true`), `"Rejected"` (if rejected with `rejectionReason`), or `"Pending Verification"`.
  - Render a status timeline derived strictly from `status`, `purokClearanceVerified`, `paymentStatus`, and `completedAt` (no separate timeline endpoint).
  - Show `rejectionReason` callout when `status == "rejected"`.
  - Enable `"Download Certificate"` button **only** when `status` is `"ready"` or `"completed"` (and `certificateUrl` exists); download PDF bytes from `GET /api/document-requests/:id/certificate` to app cache/Downloads and open via `FileProvider` intent.

### R7 — My Complaints (`ComplaintListFragment` / `screen_resident_r7_complaint_list.xml`)
- **Endpoint:** `GET /api/complaints`.
- **Behavior:**
  - `SwipeRefreshLayout` + `RecyclerView` + `EmptyStateView` + `LoadingOverlay`.
  - Each row renders `category`, `StatusBadge`, truncated `description`, `createdAt`, and an `"Overdue"` tag **strictly by calling `complaint.isOverdue()`**.
  - Tapping a card expands/collapses it **inline** (no separate detail screen) to reveal full description, attached photo thumbnails (if any), and `staffResponse` (if resolved/dismissed).
  - FAB navigates to `R8`.

### R8 — New Complaint (`ComplaintFormFragment` / `screen_resident_r8_complaint_new.xml`)
- **Endpoint:** `POST /api/complaints` (multipart).
- **Behavior:**
  - Category dropdown uses exact backend enum values: `noise`, `sanitation`, `dispute`, `infrastructure`, `other` (map friendly labels to enum keys).
  - **No Purok input field** — server attaches `purok` from the JWT.
  - Optional photo attachment (up to 3 images) via `PhotoPickerHelper`.
  - Handle `429 Too Many Requests` (rate limit) by showing the server's message and temporarily disabling Submit.

### R9 — Notifications (`NotificationListFragment` / `screen_resident_r9_notifications.xml`)
- **Endpoints:** `GET /api/notifications/me`, `PATCH /api/notifications/:id/read`.
- **Realtime:** Connect Socket.io client to room/channel `notifications:{userId}` with a 15–30s polling fallback while screen is active.
- **Behavior:**
  - `RecyclerView` rows with icon based on `type` (`request_update`, `complaint`, `announcement`, `emergency`), bold text + unread dot when `!isRead`.
  - Tapping a row calls `PATCH /api/notifications/:id/read` and navigates to the related screen via `type` + `relatedId` (e.g., `request_update` → `R6`, `complaint` → `R7`, `announcement` → `R10`, `emergency` → `R12`).

### R10 — Announcement Feed (`AnnouncementFeedFragment` / `screen_resident_r10_announcement_feed.xml`)
- **Endpoint:** `GET /api/announcements`.
- **Behavior:**
  - Server returns non-expired announcements targeted to the user's Purok (or barangay-wide).
  - Client pins the first `category == "emergency"` announcement to the top with high-visibility alert styling (`statusRejectedBg`/`statusRejectedText`).
  - Tapping a card expands/collapses the full `body` and optional attachment preview.

### R11 — Report Emergency (`EmergencyReportFormFragment` / `screen_resident_r11_emergency_report_new.xml`)
- **Endpoint:** `POST /api/emergency-reports` (multipart if photo attached).
- **Behavior:**
  - **Read-only Purok chip** populated from `SessionManager.getPurok()` (never editable).
  - `disasterType` dropdown using exact values (`flood`, `fire`, `typhoon`, `earthquake`, `landslide`).
  - `householdMembersAffected` stepper/input (`>= 1`).
  - `needs` multi-select `ChipGroup` (`food`, `water`, `medicine`, `shelter`, `evacuation`).
  - Optional photo via `PhotoPickerHelper`.
  - Handle `409 Conflict` duplicate-report warning with a confirmation dialog (`"You recently submitted a similar report. Submit again?"` passing `force=true` or confirm flag), and handle `429` rate limit cleanly.
  - On `201 Created`, show confirmation dialog with the created `reportId` reference and navigate to `R12`.

### R12 — My Emergency Reports (`EmergencyReportListFragment` / `screen_resident_r12_emergency_report_list.xml`)
- **Endpoint:** `GET /api/emergency-reports`.
- **Behavior:**
  - Create `EmergencyReportListFragment` + `EmergencyReportAdapter` using `screen_resident_r12_emergency_report_list.xml` (convert static layout to `SwipeRefreshLayout` + `RecyclerView` + `EmptyStateView` + `LoadingOverlay`).
  - Each card shows `disasterType`, `purok`, `householdMembersAffected`, `needs` summary, `createdAt`, and `StatusBadge` (`reported`, `verified`, `relief_dispatched`, `resolved`).

### R13 — My Profile (`ProfileFragment` / `screen_resident_r13_profile.xml`)
- **Endpoints:** `GET /api/profiles/me`, `PATCH /api/profiles/me`, `POST /api/profiles/me/avatar`.
- **Behavior:**
  - Display `avatarUrl`, `fullName`, `email`, `purok` (**read-only with lock icon — cannot be edited here**), `address`, `residencyType`, `ctcNumber`, `ctcOrNumber`, `sector`, `disabilities` (allowed here because it is the resident's own profile!).
  - Allow editing `mobileNumber` (and updating avatar via `PhotoPickerHelper` → `POST /api/profiles/me/avatar`).
  - `"Log Out"` shows confirmation dialog, clears `SessionManager` + disconnects Socket.io, and returns to `LoginActivity`.

---

## 5. Backend Fixes & Additions Required for Member 1 (`server/src/`)

Member 1 must ensure these routes work against the quoted camelCase schema in `server/src/database/schema.sql`:

1. **`routes/auth.js`:**
   - Update `POST /api/auth/register` to validate password policy (`>=8`, 1 uppercase, 1 number, 1 special char) and persist **all** onboarding columns (`username`, `fullName`, `email`, `passwordHash`, `mobileNumber`, `sex`, `birthdate`, `civilStatus`, `citizenship`, `birthplace`, `purok`, `address`, `residencyType`, `residencyPeriod`, `sector`, `ctcNumber`, `ctcOrNumber`, `disabilities`, `consentAcceptedAt`, `role = 'resident'`, `registrationMethod = 'online'`).
   - Replace stubbed `/send-otp` & `/verify-otp` with real DB queries on `"otpCodes"` (6-digit code, 10-minute `expiresAt`, check `consumedAt IS NULL`, mark `consumedAt = now()`, send via Resend `emailService`).
   - Update JWT expiry in `login` and `register` to `'1h'` (per `BDMS_Mobile_Build_Plan.md` §7) and verify `purok` exists in `puroks`.
2. **New `routes/profiles.js` (mount at `/api/profiles` in `index.js`):**
   - `GET /api/profiles/me`: return full own profile excluding `passwordHash`.
   - `PATCH /api/profiles/me`: allow updating safe editable fields (e.g., `mobileNumber`); **reject or ignore** changes to `role`, `purok`, `isActive`, `passwordHash`.
   - `POST /api/profiles/me/avatar`: Multer upload saving under `uploads/avatars/` and updating `"avatarUrl"`.
3. **`routes/documents.js` & `GET /api/document-types`:**
   - Mount `GET /api/document-types` in `index.js` (per spec, instead of only `/api/document-requests/types`).
   - Fix all SQL identifiers to `"documentRequests"`, `"userId"`, `"documentType"`, `"controlNumber"`, `"deliveryMethod"`, `"purokClearanceUrl"`, `"purokClearanceVerified"`, `"createdAt"`, etc.
   - **Enforce clearance rule on `POST /api/document-requests`:** reject with `400` if no file was uploaded (`req.file` missing). Generate control number `{TYPE4}-{TIMESTAMP}{RAND3}`. Look up `fee` from `config/fees.js` (`0.00` is valid for Indigency — fix `fees[documentType] || 50.00` bug where `0` falsy-defaults to `50.00`!). Accept `businessName`, `businessAddress`, `businessType` for Business Clearance.
   - Add `GET /api/document-requests/:id` (owner or `staff`/`admin` only) and `GET /api/document-requests/:id/certificate` (streams PDF if owner or `staff`/`admin`).
4. **`routes/complaints.js`:**
   - Fix SQL identifiers (`"complaintId"`, `"userId"`, `"photoUrls"`, `"slaDueAt"`, `"staffResponse"`, `"createdAt"`).
   - Set `slaDueAt` by category: `infrastructure` = 72h, others = 48h. Save uploaded `"photoUrls"`.
   - Scope `GET /api/complaints` to `WHERE "userId" = $1` for residents. Add `GET /api/complaints/:id` (owner or staff/admin).
   - Apply `rateLimitMiddleware` (`5/hour` → `429`) on `POST /api/complaints`.
5. **`routes/announcements.js`:**
   - Fix SQL (`"announcementId"`, `"targetPurok"`, `"postedBy"`, `"attachmentUrl"`, `"createdAt"`, `"expiresAt"`).
   - For residents, filter `("expiresAt" IS NULL OR "expiresAt" > now()) AND ("targetPurok" IS NULL OR cardinality("targetPurok") = 0 OR $1 = ANY("targetPurok"))`.
6. **`routes/emergencies.js`:**
   - Fix SQL (`"emergencyReports"`, `"reportId"`, `"userId"`, `"disasterType"`, `"householdMembersAffected"`, `"photoUrl"`, `"createdAt"`).
   - Support multipart photo upload on `POST /api/emergency-reports`; copy `purok` strictly from `req.user.purok` / DB profile.
   - Scope `GET /api/emergency-reports` to `WHERE "userId" = $1` for `resident` role! (Currently it returns everyone's reports!) Add `GET /api/emergency-reports/:id` (owner or staff/admin).
   - Apply `rateLimitMiddleware` (`5/hour` → `429`) and 10-minute same-`disasterType` duplicate warning (`409` unless confirmed).
7. **`routes/notifications.js`:**
   - Fix SQL (`"notificationId"`, `"userId"`, `"isRead"`, `"relatedId"`, `"deliveryStatus"`, `"createdAt"`).

---

## 6. Conflict Boundaries (Do Not Touch Without Syncing)

- Do **not** alter `StaffMainActivity` navigation or staff/admin screens (`S1–S14`).
- Do **not** add `StaffEvacCenterActivity` references or modify `AdminHubActivity`/Fragment.
- Coordinate with Member 2 on `DocumentRequest.java`, `Complaint.java`, `Announcement.java`, `EmergencyReport.java` and with Member 3 on `Profile.java`, `Notification.java`, `Purok.java` so no fields/constructors are removed.

---

## 7. Member 1 Self-Test Checklist

- [ ] Clean account registers through all 6 onboarding steps with real email OTP and lands in `ResidentMainActivity` (4 tabs).
- [ ] `GET /api/puroks` populates Step 3 dropdown (verify no hardcoded Purok arrays remain in resident code).
- [ ] R5 blocks submission until a real image is selected; Certificate of Indigency displays `PHP 0.00` (not `50.00`).
- [ ] R6 displays status timeline, clearance verification chip, and opens downloaded PDF when status is `ready`/`completed`.
- [ ] R7 marks overdue complaints using `Complaint.isOverdue()` and expands inline to show `staffResponse`.
- [ ] R9 updates unread dot/list and marks notifications read; R10 pins emergency announcement on top.
- [ ] R11 enforces read-only Purok chip, handles `409` duplicate warning and `429` rate limit; R12 lists submitted reports.
- [ ] R13 edits mobile number & avatar while keeping Purok locked; logout clears `SessionManager` token.
