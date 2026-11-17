**CLASS AND OBJECT MODEL**

**BDMS Mobile**

**Java Domain Classes Aligned With Every Database Table**

*Prepared as a focused companion document to the BDMS Mobile project proposal.*

| **Document Focus**      | **Coverage**                                      |
|-------------------------|---------------------------------------------------|
| Object-Oriented Model   | One Java entity class for every database table    |
| Database Alignment      | Fields, keys, relationships, and responsibilities |
| Implementation Examples | Complete class definitions and runtime objects    |

## 1. Purpose and Scope

This document provides the complete class and object model for BDMS Mobile. Every database table described in the project proposal has a corresponding Java domain class. Each class lists its database mapping, important attributes, methods, relationships, and a complete example implementation.

The model separates persistent entity classes from process logic. The table-backed classes documented here represent stored barangay data. Service classes (ComplaintService, EmergencyResponseService, NotificationService) coordinate multi-step workflows, but they are not a replacement for any table-backed entity class.

The profiles table stores every account — resident, staff, and admin — differentiated by a role column. On the Java side, this is modeled with a base Profile class extended by Resident and StaffMember, so that role-specific behavior (what a resident may do versus what staff may do) is expressed through normal object-oriented inheritance rather than scattered if-role-equals checks.

## 2. Table to Class Coverage

| **Database Table**    | **Java Class**                                    | **Coverage** | **Main Responsibility**                             |
|-----------------------|---------------------------------------------------|--------------|-----------------------------------------------------|
| profiles              | Profile (base) Resident, StaffMember (subclasses) | Included     | Account identity, Purok, and role-based behavior    |
| document_requests     | DocumentRequest                                   | Included     | Certificate request lifecycle and control numbers   |
| complaints            | Complaint                                         | Included     | Resident feedback, SLA tracking, and escalation     |
| announcements         | Announcement                                      | Included     | Staff broadcasts, barangay-wide or Purok-targeted   |
| emergency_reports     | EmergencyReport                                   | Included     | Disaster reports tagged with the reporter's Purok   |
| relief_distributions  | ReliefDistribution                                | Included     | Relief-goods records tied to an emergency report    |
| notifications         | Notification                                      | Included     | In-app notification delivery and read state         |
| certificate_templates | CertificateTemplate                               | Included     | Seal, signature, and signing-official configuration |
| audit_logs            | AuditLog                                          | Included     | Sensitive administrative action history             |

## 3. Class Relationship Model

The diagram shows the table-backed domain classes and their main one-to-many relationships. Foreign-key fields in the dependent class preserve the corresponding database relationship. Resident and StaffMember are omitted from the diagram as separate boxes since both map to the same profiles table and are represented there as User/Profile.

*(diagram omitted in the Markdown version)*

*Figure 1. Class relationships aligned with the database model.*

## 4. Relationship Definitions

| **Source Class** | **Cardinality** | **Target Class**   | **Database Meaning**                                                  |
|------------------|-----------------|--------------------|-----------------------------------------------------------------------|
| Profile          | 1 to many       | DocumentRequest    | document_requests.userId references profiles.id                       |
| Profile          | 1 to many       | Complaint          | complaints.userId references profiles.id                              |
| Profile          | 1 to many       | Announcement       | announcements.postedBy references profiles.id                         |
| Profile          | 1 to many       | EmergencyReport    | emergency_reports.userId references profiles.id                       |
| Profile          | 1 to many       | ReliefDistribution | relief_distributions.residentId / distributedBy reference profiles.id |
| Profile          | 1 to many       | Notification       | notifications.userId references profiles.id                           |
| Profile          | 1 to many       | AuditLog           | audit_logs.performedBy references profiles.id                         |
| EmergencyReport  | 1 to many       | ReliefDistribution | relief_distributions.reportId references emergency_reports.reportId   |

## 5. Java Package Structure

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
  repository/
    ApiRepository.java        // Retrofit-based access layer for the Node.js/Express REST API
