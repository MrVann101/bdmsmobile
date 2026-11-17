# Member 3 Implementation Plan — Admin Role (S8, S9, S11–S14 & Role Guards)

**Owner:** Member 3 (Barangay Admin Governance, Configuration & Role Access Control)  
**Screens Covered:** `S13` (Admin Hub), `S12` (User Management & Create User), `S8` (Audit Log), `S9` (Certificate Template Settings), `S11` (Notification Center), `S14` (Purok Management), plus **Role-Based Navigation & Fragment Guards** in `StaffMainActivity`  
**Authoritative References:** [BDMS_Mobile_Database_Structure.md](BDMS_Mobile_Database_Structure.md) (data), [BDMS_Mobile_Build_Plan.md](BDMS_Mobile_Build_Plan.md) (behavior/API), [BDMS_Mobile_Gemini_CLI_Prompts.md](BDMS_Mobile_Gemini_CLI_Prompts.md) (screen table)

---

## 1. Current State Audit (What Exists vs. What Is Broken)

Member 3 has **the most critical design-drift cleanup** in the repository:

| Screen / File | Current State in Repo | Problems to Fix |
| --- | --- | --- |
| **Role Menu & Guards** `StaffMainActivity.java`, `StaffNavHelper.java` | Currently shows 5 bottom nav items (`navMore` → `AdminHubActivity`) to **every** staff/admin user; zero Fragment/Activity role guards exist | Build `BottomNavigationView` dynamically in `StaffMainActivity`: **`staff` = 4 tabs only** (Admin tab completely omitted, not merely hidden); **`admin` = 5 tabs** (5th `"Admin"` tab → `S13`). Add client-side `admin` guard inside S8, S9, S11, S12, S13, S14. |
| **S13** `AdminHubActivity.java`, `screen_staff_s13_admin_hub.xml` | Displays hardcoded `"Hon. Rodrigo V. Dizon"`, `"2 failed"`; S8/S9/S14 cards only show Toasts | Migrate to `AdminHubFragment`; bind real logged-in admin name/role chip from `SessionManager`; fetch failed-count badge via `GET /api/notifications?deliveryStatus=failed`; wire all 5 rows (`S12`, `S8`, `S9`, `S11`, `S14`) via `nav_graph_staff.xml` with back navigation returning to S13. |
| **S8** `StaffEvacCenterActivity.java`, `screen_staff_s8_audit_log.xml` | **WRONG SCREEN!** Currently contains **Evacuation Center Operations** mockup (`"Evacuation Center Operations"`, `"Shelter Command Depot"`, `btnIntakeEvacuee`), copied from `stitch_bdms_mobile_ui2/s8_evacuation_center_operations` | **Retire/remove `StaffEvacCenterActivity.java`**. Replace `screen_staff_s8_audit_log.xml` with the real **Audit Log Viewer** UI (`SwipeRefreshLayout`, `RecyclerView`, filters, `EmptyStateView`, `LoadingOverlay`) and create `AuditLogFragment` bound to `GET /api/audit-logs`. |
| **S9** `screen_staff_s9_certificate_template.xml` | **WRONG SCREEN!** Currently contains **Evacuee Registration** mockup (`"Evacuee Registration"`, `btnCompleteIntake`, `"Tent B-05"`), copied from `stitch_bdms_mobile_ui2/s9_evacuee_intake_registration` | Replace `screen_staff_s9_certificate_template.xml` with **Certificate Template Settings** UI (document type selector, barangay seal & signature pickers, `officialName`, `officialTitle`, Save) and create `CertificateTemplateFragment` bound to `GET`/`PUT /api/certificate-templates/:documentType`. |
| **S11** `NotificationCenterActivity.java`, `screen_staff_s11_notification_center.xml` | Static mockup cards; only wires `btnComposeAdvisory` | Migrate to `NotificationCenterFragment` with `TabLayout` filter (`pending`/`sent`/`failed`/`cancelled`), `RecyclerView`, `StatusBadge`, `deliveryAttempts`, `failureReason`, and state-bound **Retry** / **Cancel** buttons. |
| **S12** `UserManagementActivity.java`, `screen_staff_s12_user_management.xml`, `app/src/main/res/staff/layout/screen_staff_user_create.xml` | `UserManagementActivity` only shows a Toast on `btnCreateUser`; `screen_staff_user_create.xml` sits in an invalid `res/staff/layout/` directory without IDs | Move `screen_staff_user_create.xml` to `app/src/main/res/layout/`; convert `S12` into `UserManagementFragment` + `UserCreateFragment` bound to `GET /api/admin/users?q=`, `POST /api/admin/users` (one-time Credential Slip), `PATCH /api/admin/users/:id/role` (confirmation dialog), and `PATCH /api/admin/users/:id/active`. |
| **S14** `PurokManagementActivity.java`, `screen_staff_s14_purok_management.xml` | Static XML cards; `btnAddPurok` only opens an informational `AlertDialog` | Migrate to `PurokManagementFragment` bound to `GET /api/puroks` + `POST /api/puroks` bottom sheet; ensure **add-only** (zero edit/delete buttons) and show inline `409` duplicate-name error. |

