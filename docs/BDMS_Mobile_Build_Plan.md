# BDMS Mobile — Build Plan

**Barangay Document, Complaint, and Emergency Response Management System**

An Android (Java) app for residents and barangay staff, backed by a Node.js/Express API and a PostgreSQL database. It is the mobile version of the web-based Barangay Document Management System described in the team's Research Paper, plus three new features: **complaints** (with SLA escalation), **announcements**, and **emergency reporting with Purok-based relief dispatch**.

This file is written to be handed to a coding agent. It tells the agent what to build, in what order, and — just as important — which steps only a human can do.

---

## Part A — Read This First

### A.1 Task labels

Every checklist item below is tagged:

- 🤖 **AGENT** — code, SQL, XML layouts, config templates, documentation. The agent does these without asking.
- 🧑 **HUMAN** — needs a real account, a secret, a dashboard, a judgment call, or real barangay content. **The agent must stop and ask the human**, or ask them to paste the result. It must never substitute a placeholder and carry on silently.
- 🤝 **AGENT DRAFTS, HUMAN RUNS/APPROVES** — the agent produces the artifact (a migration, a keystore command); the human runs it against a real system or reviews it first.

### A.2 Companion documents and which one wins

| File | Purpose |
| --- | --- |
| `BDMS_Mobile_Database_Structure.docx` | **Authoritative database schema** (11 tables, every column explained). If it ever disagrees with this file on data, it wins. |
| `BDMS_Mobile_Build_Plan.md` (this file) | Behavior, API contract, screens, order of work. |
| `BDMS_Mobile_Class_Object_Model.docx` | Java `model/` and `service/` classes for the Android app. |
| `BDMS_Mobile_Project_Proposal.docx` | Academic proposal for the professor. |
| `BDMS_Mobile_Stitch_UI_Prompts.md` | Ordered prompts for designing every screen in Stitch. |
| `BDMS_Mobile_Gemini_CLI_Prompts.md` | Prompts and parameter tables for building each Stitch design into Android code. |
| `research-paper-skill.skill` | Writing skill for the capstone paper (Chapters 1–5). |

> **Known drift:** the Proposal and the Class/Object Model still describe the earlier 9-table schema and a smaller attribute set. For anything about data, follow the Database Structure doc and Phase 2 of this plan. `passwordHash` intentionally has no field in the Android `Profile` class — it never leaves the server.

### A.3 Settled decisions (do not reverse)