```

## 6. Table-Backed Java Classes

Each subsection below represents one database table. The attribute table shows the direct column-to-field mapping, followed by the important behaviors and the complete Java class.

### 6.1 Profile Class

**Database table: profiles**

Represents any authenticated barangay account. Resident and StaffMember extend this class to express role-specific behavior while sharing the same underlying table.

**Attribute Mapping**

| **Java Field** | **Java Type** | **Database Column** | **Meaning**                              |
|----------------|---------------|---------------------|------------------------------------------|
| id             | UUID          | id                  | Primary key, references auth.users       |
| fullName       | String        | fullName            | Resident or staff full name              |
| email          | String        | email               | Login/contact email                      |
| mobileNumber   | String        | mobileNumber        | Philippine mobile number                 |
| purok          | String        | purok               | Structured Purok/Zone, set at onboarding |
| role           | String        | role                | resident, staff, or admin                |
| active         | boolean       | isActive            | Whether the account may be used          |

**Important Methods**

| **Method**       | **Purpose**                                 |
|------------------|---------------------------------------------|
| getFullName()    | Returns the account's display name.         |
| isActive()       | Checks whether the account may transact.    |
| getPurok()       | Returns the resident's Purok for filtering. |
| isStaffOrAdmin() | Checks whether the role is staff or admin.  |

**Profile Java Source**

```java
package com.barangay.bdms.model;

public class Profile {
    protected UUID id;
    protected String fullName;
    protected String email;
    protected String mobileNumber;
    protected String purok;
    protected String role;
    protected boolean active;

    public Profile(UUID id, String fullName, String email,
            String mobileNumber, String purok, String role, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.purok = purok;
        this.role = role;
        this.active = active;
    }

    public String getFullName() { return fullName; }
    public boolean isActive() { return active; }
    public String getPurok() { return purok; }
    public UUID getId() { return id; }
    public boolean isStaffOrAdmin() {
        return "staff".equals(role) || "admin".equals(role);
    }
}
```

### 6.2 Resident Class

**Database table: profiles (role = "resident")**

Extends Profile with resident-facing actions. No additional columns are needed; Resident narrows what an object of this role is allowed to do.

**Important Methods**

| **Method**                 | **Purpose**                                            |
|----------------------------|--------------------------------------------------------|
| canSubmitComplaint()       | Allows an active resident to file a complaint.         |
| canSubmitEmergencyReport() | Allows an active resident to file an emergency report. |

**Resident Java Source**

```java
package com.barangay.bdms.model;

public class Resident extends Profile {
    private String address;

    public Resident(UUID id, String fullName, String email,
            String mobileNumber, String purok, String address, boolean active) {
        super(id, fullName, email, mobileNumber, purok, "resident", active);
        this.address = address;
    }

    public boolean canSubmitComplaint() { return isActive(); }
    public boolean canSubmitEmergencyReport() { return isActive(); }
    public String getAddress() { return address; }
}
```

### 6.3 StaffMember Class

**Database table: profiles (role = "staff" or "admin")**

Extends Profile with staff-facing actions, including the Purok-based emergency response workflow.

**Important Methods**

| **Method**           | **Purpose**                                                               |
|----------------------|---------------------------------------------------------------------------|
| canProcessRequests() | Allows an active staff/admin to process document requests and complaints. |
| canDispatchRelief()  | Allows an active staff/admin to dispatch emergency relief.                |
| isAdmin()            | Restricts admin-only actions such as template configuration.              |

**StaffMember Java Source**

```java
package com.barangay.bdms.model;

public class StaffMember extends Profile {
    private String position;

    public StaffMember(UUID id, String fullName, String email,
            String mobileNumber, String purok, String role,
            String position, boolean active) {
        super(id, fullName, email, mobileNumber, purok, role, active);
        this.position = position;
    }