---

## 2. Role-Based Navigation & Guard Rules (Non-Negotiable)

Member 3 owns the **role gating contract** across `StaffMainActivity` and all Admin destinations (`S8`, `S9`, `S11`, `S12`, `S13`, `S14`):

1. **`StaffMainActivity` Bottom Menu:**
   - Read `SessionManager.getRole()` on startup.
   - If `"staff"`: populate `BottomNavigationView` with **4 items only**: `Dashboard (S1)`, `Complaints (S3)`, `Emergency (S5)`, `Announcements (S4)`. **Never inflate and hide the Admin tab — omit the menu item completely.**
   - If `"admin"`: populate `BottomNavigationView` with those 4 items **plus** the 5th `"Admin"` item pointing to `AdminHubFragment (S13)`.
   - If `"resident"` or unauthenticated: immediately finish and redirect to `LoginActivity`.
2. **In-Fragment Guard (Second Line of Defense):**
   - Extract a small helper (e.g., `AdminGuard.requireAdmin(Fragment)`):
     ```java
     if (!sessionManager.isAdmin()) {
         Toast.makeText(requireContext(), "Not authorized", Toast.LENGTH_SHORT).show();
         NavHostFragment.findNavController(fragment).popBackStack();
         return false;
     }
     ```
   - Call this guard in `onCreate` / `onViewCreated` of **every** Admin Fragment: `AdminHubFragment (S13)`, `AuditLogFragment (S8)`, `CertificateTemplateFragment (S9)`, `NotificationCenterFragment (S11)`, `UserManagementFragment (S12)`, `UserCreateFragment (S12)`, and `PurokManagementFragment (S14)`.
3. **Server Enforcement:**
   - Every Admin Express route must use `authenticateToken, requireRole('admin')` returning `403 Forbidden` to `staff` or `resident` JWTs.

---

## 3. Shared Models to Complete (`app/src/main/java/com/barangay/bdms/model/`)

- **`AuditLog.java`:** Add `ipAddress`, `userAgent`, `metadata` (`Map<String, Object>` or `JsonObject`), plus optional joined actor display name (`performedByName` / `performedByEmail`) and getters for `performedBy`, `targetId`, `createdAt`, `ipAddress`, `userAgent`.
- **`CertificateTemplate.java`:** Add `updatedAt` (`OffsetDateTime`) and getters for `sealUrl`, `signatureUrl`, `updatedAt`.
- **`Notification.java`:** Ensure `deliveryStatus`, `deliveryAttempts`, `lastAttemptAt`, `failureReason`, `type`, `relatedId`, and recipient metadata are accessible for `S11`.
- **`Purok.java`:** Ensure `createdAt` uses `OffsetDateTime` (consistent with `ApiClient` Gson adapter) or clean ISO formatting.

---

## 4. Screen-by-Screen Implementation Specs (S13, S8, S9, S11, S12, S14)

