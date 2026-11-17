package com.barangay.bdms.repository;

import android.content.Context;

import androidx.annotation.Nullable;

import com.barangay.bdms.model.*;
import com.barangay.bdms.network.ApiClient;
import com.barangay.bdms.network.ApiService;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;

import java.util.List;
import java.util.Map;

public class BdmsRepository {
    private final ApiService apiService;

    public BdmsRepository() {
        this.apiService = ApiClient.getApiService();
    }

    public BdmsRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    // === MEMBER 1 ===

    public Call<Map<String, Object>> login(Map<String, String> body) {
        return apiService.login(body);
    }

    public Call<Map<String, Object>> register(Map<String, Object> body) {
        return apiService.register(body);
    }

    public Call<Map<String, Object>> sendOtp(Map<String, String> body) {
        return apiService.sendOtp(body);
    }

    public Call<Map<String, Object>> verifyOtp(Map<String, String> body) {
        return apiService.verifyOtp(body);
    }

    public Call<Profile> getMyProfile() {
        return apiService.getMyProfile();
    }

    public Call<Profile> updateMyProfile(Map<String, Object> body) {
        return apiService.updateMyProfile(body);
    }

    public Call<Profile> uploadAvatar(MultipartBody.Part avatar) {
        return apiService.uploadAvatar(avatar);
    }

    public Call<List<Purok>> getPuroks() {
        return apiService.getPuroks();
    }

    public Call<List<DocumentTypeInfo>> getDocumentTypes() {
        return apiService.getDocumentTypes();
    }

    public Call<List<DocumentRequest>> getDocumentRequests(@Nullable String status) {
        return apiService.getDocumentRequests(status);
    }

    public Call<DocumentRequest> getDocumentRequestDetail(String id) {
        return apiService.getDocumentRequestDetail(id);
    }

    public Call<DocumentRequest> createDocumentRequest(
            RequestBody type,
            RequestBody purpose,
            RequestBody delivery,
            @Nullable RequestBody businessName,
            @Nullable RequestBody businessAddress,
            @Nullable RequestBody businessType,
            MultipartBody.Part clearance
    ) {
        return apiService.createDocumentRequest(
                type,
                purpose,
                delivery,
                businessName,
                businessAddress,
                businessType,
                clearance
        );
    }

    public Call<ResponseBody> downloadCertificate(String id) {
        return apiService.downloadCertificate(id);
    }

    public Call<List<Complaint>> getComplaints() {
        return apiService.getComplaints();
    }

    public Call<Complaint> getComplaintDetail(String id) {
        return apiService.getComplaintDetail(id);
    }

    public Call<Complaint> createComplaint(
            RequestBody category,
            RequestBody description,
            @Nullable List<MultipartBody.Part> photos
    ) {
        return apiService.createComplaint(category, description, photos);
    }

    public Call<List<Announcement>> getAnnouncements() {
        return apiService.getAnnouncements();
    }

    public Call<List<EmergencyReport>> getEmergencyReports() {
        return apiService.getEmergencyReports();
    }

    public Call<EmergencyReport> getEmergencyReportDetail(String id) {
        return apiService.getEmergencyReportDetail(id);
    }

    public Call<EmergencyReport> createEmergencyReportMultipart(
            RequestBody disasterType,
            RequestBody householdMembersAffected,
            RequestBody needsJson,
            @Nullable RequestBody confirmDuplicate,
            @Nullable MultipartBody.Part photo
    ) {
        return apiService.createEmergencyReportMultipart(
                disasterType,
                householdMembersAffected,
                needsJson,
                confirmDuplicate,
                photo
        );
    }

    public Call<EmergencyReport> createEmergencyReport(Map<String, Object> body) {
        return apiService.createEmergencyReport(body);
    }

    public Call<List<Notification>> getMyNotifications() {
        return apiService.getMyNotifications();
    }

    public Call<Notification> markNotificationRead(String id) {
        return apiService.markNotificationRead(id);
    }
}
