# BDMS Mobile — Master Implementation Overview & Output Verification Guide

**System:** BDMS Mobile (*Barangay Document, Complaint, and Emergency Response Management System*)  
**Stack:** Native Android (Java 11, XML Views, Material Design 3, Retrofit + OkHttp) + Node.js/Express REST API + PostgreSQL  
**Team Split:** 3 Members — **Member 1 (Resident / User)**, **Member 2 (Staff)**, **Member 3 (Admin)**

---

## 1. Purpose of This Document

Most screen layouts in `app/src/main/res/layout/` already exist as visual mockups, **but their runtime behavior, data binding, navigation, and backend API connections are incomplete or mocked**. This Overview defines:

1. **What the finished application must be** (authoritative architecture, roles, and end-to-end features).
2. **Current repo gaps & shared blockers** that affect all 3 members before or during feature implementation.
3. **Exact ownership boundaries** across Member 1, Member 2, and Member 3 so no two members overwrite the same files or invent incompatible contracts.
4. **Master Output Verification Checklist** so the team lead can inspect and test each member's deliverables against the target specification.

### Companion Documents & Precedence Rules

If documents disagree, resolve conflicts strictly in this order (per `GEMINI.md`):

| Priority | File | Governs |
| --- | --- | --- |
| 1 | `docs/BDMS_Mobile_Database_Structure.md` | Table names, column names, types, constraints, and data privacy rules |
| 2 | `docs/BDMS_Mobile_Build_Plan.md` | API contracts, role rules, screen specs, and business logic |
| 3 | `docs/BDMS_Mobile_Class_Object_Model.md` | Core Java `model/` entity design and helper methods |
| 4 | `docs/BDMS_Mobile_Gemini_CLI_Prompts.md` | Screen-to-Fragment/Layout/Endpoint mapping tables (R1–R13, S1–S14) |

Companion implementation plans for each teammate:
- [BDMS_Member1_User_Implementation.md](BDMS_Member1_User_Implementation.md) — Resident / User Role (`R1–R13`)
- [BDMS_Member2_Staff_Implementation.md](BDMS_Member2_Staff_Implementation.md) — Staff Role (`S1–S7`, `S10`)
- [BDMS_Member3_Admin_Implementation.md](BDMS_Member3_Admin_Implementation.md) — Admin Role (`S8`, `S9`, `S11–S14` + Role-Based Navigation Guards)

---

## 2. What the Finished Application Should Be

BDMS Mobile digitizes a single Philippine barangay's operations across three roles sharing one PostgreSQL database (`profiles` differentiated by `role = 'resident' | 'staff' | 'admin'`):

### 2.1 High-Level User Journeys

| Role | Core Capabilities | Navigation Structure |
| --- | --- | --- |
| **Resident (`resident`)** | Register via 6-step onboarding + email OTP (`R2`); log in (`R1`); view dashboard (`R3`); submit & track document requests with mandatory Purok Clearance upload and download PDF certificates (`R4–R6`); file complaints & view SLA overdue status + staff responses (`R7–R8`); receive live/polled notifications (`R9`); read Purok-filtered announcements (`R10`); report emergencies auto-tagged with their Purok (`R11–R12`); manage mobile number & avatar (`R13`). | `LoginActivity` / `OnboardingActivity` → `ResidentMainActivity` hosting **4 bottom tabs** via Jetpack Navigation: **Home (R3)**, **Requests (R4→R5/R6)**, **Community (R7/R8, R10, R11/R12)**, **Profile (R13)** (+ Notification bell → R9). |
| **Staff (`staff`)** | View operational dashboard & walk-in shortcut (`S1`); verify/reject Purok Clearances, record manual payment metadata, advance request statuses & trigger PDF generation (`S2`); process SLA-sorted complaint queue with mandatory responses (`S3`); publish barangay-wide or Purok-targeted announcements (`S4`); view emergency reports grouped by Purok (`S5`), verify reports (`S6`), and dispatch relief goods without duplicate dispatches (`S7`); search/register walk-in residents with one-time credential slips & create clearance-exempt walk-in requests (`S10`). | `LoginActivity` → `StaffMainActivity` hosting **4 bottom tabs** (`Dashboard`, `Complaints`, `Emergency`, `Announcements`) + top-bar **Walk-In (`S10`)** shortcut. **No Admin tab exists in a staff menu.** |
| **Admin (`admin`)** | Everything Staff can do **plus** governance & configuration: Admin Hub (`S13`), User Management (`S12`: create staff/admin accounts with one-time credential slips, change roles with confirmation, activate/deactivate), immutable Audit Log (`S8`), Certificate Template settings (`S9`: seal, signature, signing official), Notification Center (`S11`: inspect email delivery, retry failed, cancel pending), and add-only Purok Management (`S14`). | `LoginActivity` → `StaffMainActivity` with **5 bottom tabs** (same 4 Staff tabs + **5th "Admin" tab** → `AdminHubFragment (S13)` → `S12`, `S8`, `S9`, `S11`, `S14` with back navigation to `S13`). |