### S13 — Admin Hub (`AdminHubFragment` / `screen_staff_s13_admin_hub.xml`)
- **Endpoint (optional badge):** `GET /api/notifications?deliveryStatus=failed`.
- **Behavior:**
  - Run `AdminGuard` immediately.
  - Populate header card with live `SessionManager.getFullName()` and `"ADMIN"` role chip (remove `"Hon. Rodrigo V. Dizon"` and fake `"Data Vault Sync"` card if it implies non-existent sync endpoints).
  - Fetch `GET /api/notifications?deliveryStatus=failed` and show `list.size() + " failed"` badge on the Notification Center row (hide badge when `0`).
  - Render the 5 tool rows in exact order, navigating inside `nav_graph_staff.xml` (each destination shows a toolbar back arrow returning to `S13`):
    1. **User Management** → `UserManagementFragment (S12)`
    2. **Audit Log** → `AuditLogFragment (S8)`
    3. **Certificate Templates** → `CertificateTemplateFragment (S9)`
    4. **Notification Center** → `NotificationCenterFragment (S11)`
    5. **Manage Puroks** → `PurokManagementFragment (S14)`

### S8 — Audit Log Viewer (`AuditLogFragment` / `screen_staff_s8_audit_log.xml`)
- **Action Required First:** Remove `StaffEvacCenterActivity.java` (and its manifest entry) and replace `screen_staff_s8_audit_log.xml` (currently Evacuation Center Operations) with a clean Audit Log layout using `@color/*` tokens and `@layout/card_container`.
- **Endpoint:** `GET /api/audit-logs` (support optional query filters like `?action=` or search).
- **Behavior:**
  - Run `AdminGuard`.
  - `SwipeRefreshLayout` + `RecyclerView` + `EmptyStateView` + `LoadingOverlay`.
  - Each row shows: `action` badge, actor (`performedBy` / actor name), `details`, `targetId`, `ipAddress` & `userAgent` (expandable or secondary line), and formatted `createdAt`.
  - **Strictly read-only** — zero edit or delete controls.

### S9 — Certificate Template Settings (`CertificateTemplateFragment` / `screen_staff_s9_certificate_template.xml`)
- **Action Required First:** Replace `screen_staff_s9_certificate_template.xml` (currently Evacuee Intake Registration) with Certificate Template Settings UI.
- **Endpoints:** `GET /api/document-types`, `GET /api/certificate-templates/:documentType`, `PUT /api/certificate-templates/:documentType` (multipart or image upload + fields).
- **Behavior:**
  - Run `AdminGuard`.
  - `documentType` dropdown populated from `GET /api/document-types`.
  - Changing `documentType` calls `GET /api/certificate-templates/:documentType` and populates:
    - Barangay Seal preview + `"Change Seal"` button (`PhotoPickerHelper`)
    - Official Signature preview + `"Change Signature"` button (`PhotoPickerHelper`)
    - `officialName` and `officialTitle` text inputs
    - Status indicator (`CertificateTemplate.isConfigured()`)
  - `"Save Template"` validates inputs, shows `LoadingOverlay`, and submits `PUT /api/certificate-templates/:documentType`.

### S11 — Notification Center (`NotificationCenterFragment` / `screen_staff_s11_notification_center.xml`)
- **Endpoints:** `GET /api/notifications` (`?deliveryStatus=`), `POST /api/notifications/:id/retry`, `POST /api/notifications/:id/cancel`.
- **Behavior:**
  - Run `AdminGuard`.
  - `TabLayout` filter tabs: `All`, `Pending`, `Sent`, `Failed`, `Cancelled`.
  - `SwipeRefreshLayout` + `RecyclerView` + `EmptyStateView` + `LoadingOverlay`.
  - Each row displays `title`, `type`, recipient (`userId` / recipient email if joined), `StatusBadge` bound to `deliveryStatus` (`pending` amber, `sent` green, `failed` red, `cancelled` muted grey), `deliveryAttempts`, `lastAttemptAt`, and `failureReason` (visible when `failed`).
  - **Hard Button-State Rule:**
    - `"Retry"` button: visible/enabled **only** when `deliveryStatus == "failed"` → calls `POST /api/notifications/:id/retry` and refreshes list.
    - `"Cancel"` button: visible/enabled **only** when `deliveryStatus == "pending"` → calls `POST /api/notifications/:id/cancel` and refreshes list.
    - Rows are **never deleted**.

