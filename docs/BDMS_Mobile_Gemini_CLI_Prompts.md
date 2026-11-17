# BDMS Mobile — Gemini CLI Prompts (Stitch → Android)

A prompt library for **Gemini CLI**, for the step after `BDMS_Mobile_Stitch_UI_Prompts.md`: fetching each generated Stitch design and turning it into real Android XML layouts + Java code inside the project structure already defined in `BDMS_Mobile_Build_Plan.md`.

## How to use this

1. Run the **Master Project Context Prompt** once at the start of your Gemini CLI session (or once per project if your CLI setup persists context across sessions). This gives Gemini the package structure, design tokens, and conventions so every screen it builds afterward is consistent.
2. For each screen, fill in the **Reusable Per-Screen Build Prompt** template using the matching row from the **Screen Parameter Tables** below, and run it.
3. Build screens in the same order you generated them in Stitch — Resident screens first (R1–R13), then Staff/Admin (S1–S14) — since later screens sometimes reuse components (like `StatusBadge`) that earlier ones define.
4. After all screens in a set are built, run the **Navigation Wiring Prompt** once to connect them.
5. Run the **QA/Consistency Prompt** at the end to catch anything that drifted from the build plan during generation.

---

## Master Project Context Prompt

> Paste this once at the start of your Gemini CLI session, before building any screen.

```
You are building the Android (Java) client for "BDMS Mobile" — a Barangay Document,
Complaint, and Emergency Response Management System. Use the following project context
for every screen you build in this session; don't ask me to repeat it.

STACK: Native Android, Java (no Kotlin). XML layouts + Material Components for Android
(Material Design 3). Retrofit + OkHttp for networking against a custom Node.js/Express
REST API (base URL will be provided separately via BuildConfig — do not hardcode it).
Navigation via the Jetpack Navigation Component (Java-compatible), single Activity per
role with a BottomNavigationView hosting NavHostFragments.

PACKAGE STRUCTURE (create files under this exact layout):
src/main/java/com/barangay/bdms/
  model/        — Profile, Resident, StaffMember, DocumentRequest, Complaint,
                  Announcement, EmergencyReport, ReliefDistribution, Notification,
                  CertificateTemplate, AuditLog, Purok (name, description) (plain Java POJOs, fields matching the
                  Node API's JSON field names exactly, camelCase)
  network/      — ApiService.java (Retrofit interface), ApiClient.java (Retrofit/OkHttp
                  singleton with a Bearer-token auth interceptor)
  repository/   — BdmsRepository.java (wraps ApiService calls, maps JSON to model
                  classes, exposes simple callback/LiveData-style methods to the UI)
  ui/auth/      — LoginActivity, OnboardingActivity + step Fragments
  ui/resident/  — one Fragment per Resident screen (R1–R13)
  ui/staff/     — one Fragment per Staff/Admin screen (S1–S14)
  ui/common/    — shared custom views: StatusBadge, EmptyStateView, LoadingOverlay,
                  NotificationBellView, PhotoPickerHelper

DESIGN TOKENS (put these in res/values/colors.xml once, then reference by name in every
layout you generate — never hardcode hex values inline in a layout file):
  colorPrimary        #059669
  colorPrimaryDark    #047857
  colorPrimaryLight   #D1FAE5
  colorBackground     #FCFCFC
  colorSurface        #FFFFFF
  colorTextPrimary    #171717
  colorTextSecondary  #52525B
  colorTextMuted      #A1A1AA
  colorOutline        #E5E5E5
  statusPendingBg #FFFBEB  statusPendingText #B45309
  statusProcessingBg #EFF6FF  statusProcessingText #1D4ED8
  statusReadyBg #ECFEFF  statusReadyText #0E7490
  statusCompletedBg #F0FDF4  statusCompletedText #15803D
  statusRejectedBg #FEF2F2  statusRejectedText #B91C1C

CONVENTIONS:
- Every generated layout file is named screen_<snake_case_screen_name>.xml
  (e.g. screen_document_request_list.xml), with a matching Fragment class
  <PascalCaseScreenName>Fragment.java in the appropriate ui/ subpackage.
- Cards use a reusable `@layout/card_container` style (12dp corner radius, 1dp
  elevation) rather than repeating the same CardView attributes in every file.
- Status badges are always rendered via the shared `StatusBadge` custom view, never a
  plain TextView with manually-set colors.
- Every screen that lists data wraps its RecyclerView in a SwipeRefreshLayout and shows
  `EmptyStateView` when the list is empty, and `LoadingOverlay` while the initial fetch
  is in flight.
- Never invent an API endpoint, field name, or status value that isn't in the parameter
  table I give you for that screen — if something in the fetched design implies data
  that doesn't map to a known endpoint/field, flag it to me instead of guessing.

Confirm you've internalized this context, then wait for my first per-screen build
prompt.
```

