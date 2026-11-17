# BDMS Mobile — Project Context (read this first)

## What this project is

BDMS Mobile is a native Android (Java) app for a Philippine barangay: document requests
(with Purok Clearance and walk-in service), complaints with SLA escalation, announcements,
and emergency reports with Purok-based relief dispatch. The backend is a separate
Node.js/Express + PostgreSQL API (the `server/` folder). My job in this repo is building the
Android client, one screen at a time, from designs I already generated in Stitch.

## Documents (in `docs/`) and which one wins

| File | Use it for |
|---|---|
| `docs/BDMS_Mobile_Database_Structure.md` | Exact field names, types, and data rules. **Wins on anything about data.** |
| `docs/BDMS_Mobile_Build_Plan.md` | Behavior, API endpoints, screen specs (Phase 5), role rules. **Wins on behavior, API, and screens.** |
| `docs/BDMS_Mobile_Class_Object_Model.md` | The Java `model/` classes only. It is narrower than the database (e.g. fewer `Profile` fields). |
| `docs/BDMS_Mobile_Gemini_CLI_Prompts.md` | The per-screen build prompt template and the parameter tables (layout file, Fragment, endpoints, models per screen). |

If documents disagree, follow that order and tell me about the conflict. Do not guess.

## Working rules

1. **Never invent** an endpoint, field name, or status value. If a design implies something
   that is not in the docs, flag it and ask.
2. **Never hardcode** hex colors in layouts (use `colors.xml` tokens), Purok names (read
   `GET /api/puroks`), fees, or the API base URL (use `BuildConfig`).
3. **Work one screen at a time**, in order R1 → R13, then S1 → S14. After each screen, list
   every file you created or modified.
4. **Stop and ask** when something needs a human: real keys or secrets, accounts, the
   Stitch connection, real barangay content. Don't substitute a placeholder and carry on.
5. **Report, don't silently fix.** When reviewing, list problems first and wait for my OK.
6. **Roles:** the staff app shows 4 tabs for `staff` and a 5th "Admin" tab only for
   `admin`. Leave the Admin tab out of a staff account's menu entirely, and guard S8, S9,
   S11, S12, S13 and S14 inside each Fragment too.
7. **Sensitive fields** (`passwordHash`, `ctcNumber`, `ctcOrNumber`, `disabilities`) never
   appear on any list of multiple residents.
8. **Android versions:** `minSdk` is 23 (Android 6.0 Marshmallow). Leave `compileSdk` and
   `targetSdk` at Android Studio's default. Enable core library desugaring in
   `app/build.gradle`, because `java.time.OffsetDateTime` is not available natively below
   API 26. Don't use an API newer than 23 without a fallback.

## How to build a screen

When I say "build <ID>" (for example "build R1"): find that ID's row in the parameter
tables in `docs/BDMS_Mobile_Gemini_CLI_Prompts.md`, fetch the Stitch design named in that
row using the Stitch MCP tool, then follow the per-screen build prompt template in the
same file. The layout file, Fragment class, models, and endpoints come from that row.

## Project context

```
You are building the Android (Java) client for "BDMS Mobile" — a Barangay Document,
Complaint, and Emergency Response Management System. Use the following project context
for every screen you build; don't ask me to repeat it.

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
```