### S12 — User Management (`UserManagementFragment`, `UserCreateFragment`)
- **Layouts:** `screen_staff_s12_user_management.xml` + move `screen_staff_user_create.xml` from `app/src/main/res/staff/layout/` to `app/src/main/res/layout/`.
- **Endpoints:** `GET /api/admin/users?q=`, `POST /api/admin/users`, `PATCH /api/admin/users/:id/role`, `PATCH /api/admin/users/:id/active`.
- **Behavior (`UserManagementFragment`):**
  - Run `AdminGuard`.
  - Search bar triggers `GET /api/admin/users?q=...`.
  - `SwipeRefreshLayout` + `RecyclerView` + `EmptyStateView` + `LoadingOverlay`.
  - Each row renders `fullName`, `email`, `purok`, role chip (`resident` | `staff` | `admin`), active/inactive switch (`PATCH /api/admin/users/:id/active`), and a `"Change Role"` action.
  - **Sensitive Field Rule:** Ensure `passwordHash`, `ctcNumber`, `ctcOrNumber`, and `disabilities` are never returned by `GET /api/admin/users` or rendered in row cards.
  - **Role Change Rule:** Tapping `"Change Role"` shows a role picker + **confirmation `AlertDialog`** (`"Change <name>'s role from <old> to <new>?"`) before calling `PATCH /api/admin/users/:id/role`. Prevent an admin from accidentally locking out themselves if desired.
  - FAB / button `"Create Staff/Admin Account"` navigates to `UserCreateFragment`.
- **Behavior (`UserCreateFragment`):**
  - Run `AdminGuard`.
  - Collect `fullName`, `email`, `username` (optional/derived), `mobileNumber`, `purok` (live from `GET /api/puroks`), and `role` (`staff` | `admin`).
  - Submit calls `POST /api/admin/users` (server auto-generates secure temporary password).
  - **Hard Rule — One-Time Credential Slip:** Display the returned email and temporary password inside a Credential Slip card with a Copy button. **Never store the plaintext password or re-fetch it after leaving the screen.**

### S14 — Purok Management (`PurokManagementFragment` / `screen_staff_s14_purok_management.xml`)
- **Endpoints:** `GET /api/puroks`, `POST /api/puroks`.
- **Behavior:**
  - Run `AdminGuard`.
  - `SwipeRefreshLayout` + `RecyclerView` + `EmptyStateView` + `LoadingOverlay`.
  - Display Purok `name`, `description`, and an info banner explaining that Puroks in use cannot be renamed or removed in v1.
  - **Hard Rule:** Display-only rows — **zero edit or delete buttons**.
  - `"+ Add Purok"` opens a Material `BottomSheetDialog` with `name` (required) and `description` (optional) + `"Save"`.
  - Submit calls `POST /api/puroks`:
    - `201 Created`: dismiss sheet, refresh list (immediately feeds R2, S4, S5, S7).
    - `409 Conflict`: keep sheet open and display inline error on the `name` `TextInputLayout` (`"A Purok with this name already exists"`).

---

## 5. Backend Fixes & Additions Required for Member 3 (`server/src/`)

1. **Shared `services/auditLogService.js` (used across Member 2 & Member 3 controllers):**
   - Implement `writeAuditLog(req, action, targetId, details, metadata = null, client = db)` inserting into `"auditLogs"` (`"performedBy"`, `action`, `"targetId"`, `details`, `"ipAddress"`, `"userAgent"`, `metadata`) capturing `req.ip` and `req.headers['user-agent']`.
2. **New `routes/auditLogs.js` (mount at `/api/audit-logs`, gated `requireRole('admin')`):**
   - `GET /api/audit-logs`: join `profiles` for actor `fullName`/`email`/`role` (safe columns only), support filters (`?action=`, `?performedBy=`), `ORDER BY "createdAt" DESC`.
   - **Never** create `PATCH`, `PUT`, or `DELETE` routes for `auditLogs` (immutability rule).
3. **New `routes/certificateTemplates.js` (mount at `/api/certificate-templates`):**
   - `GET /api/certificate-templates/:documentType`: `authenticateToken` (any authenticated user or staff/admin) — return template row for that `documentType`.
   - `PUT /api/certificate-templates/:documentType`: `authenticateToken, requireRole('admin')` — accept Multer uploads (`seal`, `signature`) + `officialName`, `officialTitle`; upsert into `"certificateTemplates"` updating `"updatedAt" = now()`; write audit log `TEMPLATE_UPDATED`.