---

## Reusable Per-Screen Build Prompt Template

> Copy this block for each screen, filling in the bracketed values from the parameter tables below.

```
Fetch the Stitch design named "[STITCH_DESIGN_NAME]" using the Stitch MCP tool
available in this session.

Build it as:
- Layout file: res/layout/[LAYOUT_FILE].xml
- Fragment/Activity class: [JAVA_CLASS] in package [PACKAGE]

Data binding:
- Model class(es): [MODEL_CLASSES]
- API call(s) via BdmsRepository → ApiService: [ENDPOINTS]
- [EXTRA_NOTES]

Follow the Master Project Context exactly — reuse colors.xml tokens, the shared
StatusBadge/EmptyStateView/LoadingOverlay components, and the card_container style
rather than redefining them. Match the fetched Stitch design's layout and content as
closely as Android Views/Material Components allow; where something in the design
can't be represented directly (e.g. a shadow style, an exact spacing value), pick the
closest Material Design 3 equivalent and note the substitution in a one-line code
comment.

After generating the files, list exactly what you created/modified.
```

---

## Screen Parameter Tables

### Resident Screens (R1–R13)

| ID | Stitch design name | Layout file | Java class (package `ui.resident` unless noted) | Model(s) | Endpoint(s) | Extra notes |
|---|---|---|---|---|---|---|
| R1 | Login | `screen_login.xml` | `LoginFragment` (`ui.auth`) | Profile | `POST /api/auth/login` | On success, store JWT and route to `ResidentMainActivity` or `StaffMainActivity` based on returned `role`. |
| R2 | Onboarding — Step 3 of 6 | `screen_onboarding_step3.xml` (build all 6 steps as separate layouts/Fragments in one `ViewPager2`) | `OnboardingActivity` + `OnboardingStepXFragment` (`ui.auth`) | Profile | `POST /api/auth/register`, `POST /api/auth/send-otp`, `POST /api/auth/verify-otp`, `GET /api/puroks` | The Purok field on Step 3 is a dropdown populated live from `GET /api/puroks` — never hardcode a Purok list. |
| R3 | Resident Home Dashboard | `screen_resident_dashboard.xml` | `ResidentDashboardFragment` | DocumentRequest, Complaint, Notification, Announcement | `GET /api/document-requests`, `GET /api/complaints`, `GET /api/notifications/me`, `GET /api/announcements` | Stat cards are counts derived client-side from the fetched lists; no separate "stats" endpoint exists. |
| R4 | My Document Requests (list) | `screen_document_request_list.xml` | `DocumentRequestListFragment` | DocumentRequest | `GET /api/document-requests` | FAB navigates to R5. |
| R5 | New Document Request | `screen_document_request_new.xml` | `DocumentRequestFormFragment` | DocumentRequest | `GET /api/document-types` (dropdown + fee preview), `POST /api/document-requests` (multipart) | Submit button must stay disabled until a Purok Clearance photo is attached — enforce this client-side in addition to the server-side check. |
| R6 | Document Request Detail | `screen_document_request_detail.xml` | `DocumentRequestDetailFragment` | DocumentRequest | `GET /api/document-requests/:id`, `GET /api/document-requests/:id/certificate` | Render the status timeline from `status`/`paymentStatus`/`purokClearanceVerified`/`completedAt` — don't invent a separate timeline endpoint. |
| R7 | My Complaints (list) | `screen_complaint_list.xml` | `ComplaintListFragment` | Complaint | `GET /api/complaints` | "Overdue" tag renders when `Complaint.isOverdue()` (mirror the Java model method, don't recompute the logic inline in the Fragment). Tapping a card expands it inline to show `staffResponse` — there is no separate complaint detail screen. |
| R8 | New Complaint | `screen_complaint_new.xml` | `ComplaintFormFragment` | Complaint | `POST /api/complaints` (multipart if photo attached) | No Purok field in this form — it's attached server-side from the JWT. |
| R9 | Notifications | `screen_notifications.xml` | `NotificationListFragment` | Notification | `GET /api/notifications/me`, `PATCH /api/notifications/:id/read` | Also wire the Socket.io listener here (or the 15–30s polling fallback) to refresh the list live. |
| R10 | Announcement Feed | `screen_announcement_feed.xml` | `AnnouncementFeedFragment` | Announcement | `GET /api/announcements` | Pin the first `category == "emergency"` item to the top of the list client-side. |
| R11 | Report Emergency | `screen_emergency_report_new.xml` | `EmergencyReportFormFragment` | EmergencyReport | `POST /api/emergency-reports` (multipart) | The Purok chip is read-only — populate it from the logged-in user's `Profile.getPurok()`, never an editable field. |
| R12 | My Emergency Reports | `screen_emergency_report_list.xml` | `EmergencyReportListFragment` | EmergencyReport | `GET /api/emergency-reports` | — |
| R13 | My Profile | `screen_profile.xml` | `ProfileFragment` | Profile | `GET /api/profiles/me`, `PATCH /api/profiles/me`, `POST /api/profiles/me/avatar` | Purok field renders read-only (lock icon) — no PATCH path should allow changing it from this screen. |

### Staff / Admin Screens (S1–S14)

| ID | Stitch design name | Layout file | Java class (package `ui.staff`) | Model(s) | Endpoint(s) | Extra notes |
|---|---|---|---|---|---|---|
| S1 | Staff Dashboard | `screen_staff_dashboard.xml` | `StaffDashboardFragment` | DocumentRequest, Complaint, EmergencyReport | Aggregated `GET` calls to the relevant list endpoints | "Walk-In" button in the top bar navigates to S10. |
| S2 | Document Request Queue & Detail | `screen_staff_request_queue.xml`, `screen_staff_request_detail.xml` | `StaffRequestQueueFragment`, `StaffRequestDetailFragment` | DocumentRequest | `GET /api/document-requests`, `PATCH /api/document-requests/:id` (also records payment: `paymentMethod`, `paymentOrNumber` → `paymentStatus = confirmed`), `PATCH /api/document-requests/:id/verify-clearance` | Render the walk-in "Exempted" badge instead of the clearance image/verify buttons whenever `isWalkIn == true`. |
| S3 | Complaint Queue | `screen_staff_complaint_queue.xml` | `StaffComplaintQueueFragment` | Complaint | `GET /api/complaints`, `PATCH /api/complaints/:id/resolve` | Sort list by `(priority desc, slaDueAt asc)` — request the server do this via `ORDER BY`; don't re-sort a large list client-side. The detail view has a multiline staff-response field plus Resolve / Dismiss buttons; both require a response. |
| S4 | New Announcement | `screen_staff_announcement_new.xml` | `AnnouncementComposerFragment` | Announcement | `POST /api/announcements`, `GET /api/puroks` | Purok chip list populates live from `GET /api/puroks`, same as R2 — reuse the same adapter/component if practical. |
| S5 | Emergency Response Dashboard | `screen_staff_emergency_dashboard.xml` | `EmergencyResponseDashboardFragment` | EmergencyReport | `GET /api/emergency-reports/grouped-by-purok` | The server already returns data grouped by Purok — render the filter chips from the response's keys, don't re-group client-side. |
| S6 | Emergency Report Detail | `screen_staff_emergency_detail.xml` | `EmergencyReportDetailFragment` | EmergencyReport | `GET /api/emergency-reports/:id`, `PATCH /api/emergency-reports/:id/verify` | "Dispatch Relief" button navigates to S7, and stays disabled until `status == "verified"`. |
| S7 | Relief Dispatch | `screen_staff_relief_dispatch.xml` | `ReliefDispatchFragment` | EmergencyReport, ReliefDistribution | `GET /api/relief-distributions?reportId=`, `POST /api/relief-distributions` | Fetch existing distributions first to grey out already-served residents before rendering the checklist. |
| S8 | Audit Log | `screen_staff_audit_log.xml` | `AuditLogFragment` | AuditLog | `GET /api/audit-logs` | Admin-only — guard navigation to this Fragment on the client in addition to the server's `roleMiddleware` check. |
| S9 | Certificate Template Settings | `screen_staff_certificate_template.xml` | `CertificateTemplateFragment` | CertificateTemplate | `GET /api/certificate-templates/:documentType`, `PUT /api/certificate-templates/:documentType` | Admin-only, same guard note as S8. |
| S10 | Walk-In Service (3 frames) | `screen_staff_walkin_search.xml`, `screen_staff_walkin_register.xml`, `screen_staff_walkin_request.xml` | `WalkInSearchFragment`, `WalkInRegisterFragment`, `WalkInRequestFragment` | Profile, DocumentRequest | `GET /api/residents/search`, `POST /api/residents/walk-in`, `POST /api/document-requests/walk-in` | The Credential Slip card's password is shown exactly once, from the register-call response — never persist or re-fetch it. |
| S11 | Notification Center | `screen_staff_notification_center.xml` | `NotificationCenterFragment` | Notification | `GET /api/notifications`, `POST /api/notifications/:id/retry`, `POST /api/notifications/:id/cancel` | Admin-only. "Retry"/"Cancel" buttons bind their enabled state directly to `deliveryStatus`, per row. |
| S12 | User Management | `screen_staff_user_management.xml`, `screen_staff_user_create.xml` | `UserManagementFragment`, `UserCreateFragment` | Profile | `GET /api/admin/users?q=`, `POST /api/admin/users`, `PATCH /api/admin/users/:id/role`, `PATCH /api/admin/users/:id/active` | Admin-only, same guard note as S8. The temporary password in the credential slip is shown once, from the create response — never persisted or re-fetched. Role changes need a confirmation dialog. |
| S13 | Admin Hub | `screen_staff_admin_hub.xml` | `AdminHubFragment` | — | Optional, for the failed-count badge: `GET /api/notifications?deliveryStatus=failed` (use the list length) | Start destination of the admin-only 5th tab. Five rows navigate to S12, S8, S9, S11, and S14. The tab exists only when the stored role is `admin`; this Fragment also guards itself (pop back + "Not authorized" if not admin). |
| S14 | Purok Management | `screen_staff_purok_management.xml` | `PurokManagementFragment` | Purok (simple POJO: `name`, `description`) | `GET /api/puroks`, `POST /api/puroks` | Admin-only. Add-only: no edit or delete controls in v1, because profiles and reports reference the Purok name. Show the server's duplicate-name error inline in the add sheet. |

---

## Navigation Wiring Prompt

> Run once after all screens in a set (Resident or Staff/Admin) are built.

```
All [Resident / Staff & Admin] screen Fragments now exist under
com.barangay.bdms.ui.[resident/staff]. Wire them into a single navigation graph:

- Create res/navigation/nav_graph_[resident/staff].xml using the Jetpack Navigation
  Component.
- [Resident]: host it inside ResidentMainActivity with a BottomNavigationView with 4
  tabs — Home (R3), Requests (R4 → R5/R6), Community (sub-tabbed R7/R8, R10, R11/R12),
  Profile (R13). Login/Onboarding (R1/R2) stay outside this graph, in their own
  Activities.
  [Staff/Admin]: host it inside StaffMainActivity. Build the BottomNavigationView from
  the logged-in account's role, read from the stored login response:
    - role == staff: 4 tabs — Dashboard (S1), Complaints (S3), Emergency Response
      (S5 → S6 → S7), Announcements (S4).
    - role == admin: the same 4 tabs plus a 5th "Admin" tab whose start destination is
      the Admin Hub (S13). From S13, navigate to S12 (User Management), S8 (Audit Log),
      S9 (Certificate Templates), S11 (Notification Center), and S14 (Purok Management),
      each with a back arrow returning to S13.
  Never add the Admin tab to a staff account's menu — leave it out entirely rather than
  hiding it. As a second line of defense, S8, S9, S11, S12, S13 and S14 must each check
  the role when they open and, if it is not admin, pop back and show "Not authorized"
  (the server also returns 403). S10 (Walk-In) is reached via the shortcut button on S1,
  not its own tab.
- Wire every "tap a list row → open detail" and "tap FAB → open form" transition called
  out in the per-screen notes above as a navigation action in the graph, with the
  correct argument (e.g. requestId, reportId) passed via Safe Args or a Bundle.
- Confirm back-stack behavior: tapping a bottom nav tab should not stack duplicate
  destinations: use popUpTo/singleTop on each tab's start destination.

List the navigation actions you created.
```

---

## QA / Consistency Prompt

> Run at the end, after navigation is wired, to catch drift.

```
Review everything built in this session against BDMS_Mobile_Build_Plan.md — in
particular the screen spec in Phase 5 and the "Must visibly include" table in Phase 6.1.
For each screen (R1–R13, S1–S14), check:

1. Does the Fragment call the exact endpoint(s) listed in its parameter table row — no
   invented endpoints, no missing ones?
2. Does every status value rendered (pending/processing/ready/completed/rejected,
   reported/verified/relief_dispatched/resolved, pending/sent/failed/cancelled) use the
   shared StatusBadge component with the correct color token, not a hardcoded color?
3. Are the three hard business rules visually enforced, not just present in the layout:
   - R5: Submit disabled until a Purok Clearance photo is attached
   - S2: the walk-in exemption badge replaces the clearance card when isWalkIn == true
   - S7: already-served residents render non-interactive, not just a different color
4. Does the Purok dropdown/chip list on R2, S4, S5, and S7 come from
   GET /api/puroks live, with no hardcoded Purok name anywhere in code?
5. Are passwordHash, ctcNumber, ctcOrNumber, and disabilities excluded from any screen
   that lists multiple residents (S10 search results, any staff list), and only shown
   on a resident's own R13 Profile screen?
6. Role-based navigation: log in as a staff account and confirm exactly 4 tabs and no
   Admin tab; log in as an admin and confirm 5 tabs, with the Admin tab opening S13.
   Confirm S8, S9, S11, S12, S13 and S14 each guard themselves and cannot be reached by
   a staff account through any route.
7. Admin-screen rules: S14 has no edit or delete controls; S12 asks for confirmation
   before a role change; the credential-slip password on S10 and S12 is shown once and
   never stored or re-fetched.

Report findings as a short table: screen ID, check, pass/fail, and what to fix if it
fails. Don't silently fix anything — list it first so I can confirm before you change
code.
```