    public boolean canProcessRequests() { return isActive(); }
    public boolean canDispatchRelief() { return isActive(); }
    public boolean isAdmin() { return "admin".equals(role); }
}
```

### 6.4 DocumentRequest Class

**Database table: document_requests**

Represents a resident's request for an official barangay certificate and tracks it from submission to completion.

**Attribute Mapping**

| **Java Field** | **Java Type**  | **Database Column** | **Meaning**                                             |
|----------------|----------------|---------------------|---------------------------------------------------------|
| requestId      | UUID           | requestId           | Primary key                                             |
| userId         | UUID           | userId              | Foreign key to Profile                                  |
| documentType   | String         | documentType        | Clearance, Indigency, Residency, ID, Business Clearance |
| controlNumber  | String         | controlNumber       | Unique generated control number                         |
| fee            | BigDecimal     | fee                 | Document fee (PHP)                                      |
| status         | String         | status              | pending, processing, completed, rejected                |
| createdAt      | OffsetDateTime | createdAt           | Submission timestamp                                    |

**Important Methods**

| **Method**              | **Purpose**                                                             |
|-------------------------|-------------------------------------------------------------------------|
| generateControlNumber() | Builds a unique control number from type, timestamp, and random suffix. |
| updateStatus(newStatus) | Advances the request through its lifecycle.                             |
| isPending()             | Checks whether the request is awaiting action.                          |

**DocumentRequest Java Source**

```java
package com.barangay.bdms.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DocumentRequest {
    private UUID requestId;
    private UUID userId;
    private String documentType;
    private String controlNumber;
    private String purpose;
    private String deliveryMethod;
    private BigDecimal fee;
    private String status;
    private OffsetDateTime createdAt;

    public DocumentRequest(UUID requestId, UUID userId, String documentType,
            String purpose, String deliveryMethod, BigDecimal fee) {
        this.requestId = requestId;
        this.userId = userId;
        this.documentType = documentType;
        this.purpose = purpose;
        this.deliveryMethod = deliveryMethod;
        this.fee = fee;
        this.status = "pending";
        this.createdAt = OffsetDateTime.now();
    }

    public String generateControlNumber() {
        String prefix = documentType.substring(0, Math.min(4, documentType.length())).toUpperCase();
        this.controlNumber = prefix + "-" + System.currentTimeMillis();
        return controlNumber;
    }

    public void updateStatus(String newStatus) { this.status = newStatus; }
    public boolean isPending() { return "pending".equals(status); }
    public UUID getRequestId() { return requestId; }
}
```

### 6.5 Complaint Class

**Database table: complaints**

Represents a resident's non-emergency concern and owns SLA-deadline and escalation behavior.

**Attribute Mapping**

| **Java Field** | **Java Type**  | **Database Column** | **Meaning**                             |
|----------------|----------------|---------------------|-----------------------------------------|
| complaintId    | UUID           | complaintId         | Primary key                             |
| userId         | UUID           | userId              | Foreign key to Profile (reporter)       |
| purok          | String         | purok               | Denormalized Purok for filtering        |
| status         | String         | status              | pending, reviewing, resolved, dismissed |
| priority       | String         | priority            | normal or high                          |
| slaDueAt       | OffsetDateTime | slaDueAt            | Escalation deadline                     |
| handledBy      | UUID           | handledBy           | Foreign key to Profile (staff)          |
| staffResponse  | String         | staffResponse       | Staff reply shown to the resident       |

**Important Methods**

| **Method**                 | **Purpose**                                                                  |
|----------------------------|------------------------------------------------------------------------------|
| isOverdue()                | Checks whether the current time has passed slaDueAt.                         |
| escalate()                 | Raises priority and records the escalation time.                             |
| resolve(response, staffId) | Marks the complaint resolved and stores the staff response in staffResponse. |

**Complaint Java Source**

```java
package com.barangay.bdms.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Complaint {
    private UUID complaintId;
    private UUID userId;
    private String purok;
    private String category;
    private String description;
    private String status;
    private String priority;
    private OffsetDateTime slaDueAt;
    private OffsetDateTime escalatedAt;
    private UUID handledBy;
    private String staffResponse;

    public Complaint(UUID complaintId, UUID userId, String purok,
            String category, String description, OffsetDateTime slaDueAt) {
        this.complaintId = complaintId;
        this.userId = userId;
        this.purok = purok;
        this.category = category;
        this.description = description;
        this.status = "pending";
        this.priority = "normal";
        this.slaDueAt = slaDueAt;
    }

    public boolean isOverdue() {
        return status.equals("pending") && OffsetDateTime.now().isAfter(slaDueAt);
    }

    public void escalate() {
        this.priority = "high";
        this.escalatedAt = OffsetDateTime.now();
    }

    public void resolve(String response, UUID staffId) {
        this.status = "resolved";
        this.handledBy = staffId;
        this.staffResponse = response;
    }

    public String getPurok() { return purok; }
    public String getPriority() { return priority; }
    public String getStaffResponse() { return staffResponse; }
}
```

### 6.6 Announcement Class

**Database table: announcements**

Represents a staff-issued broadcast, either barangay-wide or targeted to specific Puroks.

**Attribute Mapping**

| **Java Field** | **Java Type**  | **Database Column** | **Meaning**                          |
|----------------|----------------|---------------------|--------------------------------------|
| announcementId | UUID           | announcementId      | Primary key                          |
| title          | String         | title               | Announcement headline                |
| targetPurok    | List\<String\> | targetPurok         | Empty/null = barangay-wide           |
| postedBy       | UUID           | postedBy            | Foreign key to Profile (staff/admin) |
| expiresAt      | OffsetDateTime | expiresAt           | Optional expiry                      |

**Important Methods**

| **Method**          | **Purpose**                                                |
|---------------------|------------------------------------------------------------|
| isBarangayWide()    | Checks whether targetPurok is empty.                       |
| isTargetedTo(purok) | Checks whether a given Purok should see this announcement. |
| isExpired()         | Checks whether the announcement is past its expiry.        |

**Announcement Java Source**

```java
package com.barangay.bdms.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class Announcement {
    private UUID announcementId;
    private String title;
    private String body;
    private String category;
    private List<String> targetPurok;
    private UUID postedBy;
    private OffsetDateTime expiresAt;

    public Announcement(UUID announcementId, String title, String body,
            String category, List<String> targetPurok, UUID postedBy) {
        this.announcementId = announcementId;
        this.title = title;
        this.body = body;
        this.category = category;
        this.targetPurok = targetPurok;
        this.postedBy = postedBy;
    }

    public boolean isBarangayWide() {
        return targetPurok == null || targetPurok.isEmpty();
    }

    public boolean isTargetedTo(String purok) {
        return isBarangayWide() || targetPurok.contains(purok);
    }

    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }
}
```

### 6.7 EmergencyReport Class

**Database table: emergency_reports**

Represents a resident's disaster report, automatically tagged with the reporter's Purok, and drives the relief-dispatch workflow.

**Attribute Mapping**

| **Java Field** | **Java Type**  | **Database Column** | **Meaning**                                     |
|----------------|----------------|---------------------|-------------------------------------------------|
| reportId       | UUID           | reportId            | Primary key                                     |
| userId         | UUID           | userId              | Foreign key to Profile (reporter)               |
| purok          | String         | purok               | Copied from Profile at submission time          |
| disasterType   | String         | disasterType        | flood, fire, typhoon, earthquake, landslide     |
| needs          | List\<String\> | needs               | food, water, medicine, shelter, evacuation      |
| status         | String         | status              | reported, verified, relief_dispatched, resolved |

**Important Methods**

| **Method**             | **Purpose**                                              |
|------------------------|----------------------------------------------------------|
| verify()               | Marks a report as genuine and ready for relief planning. |
| markReliefDispatched() | Advances status once relief has been recorded.           |
| isOpen()               | Checks whether the report still needs action.            |

**EmergencyReport Java Source**

```java
package com.barangay.bdms.model;