### 2.2 Non-Negotiable System Rules

1. **No invented endpoints, fields, or statuses.**
   - Document Request statuses: `pending`, `processing`, `ready`, `completed`, `rejected`.
   - Payment methods: `cash`, `debit_card`, `credit_card`, `waived`, `demo` (`paymentStatus`: `pending` | `confirmed`). **No payment gateway.**
   - Complaint statuses: `pending`, `reviewing`, `resolved`, `dismissed`; priority: `normal` | `high`.
   - Emergency statuses: `reported`, `verified`, `relief_dispatched`, `resolved`.
   - Notification delivery statuses: `pending`, `sent`, `failed`, `cancelled`. **Email (Resend) + in-app only — never SMS.**
2. **No hardcoded colors, Puroks, fees, or server URLs.**
   - Use `@color/...` tokens in `app/src/main/res/values/colors.xml` and `StatusBadge` for all status pills.
   - Read Puroks live from `GET /api/puroks` and document fees from `GET /api/document-types`.
   - Configure `API_BASE_URL` via `BuildConfig` in `app/build.gradle.kts` (or `build.gradle`), not a hardcoded string in `ApiClient.java`.
3. **Hard Business Rules:**
   - **R5:** Resident cannot submit an online document request until a Purok Clearance image is attached (client + server enforced).
   - **S2:** Walk-in requests (`isWalkIn = true`) display an `"Exempted — Walk-In"` badge and hide clearance verification buttons.
   - **S6 → S7:** `"Dispatch Relief"` stays disabled until emergency report `status == "verified"`. On S7, residents who already have a `ReliefDistribution` row for that `reportId` are rendered disabled/non-interactive, backed by DB `UNIQUE ("reportId", "residentId")`.
   - **S10 / S12:** One-time generated temporary passwords on Credential Slips are returned once in the `POST` response and never persisted in plaintext or re-fetched.
   - **S14:** Puroks are add-only in v1 (no edit/delete UI or endpoint).
4. **Sensitive Field Protection:**
   - `passwordHash` never leaves the server.
   - `ctcNumber`, `ctcOrNumber`, and `disabilities` appear only on a resident's own profile (`R13` / `GET /api/profiles/me`), never in multi-resident search or staff/admin user lists.
5. **Android Version Compatibility:**
   - `minSdk` stays `23` (Android 6.0) with core library desugaring enabled so `java.time.OffsetDateTime` works on API 23–25. Do not use API > 23 methods without compatibility wrappers/fallbacks.

---

## 3. Critical Current Codebase Gaps (Must Align Across All 3 Members)

Inspecting the repository revealed several structural issues in the current code that **must be fixed as shared foundation work** before/alongside the 3 member branches so everyone's work integrates cleanly:

### 3.1 Backend SQL Schema vs. Route Query Mismatch (Critical Runtime Bug)
- `server/migrations` does not exist yet; schema lives in `server/src/database/schema.sql`, which uses **quoted camelCase** table and column names (e.g., `"documentRequests"`, `"userId"`, `"createdAt"`, `"purokClearanceUrl"`, `"slaDueAt"`, `"emergencyReports"`).
- However, current route files (`server/src/routes/documents.js`, `complaints.js`, `announcements.js`, `emergencies.js`, `notifications.js`, `puroks.js`) query **unquoted snake_case** names (e.g., `document_requests`, `user_id`, `created_at`, `sla_due_at`) that **do not exist** in `schema.sql`! (Exception: `complaints.js` mixes `slaDueAt` unquoted, which PostgreSQL folds to `sladueat`, and fails.)
- **Resolution:** Standardize all SQL queries in `server/src/` to use the exact quoted camelCase identifiers from `server/src/database/schema.sql` (and `docs/BDMS_Mobile_Database_Structure.md`).

### 3.2 Missing `OffsetDateTime` Gson Adapter & Base URL / Session Storage
- `app/src/main/java/com/barangay/bdms/network/ApiClient.java` creates `GsonConverterFactory.create()` with **no `OffsetDateTime` `JsonDeserializer`/`JsonSerializer`**. Any API JSON returning ISO-8601 timestamps into `DocumentRequest`, `Complaint`, `Announcement`, `Notification`, `ReliefDistribution`, or `AuditLog` will fail or misparse.
- `ApiClient.java` currently hardcodes `BASE_URL = "http://10.0.2.2:5000/api/"` instead of `BuildConfig.API_BASE_URL` and only stores the JWT (`KEY_JWT`) — it does not store `userId`, `role`, `purok`, or `fullName` in a shared `SessionManager` (`EncryptedSharedPreferences`).
- **Resolution (Shared Foundation):**
  - Register a `Gson` `OffsetDateTime` adapter in `ApiClient.java`.
  - Add `buildConfigField("String", "API_BASE_URL", ...)` in `app/build.gradle.kts` (`buildFeatures { buildConfig = true }`), plus a debug-only `network_security_config.xml` for `10.0.2.2`/LAN HTTP.
  - Create `SessionManager.java` (backed by `EncryptedSharedPreferences`) holding JWT, `userId`, `role`, `purok`, `fullName`, `email` so `StaffMainActivity` and all Fragments read a single source of truth.

### 3.3 Current Android UI Is Static Mockups & Isolated Activities
- **Layouts lack dynamic containers:** None of the `res/layout/screen_*.xml` files currently contain a `RecyclerView` or `SwipeRefreshLayout`; list screens (`R4`, `R7`, `R9`, `R10`, `R12`, `S2`, `S3`, `S5`, `S7`, `S8`, `S11`, `S12`, `S14`) render hardcoded static sample cards in XML.
- **Activity-per-screen vs. Jetpack Navigation:** `AndroidManifest.xml` currently declares 26 separate `AppCompatActivity` screens with manual `LinearLayout` bottom nav bars (`ResidentMainActivity`, `StaffNavHelper`), while `docs/BDMS_Mobile_Build_Plan.md` §5.2 and `GEMINI.md` specify **1 `ResidentMainActivity` + `nav_graph_resident.xml`** and **1 `StaffMainActivity` + `nav_graph_staff.xml`** using `BottomNavigationView` hosting `NavHostFragment`s (`*Fragment.java`). (A few partial Fragments already exist: `ResidentDashboardFragment`, `DocumentRequestFormFragment`, `ComplaintFormFragment`, `NotificationListFragment`.)
- **Offline/Demo Bypasses in Java:**
  - `LoginActivity` immediately skips the API if email contains `"admin"`, `"staff"`, or `"clerk"` and logs in with `"offline_demo_token"` when the server fails/errors.
  - `OnboardingWizardActivity` submits hardcoded `"maria_santos92"` data regardless of input.
  - `DocumentRequestFormActivity` / `DocumentRequestFormFragment` upload `"mock_image_bytes"` instead of a real selected image.
