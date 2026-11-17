package com.barangay.bdms.network;

import com.barangay.bdms.model.*;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface ApiService {

    // === MEMBER 1 (Auth, Profile & Resident Endpoints) ===

    @POST("auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> body);

    @POST("auth/register")
    Call<Map<String, Object>> register(@Body Map<String, Object> body);

    @POST("auth/send-otp")
    Call<Map<String, Object>> sendOtp(@Body Map<String, String> body);

    @POST("auth/verify-otp")
    Call<Map<String, Object>> verifyOtp(@Body Map<String, String> body);

    @GET("profiles/me")
    Call<Profile> getMyProfile();

    @PATCH("profiles/me")
    Call<Profile> updateMyProfile(@Body Map<String, Object> body);

    @Multipart
    @POST("profiles/me/avatar")
    Call<Profile> uploadAvatar(@Part MultipartBody.Part avatar);

    @GET("puroks")
    Call<List<Purok>> getPuroks();

    @GET("document-types")
    Call<List<DocumentTypeInfo>> getDocumentTypes();

    @GET("document-requests")
    Call<List<DocumentRequest>> getDocumentRequests(@Query("status") String status);

    @GET("document-requests/{id}")
    Call<DocumentRequest> getDocumentRequestDetail(@Path("id") String id);

    @Multipart
    @POST("document-requests")
    Call<DocumentRequest> createDocumentRequest(
            @Part("documentType") RequestBody documentType,
            @Part("purpose") RequestBody purpose,
            @Part("deliveryMethod") RequestBody deliveryMethod,
            @Part("businessName") RequestBody businessName,
            @Part("businessAddress") RequestBody businessAddress,
            @Part("businessType") RequestBody businessType,
            @Part MultipartBody.Part purokClearance
    );

    @Streaming
    @GET("document-requests/{id}/certificate")
    Call<ResponseBody> downloadCertificate(@Path("id") String id);

    @GET("complaints")
    Call<List<Complaint>> getComplaints();

    @GET("complaints/{id}")
    Call<Complaint> getComplaintDetail(@Path("id") String id);

    @Multipart
    @POST("complaints")
    Call<Complaint> createComplaint(
            @Part("category") RequestBody category,
            @Part("description") RequestBody description,
            @Part List<MultipartBody.Part> photos
    );

    @GET("announcements")
    Call<List<Announcement>> getAnnouncements();

    @GET("emergency-reports")
    Call<List<EmergencyReport>> getEmergencyReports();

    @GET("emergency-reports/{id}")
    Call<EmergencyReport> getEmergencyReportDetail(@Path("id") String id);

    @Multipart
    @POST("emergency-reports")
    Call<EmergencyReport> createEmergencyReportMultipart(
            @Part("disasterType") RequestBody disasterType,
            @Part("householdMembersAffected") RequestBody householdMembersAffected,
            @Part("needs") RequestBody needsJson,
            @Part("confirmDuplicate") RequestBody confirmDuplicate,
            @Part MultipartBody.Part photo
    );

    @GET("notifications/me")
    Call<List<Notification>> getMyNotifications();

    @PATCH("notifications/{id}/read")
    Call<Notification> markNotificationRead(@Path("id") String id);

    // === MEMBER 2 & 3 (Preserved Staff / Admin Endpoints) ===

    @POST("puroks")
    Call<Purok> createPurok(@Body Map<String, String> body);

    @PATCH("document-requests/{id}")
    Call<DocumentRequest> updateDocumentRequestStatus(@Path("id") String id, @Body Map<String, Object> body);

    @PATCH("document-requests/{id}/verify-clearance")
    Call<DocumentRequest> verifyClearance(@Path("id") String id, @Body Map<String, Object> body);

    @PATCH("complaints/{id}/resolve")
    Call<Complaint> resolveComplaint(@Path("id") String id, @Body Map<String, Object> body);

    @POST("announcements")
    Call<Announcement> createAnnouncement(@Body Map<String, Object> body);

    @GET("emergency-reports/grouped-by-purok")
    Call<Map<String, List<EmergencyReport>>> getEmergencyReportsGroupedByPurok();

    @POST("emergency-reports")
    Call<EmergencyReport> createEmergencyReport(@Body Map<String, Object> body);

    @PATCH("emergency-reports/{id}/verify")
    Call<EmergencyReport> verifyEmergencyReport(@Path("id") String id);

    @GET("relief-distributions")
    Call<List<ReliefDistribution>> getReliefDistributions(@Query("reportId") String reportId);

    @POST("relief-distributions")
    Call<ReliefDistribution> dispatchRelief(@Body Map<String, Object> body);

    @GET("notifications")
    Call<List<Notification>> getAllNotifications(@Query("deliveryStatus") String deliveryStatus);

    @POST("notifications/{id}/retry")
    Call<Notification> retryNotification(@Path("id") String id);

    @POST("notifications/{id}/cancel")
    Call<Notification> cancelNotification(@Path("id") String id);

    @GET("certificate-templates/{documentType}")
    Call<CertificateTemplate> getCertificateTemplate(@Path("documentType") String documentType);

    @PUT("certificate-templates/{documentType}")
    Call<CertificateTemplate> updateCertificateTemplate(@Path("documentType") String documentType, @Body Map<String, Object> body);

    @GET("audit-logs")
    Call<List<AuditLog>> getAuditLogs();

    @GET("admin/users")
    Call<List<Profile>> searchAdminUsers(@Query("q") String query);

    @POST("admin/users")
    Call<Profile> createAdminUser(@Body Map<String, Object> body);

    @PATCH("admin/users/{id}/role")
    Call<Profile> updateUserRole(@Path("id") String id, @Body Map<String, String> body);

    @PATCH("admin/users/{id}/active")
    Call<Profile> updateUserActiveStatus(@Path("id") String id, @Body Map<String, Boolean> body);
}