import java.util.List;
import java.util.UUID;

public class EmergencyReport {
    private UUID reportId;
    private UUID userId;
    private String purok;
    private String disasterType;
    private int householdMembersAffected;
    private List<String> needs;
    private String status;

    public EmergencyReport(UUID reportId, UUID userId, String purok,
            String disasterType, int householdMembersAffected, List<String> needs) {
        this.reportId = reportId;
        this.userId = userId;
        this.purok = purok;
        this.disasterType = disasterType;
        this.householdMembersAffected = householdMembersAffected;
        this.needs = needs;
        this.status = "reported";
    }

    public void verify() { this.status = "verified"; }
    public void markReliefDispatched() { this.status = "relief_dispatched"; }
    public boolean isOpen() { return !status.equals("resolved"); }
    public String getPurok() { return purok; }
    public UUID getUserId() { return userId; }
    public UUID getReportId() { return reportId; }
}
```

### 6.8 ReliefDistribution Class

**Database table: relief_distributions**

Links one resident's received relief goods to an emergency report, giving staff an auditable, duplicate-safe distribution record.

**Attribute Mapping**

| **Java Field** | **Java Type** | **Database Column** | **Meaning**                             |
|----------------|---------------|---------------------|-----------------------------------------|
| distributionId | UUID          | distributionId      | Primary key                             |
| reportId       | UUID          | reportId            | Foreign key to EmergencyReport          |
| residentId     | UUID          | residentId          | Foreign key to Profile (recipient)      |
| itemsGiven     | String        | itemsGiven          | Description of relief goods distributed |
| distributedBy  | UUID          | distributedBy       | Foreign key to Profile (staff)          |

**Important Methods**

| **Method**             | **Purpose**                                   |
|------------------------|-----------------------------------------------|
| describeDistribution() | Builds a readable summary line for reporting. |

**ReliefDistribution Java Source**

```java
package com.barangay.bdms.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ReliefDistribution {
    private UUID distributionId;
    private UUID reportId;
    private UUID residentId;
    private String purok;
    private String itemsGiven;
    private UUID distributedBy;
    private OffsetDateTime createdAt;

    public ReliefDistribution(UUID distributionId, UUID reportId, UUID residentId,
            String purok, String itemsGiven, UUID distributedBy) {
        this.distributionId = distributionId;
        this.reportId = reportId;
        this.residentId = residentId;
        this.purok = purok;
        this.itemsGiven = itemsGiven;
        this.distributedBy = distributedBy;
        this.createdAt = OffsetDateTime.now();
    }

    public String describeDistribution() {
        return itemsGiven + " given to resident " + residentId + " (" + purok + ")";
    }

    public UUID getResidentId() { return residentId; }
}
```

### 6.9 Notification Class

**Database table: notifications**

Represents one in-app (and mirrored email) notification delivered to a resident or staff member.

**Attribute Mapping**

| **Java Field** | **Java Type** | **Database Column** | **Meaning**                                        |
|----------------|---------------|---------------------|----------------------------------------------------|
| notificationId | UUID          | notificationId      | Primary key                                        |
| userId         | UUID          | userId              | Foreign key to Profile (recipient)                 |
| type           | String        | type                | request_update, complaint, announcement, emergency |
| isRead         | boolean       | isRead              | Whether the recipient has read it                  |

**Important Methods**

| **Method**   | **Purpose**                                       |
|--------------|---------------------------------------------------|
| markAsRead() | Flips isRead to true when the recipient opens it. |

**Notification Java Source**

```java
package com.barangay.bdms.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Notification {
    private UUID notificationId;
    private UUID userId;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private OffsetDateTime createdAt;

    public Notification(UUID notificationId, UUID userId, String title,
            String message, String type) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
        this.createdAt = OffsetDateTime.now();
    }

    public void markAsRead() { this.isRead = true; }
    public UUID getUserId() { return userId; }
}
```

### 6.10 CertificateTemplate Class

**Database table: certificate_templates**

Stores the barangay seal, official signature, and signing-official details used when generating certificate PDFs.

**Attribute Mapping**

| **Java Field** | **Java Type** | **Database Column** | **Meaning**                                    |
|----------------|---------------|---------------------|------------------------------------------------|
| templateId     | UUID          | templateId          | Primary key                                    |
| documentType   | String        | documentType        | Certificate type this configuration applies to |
| sealUrl        | String        | sealUrl             | Barangay seal image URL                        |
| officialName   | String        | officialName        | Signing official's name                        |

**Important Methods**

| **Method**     | **Purpose**                                     |
|----------------|-------------------------------------------------|
| isConfigured() | Checks whether seal and signature are both set. |

**CertificateTemplate Java Source**

```java
package com.barangay.bdms.model;