- **Invalid Layout Folder & Drifted S8/S9 Mockups:**
  - `app/src/main/res/staff/layout/` (`screen_staff_user_create.xml`, `screen_staff_walkin_search.xml`, `screen_staff_walkin_register.xml`, `screen_staff_walkin_request.xml`, plus `task.artifact.md`) is **not** recognized by Android Gradle's default `res/layout/` resource merger and is unused by `R.layout.*`.
  - `screen_staff_s8_audit_log.xml`, `screen_staff_s9_certificate_template.xml`, and `StaffEvacCenterActivity.java` currently show **Evacuation Center Operations / Evacuee Registration** mockups (from `stitch_bdms_mobile_ui2/s8_*` and `s9_*`) instead of **Audit Log (S8)** and **Certificate Template Settings (S9)**.
- **Missing Shared Helpers:** `PhotoPickerHelper.java` (with `FileProvider` in `AndroidManifest.xml`) and `NotificationBellView.java` do not exist yet in `ui/common/`.

### 3.4 Missing Backend Routes & Services
In `server/src/`:
- **Missing entirely:** `profiles/me` routes (`GET`, `PATCH`, `POST /avatar`), `/api/document-types` alias (currently mounted at `/api/document-requests/types` instead of `/api/document-types`), `GET /api/document-requests/:id`, `GET /api/document-requests/:id/certificate` (PDFKit generation), `POST /api/document-requests/walk-in`, `GET /api/residents/search`, `POST /api/residents/walk-in`, `GET /api/emergency-reports/:id`, `/api/relief-distributions` (`GET` & transactional `POST`), `POST /api/notifications/:id/retry` & `/cancel`, `/api/certificate-templates/:documentType` (`GET`, `PUT`), `/api/audit-logs`, `writeAuditLog()`, `slaEscalationJob.js`, `emailService.js` (Resend), `rateLimitMiddleware.js`, and authenticated upload serving (currently `/uploads` is open static).

---

## 4. Shared Foundation & Ownership Matrix (3 Members)

### Step 0 — Shared Foundation (Complete Before Parallel Work or Assign to Tech Lead / Joint PR)
These shared files affect every role and should be locked in once before members edit their role modules:
1. **Android Core:**
   - `app/build.gradle.kts`: enable `buildConfig = true`, define `API_BASE_URL`, add Socket.io Java client if using realtime sockets, ensure `minSdk = 23` + desugaring stays intact.
   - `app/src/main/AndroidManifest.xml`: add `FileProvider`, debug-only `network_security_config.xml`, clean up Activity declarations as screens migrate to Fragments.
   - `app/src/main/java/com/barangay/bdms/network/ApiClient.java` + `SessionManager.java`: `BuildConfig.API_BASE_URL`, `OffsetDateTime` Gson adapter, JWT + user session persistence (`id`, `role`, `purok`, `fullName`, `email`).
   - `app/src/main/java/com/barangay/bdms/ui/common/`: complete `PhotoPickerHelper.java`, `NotificationBellView.java`, verify `StatusBadge.java` handles `cancelled` (muted grey) and pill background shape cleanly alongside `EmptyStateView.java` and `LoadingOverlay.java`.
   - Move valid XML files from `app/src/main/res/staff/layout/*.xml` to `app/src/main/res/layout/` and delete `app/src/main/res/staff/layout/task.artifact.md`.
2. **Backend Core:**
   - `server/src/middleware/auth.js` (ensure `isActive` check or short-lived JWT ~1h per spec), `server/src/middleware/uploadMiddleware.js` (jpeg/png/webp, 5 MB limit, authenticated file serving), `server/src/services/auditLogService.js` (`writeAuditLog`), `server/src/services/emailService.js` (Resend wrapper), `server/src/services/notificationService.js` (in-app + Socket.io room `notifications:{userId}` + email tracking), `server/src/routes/puroks.js` (`"createdAt"` fix + `409` duplicate check).

### Role Ownership Matrix (Zero File Collisions)

