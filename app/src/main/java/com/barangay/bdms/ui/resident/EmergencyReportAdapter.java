package com.barangay.bdms.ui.resident;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.barangay.bdms.R;
import com.barangay.bdms.model.EmergencyReport;
import com.barangay.bdms.network.AuthenticatedImageLoader;
import com.barangay.bdms.ui.common.DateFormatUtils;
import com.barangay.bdms.ui.common.StatusBadge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EmergencyReportAdapter extends RecyclerView.Adapter<EmergencyReportAdapter.ViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(@NonNull EmergencyReport report);
    }

    private final List<EmergencyReport> items = new ArrayList<>();
    private final OnReportClickListener listener;

    public EmergencyReportAdapter(@NonNull OnReportClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<EmergencyReport> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyReport report = items.get(position);
        Context ctx = holder.itemView.getContext();
        String type = report.getDisasterType() != null
                ? report.getDisasterType().toUpperCase(Locale.US)
                : "EMERGENCY";
        holder.tvDisasterType.setText(type);
        holder.badgeStatus.setStatus(report.getStatus());
        holder.tvPurok.setText("Purok: " + (report.getPurok() != null ? report.getPurok() : "—"));

        String needsSummary = report.getNeeds() != null && !report.getNeeds().isEmpty()
                ? TextUtils.join(", ", report.getNeeds())
                : "None specified";
        holder.tvDetails.setText(
                "Household Affected: " + report.getHouseholdMembersAffected()
                        + "\nNeeds: " + needsSummary
        );
        holder.tvDate.setText(DateFormatUtils.formatDateTime(report.getCreatedAt()));

        String photoUrl = report.getPhotoUrl();
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            holder.ivPhoto.setVisibility(View.VISIBLE);
            AuthenticatedImageLoader.load(ctx, photoUrl.trim(), holder.ivPhoto, R.drawable.ic_bdms_logo, 640);
        } else {
            holder.ivPhoto.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onReportClick(report));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDisasterType, tvPurok, tvDetails, tvDate;
        final StatusBadge badgeStatus;
        final ImageView ivPhoto;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDisasterType = itemView.findViewById(R.id.tvDisasterType);
            badgeStatus = itemView.findViewById(R.id.badgeEmergencyStatus);
            tvPurok = itemView.findViewById(R.id.tvEmergencyPurok);
            tvDetails = itemView.findViewById(R.id.tvEmergencyDetails);
            tvDate = itemView.findViewById(R.id.tvEmergencyDate);
            ivPhoto = itemView.findViewById(R.id.ivEmergencyPhoto);
        }
    }
}