4. **`routes/notifications.js` (Admin Notification Center endpoints):**
   - Fix `GET /api/notifications` (`requireRole('admin')`): fix column `"deliveryStatus"` and `"createdAt"` (currently queries `delivery_status` and `created_at`), join recipient `fullName`/`email`.
   - Add `POST /api/notifications/:id/retry` (`requireRole('admin')`): verify notification is `failed`, re-attempt Resend email send via `emailService`, increment `"deliveryAttempts"`, update `"lastAttemptAt" = now()`, set `"deliveryStatus" = 'sent'` (or `'failed'` + `"failureReason"`), write audit log `NOTIFICATION_RETRIED`.
   - Add `POST /api/notifications/:id/cancel` (`requireRole('admin')`): verify notification is `pending`, set `"deliveryStatus" = 'cancelled'` (never delete row), write audit log `NOTIFICATION_CANCELLED`.
5. **`routes/admin.js` (User Management fixes):**
   - `GET /api/admin/users`: confirm `ctcNumber`, `ctcOrNumber`, `disabilities`, `passwordHash` are excluded from `SELECT` (already safe in `admin.js`, keep it strict).
   - `POST /api/admin/users`: generate a strong random temporary password server-side if omitted (do not default to `'Staff@123'`), store bcrypt hash, write audit log `ADMIN_USER_CREATED`, and return `{ user, temporaryPassword }` **once** in the `201` response.
   - `PATCH /api/admin/users/:id/role`: validate `role` is `'resident' | 'staff' | 'admin'`, update DB, write audit log `USER_ROLE_CHANGED` (recording old & new role in `details`/`metadata`).
   - `PATCH /api/admin/users/:id/active`: update `"isActive"`, write audit log `USER_ACTIVE_CHANGED`.
6. **`routes/puroks.js`:**
   - Fix `SELECT name, description, "createdAt" FROM puroks ORDER BY name ASC` (currently uses unquoted `created_at`).
   - On `POST /api/puroks` (`requireRole('admin')`): catch PostgreSQL unique violation (`err.code === '23505'`) and return **`409`** (`{ error: 'Purok already exists' }`) instead of `500`, and write audit log `PUROK_ADDED`.
   - Do **not** add `PUT`, `PATCH`, or `DELETE` routes for `puroks`.

---

## 6. Conflict Boundaries (Do Not Touch Without Syncing)

- Do **not** alter Resident screens (`R1–R13`) or Staff operational screens (`S1–S7`, `S10`) other than wiring `StaffMainActivity`'s 4-tab vs. 5-tab `BottomNavigationView` and `nav_graph_staff.xml` cleanly with Member 2.
- Ensure `StaffEvacCenterActivity.java` is cleanly removed from `AndroidManifest.xml` and `ui/staff/` so it does not confuse QA or break compilation.

---

## 7. Member 3 Self-Test Checklist

- [ ] Logging in as `staff` shows **4 tabs only** (no Admin tab anywhere in `BottomNavigationView`); logging in as `admin` shows **5 tabs** with Admin opening `S13`.
- [ ] Attempting to open `S8`, `S9`, `S11`, `S12`, `S13`, or `S14` as a `staff` user triggers `"Not authorized"` + popBack on Android and `403` on Express.
- [ ] `S8` displays real audit log records (no Evacuation Center UI remains); zero edit/delete options exist.
- [ ] `S9` saves real seal/signature images and official name/title per document type, and Member 2's PDF generator reflects the updated template.
- [ ] `S11` shows `failed` rows with `failureReason`, Retry enabled only on `failed`, Cancel enabled only on `pending`, and preserves row history.
- [ ] `S12` never exposes `ctcNumber`/`ctcOrNumber`/`disabilities`/`passwordHash`, prompts confirmation before role changes, and shows temporary password **once** on user creation.
- [ ] `S14` has no edit/delete buttons, rejects duplicate Purok names with an inline `409` error, and newly added Puroks appear immediately in `R2`, `S4`, `S5`, and `S7`.