| Area | Member 1 — Resident / User | Member 2 — Staff | Member 3 — Admin |
| --- | --- | --- | --- |
| **Screens** | **R1–R13** (`Login`, `Onboarding` 6 steps, `ResidentDashboard`, `DocumentRequestList/New/Detail`, `ComplaintList/New`, `Notifications`, `Announcements`, `EmergencyReportNew/List`, `Profile`) | **S1–S7, S10** (`StaffDashboard`, `StaffRequestQueue/Detail`, `StaffComplaintQueue`, `AnnouncementComposer`, `EmergencyResponseDashboard`, `EmergencyReportDetail`, `ReliefDispatch`, `WalkInSearch/Register/Request`) | **S8, S9, S11, S12, S13, S14** (`AdminHub`, `AuditLog`, `CertificateTemplate`, `NotificationCenter`, `UserManagement/Create`, `PurokManagement`) + **Role-Based Tab & Guard Logic** in `StaffMainActivity` |
| **Android Packages** | `ui/auth/*`, `ui/resident/*`, `res/navigation/nav_graph_resident.xml` | `ui/staff/` (`StaffDashboardFragment`, `StaffRequest*`, `StaffComplaint*`, `AnnouncementComposerFragment`, `EmergencyResponse*`, `ReliefDispatch*`, `WalkIn*`) | `ui/staff/` (`StaffMainActivity` role menu builder, `AdminHubFragment`, `AuditLogFragment`, `CertificateTemplateFragment`, `NotificationCenterFragment`, `UserManagement*`, `PurokManagementFragment`, `nav_graph_staff.xml`) |
| **Models Owned** | `Profile`, `Resident`, `DocumentRequest` (resident-visible fields), `Complaint`, `Announcement`, `EmergencyReport`, `Notification` | `DocumentRequest` (staff/payment/clearance fields), `ReliefDistribution`, `StaffMember` | `AuditLog`, `CertificateTemplate`, `Purok`, `Notification` (delivery tracking fields) |
| **API / Repo Additions** | `auth.*`, `profiles.*`, resident paths of `document-requests`, `complaints`, `announcements`, `emergency-reports`, `notifications/me`, `GET /api/document-types`, `GET /api/puroks` | staff paths of `document-requests` (`verify-clearance`, status/payment, PDF generation, `/walk-in`), `residents/search`, `residents/walk-in`, `complaints/:id/resolve` + SLA cron, `announcements` POST + fan-out, `emergencies` (`grouped-by-purok`, `verify`), `relief-distributions` | `admin/users*`, `audit-logs`, `certificate-templates`, `notifications` (admin list, `retry`, `cancel`), `puroks` POST, `writeAuditLog` enforcement on all admin actions |

> **Coordination Rule on Shared Files (`ApiService.java`, `BdmsRepository.java`, `nav_graph_staff.xml`):**  
> Each member only adds methods/destinations inside clearly commented sections (`// === MEMBER 1 ===`, `// === MEMBER 2 ===`, `// === MEMBER 3 ===`) or creates role-specific repository wrappers (`ResidentRepository`, `StaffRepository`, `AdminRepository` delegating to `ApiService`) if merging conflicts occur.

---

## 5. Master Output Verification Checklist (Checking Each Member's Output)

Use this checklist to verify the integrated work of all 3 members before marking the app complete.

### 5.1 Architectural & Code-Quality Gate (Applies to All 3 Members)
- [ ] Zero demo/offline bypasses remain in `LoginActivity`, `OnboardingActivity`, or forms (`"offline_demo_token"`, `"mock_image_bytes"`, hardcoded `"maria_santos92"` removed).
- [ ] Every list screen (`R4`, `R7`, `R9`, `R10`, `R12`, `S2`, `S3`, `S5`, `S7`, `S8`, `S11`, `S12`, `S14`) uses `RecyclerView` + `SwipeRefreshLayout` + `EmptyStateView` + `LoadingOverlay` — no static mockup cards left in production layouts.
- [ ] Every status pill uses `StatusBadge` and `@color/...` tokens (`colors.xml`), not inline hex colors.
- [ ] All `puroks` dropdowns/chips (`R2`, `S4`, `S5`, `S7`, `S10`) populate dynamically from `GET /api/puroks`.
- [ ] `ApiClient` parses `OffsetDateTime` cleanly without Gson reflection crashes, and reads `BuildConfig.API_BASE_URL`.
- [ ] Backend SQL queries match `server/src/database/schema.sql` quoted camelCase table/column names with parameterized queries (`$1, $2, ...`) only.
- [ ] App compiles (`./gradlew assembleDebug`) and runs on Android API 23+ (Marshmallow) and API 26+ (Oreo).