import java.util.UUID;

public class CertificateTemplate {
    private UUID templateId;
    private String documentType;
    private String sealUrl;
    private String signatureUrl;
    private String officialName;
    private String officialTitle;

    public CertificateTemplate(UUID templateId, String documentType,
            String sealUrl, String signatureUrl,
            String officialName, String officialTitle) {
        this.templateId = templateId;
        this.documentType = documentType;
        this.sealUrl = sealUrl;
        this.signatureUrl = signatureUrl;
        this.officialName = officialName;
        this.officialTitle = officialTitle;
    }

    public boolean isConfigured() {
        return sealUrl != null && signatureUrl != null;
    }
}
```

### 6.11 AuditLog Class

**Database table: audit_logs**

Records who performed a sensitive action, what record changed, and when — required for RA 10173 compliance.

**Attribute Mapping**

| **Java Field** | **Java Type** | **Database Column** | **Meaning**                            |
|----------------|---------------|---------------------|----------------------------------------|
| auditLogId     | UUID          | auditLogId          | Primary key                            |
| performedBy    | UUID          | performedBy         | Foreign key to Profile (actor)         |
| action         | String        | action              | e.g., STATUS_CHANGE, RELIEF_DISPATCHED |
| targetId       | UUID          | targetId            | Affected record identifier             |

**Important Methods**

| **Method**       | **Purpose**                           |
|------------------|---------------------------------------|
| describeAction() | Builds a readable action description. |

**AuditLog Java Source**

```java
package com.barangay.bdms.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AuditLog {
    private UUID auditLogId;
    private UUID performedBy;
    private String action;
    private UUID targetId;
    private String details;
    private OffsetDateTime createdAt;

    public AuditLog(UUID auditLogId, UUID performedBy, String action,
            UUID targetId, String details) {
        this.auditLogId = auditLogId;
        this.performedBy = performedBy;
        this.action = action;
        this.targetId = targetId;
        this.details = details;
        this.createdAt = OffsetDateTime.now();
    }

    public String describeAction() {
        return action + " on " + targetId + " by " + performedBy;
    }
}
```

## 7. Runtime Object Examples

A class is the blueprint. An object is a specific runtime instance created from that class. The example below creates an object for the key table-backed classes involved in the Purok-based emergency relief workflow.

```
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