| Decision | Detail |
| --- | --- |
| Backend | Self-built **Node.js/Express** API on **plain PostgreSQL** (professor requirement). No managed backend service. |
| Language | **Java only** on Android. XML Views + Material Components. No Kotlin, no Jetpack Compose. |
| Android versions | **`minSdk` 23 (Marshmallow)**, per the professor, so the app runs on older phones and lighter emulators. Test on Marshmallow and Oreo (API 26/27); there is no upper limit, so newer phones still work. Keep `compileSdk` and `targetSdk` at Android Studio's default. Enable **core library desugaring** (`java.time` needs API 26 natively). |
| Auth | Custom JWT + bcrypt. Authorization is enforced in Express middleware, not the database. |
| Notifications | **Email (Resend) + in-app only. No SMS** — out of scope for cost. This includes emergency broadcasts. |
| Payments | **No payment gateway** (the paper's stated limitation). Payment fields are staff-recorded metadata. |
| Offline | No offline mode in v1 (also a stated limitation). |
| Single barangay | One barangay deployment, not multi-tenant. |
| Purok | Two different things: **Purok** = the resident's location (a lookup table), and **Purok Clearance** = an uploaded endorsement document required for online document requests. Never conflate them. |

### A.4 Tech stack

| Layer | Choice |
| --- | --- |
| Android | Android Studio, Java, XML Views, Material Components (Material Design 3), Jetpack Navigation Component, Retrofit + OkHttp |
| Navigation | One main Activity per role, `BottomNavigationView` hosting `NavHostFragment`s |
| Backend | Node.js (LTS) + Express |
| Database | PostgreSQL, accessed with the `pg` driver and parameterized SQL |
| Auth | `jsonwebtoken` + `bcrypt` |
| Uploads | `multer`, files on the server's disk (`server/uploads/`) |
| Certificates | `pdfkit` (pure JavaScript, no headless browser) |
| Email | Resend |
| Realtime | Socket.io, with 15–30 second polling as the fallback |
| Scheduled jobs | `node-cron` inside the Node process |
| Design / UI build | Stitch (design) → Gemini CLI (build) |
| Testing | Jest + Supertest (API), JUnit (Android models) |

### A.5 Roles

| Role | Can |
| --- | --- |
| **resident** | Manage own profile; create document requests (with Purok Clearance upload), complaints, and emergency reports; read announcements; read own notifications. |
| **staff** | Everything staff-facing: process requests and verify clearances, record payments, work the complaint queue, publish announcements, run emergency response and relief dispatch, and run the walk-in service. |
| **admin** | Everything staff can do, plus: audit log, certificate templates, Notification Center, Purok list management, and user management (create staff/admin accounts, assign roles, deactivate). |

### A.6 Who does what

| If the task needs… | Who |
| --- | --- |
| An account signup, a credit card, an email/phone verification | 🧑 |
| A secret, password, connection string, or token | 🧑 obtains it; the agent only reads it from a git-ignored `.env` |
| Java, Node.js, SQL, XML, Retrofit interfaces | 🤖 |
| Installing/provisioning PostgreSQL or hosting the Node server | 🧑 |
| Approving each Stitch design | 🧑 |
| Real barangay content (Purok names, seal, official's name, fees) | 🧑 |

| Hand to the agent (in a git-ignored `.env`, never in chat or git) | Keep to yourself |
| --- | --- |
| This plan, the companion docs, and the two prompt files | Production database admin password |
| `DATABASE_URL` for a dev database | Production `JWT_SECRET` |
| Dev `JWT_SECRET`, Resend API key | Android signing keystore and its passwords |
| Purok list, seal, official's details, fees, RA 10173 wording | Hosting and domain-registrar logins |

### A.7 Order of work

| Step | What | Who | Phase |
| --- | --- | --- | --- |
| 1 | Get the proposal approved before building (Research Paper Skill if you still need Chapters 1–5) | 🧑 | — |
| 2 | Install Node.js, Android Studio, PostgreSQL; create a Resend account; confirm package name `com.barangay.bdms` | 🧑 | 0 |
| 3 | Gather real barangay content: Purok names, seal, official, fees, consent wording | 🧑 | 1 |
| 4 | Create the database, seed Puroks and the first admin | 🤝 | 2 |
| 5 | Build the Express API; test every endpoint in Postman/Insomnia **before** touching Android | 🤖 | 3 |
| 6 | Scaffold Android: models, `ApiService`, `ApiClient`, `BdmsRepository` | 🤖 | 4 |
| 7 | Review the screen spec | 🧑 | 5 |
| 8 | Design all 27 screens in Stitch, one at a time, R1→R13 then S1→S14 | 🧑 + Stitch | 6 |
| 9 | Build each approved design with Gemini CLI, wire navigation, run the QA prompt | 🤖 + 🧑 review | 6 |
| 10 | Finish feature logic module by module | 🤖 | 7–12 |
| 11 | Add rate limiting and audit logging | 🤖 | 13–14 |
| 12 | Test everything and walk the flagship flows yourself | 🤖 + 🧑 | 15 |
| 13 | Choose hosting, make the keystore, seed demo data, rehearse | 🧑 | 16 |

---

## Part B — Foundation

## Phase 0 — Environment Setup

**Goal:** every tool installed and one empty project in each of `server/` and `app/`.

- [ ] 🧑 Install **Node.js (LTS)** and `npm`.
- [ ] 🧑 Install **PostgreSQL** locally (installer, Docker, or Postgres.app) **or** create a hosted instance (Render, Railway, Neon, etc.). Note the connection string: `postgresql://user:password@host:port/dbname`.
- [ ] 🧑 Install **Android Studio** (latest stable) and the Android SDK.
- [ ] 🧑 Create a **Resend** account, verify a sending domain (or use the sandbox domain while developing), and copy the API key.
- [ ] 🧑 Confirm the package name **`com.barangay.bdms`**. Every companion doc assumes it; renaming later touches every file.
- [ ] 🤖 Scaffold `server/` (Express) and `app/` (Android Studio project) as sibling folders.
- [ ] 🤖 Initialize `server/package.json` with: `express`, `pg`, `bcrypt`, `jsonwebtoken`, `multer`, `dotenv`, `cors`, `node-cron`, `socket.io`, `pdfkit`, `resend`; dev: `nodemon`, `jest`, `supertest`.
- [ ] 🤝 Agent writes `server/.env.example` (`DATABASE_URL`, `JWT_SECRET`, `RESEND_API_KEY`, `RESEND_FROM_EMAIL`, `PORT`); the human creates the real, git-ignored `server/.env`.
- [ ] 🤖 Add Android dependencies: `retrofit2`, `converter-gson`, `okhttp3`, a Socket.io Java client, `androidx.navigation:navigation-fragment` and `navigation-ui`, `com.google.android.material:material`, `androidx.recyclerview`, `androidx.swiperefreshlayout`, `androidx.lifecycle:lifecycle-viewmodel`, `androidx.security:security-crypto`. Do **not** add `androidx.room` — the API is the only source of truth in v1.
- [ ] 🤝 Agent writes `GET /api/health`; human runs `npm run dev` and confirms the server starts and reaches Postgres.

---

## Phase 1 — Real Barangay Content (Human Only)

**Goal:** collect everything the agent cannot invent. Screens can be built with clearly marked placeholders, but these are blockers for a correct app.

- [ ] 🧑 **The barangay's real Purok/Zone names.** They go into the `puroks` table once; every dropdown and filter reads them live from `GET /api/puroks`. Nothing is hardcoded in the app.
- [ ] 🧑 **Fees** for the 5 certificate types (Barangay Clearance, Certificate of Indigency, Certificate of Residency, Barangay ID, Business Clearance). They go in `server/src/config/fees.js` and are served by `GET /api/document-types`.
- [ ] 🧑 **Barangay seal image** (PNG/SVG).
- [ ] 🧑 **Signing official's name, title, and signature image.**
- [ ] 🧑 **First admin account details** (name, email) for the seed script.
- [ ] 🧑 **RA 10173 consent wording**, if the professor or barangay requires specific text for onboarding Step 5.
- [ ] 🧑 **App name, icon, and branding** if different from the default emerald theme.
- [ ] 🧑 **Demo data** for the defense: a few fake residents across 2–3 Puroks, a couple of open complaints (one overdue), and one emergency scenario.

---

## Phase 2 — Database

**Goal:** the complete 11-table PostgreSQL schema from `BDMS_Mobile_Database_Structure.docx`.

### 2.1 Schema

🤖 The agent writes this as `server/migrations/001_init.sql`. 🧑 The human runs it against the real database (`psql`, pgAdmin, TablePlus), or the agent runs it via `npm run migrate` if it has shell access to a dev database.

```sql
-- Extensions
create extension if not exists pgcrypto;   -- gen_random_uuid()

-- PUROKS — staff-managed lookup list
create table puroks (
  name text primary key,
  description text,
  "createdAt" timestamptz default now()
);

-- OTP CODES — email verification, keyed by email (no profiles FK,
-- since a code can be issued before the account exists)
create table "otpCodes" (
  "otpId" uuid primary key default gen_random_uuid(),
  email text not null,
  code text not null,
  purpose text default 'registration',
  "expiresAt" timestamptz not null,
  "consumedAt" timestamptz,
  "createdAt" timestamptz default now()
);

-- PROFILES — root identity table, holds every field the 6-step
-- onboarding form collects
create table profiles (
  id uuid primary key default gen_random_uuid(),
  username text unique not null,
  "fullName" text not null,
  email text unique not null,
  "passwordHash" text not null,     -- bcrypt hash; server-only, never returned by the API
  "mobileNumber" text,
  sex text,
  birthdate date,
  "civilStatus" text,
  citizenship text,
  birthplace text,
  purok text references puroks(name),   -- structured Purok/Zone, captured at onboarding (Step 3)
  address text,
  "residencyType" text,
  "residencyPeriod" text,
  sector text[],
  "ctcNumber" text,
  "ctcOrNumber" text,
  disabilities text[],
  "avatarUrl" text,
  "consentAcceptedAt" timestamptz,  -- RA 10173 notice, Step 5
  role text not null default 'resident',   -- resident | staff | admin
  "registrationMethod" text default 'online',   -- online | walk_in
  "registeredBy" uuid references profiles(id),  -- staff who walk-in registered this account
  "isActive" boolean default true,
  "createdAt" timestamptz default now()
);

-- DOCUMENT REQUESTS
create table "documentRequests" (
  "requestId" uuid primary key default gen_random_uuid(),
  "userId" uuid references profiles(id) not null,
  "documentType" text not null,     -- Barangay Clearance, Certificate of Indigency,
                                     -- Certificate of Residency, Barangay ID, Business Clearance
  "controlNumber" text unique,
  purpose text,
  "deliveryMethod" text,            -- pickup | email | both
  fee numeric(10,2),
  status text default 'pending',    -- pending | processing | ready | completed | rejected
  "paymentStatus" text default 'pending',   -- pending | confirmed
  "paymentMethod" text,              -- cash | debit_card | credit_card | waived | demo
  "paymentOrNumber" text,           -- Official Receipt number for the payment
  "rejectionReason" text,
  "staffNotes" text,
  "businessName" text,              -- Business Clearance only
  "businessAddress" text,           -- Business Clearance only
  "businessType" text,              -- Business Clearance only
  "isWalkIn" boolean default false,
  "purokClearanceUrl" text,         -- required for online requests; not for walk-in
  "purokClearanceVerified" boolean default false,   -- auto-true for walk-in (exempted)
  "purokClearanceVerifiedBy" uuid references profiles(id),
  "purokClearanceVerifiedAt" timestamptz,
  "certificateUrl" text,            -- generated PDF path, once completed
  "processedBy" uuid references profiles(id),
  "createdAt" timestamptz default now(),
  "completedAt" timestamptz
);

-- COMPLAINTS
create table complaints (
  "complaintId" uuid primary key default gen_random_uuid(),
  "userId" uuid references profiles(id) not null,
  purok text references puroks(name),   -- denormalized from profiles for fast filtering
  category text not null,           -- noise, sanitation, dispute, infrastructure, other
  description text not null,
  "photoUrls" text[],
  status text default 'pending',    -- pending | reviewing | resolved | dismissed
  priority text default 'normal',   -- normal | high
  "slaDueAt" timestamptz,
  "escalatedAt" timestamptz,
  "handledBy" uuid references profiles(id),
  "staffResponse" text,            -- staff reply shown to the resident on resolve/dismiss
  "createdAt" timestamptz default now(),
  "resolvedAt" timestamptz
);

-- ANNOUNCEMENTS
create table announcements (
  "announcementId" uuid primary key default gen_random_uuid(),
  title text not null,
  body text not null,
  category text default 'general',  -- general | event | emergency
  "targetPurok" text[],             -- null/empty = barangay-wide; NOT FK-enforced by
                                     -- Postgres (array columns can't carry a FK) — validate
                                     -- each element against puroks in the Node.js API instead
  "postedBy" uuid references profiles(id) not null,
  "attachmentUrl" text,
  "createdAt" timestamptz default now(),
  "expiresAt" timestamptz
);

-- EMERGENCY REPORTS
create table "emergencyReports" (
  "reportId" uuid primary key default gen_random_uuid(),
  "userId" uuid references profiles(id) not null,
  purok text not null references puroks(name),   -- copied from profiles.purok at submission time
  "disasterType" text not null,     -- flood, fire, typhoon, earthquake, landslide
  "householdMembersAffected" int,
  needs text[],                     -- food, water, medicine, shelter, evacuation
  "photoUrl" text,
  status text default 'reported',   -- reported | verified | relief_dispatched | resolved
  "createdAt" timestamptz default now()
);

-- RELIEF DISTRIBUTIONS
create table "reliefDistributions" (
  "distributionId" uuid primary key default gen_random_uuid(),
  "reportId" uuid references "emergencyReports"("reportId") not null,
  "residentId" uuid references profiles(id) not null,
  purok text references puroks(name),
  "itemsGiven" text,
  "distributedBy" uuid references profiles(id) not null,
  "createdAt" timestamptz default now(),
  unique ("reportId", "residentId")   -- hard DB-level duplicate-dispatch guard
);

-- NOTIFICATIONS
create table notifications (
  "notificationId" uuid primary key default gen_random_uuid(),
  "userId" uuid references profiles(id) not null,
  title text not null,
  message text not null,
  type text not null,               -- request_update | complaint | announcement | emergency
  "isRead" boolean default false,
  "relatedId" uuid,
  "deliveryStatus" text default 'pending',  -- pending | sent | failed | cancelled (email only)
  "deliveryAttempts" int default 0,
  "lastAttemptAt" timestamptz,
  "failureReason" text,
  "createdAt" timestamptz default now()
);

-- CERTIFICATE TEMPLATES
create table "certificateTemplates" (
  "templateId" uuid primary key default gen_random_uuid(),
  "documentType" text not null,
  "sealUrl" text,
  "signatureUrl" text,
  "officialName" text,
  "officialTitle" text,
  "updatedAt" timestamptz default now()
);

-- AUDIT LOGS
create table "auditLogs" (
  "auditLogId" uuid primary key default gen_random_uuid(),
  "performedBy" uuid references profiles(id) not null,
  action text not null,             -- e.g. STATUS_CHANGE, RELIEF_DISPATCHED, COMPLAINT_RESOLVED
  "targetId" uuid,
  details text,
  "ipAddress" text,
  "userAgent" text,
  metadata jsonb,
  "createdAt" timestamptz default now()
);

-- INDEXES (beyond the automatic PK/unique indexes)
create index idx_profiles_purok on profiles(purok);
create index idx_complaints_purok on complaints(purok);
create index idx_complaints_status on complaints(status);
create index idx_emergencyreports_purok on "emergencyReports"(purok);
create index idx_emergencyreports_status on "emergencyReports"(status);
create index idx_documentrequests_status on "documentRequests"(status);
create index idx_documentrequests_user on "documentRequests"("userId");
create index idx_notifications_user_unread on notifications("userId", "isRead");
create index idx_otpcodes_email on "otpCodes"(email);
create index idx_documentrequests_clearance on "documentRequests"("purokClearanceVerified");
create index idx_notifications_delivery_status on notifications("deliveryStatus");
```

### 2.2 Relationships

| Parent | Relationship | Child |
| --- | --- | --- |
| puroks | 1–many | profiles.purok, complaints.purok, emergencyReports.purok, reliefDistributions.purok |
| profiles | 1–many | documentRequests (userId, processedBy, purokClearanceVerifiedBy) |
| profiles | 1–many | profiles.registeredBy (self-referencing — staff → walk-in-registered residents) |
| profiles | 1–many | complaints |
| profiles | 1–many | announcements (postedBy) |
| profiles | 1–many | emergencyReports |
| profiles | 1–many | reliefDistributions (residentId / distributedBy) |
| profiles | 1–many | notifications |
| profiles | 1–many | auditLogs |
| emergencyReports | 1–many | reliefDistributions |

`otpCodes` has no foreign key to `profiles`: it is looked up by `email`, because a code can be issued before the account exists.

### 2.3 Business rules the schema does not enforce

These live in the Express API (see 2.4):

- **Purok Clearance.** An online request (`isWalkIn = false`) cannot be created without `purokClearanceUrl`, and cannot move past `pending` until `purokClearanceVerified = true`. A walk-in request has `purokClearanceVerified` set to `true` at creation, with `purokClearanceVerifiedBy` recording the staff member — so the exemption stays auditable.
- **`announcements.targetPurok`** is a `TEXT[]`; PostgreSQL cannot put a foreign key on array elements, so the API must validate every element against `puroks.name` before insert.
- **Sensitive fields.** `passwordHash` is never returned by any endpoint. `ctcNumber`, `ctcOrNumber`, and `disabilities` appear only on a resident's own profile, never in staff list or search results.
- **Request status lifecycle:** `pending → processing → ready → completed`, or `rejected` (with `rejectionReason`). `paymentMethod` is one of `cash`, `debit_card`, `credit_card`, `waived`, `demo`.

### 2.4 Authorization model

Plain PostgreSQL does not filter rows by the requesting user, so **authorization is enforced entirely in Express** (Phase 3): every query scopes rows by the authenticated user's id/role, and every route declares who may call it.

- [ ] 🤖 Scope every resident-facing query by `req.user.id`; gate staff/admin routes with `requireRole`.
- [ ] 🧑 Review the authorization middleware before demoing with real data. Because enforcement lives in application code, one mistaken route can expose data a database policy would have caught. The authorization tests in Phase 15 are the safeguard.

### 2.5 SLA escalation job

Complaints get a deadline (`slaDueAt`) when created; a scheduled job flags overdue ones as high priority. This implements the "SLA Escalation Scan" from the Project Proposal and `Complaint.isOverdue()` / `escalate()` from the Class Model.

```js
// server/src/jobs/slaEscalationJob.js
const cron = require('node-cron');
const db = require('../config/db');

function startSlaEscalationJob() {
  // Runs every hour, on the hour
  cron.schedule('0 * * * *', async () => {
    await db.query(`
      update complaints
      set priority = 'high', "escalatedAt" = now()
      where status = 'pending'
        and "slaDueAt" < now()
        and "escalatedAt" is null
    `);
    // TODO: after updating, query the newly-escalated rows and
    // call notificationService.notifyStaffOfEscalation() for each (Phase 12)
  });
}

module.exports = { startSlaEscalationJob };
```

- [ ] 🤖 Write the job and start it once from `server.js`.
- [ ] 🤖 Set `slaDueAt` in the complaint controller at insert time, by category: infrastructure 72h, dispute 48h, everything else 48h.
- [ ] 🤖 Extract the escalation query into a plain function so tests can call it directly, not only through the cron callback.
- [ ] 🧑 Note the limitation: the job only runs while the Node process is alive. If the server sleeps (free hosting tiers), escalation pauses.

### 2.6 Seed data and housekeeping

- [ ] 🤖 Write `server/scripts/seed.js` to insert the human-supplied Puroks, one `certificateTemplates` row per document type, and the first admin account (bcrypt-hashed password, printed once to the console).
- [ ] 🧑 Run the seed script after the migration. Without an admin account nobody can create staff accounts.
- [ ] 🤖 Delete or ignore expired `otpCodes` rows with a simple periodic query; no dedicated job is needed at this scale.
- [ ] 🧑 Confirm file storage: `server/uploads/` on disk for development. Production needs a persistent disk, or a cloud bucket.

---

## Phase 3 — Node.js/Express Backend

**Goal:** the REST API the Android app talks to. 🤖 All agent work, once the database, `JWT_SECRET`, and Resend key exist.

### 3.1 Project structure

```
server/
  src/
    config/
      db.js                  -- pg Pool, reads DATABASE_URL from .env
    middleware/
      authMiddleware.js       -- verifies JWT, attaches req.user
      roleMiddleware.js       -- requireRole('staff','admin')
      rateLimitMiddleware.js  -- Phase 13 anti-abuse checks
      uploadMiddleware.js     -- multer config per upload type
    routes/
      authRoutes.js
      profileRoutes.js
      residentRoutes.js       -- walk-in registration + resident search (staff/admin)
      documentRequestRoutes.js
      complaintRoutes.js
      announcementRoutes.js
      emergencyReportRoutes.js
      reliefDistributionRoutes.js
      notificationRoutes.js
      certificateTemplateRoutes.js
      auditLogRoutes.js
      purokRoutes.js
      adminUserRoutes.js      -- staff/admin account management (admin only)
    controllers/
      authController.js               -- also owns otpCodes read/write
      profileController.js
      residentController.js           -- walk-in registration, credential-slip generation, resident search
      documentRequestController.js    -- also owns Purok Clearance upload/verify and the walk-in request path
      complaintController.js          -- mirrors ComplaintService (Class Model §6)
      announcementController.js
      emergencyResponseController.js  -- mirrors EmergencyResponseService (Class Model §6)
      reliefDistributionController.js
      notificationController.js       -- mirrors NotificationService (Class Model §6); also owns retry/cancel
      certificateTemplateController.js
      auditLogController.js           -- also attaches ipAddress/userAgent from the request object
      purokController.js
      adminUserController.js          -- create staff/admin accounts, assign roles, activate/deactivate
    services/
      certificateService.js           -- builds certificate PDFs with pdfkit, one layout per documentType
      emailService.js                 -- Resend wrapper (send, retry, record delivery status)
    jobs/
      slaEscalationJob.js
    sockets/
      notificationSocket.js
    app.js                   -- Express app, middleware wiring
    server.js                -- entry point, starts HTTP + Socket.io + cron job
  migrations/
    001_init.sql
  scripts/
    seed.js                  -- seeds puroks, certificate template rows, and the first admin account
  uploads/                    -- local file storage (dev)
  .env.example
  package.json
```

### 3.2 Endpoint map

This is the contract the Android `ApiService` is built against. "Authenticated" means any logged-in user.

| Method & Path | Purpose | Auth |
| --- | --- | --- |
| `POST /api/auth/register` | Create account (Step 1 of onboarding) | Public |
| `POST /api/auth/send-otp` | Send OTP (Resend email) | Public |
| `POST /api/auth/verify-otp` | Verify OTP, activate account | Public |
| `POST /api/auth/login` | Authenticate, returns JWT | Public |
| `GET /api/profiles/me` | Get own profile | Authenticated |
| `PATCH /api/profiles/me` | Update own profile | Authenticated |
| `POST /api/profiles/me/avatar` | Upload avatar (multer) | Authenticated |
| `POST /api/document-requests` | Create a request (multipart — includes the required `purokClearanceUrl` photo/scan for online requests) | Resident |
| `GET /api/document-requests` | List (own, or all if staff/admin — `?status=` filter) | Authenticated |
| `GET /api/document-requests/:id` | Detail (includes the Purok Clearance image for staff review) | Authenticated (own or staff/admin) |
| `PATCH /api/document-requests/:id` | Update status (`updateStatus()`) — `pending → processing → ready → completed`, or `rejected` | Staff/Admin |
| `PATCH /api/document-requests/:id/verify-clearance` | Approve or reject the uploaded Purok Clearance image | Staff/Admin |
| `POST /api/residents/walk-in` | Register a walk-in resident; returns a one-time credential slip (email + temp password) | Staff/Admin |
| `GET /api/residents/search?q=` | Search existing residents by name/email/mobile (duplicate check before a walk-in registration) | Staff/Admin |
| `POST /api/document-requests/walk-in` | Create a request on behalf of a resident physically present — `isWalkIn = true`, clearance auto-exempted | Staff/Admin |
| `POST /api/complaints` | Create a complaint | Resident |
| `GET /api/complaints` | List (own, or all sorted by priority/SLA if staff/admin) | Authenticated |
| `GET /api/complaints/:id` | Detail | Authenticated (own or staff/admin) |
| `PATCH /api/complaints/:id/resolve` | Set `status` to `reviewing`, `resolved`, or `dismissed` (`resolve(response, staffId)`); a `staffResponse` is required for resolved/dismissed | Staff/Admin |
| `POST /api/announcements` | Publish (`publish()`) | Staff/Admin |
| `GET /api/announcements` | List (targeted-to-me for residents; all for staff) | Authenticated |
| `POST /api/emergency-reports` | Submit a report | Resident |
| `GET /api/emergency-reports` | List (own for residents) | Authenticated |
| `GET /api/emergency-reports/:id` | Report detail | Own resident or Staff/Admin |
| `GET /api/emergency-reports/grouped-by-purok` | Server-side `groupReportsByPurok()` — returns a map keyed by Purok | Staff/Admin |
| `PATCH /api/emergency-reports/:id/verify` | `verify()` | Staff/Admin |
| `POST /api/relief-distributions` | `dispatchReliefToPurok()` — the flagship transaction | Staff/Admin |
| `GET /api/relief-distributions?reportId=` | List distributions for a report (dedupe check support) | Staff/Admin |
| `GET /api/notifications/me` | List own notifications | Authenticated |
| `PATCH /api/notifications/:id/read` | `markAsRead()` | Authenticated (own only) |
| `GET /api/notifications` | Notification Center — all notifications, filterable by `deliveryStatus` | Admin |
| `POST /api/notifications/:id/retry` | Re-attempt a failed email delivery | Admin |
| `POST /api/notifications/:id/cancel` | Cancel a pending notification (sets `deliveryStatus = 'cancelled'`) | Admin |
| `GET /api/certificate-templates/:documentType` | Get template config | Authenticated |
| `PUT /api/certificate-templates/:documentType` | Update template config | Admin |
| `GET /api/audit-logs` | List (with filters) | Admin |
| `GET /api/puroks` | List all Puroks — the single source the Android dropdown/filters in R2, R11, S4, S5, S7 all read from, instead of a hardcoded list | Authenticated |
| `POST /api/puroks` | Add a Purok to the list (S14). Returns `409` for a duplicate name. There is intentionally no rename or delete endpoint — `profiles`, `complaints`, `emergencyReports`, and `reliefDistributions` reference the name | Admin |
| `GET /api/document-types` | The 5 certificate types with their fees (from server config) — feeds the R5 dropdown and fee preview | Authenticated |
| `GET /api/document-requests/:id/certificate` | Download the generated certificate PDF | Own resident or Staff/Admin |
| `GET /api/admin/users?q=` | Search/list accounts | Admin |
| `POST /api/admin/users` | Create a staff or admin account; returns a one-time credential slip | Admin |
| `PATCH /api/admin/users/:id/role` | Assign `resident` / `staff` / `admin` (audit-logged) | Admin |
| `PATCH /api/admin/users/:id/active` | Activate or deactivate an account | Admin |

### 3.3 Implementation rules

- [ ] 🤖 Every route is a thin route → controller → parameterized SQL (`$1, $2, …`). Never concatenate SQL strings.
- [ ] 🤖 Every controller enforces the rule in its "Auth" column in code.
- [ ] 🤖 `authMiddleware` verifies `Authorization: Bearer <jwt>` and attaches `req.user = { id, role, purok }`.
- [ ] 🤖 `uploadMiddleware` accepts images only (jpeg/png/webp), max 5 MB, and stores files under `uploads/<type>/`. Serve uploads over authenticated routes, not as an open static folder, for Purok Clearance scans and complaint photos.
- [ ] 🤖 Return consistent JSON errors (`{ error: "…" }`) with correct status codes; never leak stack traces or SQL errors to the client.
- [ ] 🤖 Enable CORS only as needed; load every secret from `.env`.
- [ ] 🤖 Call `writeAuditLog(req, action, targetId, details)` from every sensitive staff/admin controller (Phase 14).

---

## Phase 4 — Android Foundation

**Goal:** the project skeleton, matching the Class and Object Model document. 🤖 All agent work.

```
src/main/java/com/barangay/bdms/
  model/
    Profile.java
    Resident.java
    StaffMember.java
    DocumentRequest.java
    Complaint.java
    Announcement.java
    EmergencyReport.java
    ReliefDistribution.java
    Notification.java
    CertificateTemplate.java
    AuditLog.java
  service/
    ComplaintService.java
    EmergencyResponseService.java
    NotificationService.java
  network/
    ApiService.java          -- Retrofit interface, one method per endpoint in the Phase 3 endpoint map
    ApiClient.java            -- Retrofit/OkHttp singleton, base URL + auth interceptor
  repository/
    BdmsRepository.java      -- wraps ApiService calls, converts JSON → model classes
  ui/
    auth/            -- LoginActivity, OnboardingActivity + 6 step Fragments
    resident/         -- ResidentMainActivity + Fragments (see Phase 5)
    staff/             -- StaffMainActivity + Fragments (see Phase 5)
    common/            -- shared adapters, ViewModels, custom Views
```

- [ ] 🤖 Create each `model/` class exactly as specified in the Class and Object Model (fields, constructors, methods). Do not re-derive them.
- [ ] 🤖 Create `ApiService.java` with one Retrofit method per endpoint in the Phase 3 map.
- [ ] 🤖 Create `ApiClient.java`: Retrofit/OkHttp singleton with the base URL from `BuildConfig` and an interceptor that adds `Authorization: Bearer <jwt>`.
- [ ] 🤖 Create `BdmsRepository.java`: the single place UI code calls; wraps `ApiService` and maps JSON to model classes.
- [ ] 🤖 Build `Profile → Resident / StaffMember` from the `role` returned by `GET /api/profiles/me`.
- [ ] 🤖 Add a small `Purok` POJO (`name`, `description`) to `model/`. It is not in the Class and Object Model document, which predates the `puroks` table.
- [ ] 🤖 Keep the logged-in role in one place (e.g. a `SessionManager` backed by `EncryptedSharedPreferences`) so `StaffMainActivity` and every admin Fragment read the same value.
- [ ] 🤖 Store the JWT in `EncryptedSharedPreferences`.
- [ ] 🤖 In `AndroidManifest.xml`, add the `INTERNET` permission. For development only, add a **debug-only** network security config that allows cleartext HTTP to `10.0.2.2` (the emulator's address for the host machine) and the dev laptop's LAN IP; release builds must use HTTPS only.
- [ ] 🤖 For camera captures in `PhotoPickerHelper`, declare a `FileProvider` in the manifest. Gallery picking uses the system photo picker and needs no storage permission.
- [ ] 🤖 Use **Groovy DSL** Gradle files (`build.gradle`), not Kotlin DSL (`build.gradle.kts`), so the project contains no Kotlin at all.
- [ ] 🤖 Set `minSdk 23`. In `app/build.gradle`, enable **core library desugaring**: `compileOptions { coreLibraryDesugaringEnabled true }` plus `coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:<latest 2.x>'`. Without it, the `OffsetDateTime` fields in the model classes compile but crash at runtime on Android 6.0–8.1 (API 23–25).
- [ ] 🤖 Do not call APIs newer than API 23 without a fallback; Android Studio's lint will flag them.
- [ ] 🤖 **Library minimums are drifting up.** Google's AndroidX release notes say the default minimum moved from API 21 to 23, and that newer releases (for example Navigation 2.10) are moving to API 24. If a sync or build fails with "minSdkVersion 23 cannot be smaller than version 24 declared in library …", pin that library to the newest version that still supports API 23 (in `gradle/libs.versions.toml` or `app/build.gradle`). **Never raise `minSdk` silently.** Stop and ask the human, because the professor specified Marshmallow.
- [ ] 🧑 Ask the professor whether Marshmallow (API 23) is a hard minimum or whether Nougat (API 24) is acceptable. If several libraries require 24, that answer decides what to do.
- [ ] 🧑 Confirm the base URL: `http://10.0.2.2:<port>/api` for an emulator talking to a local server; the deployed URL (Phase 16) for a physical device or release build.

---

## Part C — Screens and UI

## Phase 5 — Screen Specification

The written spec for every screen: what it shows, which endpoint feeds it, what it does. Stitch (design) and Gemini CLI (build) are both checked against it in Phase 6.

### 5.1 Design tokens

| Token | Value | Used for |
| --- | --- | --- |
| `colorPrimary` (accent) | `#059669` (emerald) | Buttons, active nav icon, links, FAB |
| `colorPrimaryDark` | `#047857` | Pressed/hover state |
| `colorPrimaryLight` | `#D1FAE5` | Chip backgrounds, selected-state tints |
| `colorBackground` | `#FCFCFC` | Screen background |
| `colorSurface` | `#FFFFFF` | Cards |
| `colorOnSurface` / text primary | `#171717` | Primary text |
| `colorTextSecondary` | `#52525B` | Secondary text, hints |
| `colorTextMuted` | `#A1A1AA` | Placeholder/disabled text |
| `colorOutline` (border) | `#E5E5E5` | Card strokes, dividers |
| Typography | Roboto (system default) | Stand-in for Geist Sans/Mono used on web |
| Heading style | 600 weight, tight letter spacing | Screen titles, card titles |
| Corner radius | 12dp (cards), 8dp (buttons/inputs) | `MaterialCardView`, `MaterialButton` shape appearance |
| Elevation | 1dp resting, 3dp pressed | `MaterialCardView` default + ripple |

**Status badge colors** (used for every request, complaint, report, and notification status):

| Status | Background | Text |
| --- | --- | --- |
| `pending` / `reported` | `#FFFBEB` | `#B45309` |
| `processing` / `reviewing` / `verified` | `#EFF6FF` | `#1D4ED8` |
| `completed` / `resolved` | `#F0FDF4` | `#15803D` |
| `rejected` / `dismissed` | `#FEF2F2` | `#B91C1C` |
| `high` priority (complaints) | `#FEF2F2` | `#B91C1C` |
| `relief_dispatched` | `#EFF6FF` | `#1D4ED8` |

Mapping: `pending`/`reported` → amber; `processing`/`reviewing`/`verified`/`relief_dispatched` → blue; `ready` → cyan; `completed`/`resolved`/`sent` → green; `rejected`/`dismissed`/`failed`/`high` → red; `cancelled` → muted grey.

### 5.2 Navigation

```
LoginActivity
  → OnboardingActivity (6-step ViewPager2 or sequential Fragments)
  → ResidentMainActivity (role = resident)
      Tab 1: Home (dashboard)
      Tab 2: Requests
      Tab 3: Community (complaints + announcements + emergency, sub-tabbed)
      Tab 4: Profile
  → StaffMainActivity (role = staff or admin; the tab bar is built from the role)
      Tab 1: Dashboard (document request queue lives here; "Walk-In" shortcut → S10 is reachable from any tab via the top bar)
      Tab 2: Complaints
      Tab 3: Emergency Response
      Tab 4: Announcements
      Tab 5 — "Admin" (admin accounts only; absent for staff) → Admin Hub (S13)
              S13 opens: User Management (S12), Audit Log (S8), Certificate Templates (S9),
                         Notification Center (S11), Manage Puroks (S14)
```

| Account | Bottom tabs |
| --- | --- |
| **staff** | 4 tabs: Dashboard, Complaints, Emergency, Announcements |
| **admin** | The same 4 tabs **plus** a 5th "Admin" tab → S13 |

- Each tab hosts a `NavHostFragment` so list → detail → form flows stay inside one Activity.
- S10 (Walk-In) is a shortcut from S1, not a tab, because it is an occasional action.
- **The Admin tab is left out of a staff account's menu entirely** (not merely hidden), using the role from the login response. This is a usability measure only — the real protection is the server's `403` on every admin endpoint, plus a client-side guard inside each admin Fragment (S8, S9, S11, S12, S13, S14) that pops back with "Not authorized" if the role is not `admin`.
- S8, S9, S11, S12 and S14 each have a back arrow returning to the Admin Hub.
- Tabs must not stack duplicate destinations (`popUpTo` / `singleTop` on each tab's start destination).

### 5.3 Resident screens

| # | Screen | Layout / Components | Data Source (API) | Key Actions |
| --- | --- | --- | --- | --- |
| R1 | **Login** | `TextInputLayout` (email, password), `MaterialButton` ("Log In") | `POST /api/auth/login` | Route to R3 (resident) or S1 (staff/admin) by returned role |
| R2 | **Onboarding (6 steps)** | `ViewPager2` + step indicator. Step 1: username/email/password. Step 2: fullName, sex, birthdate, civilStatus, citizenship, birthplace. Step 3: Purok dropdown/`ChipGroup` sourced from `GET /api/puroks` (required, first field), then address, residencyType, residencyPeriod, sector `ChipGroup`. Step 4: ctcNumber, ctcOrNumber, disabilities, avatar photo picker. Step 5: RA 10173 consent checkbox. Step 6: OTP | `POST /api/auth/register` (writes every field above to `profiles`), then `POST /api/auth/send-otp` / `verify-otp` (against `otpCodes`) | Step 6 = OTP `TextInputLayout` + "Verify" |
| R3 | **Resident Home / Dashboard** | `SwipeRefreshLayout` + `RecyclerView` of status cards + horizontal announcement strip | `GET /api/document-requests`, `/complaints`, `/notifications/me`, `/announcements` | Tap card → detail; bell icon → R9 |
| R4 | **Document Request List** | `RecyclerView` of cards: `documentType`, `StatusBadge`, `createdAt` | `GET /api/document-requests` | FAB "+" → R5 |
| R5 | **New Document Request** | `AutoCompleteTextView` (documentType), `TextInputLayout` (purpose), `RadioGroup` (deliveryMethod), **mandatory photo picker: "Upload Purok Clearance"** (camera/gallery, required before Submit is enabled), "Submit" | `GET /api/document-types` (dropdown + fee preview), `POST /api/document-requests` (multipart) | Back to R4 on success |
| R6 | **Document Request Detail** | Control number, `StatusBadge` (now includes `ready`), status timeline, a **Clearance Status** badge (Pending Verification / Verified / Rejected), "Download Certificate" (when `ready` or `completed`) | `GET /api/document-requests/:id`, `GET /api/document-requests/:id/certificate` | — |
| R7 | **Complaint List** | `RecyclerView`: `category`, `StatusBadge`, truncated description, "Overdue" tag | `GET /api/complaints` | FAB "+" → R8; tap a card to expand it inline and read the staff response |
| R8 | **New Complaint** | `AutoCompleteTextView` (category), multiline description, optional photo picker, "Submit" | `POST /api/complaints` (multipart if photo) | Purok auto-attached server-side from JWT user |
| R9 | **Notifications** | `RecyclerView` rows, icon by `type`, unread = bold/dot | `GET /api/notifications/me`, Socket.io channel `notifications:{userId}` | Tap → `PATCH /api/notifications/:id/read` + deep-link via `relatedId` |
| R10 | **Announcement Feed** | `RecyclerView` of cards: category chip, title, expandable body | `GET /api/announcements` | Emergency-category cards get highlighted styling |
| R11 | **New Emergency Report** | `AutoCompleteTextView` (disasterType), numeric field, `ChipGroup` (needs), photo picker, read-only Purok chip | `POST /api/emergency-reports` (multipart if photo) | Confirmation dialog with report reference |
| R12 | **My Emergency Reports** | `RecyclerView` with `StatusBadge` | `GET /api/emergency-reports` | Tap → detail |
| R13 | **Profile** | Avatar, `fullName`, `email`, `purok` (read-only), `mobileNumber` (editable), "Log Out" | `GET/PATCH /api/profiles/me`, `POST /api/profiles/me/avatar` | — |

### 5.4 Staff / Admin screens

| # | Screen | Layout / Components | Data Source (API) | Key Actions |
| --- | --- | --- | --- | --- |
| S1 | **Staff Dashboard** | Stat cards (pending requests, pending/overdue complaints, open emergency reports) + a prominent **"Walk-In"** quick-action shortcut (top bar and sidebar) | Aggregated `GET` calls to the relevant list endpoints | Tap stat → filtered list; tap "Walk-In" → S10. The bottom bar has 4 tabs for staff and 5 for admin (see 5.2) |
| S2 | **Document Request Queue / Detail** | `RecyclerView` + `TabLayout` filter by status (`pending`/`processing`/`ready`/`completed`/`rejected`); detail view shows the uploaded **Purok Clearance image** (tap to expand/lightbox) with "Verify" / "Reject" buttons — hidden entirely for walk-in requests, which show an "Exempted (Walk-In)" badge instead; a **Payment** panel with a method dropdown (`cash` / `debit_card` / `credit_card` / `waived` / `demo`), an OR-number field, and a "Confirm Payment" button | `GET /api/document-requests`, `PATCH /api/document-requests/:id/verify-clearance` | `PATCH /api/document-requests/:id` to advance status and record payment (sets `paymentStatus = confirmed`) |
| S3 | **Complaint Queue** | `RecyclerView` sorted by `(priority desc, slaDueAt asc)` — this ordering can be done by the API query itself (`ORDER BY`) rather than a client-side `PriorityQueue`, since the server already has the data; the `PriorityQueue<Complaint>` structure documented in the Class Model still applies if the client re-sorts after local updates | `GET /api/complaints` | `PATCH /api/complaints/:id/resolve` (detail view: response text field + Resolve / Dismiss buttons) |
| S4 | **Announcement Composer** | Title/body fields, category dropdown, Purok multi-select `ChipGroup` populated from `GET /api/puroks` (barangay-wide toggle when none selected) | `POST /api/announcements`, `GET /api/puroks` | Publish → server fans out notifications (Phase 12) |
| S5 | **Emergency Response Dashboard** | Purok filter chips/dropdown, `RecyclerView` of report cards | `GET /api/emergency-reports/grouped-by-purok` (server builds the `HashMap<String,List<EmergencyReport>>` and returns it as JSON keyed by Purok) | Tap → S6 |
| S6 | **Emergency Report Detail** | Full report + "Verify" + "Dispatch Relief" buttons | `GET /api/emergency-reports/:id` | `PATCH /api/emergency-reports/:id/verify` → S7 |
| S7 | **Relief Dispatch** | `itemsGiven` field, checklist of verified residents, already-served rows disabled | `GET /api/relief-distributions?reportId=` (dedupe check) | `POST /api/relief-distributions` → the `DispatchReliefByPurok` flow |
| S8 | **Audit Log Viewer** (admin) | `RecyclerView` rows: action, performer, details, timestamp | `GET /api/audit-logs` | Read-only |
| S9 | **Certificate Template Config** (admin) | Seal/signature pickers, official name/title fields | `GET/PUT /api/certificate-templates/:documentType` | "Save" |
| S10 | **Walk-In Resident Service** | Step 1: search field (name/email/mobile) against existing residents to avoid duplicates. Step 2 (no match found): a registration form (same core fields as onboarding, minus OTP) → on submit, a **Credential Slip** card appears showing the generated email + temporary password with one-click copy buttons, for staff to hand to the resident. Step 3: a document-request form identical to R5 but with the clearance-upload field removed (auto-exempted) | `GET /api/residents/search`, `POST /api/residents/walk-in`, `POST /api/document-requests/walk-in` | "Create Request" → confirmation, back to S1 |
| S11 | **Notification Center** (admin) | `RecyclerView` with `TabLayout` filter by `deliveryStatus` (pending/sent/failed/cancelled); each row shows title, recipient, attempt count, and — for `failed` rows — the `failureReason`; "Retry" and "Cancel" buttons per row | `GET /api/notifications`, `POST /api/notifications/:id/retry`, `POST /api/notifications/:id/cancel` | — |
| S12 | **User Management** (admin) | Search bar (name/email); `RecyclerView` of accounts with a role chip and an active/inactive toggle; "Create Staff/Admin Account" button opens a form (name, email, role) that ends in a one-time **Credential Slip** card, same pattern as S10; per-row role change with a confirmation dialog | `GET /api/admin/users?q=`, `POST /api/admin/users`, `PATCH /api/admin/users/:id/role`, `PATCH /api/admin/users/:id/active` | Every change is audit-logged |
| S13 | **Admin Hub** (admin) | Start screen of the admin-only 5th tab: a header card (admin name + role chip) and a list of 5 tool rows, each with an icon, title, one-line description, and chevron — User Management, Audit Log, Certificate Templates, Notification Center (with a red failed-count badge), Manage Puroks | Optional, for the badge: `GET /api/notifications?deliveryStatus=failed` | Tap a row → S12 / S8 / S9 / S11 / S14 |
| S14 | **Purok Management** (admin) | `RecyclerView` of Puroks (name + optional description), display-only rows; "+ Add Purok" opens a bottom sheet (name, description, Save); inline error for a duplicate name; an info line explaining that Puroks in use can't be renamed or removed | `GET /api/puroks`, `POST /api/puroks` | Add only — no edit or delete in v1 |

### 5.5 Shared components

Build these **before** the first screen that uses them (`StatusBadge` is needed from R4 onward).

- [ ] 🤖 `StatusBadge` — takes a status string, applies the right background/text pair from 5.1.
- [ ] 🤖 `EmptyStateView` — icon + one-line message + optional action.
- [ ] 🤖 `LoadingOverlay` and a `SwipeRefreshLayout` pattern for every list.
- [ ] 🤖 `PhotoPickerHelper` — camera/gallery intent plus `multipart/form-data` upload; reused by R5, R8, R11, R13, S9.
- [ ] 🤖 `NotificationBellView` — bell with unread dot, placed in the top bar of every authenticated screen.
- [ ] 🤖 A reusable card style (12dp radius, 1dp elevation) instead of repeating CardView attributes.

---

## Phase 6 — Design in Stitch, Build with Gemini CLI

**Goal:** turn the Phase 5 spec into real Android layouts without hand-drawing XML.

- **Stitch** generates each screen from `BDMS_Mobile_Stitch_UI_Prompts.md`.
- **Gemini CLI** fetches each approved design through the Stitch MCP connection and builds it into Java + XML using `BDMS_Mobile_Gemini_CLI_Prompts.md`.

### 6.1 Design in Stitch

- [ ] 🧑 Confirm Stitch access and connect it to Gemini CLI over MCP. The agent cannot create the account or authorize the connection.
- [ ] 🧑 Generate the **Master Design System Brief** first.
- [ ] 🧑 Generate screens **one at a time, in this exact order**, approving each before starting the next:
  - **Resident:** R1 Login → R2 Onboarding → R3 Home → R4 Request List → R5 New Request → R6 Request Detail → R7 Complaint List → R8 New Complaint → R9 Notifications → R10 Announcements → R11 New Emergency Report → R12 My Emergency Reports → R13 Profile
  - **Staff/Admin** (only after all 13 Resident screens are approved): S1 Dashboard → S2 Request Queue & Detail → S3 Complaint Queue → S4 Announcement Composer → S5 Emergency Response Dashboard → S6 Emergency Report Detail → S7 Relief Dispatch → S8 Audit Log → S9 Certificate Templates → S10 Walk-In Service → S11 Notification Center → S12 User Management → S13 Admin Hub → S14 Purok Management
- [ ] 🧑 Treat R1 as the visual reference for every later screen. If a screen drifts from R1's look, regenerate it.
- [ ] 🧑 Check each design against its **"Must include"** line before approving. These are business rules, not decoration:

| Screen | Must visibly include |
| --- | --- |
| R5 | Mandatory Purok Clearance upload box; Submit disabled until a photo is attached |
| R6 | Clearance status chip; status timeline including `ready` |
| R11 | Read-only Purok chip; urgent red/amber submit button |
| S1 | Two bottom-bar variants: 4 tabs for staff, 5 tabs (extra "Admin") for admin |
| S2 | Clearance review card with Verify/Reject; "Exempted — Walk-In" badge variant; payment panel |
| S3 | Red left-edge bar on overdue/high-priority rows, sorted first |
| S5 | Purok filter chips with per-Purok counts; inline Verify on unverified cards |
| S7 | Greyed-out, non-interactive "Already Served" rows |
| S10 | Credential Slip card with copy buttons; exemption badge in the request step |
| S11 | One example per delivery status; Retry/Cancel enabled only when applicable |
| S12 | Role chip, active toggle, one-time credential slip on account creation, role-change confirmation |
| S13 | Five tool rows in order (Users, Audit Log, Templates, Notification Center with failed badge, Puroks); 5-tab bar with Admin active |
| S14 | Add button + sheet, duplicate-name error state, rows with no edit/delete controls |

### 6.2 Build with Gemini CLI

- [ ] 🧑 Run the **Master Project Context Prompt** once at the start of the session.
- [ ] 🤖 For each screen, in the same order, run the **Per-Screen Build Prompt**, filled in from the parameter tables. Each run creates one `screen_*.xml` and one `*Fragment.java`, reusing `colors.xml` tokens and the shared components from 5.5.
- [ ] 🤖 After each screen set, run the **Navigation Wiring Prompt** to create `nav_graph_resident.xml` / `nav_graph_staff.xml` and hook up the `BottomNavigationView`.
- [ ] 🤖 In `StaffMainActivity`, build the bottom navigation from the stored role: 4 tabs for `staff`, 5 for `admin` (the Admin tab starts at S13). Add a role guard to S8, S9, S11, S12, S13 and S14 that pops back with "Not authorized" for non-admins.
- [ ] 🤖 At the end, run the **QA / Consistency Prompt**. It must **report** findings, not silently fix them.
- [ ] 🧑 Review the QA report and decide what to fix. Pay particular attention to: R5 submit disabled without a clearance photo; S2's walk-in exemption badge; S7's non-interactive served rows; Purok lists coming from `GET /api/puroks`; a staff account showing exactly 4 tabs while an admin shows 5.

### 6.3 What Gemini must not do

- Invent endpoints, field names, or status values that are not in its parameter row — it should flag them.
- Hardcode hex colors in a layout (use `colors.xml`), a Purok list, a fee, or the API base URL (use `BuildConfig`).
- Show `passwordHash`, `ctcNumber`, `ctcOrNumber`, or `disabilities` on any multi-resident list.

---

## Part D — Features

Each phase below finishes the logic for one module, backend and Android together. The screens are already built from Phase 6; these phases connect them to real behavior.

## Phase 7 — Authentication and Onboarding

Screens R1, R2. Entities `Profile`, `otpCodes`.

- [ ] 🤖 `POST /api/auth/register` — validates input, hashes the password with bcrypt, inserts into `profiles` with `role = 'resident'` and `registrationMethod = 'online'`.
- [ ] 🤖 **Password policy:** at least 8 characters, one uppercase letter, one number, one special character. Validate on both the client and the server.
- [ ] 🤖 `POST /api/auth/send-otp` — generates a 6-digit code, stores it in `otpCodes` with `expiresAt = now() + 10 minutes`, emails it via Resend.
- [ ] 🤖 `POST /api/auth/verify-otp` — checks the code matches, has not expired, and has not been consumed; then sets `consumedAt`. Codes live in the database, never in memory, so a server restart cannot break a pending verification.
- [ ] 🤖 `POST /api/auth/login` — verifies the bcrypt hash, rejects inactive accounts, and returns a signed JWT with `{ id, role, purok }`, expiring in about 1 hour. Re-login on expiry is acceptable for v1.
- [ ] 🤖 The 6-step onboarding (R2):
  1. **Account:** username, email, password
  2. **Personal info:** fullName, sex, birthdate, civilStatus, citizenship, birthplace
  3. **Address:** **Purok first and required** (dropdown from `GET /api/puroks`), then address, residencyType, residencyPeriod, sector
  4. **Additional:** ctcNumber, ctcOrNumber, disabilities, avatar photo
  5. **Consent:** RA 10173 notice; sets `consentAcceptedAt`
  6. **OTP verification**
- [ ] 🤖 After login, route to `ResidentMainActivity` or `StaffMainActivity` from the JWT `role`. A resident's `mobileNumber` is edited later on R13.

---

## Phase 8 — Document Requests

Screens R4, R5, R6, S2, S10. Entity `DocumentRequest`. This is the core of the original web system.

**Control number format:** `{TYPE}-{TIMESTAMP}{RANDOM}` — first 4 alphanumeric characters of the document type, uppercased, then the Unix millisecond timestamp, then a 3-digit random suffix (e.g. `CLEA-1726912345000123`). Generated server-side and checked for uniqueness.

**Resident flow**

- [ ] 🤖 R5 loads types and fees from `GET /api/document-types`. Submit is disabled until a **Purok Clearance photo** is attached (enforced on the client **and** the server).
- [ ] 🤖 `POST /api/document-requests` (multipart) stores the clearance image, generates the control number, and sets `fee` from the server's fee config. It rejects an online request with no clearance.
- [ ] 🤖 For Business Clearance, the form also collects `businessName`, `businessAddress`, `businessType`.
- [ ] 🤖 R4 lists the resident's requests; R6 shows status, the clearance chip, the timeline, and the certificate download.

**Staff flow (S2)**

- [ ] 🤖 `PATCH /api/document-requests/:id/verify-clearance` — approve (`purokClearanceVerified = true`, with verifier and timestamp) or reject (stays `false`, resident notified to re-upload).
- [ ] 🤖 `PATCH /api/document-requests/:id` advances `pending → processing → ready → completed`, or `rejected`. It refuses to leave `pending` while an online request's clearance is unverified, requires `rejectionReason` for `rejected`, and sets `processedBy`.
- [ ] 🤖 **Payment:** staff record `paymentMethod` and `paymentOrNumber`, which sets `paymentStatus = confirmed`. No gateway is involved.
- [ ] 🤖 Staff can add internal `staffNotes`, never shown to the resident.
- [ ] 🤖 **Certificate:** moving to `ready` calls `certificateService` to build the PDF (one `pdfkit` layout per `documentType`, using the resident's profile, the request data, and that type's `certificateTemplates` row — seal, signature, official name and title). The file is saved under `uploads/certificates/` and its path stored in `certificateUrl`.
- [ ] 🤖 `GET /api/document-requests/:id/certificate` streams the PDF to the owning resident or to staff.
- [ ] 🤖 Delivery: if `deliveryMethod` is `email` or `both`, email the PDF when the request becomes `ready`. For `pickup`, send an in-app + email "ready for pickup at the Barangay Hall" notice. `completed` means it was handed over or delivered.
- [ ] 🤖 Every status change creates a notification and an audit log entry.

**Walk-in flow (S10)**

- [ ] 🤖 `GET /api/residents/search?q=` — searches by name, email, or mobile; returns only non-sensitive fields. Staff search first to avoid duplicate registrations.
- [ ] 🤖 `POST /api/residents/walk-in` — creates a profile with `registrationMethod = 'walk_in'` and `registeredBy` = the acting staff member, generates a temporary password, stores only its bcrypt hash, and returns the plaintext **once** as a credential slip (email + password). It is never stored or logged in plaintext.
- [ ] 🤖 `POST /api/document-requests/walk-in` — creates the request with `isWalkIn = true`, `purokClearanceVerified = true`, `purokClearanceVerifiedBy` = the staff member; no clearance image or online payment step. Write an audit log entry `CLEARANCE_EXEMPTED_WALKIN`.

---

## Phase 9 — Complaints (SLA and Escalation)

Screens R7, R8, S3. Entity `Complaint`, service `ComplaintService`.

- [ ] 🤖 `POST /api/complaints` (multipart if a photo is attached) — copies `purok` from the user's profile (the form has no Purok field), sets `slaDueAt` by category.
- [ ] 🤖 R7 shows status and an **Overdue** tag when `Complaint.isOverdue()` is true — reuse the model method rather than recomputing in the Fragment.
- [ ] 🤖 S3 lists complaints ordered by `priority DESC, slaDueAt ASC` **in the SQL query**, so the server does the sorting. (The `PriorityQueue<Complaint>` in the Class Model applies if the client re-sorts after local edits.) Overdue/high rows get the red left-edge bar.
- [ ] 🤖 `PATCH /api/complaints/:id/resolve` — accepts `status` of `reviewing` (staff picked it up), `resolved`, or `dismissed`. For the last two it requires a reply, stored in `staffResponse`, and sets `handledBy` and `resolvedAt`. It notifies the resident and writes `COMPLAINT_RESOLVED` to the audit log. R7 shows the reply when the resident expands the card.
- [ ] 🤖 When the SLA job escalates a complaint, notify all staff/admin (in-app + email), not the resident.

---

## Phase 10 — Announcements

Screens R10, S4. Entity `Announcement`.

- [ ] 🤖 `POST /api/announcements` (staff/admin) — title, body, category (`general`/`event`/`emergency`), optional `targetPurok`. Empty means barangay-wide. Validate each Purok against `puroks`.
- [ ] 🤖 Fan-out: after the insert, create one `notifications` row per targeted resident (`profiles` where `purok = ANY(targetPurok)`, or all active residents if barangay-wide) and queue the email batch.
- [ ] 🤖 `GET /api/announcements` returns only announcements for the caller's Purok (via `Announcement.isTargetedTo`) that are not expired; staff see all.
- [ ] 🤖 R10 pins the first `emergency` announcement at the top with red styling.

---

## Phase 11 — Emergency Reports and Purok Relief Dispatch

Screens R11, R12, S5, S6, S7. Entities `EmergencyReport`, `ReliefDistribution`, service `EmergencyResponseService`. This is the flagship feature, and it fulfils the Research Paper's own Chapter 5 recommendation to extend the system into emergency coordination.

- [ ] 🤖 `POST /api/emergency-reports` (multipart if photo) — `purok` is copied **server-side** from the user's profile; never trust a client-sent Purok. Initial status `reported`.
- [ ] 🤖 R11's Purok chip is read-only, filled from the profile.
- [ ] 🤖 `GET /api/emergency-reports/grouped-by-purok` (staff/admin) — one SQL query, grouped in JavaScript into an object keyed by Purok (the `HashMap<String, List<EmergencyReport>>` from the Class Model). S5 builds its filter chips from the response keys.
- [ ] 🤖 `PATCH /api/emergency-reports/:id/verify` moves `reported → verified` and notifies the resident.
- [ ] 🤖 S6's "Dispatch Relief" button stays disabled until `status = verified`.
- [ ] 🤖 S7 first fetches existing distributions for the report (`GET /api/relief-distributions?reportId=`) and renders already-served residents greyed out and non-interactive.
- [ ] 🤖 `POST /api/relief-distributions` runs in **one database transaction** (`BEGIN … COMMIT`):
  1. Re-check that `(reportId, residentId)` is not already in `reliefDistributions` (the application-level `HashSet<UUID>` check).
  2. Insert the `reliefDistributions` row.
  3. Update `emergencyReports.status` to `relief_dispatched`.
  4. Insert an `auditLogs` row (`RELIEF_DISPATCHED`).
  5. Insert a `notifications` row for the resident.
- [ ] 🤖 The `unique("reportId","residentId")` constraint is the hard backstop. Catch a unique-violation and return a clear message — never a raw 500.
- [ ] 🤖 Follow the 12-step flow and the `DispatchReliefByPurok` pseudocode in Section 7 of the Project Proposal; do not change the step order.
- [ ] 🤖 Mark a report `resolved` once every affected resident has a distribution row.

---

## Phase 12 — Notifications (Email and In-App)

Screens R9, S11, `NotificationBellView`. Entity `Notification`, service `NotificationService`. **No SMS.**

- [ ] 🤖 `notificationController` implements `pushInApp()` (insert a row and emit over Socket.io), `sendEmail()` (via Resend), and `broadcastToPurok(purok, message)`. Use a FIFO queue (`Queue<Notification>` in the Class Model) so a Purok-wide broadcast is delivered in order.
- [ ] 🤖 **Delivery tracking:** an email row starts `deliveryStatus = 'pending'`; on a successful Resend response set `sent`; on failure set `failed`, store `failureReason`, and update `deliveryAttempts` and `lastAttemptAt`. In-app-only rows are `sent` immediately.
- [ ] 🤖 Real-time: Socket.io, with the server emitting to a per-user room (`notifications:{userId}`) whenever a row is created. Android subscribes with the Socket.io client; fall back to polling `GET /api/notifications/me` every 15–30 seconds if the socket is too heavy.
- [ ] 🤖 Send email for: OTP, certificate ready, complaint resolved, complaint escalated (to staff), emergency verified, relief dispatched.
- [ ] 🤖 R9: unread count, mark-as-read (`PATCH /api/notifications/:id/read`), tap-through to the related record via `relatedId`.
- [ ] 🤖 **Notification Center (S11, admin):** `GET /api/notifications` filterable by `deliveryStatus`; `POST /api/notifications/:id/retry` re-attempts the Resend call and updates the tracking fields; `POST /api/notifications/:id/cancel` sets `cancelled` without deleting the row.

---

## Phase 13 — Rate Limiting and Anti-Abuse

- [ ] 🤖 `rateLimitMiddleware` on `POST /api/complaints` and `POST /api/emergency-reports`: count the user's rows in the last hour and return `429` above a threshold (default 5/hour).
- [ ] 🤖 This runs **server-side only**, so a modified APK cannot bypass it.
- [ ] 🤖 Emergency duplicate guard: a second report from the same user with the same `disasterType` within 10 minutes returns a `409`-style warning the app can show as "Submit again?" — a warning, **not** a hard block, so an accidental double-tap during a real emergency is not treated as abuse.
- [ ] 🤖 R8 and R11 show a friendly message and briefly disable Submit when the server returns the limit response.

---

## Phase 14 — Audit Logging

Screen S8 (admin). Entity `AuditLog`.

- [ ] 🤖 Write a shared `writeAuditLog(req, action, targetId, details)` helper that fills `ipAddress` from `req.ip` and `userAgent` from `req.headers['user-agent']`, so no call site repeats that logic.
- [ ] 🤖 Log, inside the same request or transaction: complaint resolution/escalation, relief dispatch, document status change, Purok Clearance verification and exemption, payment confirmation, walk-in registration, announcement post, certificate template update, user creation, role change, and account activation/deactivation.
- [ ] 🤖 `GET /api/audit-logs` (admin) powers S8.
- [ ] 🤖 There is **no** `PATCH` or `DELETE` route for `auditLogs` at all. Immutability comes from never writing those routes.

---

## Part E — Quality and Release

## Phase 15 — Testing

- [ ] 🤖 **Model unit tests (JUnit):** `Complaint.isOverdue()`, `Announcement.isTargetedTo()`, `EmergencyReport` status transitions.
- [ ] 🤖 **API tests (Jest + Supertest)** for every endpoint: happy path, wrong role/no token, and validation failure.
- [ ] 🤖 **Authorization tests:** call every staff/admin endpoint with a resident JWT and confirm `403`; call admin-only endpoints (audit log, Notification Center, user management, templates) with a staff JWT.
- [ ] 🤖 **Data-exposure test:** confirm `passwordHash` never appears in any response, and `ctcNumber`/`ctcOrNumber`/`disabilities` never appear in resident search or staff lists.
- [ ] 🤖 **Purok Clearance test:** online request without `purokClearanceUrl` is rejected; status cannot leave `pending` while `purokClearanceVerified = false`.
- [ ] 🤖 **Walk-in test:** the walk-in request has `purokClearanceVerified = true` automatically and an audit entry exists; the credential slip password is returned once and never stored in plaintext.
- [ ] 🤖 **Certificate test:** moving to `ready` produces a PDF, sets `certificateUrl`, and the download endpoint returns it only to the owner or staff.
- [ ] 🤖 **Rate-limit test:** more than the allowed submissions per hour returns `429`.
- [ ] 🤖 **Duplicate-dispatch test:** two `POST /api/relief-distributions` for the same `(reportId, residentId)` — the second fails gracefully, not with a 500.
- [ ] 🤖 **`targetPurok` validation test:** an announcement with an unknown Purok is rejected.
- [ ] 🤖 **SLA test:** backdate a complaint's `slaDueAt`, call the escalation function directly, and confirm `priority`, `escalatedAt`, and a staff notification.
- [ ] 🤖 **Notification retry/cancel test:** force a `failed` delivery, retry it, and confirm `deliveryAttempts` increments; cancel a pending one and confirm it stops.
- [ ] 🤖 **Role-based navigation test:** log in as staff and confirm 4 tabs with no Admin tab; log in as admin and confirm 5 tabs with the Admin tab opening S13; confirm each admin screen guards itself and cannot be reached by a staff account.
- [ ] 🤖 **Purok management test:** `POST /api/puroks` with a duplicate name returns `409`; a staff JWT gets `403`; the new Purok appears in the R2, S4, S5 and S7 dropdowns without an app update.
- [ ] 🤖 **Android UI pass:** open every screen and check empty, loading, and error states.
- [ ] 🧑 Provide a physical device or emulator with camera access for the photo flows (R5, R8, R11, R13).
- [ ] 🧑 Test on an **API 23 (Marshmallow)** emulator, an **API 26 or 27 (Oreo)** emulator, and one recent version. On each, check login, a list screen, a photo upload, and a certificate download. Test over plain HTTP to your laptop first; Android older than 7.1.1 may not trust a hosted server's HTTPS certificate.
- [ ] 🧑 Walk through the **Purok relief flow** (R11 → S5 → S6 → S7) yourself at least once before the defense.
- [ ] 🧑 Walk through the **walk-in flow** (S10) yourself: search, register, hand over the credential slip, create the request.

---

## Phase 16 — Deployment and Signing

- [ ] 🧑 Decide where the Node server runs for the defense: a free-tier host (Render, Railway, Fly.io), a VPS, or the group's own laptop on the same Wi-Fi as the demo phone. The laptop option avoids a free tier sleeping mid-demo — but confirm the firewall allows the chosen port.
- [ ] 🧑 Provision the production PostgreSQL instance and set the production `DATABASE_URL`.
- [ ] 🤝 Agent writes the production migration (same SQL as Phase 2); the human runs it, then runs the seed script.
- [ ] 🧑 Set the production Resend key and sender domain, and a strong production `JWT_SECRET` different from the dev one.
- [ ] 🤝 Agent runs `keytool -genkey …` to create the signing keystore; the human stores the `.jks` and its passwords safely, **outside git**. Losing them means the app can never be updated under the same listing.
- [ ] 🤖 Build a signed release APK/AAB pointed at the production API.
- [ ] 🧑 Smoke-test the signed build against the production server before the defense.
- [ ] 🧑 Seed the demo data from Phase 1.
- [ ] 🧑 Google Play is optional — a direct-install APK is enough for a classroom defense. If publishing, create the Play Console account yourself.

---

## Milestone Timeline

| Week | Milestone |
| --- | --- |
| 1 | Phases 0–2: tools installed, real content gathered, database migrated and seeded |
| 2 | Phase 3: Express API built and tested endpoint-by-endpoint in Postman/Insomnia |
| 3 | Phase 4 + Phase 6 begins: Android foundation; Master Design System Brief and R1–R3 designed in Stitch |
| 4 | Phase 6: R4–R13 designed and built; Resident navigation wired; Phase 7 (auth + onboarding) working end to end |
| 5 | Phase 6: S1–S14 designed and built; Staff navigation wired; Phase 8 (requests, Purok Clearance, certificates, walk-in) complete |
| 6 | Phases 9–11: complaints, announcements, emergency + relief dispatch (flagship) complete |
| 7 | Phases 12–14: notifications, rate limiting, audit logging; run the Gemini CLI QA prompt and fix findings |
| 8 | Phases 15–16: full test pass, hosting, keystore, demo data, rehearsal |

---

## Can an Agent Build This From This File Alone?

Mostly, yes. With the companion documents the agent has:

- The exact schema (copy-pasteable SQL), with every business rule the schema cannot enforce written out
- A full REST contract: every endpoint, method, and who may call it
- Exact Java classes (Class and Object Model) and a screen-by-screen spec with the Android components for each
- A design-and-build pipeline: ordered Stitch prompts and Gemini CLI prompts with parameter tables
- The flagship dispatch sequence, including its database transaction
- A marker on every checklist item: proceed (🤖) or stop and ask (🧑 / 🤝)

**What the agent cannot do and must ask the human for:**

- A real PostgreSQL connection string, production `JWT_SECRET`, and the Resend API key
- Creating or authorizing the Stitch account and its MCP connection to Gemini CLI
- Approving each Stitch design against its "Must include" line
- Real Purok names, fees, seal, signature, official's details, first-admin details, and consent wording
- Keystore safekeeping, hosting choice, and demo data

---

## Known Gaps and Future Work

**Deliberate exclusions**

- **SMS notifications** — out of scope (cost). Email + in-app only, including emergency broadcasts.
- **Online payment gateway** — out of scope, per the Research Paper's limitation.
- **Offline mode** — v1 needs a live connection.

**Open items and trade-offs**

- **No push notifications (FCM).** A resident with the app closed during a disaster sees nothing until they reopen it.
- **No offline queueing** for emergency reports, although connectivity often fails first in a flood or typhoon.
- **No Purok-level analytics or admin reports dashboard**, and **no CSV export** (the Research Paper lists reports and export as admin capabilities). Add `GET /api/admin/reports` and an export endpoint if time allows.
- **Single-language UI** — no Filipino/English toggle.
- **Puroks can be added but not renamed or deleted in the app.** Other tables reference the Purok name, so a rename is a developer-run data migration, not a screen. Decide the real names before launch.
- **Purok Clearance review is manual.** Staff visually approve or reject the photo; there is no OCR. This mirrors the real process but ties processing time to staff availability.
- **Authorization lives in application code.** A bug in one Express route is not caught by a database policy; the Phase 15 authorization tests are the mitigation.
- **The SLA job only runs while the Node process is alive.**
- **No scaling work** (PM2, pooled managed Postgres) — fine for a classroom demo.
- **Stitch designs are only as faithful as their approval.** Nothing automated confirms a design matches the spec; the human check and the Gemini CLI QA prompt are the safeguards.
- **Uploads on local disk** need a persistent disk or a cloud bucket in production.

---

*Where documents disagree, follow this order: Database Structure (data) → this Build Plan (behavior, API, screens) → everything else. Every task is tagged 🤖 AGENT, 🧑 HUMAN, or 🤝 BOTH so nothing is attempted, or invented, by the wrong party.*