### 5.2 Member 1 Output Check (User / Resident Role — R1–R13)
- [ ] **R1 Login:** Valid resident logs in via `POST /api/auth/login` and lands in `ResidentMainActivity` (4 tabs). Wrong password returns error; deactivated user (`403`) is blocked.
- [ ] **R2 Onboarding:** 6-step `ViewPager2` validates password policy (`>=8 chars`, uppercase, number, special char), loads Puroks from `GET /api/puroks` as required first field on Step 3, records RA 10173 consent on Step 5, sends & verifies real 6-digit DB-persisted OTP (`otpCodes`) on Step 6.
- [ ] **R3 Dashboard:** Stat counts, recent items, and announcements reflect live API data; bell icon opens R9.
- [ ] **R4–R6 Document Requests:**
  - R5 fetches types & fees from `GET /api/document-types`; Business Clearance reveals `businessName`/`businessAddress`/`businessType`; **Submit stays disabled until a real Purok Clearance photo is selected** and uploaded via multipart `POST /api/document-requests`.
  - R6 renders control number, clearance status badge, status timeline (`pending → processing → ready → completed` / `rejected`), rejection reason (when rejected), and downloads the PDF certificate via `GET /api/document-requests/:id/certificate` when `ready`/`completed`.
- [ ] **R7–R8 Complaints:** R8 submits without a Purok field (attached from JWT); `429` rate limit shows user-friendly message; R7 displays overdue tag via `Complaint.isOverdue()` and expands inline to show `staffResponse`.
- [ ] **R9–R10 Notifications & Announcements:** R9 marks notifications read (`PATCH /api/notifications/:id/read`), deep-links via `relatedId`, updates via Socket.io or polling fallback; R10 filters expired/non-targeted announcements and pins the first `emergency` item at the top.
- [ ] **R11–R12 Emergency Reports:** R11 shows read-only Purok chip from user profile, submits multipart report, handles `409` duplicate warning ("Submit again?") and `429`; R12 lists resident's own emergency reports with `StatusBadge`.
- [ ] **R13 Profile:** Loads `GET /api/profiles/me`, allows updating `mobileNumber` and uploading avatar (`POST /api/profiles/me/avatar`), keeps Purok read-only, and clears `SessionManager` on logout.

### 5.3 Member 2 Output Check (Staff Role — S1–S7, S10)
- [ ] **Role Menu Check (Staff account):** Logging in as `staff` opens `StaffMainActivity` with **exactly 4 bottom tabs** (`Dashboard`, `Complaints`, `Emergency`, `Announcements`). **No Admin tab is visible or present in the menu.**
- [ ] **S1 Dashboard & S10 Shortcut:** Live queue counts work; tapping `"Walk-In"` opens S10.
- [ ] **S2 Request Queue & Detail:**
  - Online request shows Purok Clearance image (zoomable) with Verify/Reject buttons; advancing past `pending` fails while unverified.
  - Walk-in request (`isWalkIn = true`) hides clearance image & buttons and renders `"Exempted — Walk-In"` badge.
  - Recording payment (`paymentMethod` + `paymentOrNumber`) updates `paymentStatus = confirmed`; moving to `ready` generates a real PDF certificate on the server and sends email/in-app notice.
- [ ] **S3 Complaint Queue:** Sorted server-side (`priority DESC, slaDueAt ASC`); overdue/high-priority rows display a red left-edge bar; Resolve/Dismiss enforces non-empty `staffResponse`.
- [ ] **S4 Announcements:** Purok multi-select chips load from `GET /api/puroks`; publishing creates targeted notifications and validates Purok names server-side.
- [ ] **S5–S7 Emergency & Relief Dispatch (Flagship Flow):**
  - S5 renders Purok filter chips directly from `GET /api/emergency-reports/grouped-by-purok` keys with per-Purok counts; inline Verify transitions `reported → verified`.
  - S6 keeps `"Dispatch Relief"` disabled until `status == "verified"`.
  - S7 fetches `GET /api/relief-distributions?reportId=`, disables/greys out already-served residents, and `POST /api/relief-distributions` runs atomically (distribution + report update + audit log + notification), preventing duplicates cleanly.
