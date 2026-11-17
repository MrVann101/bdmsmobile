package com.barangay.bdms.ui.resident;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.barangay.bdms.R;
import com.barangay.bdms.model.Complaint;
import com.barangay.bdms.network.AuthenticatedImageLoader;
import com.barangay.bdms.ui.common.DateFormatUtils;
import com.barangay.bdms.ui.common.NavUtils;
import com.barangay.bdms.ui.common.StatusBadge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.ViewHolder> {

    private final List<Complaint> items = new ArrayList<>();
    private final Set<UUID> expandedIds = new HashSet<>();

    public void submitList(@NonNull List<Complaint> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_complaint, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Complaint c = items.get(position);
        Context ctx = holder.itemView.getContext();
        String cat = c.getCategory() != null ? c.getCategory().toUpperCase(Locale.US) : "OTHER";
        holder.tvCategory.setText(cat);
        holder.badgeStatus.setStatus(c.getStatus());

        // Overdue tag renders strictly via Complaint.isOverdue()
        holder.tvOverdueTag.setVisibility(c.isOverdue() ? View.VISIBLE : View.GONE);

        holder.tvDescription.setText(c.getDescription() != null ? c.getDescription() : "");
        holder.tvDate.setText(DateFormatUtils.formatDateTime(c.getCreatedAt()));

        boolean isExpanded = c.getComplaintId() != null && expandedIds.contains(c.getComplaintId());
        holder.layoutExpanded.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.tvDescription.setMaxLines(isExpanded ? Integer.MAX_VALUE : 2);
        holder.tvDescription.setEllipsize(isExpanded ? null : TextUtils.TruncateAt.END);
        holder.tvExpandHint.setText(isExpanded ? "Hide details" : "Tap to view response");

        if (isExpanded) {
            holder.tvSlaDue.setText("SLA Target: " + DateFormatUtils.formatDateTime(c.getSlaDueAt()));
            String reply = c.getStaffResponse();
            holder.tvStaffResponse.setText(reply != null && !reply.trim().isEmpty()
                    ? reply.trim()
                    : "Awaiting staff review.");
            bindPhotos(ctx, holder, c.getPhotoUrls());
        } else {
            holder.layoutPhotos.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (c.getComplaintId() == null) return;
            if (expandedIds.contains(c.getComplaintId())) {
                expandedIds.remove(c.getComplaintId());
            } else {
                expandedIds.add(c.getComplaintId());
            }
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION) {
                notifyItemChanged(adapterPos);
            }
        });
    }

    private void bindPhotos(@NonNull Context ctx, @NonNull ViewHolder holder, List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            holder.layoutPhotos.setVisibility(View.GONE);
            return;
        }
        holder.layoutPhotos.setVisibility(View.VISIBLE);
        ImageView[] slots = {holder.ivPhoto1, holder.ivPhoto2, holder.ivPhoto3};
        for (int i = 0; i < slots.length; i++) {
            ImageView iv = slots[i];
            if (i < photoUrls.size() && photoUrls.get(i) != null && !photoUrls.get(i).trim().isEmpty()) {
                String path = photoUrls.get(i).trim();
                iv.setVisibility(View.VISIBLE);
                AuthenticatedImageLoader.load(ctx, path, iv, R.drawable.ic_bdms_logo, 320);
                iv.setOnClickListener(v -> NavUtils.showAuthenticatedImageDialog(ctx, "Complaint Evidence Photo", path));
            } else {
                iv.setVisibility(View.GONE);
                iv.setOnClickListener(null);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCategory, tvOverdueTag, tvDescription, tvDate, tvExpandHint, tvSlaDue, tvStaffResponse;
        final StatusBadge badgeStatus;
        final LinearLayout layoutExpanded, layoutPhotos;
        final ImageView ivPhoto1, ivPhoto2, ivPhoto3;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvComplaintCategory);
            tvOverdueTag = itemView.findViewById(R.id.tvOverdueTag);
            badgeStatus = itemView.findViewById(R.id.badgeComplaintStatus);
            tvDescription = itemView.findViewById(R.id.tvComplaintDescription);
            tvDate = itemView.findViewById(R.id.tvComplaintDate);
            tvExpandHint = itemView.findViewById(R.id.tvExpandHint);
            layoutExpanded = itemView.findViewById(R.id.layoutExpandedResponse);
            layoutPhotos = itemView.findViewById(R.id.layoutComplaintPhotos);
            ivPhoto1 = itemView.findViewById(R.id.ivComplaintPhoto1);
            ivPhoto2 = itemView.findViewById(R.id.ivComplaintPhoto2);
            ivPhoto3 = itemView.findViewById(R.id.ivComplaintPhoto3);
            tvSlaDue = itemView.findViewById(R.id.tvSlaDue);
            tvStaffResponse = itemView.findViewById(R.id.tvStaffResponse);
        }
    }
}
