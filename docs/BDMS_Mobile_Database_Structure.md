**DATABASE STRUCTURE**

**BDMS Mobile**

**PostgreSQL Schema Reference — Complete Revision**

*Revised after a full feature-by-feature gap check against the Project Proposal and the original system's onboarding and payment workflows.*

| **Field**                     | **Value**                                                                                                                                                        |
|-------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Database Engine               | PostgreSQL (plain — no third-party BaaS)                                                                                                                         |
| Backend Access Layer          | Node.js (Express) REST API using the pg driver                                                                                                                   |
| Authentication                | Custom JWT, issued by the Node.js backend; passwords hashed with bcrypt                                                                                          |
| Identifier Convention         | UUID primary keys (gen_random_uuid()), camelCase quoted column names                                                                                             |
| Total Tables                  | 11 (9 core feature tables + 2 supporting tables)                                                                                                                 |
| What changed in this revision | Added Puroks and OtpCodes tables; expanded Profiles with full onboarding fields; expanded DocumentRequests with payment/rejection/business fields; added indexes |

## 1. Overview

BDMS Mobile uses a single, plain PostgreSQL database as its system of record. There is no third-party backend-as-a-service layer — the Node.js/Express API is the only application that talks to the database, using the pg driver with parameterized SQL.

This revision was produced by re-checking the schema against every feature described in the Project Proposal and against the original web system's documented onboarding flow and payment workflow, which an earlier draft of this schema had simplified too far. Three gaps were found and closed:

- The 6-step onboarding form collects far more than the original Profiles table stored (sex, birthdate, civil status, citizenship, residency details, sector, CTC/OR numbers, disabilities) — these columns were missing and are now added.

- Document request processing needs payment tracking, a rejection reason, and staff notes — fields the original web system had and this schema had dropped. These are now added, along with optional business-specific fields for Business Clearance requests.

- OTP verification (used at onboarding Step 6) had no table to persist codes in, which would have made the flow unreliable across server restarts. A dedicated OtpCodes table is now added.

A fourth improvement — not a functional gap, but worth doing at the same time — replaces free-text Purok values with a proper Puroks lookup table, so a typo can no longer silently break Purok-based filtering for complaints, emergency reports, and relief dispatch.

A second gap-check pass — this time against the original Research Paper (the formal capstone document, not just the technical README) — found four more real gaps, now closed in this revision:

- Purok Clearance — the paper requires residents to upload a photo/scan of their physical Purok Clearance (a Purok leader's endorsement) before submitting any certificate request, with staff verifying the image before processing. This is a distinct concept from the Purok location field and was missing entirely.

- The Walk-In flow — staff registering a resident at the counter, generating a one-time credential slip, and creating a request on that resident's behalf (exempt from the clearance-upload and online-payment steps) — was previously treated as out of scope. The paper confirms it is core to the system, so it is now included.

- The request status lifecycle was missing the 'ready' state (generated, awaiting pickup/delivery) between 'processing' and 'completed'.

- Notifications had no delivery-status tracking, so there was no way to represent the paper's 'retry failed deliveries / cancel pending notifications' admin capability.

SMS-based notifications remain explicitly out of scope, as discussed — email and in-app notifications only, for cost reasons.

One further gap was found while rewriting the build plan: complaints had no column to store the staff reply, so a resident could never read the outcome. complaints.staffResponse was added.

**Naming and Type Conventions**

- Table and column names use camelCase and are double-quoted in SQL (e.g. "documentRequests", "userId") to preserve case in PostgreSQL, which folds unquoted identifiers to lowercase.

- All primary keys are UUID, generated with gen_random_uuid() (pgcrypto extension), except Puroks, whose natural key (the Purok name itself) is used directly as its primary key.

- All timestamps use TIMESTAMPTZ (timezone-aware); birthdate uses plain DATE.

- Repeated small values (needs, targetPurok, sector, disabilities, photoUrls) use native PostgreSQL array columns (TEXT\[\]) rather than separate join tables, since they are short, fixed-vocabulary lists.

- Free-form structured data (audit log metadata) uses JSONB.

- passwordHash is a server-only column. It is never returned by any API response and has no corresponding field in the Android-side Profile class — the Node.js backend is the only thing that ever reads or writes it.

## 2. Entity-Relationship Diagrams

### 2.1 Core Feature Tables

Profiles is the parent of every core feature table. Relief Distributions is the only table with two parents: it belongs to one Emergency Report and references Profiles twice (the resident who received relief, and the staff member who distributed it).

*(diagram omitted in the Markdown version)*

*Figure 1. Core feature table relationships.*

### 2.2 Supporting / Lookup Tables

Puroks is a small reference table — a fixed, staff-managed list of the barangay's Purok/Zone names — that every purok text column elsewhere now foreign-keys into, instead of storing an unvalidated free-text string. OtpCodes is intentionally standalone: at the moment an OTP is issued (onboarding Step 6, before the account is fully created) there is no profiles row yet to attach it to, so it is keyed by email instead.

*(diagram omitted in the Markdown version)*

*Figure 2. Supporting/lookup tables added in this revision.*

## 3. Table Definitions

Each table below lists every column, its PostgreSQL type, its constraints, and its meaning. The first row of each table is its primary key. Columns added in this gap-check revision are marked NEW.

### 3.1 Puroks

*SQL table name: "puroks"*

Staff-managed reference list of the barangay's Purok/Zone names. Added in this revision to replace unvalidated free-text Purok values everywhere else in the schema.

| **Column**  | **Type**    | **Constraints** | **Description**                                                |
|-------------|-------------|-----------------|----------------------------------------------------------------|
| **name**    | TEXT        | PRIMARY KEY     | The Purok/Zone name exactly as shown to users (e.g. "Purok 3") |
| description | TEXT        | -              | Optional note (boundaries, landmarks)                          |
| createdAt   | TIMESTAMPTZ | DEFAULT now()   | When this Purok was added to the list                          |

**PK** *= Primary Key column* **NEW** *= column added in this gap-check revision*

### 3.2 OtpCodes

*SQL table name: "otpCodes"*

Holds one-time verification codes for email OTP (onboarding Step 6). Keyed by email rather than a profile id, since the account may not exist yet at the moment a code is issued. Added in this revision — without it, OTP state only lived in server memory and would be lost on every restart.

| **Column** | **Type**    | **Constraints**                        | **Description**                                                       |
|------------|-------------|----------------------------------------|-----------------------------------------------------------------------|
| **otpId**  | UUID        | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique code record                                                    |
| email      | TEXT        | NOT NULL                               | Email address the code was sent to                                    |
| code       | TEXT        | NOT NULL                               | The 6-digit verification code                                         |
| purpose    | TEXT        | DEFAULT 'registration'                 | registration (extensible to password-reset, etc. later)               |
| expiresAt  | TIMESTAMPTZ | NOT NULL                               | Code expiry — 10 minutes after issuance                               |
| consumedAt | TIMESTAMPTZ | -                                     | Set once the code has been successfully used, so it can't be replayed |
| createdAt  | TIMESTAMPTZ | DEFAULT now()                          | When the code was generated                                           |

**PK** *= Primary Key column* **NEW** *= column added in this gap-check revision*

### 3.3 Profiles

*SQL table name: "profiles"*

Root identity table. Stores every account — resident, staff, and admin — in one table, differentiated by role. Expanded in this revision to hold every field the documented 6-step onboarding form actually collects.

| **Column**         | **Type**    | **Constraints**                        | **Description**                                                                                        |
|--------------------|-------------|----------------------------------------|--------------------------------------------------------------------------------------------------------|
| **id**             | UUID        | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique account identifier                                                                              |
| username           | TEXT        | UNIQUE, NOT NULL                       | Step 1 — chosen username                                                                               |
| fullName           | TEXT        | NOT NULL                               | Step 2 — full name                                                                                     |
| email              | TEXT        | UNIQUE, NOT NULL                       | Step 1 — login email address                                                                           |
| passwordHash       | TEXT        | NOT NULL                               | Step 1 — bcrypt-hashed password (server-only, never returned by the API)                               |
| mobileNumber       | TEXT        | -                                     | Philippine mobile number                                                                               |
| sex                | TEXT        | -                                     | Step 2                                                                                                 |
| birthdate          | DATE        | -                                     | Step 2                                                                                                 |
| civilStatus        | TEXT        | -                                     | Step 2 — Single, Married, Widowed, Separated, Annulled                                                 |
| citizenship        | TEXT        | -                                     | Step 2 — Filipino, Dual Citizen, Foreign National                                                      |
| birthplace         | TEXT        | -                                     | Step 2                                                                                                 |
| purok              | TEXT        | REFERENCES puroks(name)                | Step 3 — structured Purok/Zone, required, first field                                                  |
| address            | TEXT        | -                                     | Step 3 — street/house number detail                                                                    |
| residencyType      | TEXT        | -                                     | Step 3 — Owner, Renter, Living with Relatives, Government Housing, Others                              |
| residencyPeriod    | TEXT        | -                                     | Step 3 — duration of residency                                                                         |
| sector             | TEXT\[\]    | -                                     | Step 3 — Youth, Senior Citizen, PWD, Solo Parent, etc.                                                 |
| ctcNumber          | TEXT        | -                                     | Step 4 — Community Tax Certificate number                                                              |
| ctcOrNumber        | TEXT        | -                                     | Step 4 — Official Receipt number for the CTC (kept distinct from a document request's payment receipt) |
| disabilities       | TEXT\[\]    | -                                     | Step 4 — optional                                                                                      |
| avatarUrl          | TEXT        | -                                     | Step 4 — profile photo path (uploaded via Multer)                                                      |
| consentAcceptedAt  | TIMESTAMPTZ | -                                     | Step 5 — when the RA 10173 privacy notice was accepted                                                 |
| role               | TEXT        | NOT NULL, DEFAULT 'resident'           | resident, staff, or admin                                                                              |
| registrationMethod | TEXT        | DEFAULT 'online'                       | online or walk_in                                                                                      |
| registeredBy       | UUID        | REFERENCES profiles(id)                | Staff who performed a walk-in registration; null for self-registered residents                         |
| isActive           | BOOLEAN     | DEFAULT true                           | Whether the account may be used                                                                        |
| createdAt          | TIMESTAMPTZ | DEFAULT now()                          | Account creation timestamp                                                                             |

**PK** *= Primary Key column* **NEW** *= column added in this gap-check revision*

### 3.4 DocumentRequests

*SQL table name: "documentRequests"*

One resident's request for an official barangay certificate. Expanded in this revision with the payment-tracking, rejection, and staff-note fields the original web system had, optional Business Clearance fields, the 'ready' status, and the Purok Clearance upload/verification and walk-in exemption fields required by the Research Paper.

| **Column**               | **Type**      | **Constraints**                        | **Description**                                                                                           |
|--------------------------|---------------|----------------------------------------|-----------------------------------------------------------------------------------------------------------|
| **requestId**            | UUID          | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique request identifier                                                                                 |
| userId                   | UUID          | NOT NULL, REFERENCES profiles(id)      | Requesting resident                                                                                       |
| documentType             | TEXT          | NOT NULL                               | Clearance, Indigency, Residency, ID, Business Clearance                                                   |
| controlNumber            | TEXT          | UNIQUE                                 | Auto-generated control number                                                                             |
| purpose                  | TEXT          | -                                     | Stated purpose of the request                                                                             |
| deliveryMethod           | TEXT          | -                                     | pickup, email, or both                                                                                    |
| fee                      | NUMERIC(10,2) | -                                     | Document fee (PHP)                                                                                        |
| status                   | TEXT          | DEFAULT 'pending'                      | pending, processing, ready, completed, rejected                                                           |
| paymentStatus            | TEXT          | DEFAULT 'pending'                      | pending or confirmed                                                                                      |
| paymentMethod            | TEXT          | -                                     | cash, debit_card, credit_card, waived, or demo                                                            |
| paymentOrNumber          | TEXT          | -                                     | Official Receipt number for the payment                                                                   |
| rejectionReason          | TEXT          | -                                     | Reason shown to the resident if status = rejected                                                         |
| staffNotes               | TEXT          | -                                     | Internal notes from staff, not shown to the resident                                                      |
| businessName             | TEXT          | -                                     | Only populated for Business Clearance requests                                                            |
| businessAddress          | TEXT          | -                                     | Only populated for Business Clearance requests                                                            |
| businessType             | TEXT          | -                                     | Only populated for Business Clearance requests                                                            |
| isWalkIn                 | BOOLEAN       | DEFAULT false                          | True when staff created this request on behalf of a resident physically present                           |
| purokClearanceUrl        | TEXT          | -                                     | Resident-uploaded photo/scan of their Purok Clearance; required for online requests, not for walk-in      |
| purokClearanceVerified   | BOOLEAN       | DEFAULT false                          | Set true once staff reviews and accepts the uploaded clearance; auto-true for walk-in requests (exempted) |
| purokClearanceVerifiedBy | UUID          | REFERENCES profiles(id)                | Staff who verified the clearance (or exempted it for a walk-in)                                           |
| purokClearanceVerifiedAt | TIMESTAMPTZ   | -                                     | When the clearance was verified or exempted                                                               |
| certificateUrl           | TEXT          | -                                     | Path to the generated certificate PDF, once completed                                                     |
| processedBy              | UUID          | REFERENCES profiles(id)                | Staff/admin who processed the request                                                                     |
| createdAt                | TIMESTAMPTZ   | DEFAULT now()                          | Submission timestamp                                                                                      |
| completedAt              | TIMESTAMPTZ   | -                                     | Completion timestamp                                                                                      |

**PK** *= Primary Key column* **NEW** *= column added in this gap-check revision*

### 3.5 Complaints

*SQL table name: "complaints"*

A resident's non-emergency concern, with SLA-deadline and escalation fields.

| **Column**      | **Type**    | **Constraints**                        | **Description**                                                               |
|-----------------|-------------|----------------------------------------|-------------------------------------------------------------------------------|
| **complaintId** | UUID        | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique complaint identifier                                                   |
| userId          | UUID        | NOT NULL, REFERENCES profiles(id)      | Reporting resident                                                            |
| purok           | TEXT        | REFERENCES puroks(name)                | Denormalized from profiles for fast filtering                                 |
| category        | TEXT        | NOT NULL                               | noise, sanitation, dispute, infrastructure, other                             |
| description     | TEXT        | NOT NULL                               | Complaint details                                                             |
| photoUrls       | TEXT\[\]    | -                                     | Optional supporting photos                                                    |
| status          | TEXT        | DEFAULT 'pending'                      | pending, reviewing, resolved, dismissed                                       |
| priority        | TEXT        | DEFAULT 'normal'                       | normal or high (auto-escalated)                                               |
| slaDueAt        | TIMESTAMPTZ | -                                     | Escalation deadline, set at insert                                            |
| escalatedAt     | TIMESTAMPTZ | -                                     | Set when the SLA is breached                                                  |
| handledBy       | UUID        | REFERENCES profiles(id)                | Staff who resolved/handled it                                                 |
| staffResponse   | TEXT        | -                                     | Staff reply shown to the resident when the complaint is resolved or dismissed |
| createdAt       | TIMESTAMPTZ | DEFAULT now()                          | Submission timestamp                                                          |
| resolvedAt      | TIMESTAMPTZ | -                                     | Resolution timestamp                                                          |

**PK** *= Primary Key column* **NEW** *= column added in this gap-check revision*

### 3.6 Announcements

*SQL table name: "announcements"*

A staff-issued broadcast, barangay-wide or targeted to specific Puroks.

| **Column**         | **Type**    | **Constraints**                        | **Description**                                                                                                                                 |
|--------------------|-------------|----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| **announcementId** | UUID        | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique announcement identifier                                                                                                                  |
| title              | TEXT        | NOT NULL                               | Announcement headline                                                                                                                           |
| body               | TEXT        | NOT NULL                               | Announcement content                                                                                                                            |
| category           | TEXT        | DEFAULT 'general'                      | general, event, or emergency                                                                                                                    |
| targetPurok        | TEXT\[\]    | -                                     | Empty/null = barangay-wide. Array elements are not FK-enforced by PostgreSQL (see §4 note); validated in the Node.js API against Puroks instead |
| postedBy           | UUID        | NOT NULL, REFERENCES profiles(id)      | Staff/admin who posted it                                                                                                                       |
| attachmentUrl      | TEXT        | -                                     | Optional attachment                                                                                                                             |
| createdAt          | TIMESTAMPTZ | DEFAULT now()                          | Publish timestamp                                                                                                                               |
| expiresAt          | TIMESTAMPTZ | -                                     | Optional expiry                                                                                                                                 |

**PK** *= Primary Key column* **NEW** *= column added in this gap-check revision*

### 3.7 EmergencyReports

*SQL table name: "emergencyReports"*

A resident's disaster report, automatically tagged with the reporter's Purok.

| **Column**               | **Type**    | **Constraints**                        | **Description**                                 |
|--------------------------|-------------|----------------------------------------|-------------------------------------------------|
| **reportId**             | UUID        | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique report identifier                        |
| userId                   | UUID        | NOT NULL, REFERENCES profiles(id)      | Reporting resident                              |
| purok                    | TEXT        | NOT NULL, REFERENCES puroks(name)      | Copied from profiles.purok at submission time   |
| disasterType             | TEXT        | NOT NULL                               | flood, fire, typhoon, earthquake, landslide     |
| householdMembersAffected | INT         | -                                     | Number of affected household members            |
| needs                    | TEXT\[\]    | -                                     | food, water, medicine, shelter, evacuation      |
| photoUrl                 | TEXT        | -                                     | Optional supporting photo                       |
| status                   | TEXT        | DEFAULT 'reported'                     | reported, verified, relief_dispatched, resolved |
| createdAt                | TIMESTAMPTZ | DEFAULT now()                          | Submission timestamp                            |

**PK** *= Primary Key column* **NEW** *= column added in this gap-check revision*

### 3.8 ReliefDistributions

*SQL table name: "reliefDistributions"*

One resident's received relief goods, tied to an emergency report. The unique constraint is the database-level guard against dispatching relief twice to the same resident for the same report.

| **Column**             | **Type**    | **Constraints**                                 | **Description**                         |
|------------------------|-------------|-------------------------------------------------|-----------------------------------------|
| **distributionId**     | UUID        | PRIMARY KEY, DEFAULT gen_random_uuid()          | Unique distribution record              |
| reportId               | UUID        | NOT NULL, REFERENCES emergencyReports(reportId) | Related emergency report                |
| residentId             | UUID        | NOT NULL, REFERENCES profiles(id)               | Resident who received relief            |
| purok                  | TEXT        | REFERENCES puroks(name)                         | Purok of the distribution batch         |
| itemsGiven             | TEXT        | -                                              | Description of relief goods distributed |
| distributedBy          | UUID        | NOT NULL, REFERENCES profiles(id)               | Staff who distributed the goods         |
| createdAt              | TIMESTAMPTZ | DEFAULT now()                                   | Distribution timestamp                  |
| (reportId, residentId) | -          | UNIQUE                                          | Prevents duplicate relief dispatch      |

**PK** *= Primary Key column* **NEW** *= column added in this gap-check revision*

### 3.9 Notifications

*SQL table name: "notifications"*

One in-app (and mirrored email) notification delivered to a resident or staff member. Expanded in this revision with delivery-status tracking, so an admin Notification Center can retry failed email deliveries or cancel pending ones, as the Research Paper requires.

| **Column**         | **Type**    | **Constraints**                        | **Description**                                                                                         |
|--------------------|-------------|----------------------------------------|---------------------------------------------------------------------------------------------------------|
| **notificationId** | UUID        | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique notification identifier                                                                          |
| userId             | UUID        | NOT NULL, REFERENCES profiles(id)      | Recipient                                                                                               |
| title              | TEXT        | NOT NULL                               | Notification title                                                                                      |
| message            | TEXT        | NOT NULL                               | Notification body                                                                                       |
| type               | TEXT        | NOT NULL                               | request_update, complaint, announcement, emergency                                                      |
| isRead             | BOOLEAN     | DEFAULT false                          | Whether the recipient has read it                                                                       |
| relatedId          | UUID        | -                                     | ID of the related record, if any                                                                        |
| deliveryStatus     | TEXT        | DEFAULT 'pending'                      | pending, sent, failed, or cancelled (email delivery only — in-app rows are always considered delivered) |
| deliveryAttempts   | INT         | DEFAULT 0                              | Number of email send attempts made                                                                      |
| lastAttemptAt      | TIMESTAMPTZ | -                                     | When the most recent email attempt was made                                                             |
| failureReason      | TEXT        | -                                     | Error message from the last failed attempt, shown to the admin                                          |
| createdAt          | TIMESTAMPTZ | DEFAULT now()                          | Timestamp                                                                                               |

**PK** *= Primary Key column* **NEW** *= column added in this gap-check revision*

### 3.10 CertificateTemplates

*SQL table name: "certificateTemplates"*

The barangay seal, official signature, and signing-official details used when generating certificate PDFs.

| **Column**     | **Type**    | **Constraints**                        | **Description**                                |
|----------------|-------------|----------------------------------------|------------------------------------------------|
| **templateId** | UUID        | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique template configuration                  |
| documentType   | TEXT        | NOT NULL                               | Certificate type this configuration applies to |
| sealUrl        | TEXT        | -                                     | Barangay seal image path                       |
| signatureUrl   | TEXT        | -                                     | Official's signature image path                |
| officialName   | TEXT        | -                                     | Signing official's name                        |
| officialTitle  | TEXT        | -                                     | Signing official's title                       |
| updatedAt      | TIMESTAMPTZ | DEFAULT now()                          | Last update timestamp                          |

**PK** *= Primary Key column* **NEW** *= column added in this gap-check revision*

### 3.11 AuditLogs

*SQL table name: "auditLogs"*

An immutable record of every sensitive administrative action. Insert-only — no update or delete route exists for this table. Expanded in this revision with explicit IP address and user agent columns, since the Research Paper calls these out specifically as required for forensic/compliance purposes rather than leaving them folded into the generic metadata column.

| **Column**     | **Type**    | **Constraints**                        | **Description**                                                    |
|----------------|-------------|----------------------------------------|--------------------------------------------------------------------|
| **auditLogId** | UUID        | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique audit record                                                |
| performedBy    | UUID        | NOT NULL, REFERENCES profiles(id)      | Staff/admin who performed the action                               |
| action         | TEXT        | NOT NULL                               | e.g. STATUS_CHANGE, RELIEF_DISPATCHED, COMPLAINT_RESOLVED          |
| targetId       | UUID        | -                                     | Affected record identifier                                         |
| details        | TEXT        | -                                     | Human-readable description                                         |
| ipAddress      | TEXT        | -                                     | Request IP address, captured server-side                           |
| userAgent      | TEXT        | -                                     | Request user agent / device string, captured server-side           |
| metadata       | JSONB       | -                                     | Any additional structured context not covered by the columns above |
| createdAt      | TIMESTAMPTZ | DEFAULT now()                          | Action timestamp                                                   |

**PK** *= Primary Key column* **NEW** *= column added in this gap-check revision*

## 4. Relationships and Referential Integrity

| **Parent Table** | **Foreign Key (Child Column)**            | **Child Table**     | **Cardinality** |
|------------------|-------------------------------------------|---------------------|-----------------|
| puroks           | profiles.purok                            | profiles            | 1 to many       |
| puroks           | complaints.purok                          | complaints          | 1 to many       |
| puroks           | emergencyReports.purok                    | emergencyReports    | 1 to many       |
| puroks           | reliefDistributions.purok                 | reliefDistributions | 1 to many       |
| profiles         | documentRequests.userId                   | documentRequests    | 1 to many       |
| profiles         | documentRequests.processedBy              | documentRequests    | 1 to many       |
| profiles         | documentRequests.purokClearanceVerifiedBy | documentRequests    | 1 to many       |
| profiles         | profiles.registeredBy (self-referencing)  | profiles            | 1 to many       |
| profiles         | complaints.userId                         | complaints          | 1 to many       |
| profiles         | complaints.handledBy                      | complaints          | 1 to many       |
| profiles         | announcements.postedBy                    | announcements       | 1 to many       |
| profiles         | emergencyReports.userId                   | emergencyReports    | 1 to many       |
| profiles         | reliefDistributions.residentId            | reliefDistributions | 1 to many       |
| profiles         | reliefDistributions.distributedBy         | reliefDistributions | 1 to many       |
| profiles         | notifications.userId                      | notifications       | 1 to many       |
| profiles         | auditLogs.performedBy                     | auditLogs           | 1 to many       |
| emergencyReports | reliefDistributions.reportId              | reliefDistributions | 1 to many       |

otpCodes has no foreign key to profiles — it is looked up by email, since a code can be issued before an account exists.

**Key Constraints Summary**

- documentRequests.controlNumber — UNIQUE, generated server-side at insert.

- reliefDistributions (reportId, residentId) — UNIQUE composite constraint; the hard database-level guard preventing the same resident from being recorded twice against the same emergency report.

- profiles.email and profiles.username — both UNIQUE.

- puroks.name is used directly as its primary key (a natural key), since Purok names are short, stable, and already unique by definition.

- No table allows DELETE from the application layer for complaints, emergencyReports, or auditLogs — resolution/escalation is expressed through status changes, not row deletion, preserving a complete history.

**A Known Limitation of the Array Columns**

PostgreSQL cannot enforce a foreign key on individual elements of an array column. announcements.targetPurok (TEXT\[\]) is therefore not database-enforced against puroks.name the way the singular purok columns are — the Node.js API validates each array element against the Puroks table before insert instead. This is stated explicitly so it isn't mistaken for an oversight.

**Business Rule: Purok Clearance Requirement**

The database does not enforce this rule with a CHECK constraint, since it depends on isWalkIn, which is set at the same time as the other columns it would need to check against. It is enforced in the Node.js API layer instead: an online request (isWalkIn = false) may not be created without purokClearanceUrl present, and may not move past status = 'pending' until purokClearanceVerified = true. A walk-in request (isWalkIn = true) has purokClearanceVerified set to true automatically at creation, with purokClearanceVerifiedBy recording the staff member who registered it, since the resident's identity has already been confirmed in person.

## 5. Indexes

Beyond the automatic indexes PostgreSQL creates for primary keys and unique constraints, the following are added for columns that are filtered on frequently — Purok-based filtering (the flagship feature) and status-based queue views used throughout the staff screens.

```sql
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

## 6. Full SQL Definition (DDL)

This is the complete, runnable schema — identical to the migration used in the project build plan, so the database, the API, and this reference document never drift out of sync.

```sql
-- Extensions
create extension if not exists pgcrypto;   -- gen_random_uuid()

-- PUROKS — staff-managed lookup list
create table puroks (
  name text primary key,
  description text,
  "createdAt" timestamptz default now()
);

-- OTP CODES — email verification, keyed by email (no profiles FK)
create table "otpCodes" (
  "otpId" uuid primary key default gen_random_uuid(),
  email text not null,
  code text not null,
  purpose text default 'registration',
  "expiresAt" timestamptz not null,
  "consumedAt" timestamptz,
  "createdAt" timestamptz default now()
);

-- PROFILES — root identity table
create table profiles (
  id uuid primary key default gen_random_uuid(),
  username text unique not null,
  "fullName" text not null,
  email text unique not null,
  "passwordHash" text not null,
  "mobileNumber" text,
  sex text,
  birthdate date,
  "civilStatus" text,
  citizenship text,
  birthplace text,
  purok text references puroks(name),
  address text,
  "residencyType" text,
  "residencyPeriod" text,
  sector text[],
  "ctcNumber" text,
  "ctcOrNumber" text,
  disabilities text[],
  "avatarUrl" text,
  "consentAcceptedAt" timestamptz,
  role text not null default 'resident',
  "registrationMethod" text default 'online',   -- online | walk_in
  "registeredBy" uuid references profiles(id),  -- staff who walk-in registered this account
  "isActive" boolean default true,
  "createdAt" timestamptz default now()
);

-- DOCUMENT REQUESTS
create table "documentRequests" (
  "requestId" uuid primary key default gen_random_uuid(),
  "userId" uuid references profiles(id) not null,
  "documentType" text not null,
  "controlNumber" text unique,
  purpose text,
  "deliveryMethod" text,
  fee numeric(10,2),
  status text default 'pending',          -- pending | processing | ready | completed | rejected
  "paymentStatus" text default 'pending',
  "paymentMethod" text,                   -- cash | debit_card | credit_card | waived | demo
  "paymentOrNumber" text,
  "rejectionReason" text,
  "staffNotes" text,
  "businessName" text,
  "businessAddress" text,
  "businessType" text,
  "isWalkIn" boolean default false,
  "purokClearanceUrl" text,
  "purokClearanceVerified" boolean default false,
  "purokClearanceVerifiedBy" uuid references profiles(id),
  "purokClearanceVerifiedAt" timestamptz,
  "certificateUrl" text,
  "processedBy" uuid references profiles(id),
  "createdAt" timestamptz default now(),
  "completedAt" timestamptz
);

-- COMPLAINTS
create table complaints (
  "complaintId" uuid primary key default gen_random_uuid(),
  "userId" uuid references profiles(id) not null,
  purok text references puroks(name),
  category text not null,
  description text not null,
  "photoUrls" text[],
  status text default 'pending',
  priority text default 'normal',
  "slaDueAt" timestamptz,
  "escalatedAt" timestamptz,
  "handledBy" uuid references profiles(id),
  "staffResponse" text,
  "createdAt" timestamptz default now(),
  "resolvedAt" timestamptz
);

-- ANNOUNCEMENTS
create table announcements (
  "announcementId" uuid primary key default gen_random_uuid(),
  title text not null,
  body text not null,
  category text default 'general',
  "targetPurok" text[],
  "postedBy" uuid references profiles(id) not null,
  "attachmentUrl" text,
  "createdAt" timestamptz default now(),
  "expiresAt" timestamptz
);

-- EMERGENCY REPORTS
create table "emergencyReports" (
  "reportId" uuid primary key default gen_random_uuid(),
  "userId" uuid references profiles(id) not null,
  purok text not null references puroks(name),
  "disasterType" text not null,
  "householdMembersAffected" int,
  needs text[],
  "photoUrl" text,
  status text default 'reported',
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
  unique ("reportId", "residentId")
);

-- NOTIFICATIONS
create table notifications (
  "notificationId" uuid primary key default gen_random_uuid(),
  "userId" uuid references profiles(id) not null,
  title text not null,
  message text not null,
  type text not null,
  "isRead" boolean default false,
  "relatedId" uuid,
  "deliveryStatus" text default 'pending',  -- pending | sent | failed | cancelled
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
  action text not null,
  "targetId" uuid,
  details text,
  "ipAddress" text,
  "userAgent" text,
  metadata jsonb,
  "createdAt" timestamptz default now()
);

-- See Section 5 for the full set of indexes.
```

## 7. Implementation Notes

- This schema is created and managed by the Node.js backend's migration file (server/migrations/001_init.sql) — there is no separate managed-database dashboard involved.

- Authorization (who may read/write which rows) is enforced in the Express API layer, not the database — plain PostgreSQL has no row-level security layer by default.

- Puroks should be seeded once (via a migration or an admin screen) with the real barangay's Purok/Zone names before onboarding goes live; the app must not invent or hardcode this list.

- The SLA escalation process (complaints.slaDueAt / escalatedAt / priority) is driven by a scheduled job running inside the Node.js process (node-cron).

- OTP codes should be deleted or ignored once expiresAt has passed; a simple periodic cleanup query is sufficient for this project's scale — no separate job is required.

- File paths (avatarUrl, photoUrls, photoUrl, sealUrl, signatureUrl, certificateUrl) point to files uploaded through the Node.js backend (Multer) rather than a third-party storage bucket.

- passwordHash, ctcNumber, ctcOrNumber, and disabilities are sensitive/personal fields — the API should exclude them from any list-style response (e.g. a staff resident search) and only return them on a resident's own profile request.

- Walk-in registration: when staff create a profile with registrationMethod = 'walk_in', the API generates a temporary password, stores only its bcrypt hash, and returns the plaintext password exactly once in that response for the staff member to hand to the resident as a credential slip — it is never stored or logged in plaintext.

- A walk-in document request (isWalkIn = true) is exempt from the Purok Clearance upload requirement by policy, not by a missing check — purokClearanceVerified is explicitly set to true at creation time, with purokClearanceVerifiedBy recording which staff member performed the walk-in, so the exemption itself stays auditable.

- Notification delivery retries: a retry re-attempts sending through Resend and updates deliveryStatus/deliveryAttempts/lastAttemptAt/failureReason; a cancel simply sets deliveryStatus = 'cancelled' and stops further attempts. Neither action deletes the row — the full attempt history stays visible to staff/admin.

- ipAddress and userAgent on auditLogs are captured from the Express request object (req.ip, req.headers\['user-agent'\]) at the point each sensitive action is logged — no separate lookup or third-party service is involved.

- SMS notifications are intentionally out of scope for this project (cost) — every notification path (including Purok-wide emergency broadcasts) uses email and in-app delivery only, never the needs/disasterType fields' real-world urgency as a reason to add SMS later without reopening that cost discussion.