- [ ] **S10 Walk-In Service:** Search excludes `passwordHash`, `ctcNumber`, `ctcOrNumber`, `disabilities`; walk-in registration displays temporary password **once** on the Credential Slip; walk-in request sets `isWalkIn = true` and `purokClearanceVerified = true` with audit log `CLEARANCE_EXEMPTED_WALKIN`.

### 5.4 Member 3 Output Check (Admin Role — S8, S9, S11–S14 & Guards)
- [ ] **Role Menu & Fragment Guards:** Logging in as `admin` shows **5 bottom tabs** with the 5th **Admin** tab opening `AdminHubFragment (S13)`. Directly attempting to navigate to S8, S9, S11, S12, S13, or S14 as `staff` pops back with `"Not authorized"` on Android and `403` on Express.
- [ ] **S13 Admin Hub:** Displays admin profile/role chip, failed-email badge count, and all 5 rows (`User Management`, `Audit Log`, `Certificate Templates`, `Notification Center`, `Manage Puroks`) navigating cleanly with back arrows returning to S13.
- [ ] **S8 Audit Log (Replaces Evac Center drift):** `StaffEvacCenterActivity` / Evacuation Center UI removed from S8; reads `GET /api/audit-logs` read-only with performer, action, target, IP, user agent, details, and timestamp.
- [ ] **S9 Certificate Templates (Replaces Evacuee Intake drift):** Loads/saves seal image, signature image, `officialName`, and `officialTitle` per document type (`GET`/`PUT /api/certificate-templates/:documentType`), reflected in generated PDFs.
- [ ] **S11 Notification Center:** Tabbed by `pending` / `sent` / `failed` / `cancelled`; shows `deliveryAttempts` and `failureReason`; Retry enabled only for `failed`, Cancel enabled only for `pending`; neither deletes rows.
- [ ] **S12 User Management:** Search excludes sensitive fields (`ctcNumber`, `ctcOrNumber`, `disabilities`, `passwordHash`); role change asks for confirmation dialog; creating staff/admin shows temporary password **once** on Credential Slip; toggle active/inactive disables/enables login.
- [ ] **S14 Purok Management:** Lists Puroks from `GET /api/puroks`, **has zero edit or delete controls**, adds new Purok via `POST /api/puroks`, shows inline duplicate error on `409`, and newly added Puroks immediately appear in R2, S4, S5, and S7.

### 5.5 Cross-Role Integration Smoke Tests
Run these 4 end-to-end tests with all 3 members' code merged:
1. **Online Document Lifecycle:** Resident (`R2` → `R5`) uploads Purok Clearance → Staff (`S2`) verifies clearance, records payment, sets `ready` (PDF generated using `S9` template) → Resident (`R6`) downloads PDF → Admin (`S8`) sees audit log entries.
2. **Walk-In Counter Flow:** Staff (`S10`) searches resident → registers walk-in → copies one-time password → creates walk-in request → Staff (`S2`) sees `"Exempted — Walk-In"` badge → Resident logs in (`R1`) with slip password.
3. **Complaint SLA Escalation:** Resident (`R8`) submits complaint → backdate `slaDueAt` in DB & run SLA job → Staff (`S3`) sees high-priority red bar at top + receives notification → Staff resolves with reply → Resident (`R7`) expands card and reads `staffResponse`.
4. **Flagship Emergency Dispatch:** Admin (`S14`) adds new Purok → Resident (`R11`) reports emergency in their Purok → Staff (`S5`/`S6`) verifies → Staff (`S7`) dispatches relief once (second attempt blocked cleanly) → Resident (`R9`/`R12`) receives notification and sees `relief_dispatched` status.
