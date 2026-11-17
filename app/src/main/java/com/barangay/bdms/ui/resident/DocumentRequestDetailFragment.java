package com.barangay.bdms.ui.resident;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.barangay.bdms.R;
import com.barangay.bdms.model.DocumentRequest;
import com.barangay.bdms.repository.BdmsRepository;
import com.barangay.bdms.ui.common.ApiErrorUtils;
import com.barangay.bdms.ui.common.DateFormatUtils;
import com.barangay.bdms.ui.common.LoadingOverlay;
import com.barangay.bdms.ui.common.NavUtils;
import com.barangay.bdms.ui.common.StatusBadge;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentRequestDetailFragment extends Fragment {

    private BdmsRepository repository;
    private SwipeRefreshLayout swipeRefresh;
    private LoadingOverlay loadingOverlay;

    private LinearLayout layoutRejectionBanner, layoutDetailBusiness;
    private TextView tvRejectionReason, tvDetailDocumentType, tvDetailControlNumber;
    private TextView tvDetailPurpose, tvDetailDelivery, tvDetailFee, tvDetailBusinessInfo;
    private TextView tvClearanceChip, tvPaymentChip;
    private TextView tvTimelineStep1, tvTimelineStep2, tvTimelineStep3, tvTimelineStep4;
    private StatusBadge badgeDetailStatus;
    private MaterialButton btnCopyTracking, btnDownloadCert;

    @Nullable
    private String requestId;
    @Nullable
    private DocumentRequest currentRequest;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_resident_r6_document_request_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new BdmsRepository(requireContext());
        requestId = getArguments() != null ? getArguments().getString("requestId") : null;

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        swipeRefresh = view.findViewById(R.id.swipeRefreshDetail);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        layoutRejectionBanner = view.findViewById(R.id.layoutRejectionBanner);
        layoutDetailBusiness = view.findViewById(R.id.layoutDetailBusiness);
        tvRejectionReason = view.findViewById(R.id.tvRejectionReason);
        tvDetailDocumentType = view.findViewById(R.id.tvDetailDocumentType);
        tvDetailControlNumber = view.findViewById(R.id.tvDetailControlNumber);
        tvDetailPurpose = view.findViewById(R.id.tvDetailPurpose);
        tvDetailDelivery = view.findViewById(R.id.tvDetailDelivery);
        tvDetailFee = view.findViewById(R.id.tvDetailFee);
        tvDetailBusinessInfo = view.findViewById(R.id.tvDetailBusinessInfo);
        tvClearanceChip = view.findViewById(R.id.tvClearanceChip);
        tvPaymentChip = view.findViewById(R.id.tvPaymentChip);
        tvTimelineStep1 = view.findViewById(R.id.tvTimelineStep1);
        tvTimelineStep2 = view.findViewById(R.id.tvTimelineStep2);
        tvTimelineStep3 = view.findViewById(R.id.tvTimelineStep3);
        tvTimelineStep4 = view.findViewById(R.id.tvTimelineStep4);
        badgeDetailStatus = view.findViewById(R.id.badgeDetailStatus);
        btnCopyTracking = view.findViewById(R.id.btnCopyTracking);
        btnDownloadCert = view.findViewById(R.id.btnDownloadCert);

        btnBack.setOnClickListener(v -> NavUtils.safeNavigateUp(this));

        btnCopyTracking.setOnClickListener(v -> {
            if (currentRequest == null || currentRequest.getControlNumber() == null) return;
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Control Number", currentRequest.getControlNumber()));
                Toast.makeText(requireContext(), "Control number copied!", Toast.LENGTH_SHORT).show();
            }
        });

        btnDownloadCert.setOnClickListener(v -> downloadCertificatePdf());

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(this::fetchDetail);

        if (requestId == null || requestId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing document request ID.", Toast.LENGTH_SHORT).show();
            NavUtils.safeNavigateUp(this);
            return;
        }
        fetchDetail();
    }

    private void fetchDetail() {
        if (requestId == null) return;
        if (!swipeRefresh.isRefreshing()) {
            loadingOverlay.show();
        }
        repository.getDocumentRequestDetail(requestId).enqueue(new Callback<DocumentRequest>() {
            @Override
            public void onResponse(@NonNull Call<DocumentRequest> call, @NonNull Response<DocumentRequest> response) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    currentRequest = response.body();
                    bindRequest(currentRequest);
                } else {
                    Toast.makeText(
                            requireContext(),
                            ApiErrorUtils.parseErrorMessage(response, "Unable to load request detail."),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DocumentRequest> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindRequest(@NonNull DocumentRequest req) {
        tvDetailDocumentType.setText(req.getDocumentType() != null ? req.getDocumentType() : "Document Request");
        badgeDetailStatus.setStatus(req.getStatus());
        tvDetailControlNumber.setText(req.getControlNumber() != null ? req.getControlNumber() : "Pending");
        tvDetailPurpose.setText(req.getPurpose() != null ? req.getPurpose() : "—");
        tvDetailDelivery.setText(req.getDeliveryMethod() != null
                ? req.getDeliveryMethod().toUpperCase(Locale.US) : "PICKUP");

        BigDecimal fee = req.getFee() != null ? req.getFee() : BigDecimal.ZERO;
        tvDetailFee.setText(String.format(Locale.US, "PHP %.2f", fee.doubleValue()));

        if (req.getBusinessName() != null && !req.getBusinessName().isEmpty()) {
            layoutDetailBusiness.setVisibility(View.VISIBLE);
            tvDetailBusinessInfo.setText(
                    req.getBusinessName()
                            + "\n" + (req.getBusinessType() != null ? req.getBusinessType() : "")
                            + "\n" + (req.getBusinessAddress() != null ? req.getBusinessAddress() : "")
            );
        } else {
            layoutDetailBusiness.setVisibility(View.GONE);
        }

        if ("rejected".equalsIgnoreCase(req.getStatus())) {
            layoutRejectionBanner.setVisibility(View.VISIBLE);
            tvRejectionReason.setText(req.getRejectionReason() != null && !req.getRejectionReason().isEmpty()
                    ? req.getRejectionReason()
                    : "Rejected during barangay review.");
        } else {
            layoutRejectionBanner.setVisibility(View.GONE);
        }

        bindClearanceAndPaymentChips(req);
        bindTimeline(req);

        String st = req.getStatus() != null ? req.getStatus().toLowerCase(Locale.US) : "";
        boolean canDownload = ("ready".equals(st) || "completed".equals(st))
                && req.getCertificateUrl() != null && !req.getCertificateUrl().isEmpty();
        btnDownloadCert.setEnabled(canDownload);
    }

    private void bindClearanceAndPaymentChips(@NonNull DocumentRequest req) {
        if (req.isWalkIn()) {
            styleChip(tvClearanceChip, "Clearance: Walk-In Exempt", R.color.statusCompletedBg, R.color.statusCompletedText);
        } else if (req.isPurokClearanceVerified()) {
            styleChip(tvClearanceChip, "Clearance: Verified", R.color.statusCompletedBg, R.color.statusCompletedText);
        } else if ("rejected".equalsIgnoreCase(req.getStatus())) {
            styleChip(tvClearanceChip, "Clearance: Rejected", R.color.statusRejectedBg, R.color.statusRejectedText);
        } else {
            styleChip(tvClearanceChip, "Clearance: Pending", R.color.statusPendingBg, R.color.statusPendingText);
        }

        boolean paid = "confirmed".equalsIgnoreCase(req.getPaymentStatus());
        if (paid) {
            String orText = req.getPaymentOrNumber() != null && !req.getPaymentOrNumber().isEmpty()
                    ? " (" + req.getPaymentOrNumber() + ")" : "";
            styleChip(tvPaymentChip, "Payment: Confirmed" + orText, R.color.statusCompletedBg, R.color.statusCompletedText);
        } else {
            styleChip(tvPaymentChip, "Payment: Pending", R.color.statusPendingBg, R.color.statusPendingText);
        }
    }

    private void styleChip(TextView tv, String label, int bgRes, int textRes) {
        tv.setText(label);
        ViewCompat.setBackgroundTintList(tv, ContextCompat.getColorStateList(requireContext(), bgRes));
        tv.setTextColor(ContextCompat.getColor(requireContext(), textRes));
    }

    private void bindTimeline(@NonNull DocumentRequest req) {
        String st = req.getStatus() != null ? req.getStatus().toLowerCase(Locale.US) : "pending";
        int activeColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark);
        int mutedColor = ContextCompat.getColor(requireContext(), R.color.colorTextSecondary);
        int rejectedColor = ContextCompat.getColor(requireContext(), R.color.statusRejectedText);

        tvTimelineStep1.setText("1. Submitted — " + DateFormatUtils.formatDateTime(req.getCreatedAt()));
        tvTimelineStep1.setTextColor(activeColor);

        if ("rejected".equals(st)) {
            tvTimelineStep2.setText("2. Rejected — See rejection reason above");
            tvTimelineStep2.setTextColor(rejectedColor);
            tvTimelineStep3.setText("3. Ready — Not reached");
            tvTimelineStep3.setTextColor(mutedColor);
            tvTimelineStep4.setText("4. Completed — Not reached");
            tvTimelineStep4.setTextColor(mutedColor);
            return;
        }

        boolean step2Done = req.isPurokClearanceVerified()
                || "processing".equals(st) || "ready".equals(st) || "completed".equals(st);
        boolean step3Done = "ready".equals(st) || "completed".equals(st);
        boolean step4Done = "completed".equals(st) || req.getCompletedAt() != null;

        tvTimelineStep2.setText(step2Done
                ? "2. Clearance Verified & Processing"
                : "2. Clearance Verification Pending");
        tvTimelineStep2.setTextColor(step2Done ? activeColor : mutedColor);

        tvTimelineStep3.setText(step3Done
                ? "3. Ready for Pickup / Delivery"
                : "3. Ready (Awaiting generation)");
        tvTimelineStep3.setTextColor(step3Done ? activeColor : mutedColor);

        tvTimelineStep4.setText(step4Done
                ? (req.getCompletedAt() != null
                    ? "4. Completed — " + DateFormatUtils.formatDateTime(req.getCompletedAt())
                    : "4. Completed")
                : "4. Completed (Pending handover)");
        tvTimelineStep4.setTextColor(step4Done ? activeColor : mutedColor);
    }

    private void downloadCertificatePdf() {
        if (requestId == null || !isAdded()) return;
        Context appContext = requireContext().getApplicationContext();
        loadingOverlay.show();
        btnDownloadCert.setEnabled(false);

        repository.downloadCertificate(requestId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    ResponseBody body = response.body();
                    ioExecutor.execute(() -> writeAndOpenPdf(appContext, body));
                } else {
                    loadingOverlay.hide();
                    btnDownloadCert.setEnabled(true);
                    Toast.makeText(
                            requireContext(),
                            ApiErrorUtils.parseErrorMessage(response, "Certificate PDF not ready."),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingOverlay.hide();
                btnDownloadCert.setEnabled(true);
                Toast.makeText(requireContext(), "Download failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void writeAndOpenPdf(@NonNull Context appContext, @NonNull ResponseBody body) {
        try {
            File certDir = new File(appContext.getCacheDir(), "certificates");
            if (!certDir.exists()) certDir.mkdirs();

            String name = currentRequest != null && currentRequest.getControlNumber() != null
                    ? currentRequest.getControlNumber().replaceAll("[^A-Za-z0-9_-]", "_")
                    : "certificate";
            File outFile = new File(certDir, name + ".pdf");

            try (InputStream is = body.byteStream();
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
            }

            mainHandler.post(() -> {
                if (!isAdded()) return;
                loadingOverlay.hide();
                btnDownloadCert.setEnabled(true);
                openPdfFile(outFile);
            });
        } catch (Exception e) {
            mainHandler.post(() -> {
                if (!isAdded()) return;
                loadingOverlay.hide();
                btnDownloadCert.setEnabled(true);
                Toast.makeText(requireContext(), "Error saving certificate PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    private void openPdfFile(@NonNull File pdfFile) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                pdfFile
        );
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "Open Certificate PDF"));
        } catch (Exception e) {
            Toast.makeText(
                    requireContext(),
                    "Certificate saved to " + pdfFile.getName() + ". Install a PDF viewer to open it.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onDestroy() {
        ioExecutor.shutdownNow();
        super.onDestroy();
    }
}