Resident resident = new Resident(
    UUID.randomUUID(), "Juan Dela Cruz", "juan@example.com",
    "+639123456789", "Purok 3", "123 Mabini St.", true);

StaffMember staff = new StaffMember(
    UUID.randomUUID(), "Maria Santos", "maria@example.com",
    "+639987654321", null, "staff", "Barangay Staff", true);

EmergencyReport report = new EmergencyReport(
    UUID.randomUUID(), resident.getId(), resident.getPurok(),
    "Flood", 5, List.of("Food", "Water", "Medicine"));

report.verify();

ReliefDistribution distribution = new ReliefDistribution(
    UUID.randomUUID(), report.getReportId(), resident.getId(),
    resident.getPurok(), "Rice, canned goods, bottled water", staff.getId());

Notification notification = new Notification(
    UUID.randomUUID(), resident.getId(), "Relief Goods Available",
    "Relief goods are ready for pickup at the Barangay Hall.", "emergency");

AuditLog auditLog = new AuditLog(
    UUID.randomUUID(), staff.getId(), "RELIEF_DISPATCHED",
    report.getReportId(), "Relief dispatched to Purok 3");
```

## 8. Object Interaction Example

The following sequence shows how the table-backed objects cooperate during a Purok relief dispatch. Persistence code should save the ReliefDistribution, updated EmergencyReport, Notification, and AuditLog in one database transaction.

```
if (!resident.isActive()) {
    throw new IllegalStateException("Resident account is inactive.");
}
if (!staff.canDispatchRelief()) {
    throw new SecurityException("Staff member cannot dispatch relief.");
}
if (!report.isOpen()) {
    throw new IllegalStateException("Report is already resolved.");
}

// duplicate-dispatch check happens against a HashSet<UUID> of already-
// served resident IDs maintained by EmergencyResponseService
if (alreadyServed.contains(resident.getId())) {
    throw new IllegalStateException("Resident already received relief for this report.");
}

report.markReliefDispatched();
alreadyServed.add(resident.getId());

// Save distribution, updated report, notification, and audit log atomically.
```

## 9. Implementation Notes

- Keep one public Java class in each file and use the same class name as the filename.

- Use java.util.UUID for all primary and foreign keys, matching PostgreSQL's UUID columns.

- Use java.time.OffsetDateTime for all timestamp fields, matching PostgreSQL's TIMESTAMPTZ columns.

- Use List\<String\> to represent Postgres text\[\] array columns such as needs and targetPurok.

- Use BigDecimal for the document-request fee field; do not use float or double for monetary values.

- Access the backend through a repository/DAO layer built on Retrofit, calling the Node.js/Express REST API; table-backed classes should not contain networking code.

- Keep relief-distribution creation, report status update, notification creation, and audit logging in one database transaction.

- Do not hard-delete audit logs, complaints, or emergency reports. Use status changes to reflect resolution.

- Enforce role checks (isStaffOrAdmin(), canDispatchRelief(), isAdmin()) in the Java layer in addition to the Node.js/Express authorization middleware, so both layers agree on who may act.

- Group EmergencyReport objects by Purok using a HashMap\<String, List\<EmergencyReport\>\> in EmergencyResponseService rather than filtering the full list on every request.
