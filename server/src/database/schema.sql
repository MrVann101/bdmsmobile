-- BDMS Mobile PostgreSQL Schema (Complete Revision)

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

-- Indexes
create index if not exists idx_profiles_purok on profiles(purok);
create index if not exists idx_complaints_purok on complaints(purok);
create index if not exists idx_complaints_status on complaints(status);
create index if not exists idx_emergencyreports_purok on "emergencyReports"(purok);
create index if not exists idx_emergencyreports_status on "emergencyReports"(status);
create index if not exists idx_documentrequests_status on "documentRequests"(status);
create index if not exists idx_documentrequests_user on "documentRequests"("userId");
create index if not exists idx_notifications_user_unread on notifications("userId", "isRead");
create index if not exists idx_otpcodes_email on "otpCodes"(email);
create index if not exists idx_documentrequests_clearance on "documentRequests"("purokClearanceVerified");
create index if not exists idx_notifications_delivery_status on notifications("deliveryStatus");
