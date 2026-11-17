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
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.barangay.bdms.R;
import com.barangay.bdms.model.Announcement;
import com.barangay.bdms.network.AuthenticatedImageLoader;
import com.barangay.bdms.ui.common.DateFormatUtils;
import com.barangay.bdms.ui.common.NavUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    private final List<Announcement> items = new ArrayList<>();
    private final Set<UUID> expandedIds = new HashSet<>();
    private boolean firstPinnedEmergency = false;

    public void submitList(@NonNull List<Announcement> newItems, boolean hasPinnedEmergency) {
        items.clear();
        items.addAll(newItems);
        this.firstPinnedEmergency = hasPinnedEmergency;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement a = items.get(position);
        Context ctx = holder.itemView.getContext();
        String category = a.getCategory() != null ? a.getCategory().toLowerCase(Locale.US) : "general";
        boolean isEmergency = "emergency".equals(category);

        holder.tvCategory.setText(category.toUpperCase(Locale.US));
        holder.tvPinnedLabel.setVisibility(position == 0 && firstPinnedEmergency && isEmergency ? View.VISIBLE : View.GONE);

        if (isEmergency) {
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.statusRejectedBg));
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(ctx, R.color.statusRejectedText));
            ViewCompat.setBackgroundTintList(holder.tvCategory, ContextCompat.getColorStateList(ctx, R.color.colorSurface));
            holder.tvCategory.setTextColor(ContextCompat.getColor(ctx, R.color.statusRejectedText));
        } else if ("event".equals(category)) {
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.colorSurface));
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(ctx, R.color.colorOutline));
            ViewCompat.setBackgroundTintList(holder.tvCategory, ContextCompat.getColorStateList(ctx, R.color.statusProcessingBg));
            holder.tvCategory.setTextColor(ContextCompat.getColor(ctx, R.color.statusProcessingText));
        } else {
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.colorSurface));
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(ctx, R.color.colorOutline));
            ViewCompat.setBackgroundTintList(holder.tvCategory, ContextCompat.getColorStateList(ctx, R.color.colorPrimaryLight));
            holder.tvCategory.setTextColor(ContextCompat.getColor(ctx, R.color.colorPrimaryDark));
        }

        holder.tvTarget.setText(a.isBarangayWide()
                ? "Barangay-Wide"
                : TextUtils.join(", ", a.getTargetPurok()));
        holder.tvTitle.setText(a.getTitle() != null ? a.getTitle() : "");
        holder.tvBody.setText(a.getBody() != null ? a.getBody() : "");
        holder.tvDate.setText(DateFormatUtils.formatDateTime(a.getCreatedAt()));

        boolean expanded = a.getAnnouncementId() != null && expandedIds.contains(a.getAnnouncementId());
        holder.tvBody.setMaxLines(expanded ? Integer.MAX_VALUE : 3);
        holder.tvBody.setEllipsize(expanded ? null : TextUtils.TruncateAt.END);
        holder.tvExpand.setText(expanded ? "Show less" : "Read more");

        String attachmentUrl = a.getAttachmentUrl();
        boolean hasAttachment = expanded && attachmentUrl != null && !attachmentUrl.trim().isEmpty();
        if (hasAttachment) {
            String trimmedPath = attachmentUrl.trim();
            holder.layoutAttachment.setVisibility(View.VISIBLE);
            AuthenticatedImageLoader.load(ctx, trimmedPath, holder.ivAttachment, R.drawable.ic_bdms_logo, 800);
            holder.ivAttachment.setOnClickListener(v ->
                    NavUtils.showAuthenticatedImageDialog(ctx, a.getTitle() != null ? a.getTitle() : "Announcement Attachment", trimmedPath)
            );
        } else {
            holder.layoutAttachment.setVisibility(View.GONE);
            holder.ivAttachment.setOnClickListener(null);
        }

        holder.itemView.setOnClickListener(v -> {
            if (a.getAnnouncementId() == null) return;
            if (expandedIds.contains(a.getAnnouncementId())) {
                expandedIds.remove(a.getAnnouncementId());
            } else {
                expandedIds.add(a.getAnnouncementId());
            }
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                notifyItemChanged(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final TextView tvCategory, tvPinnedLabel, tvTarget, tvTitle, tvBody, tvDate, tvExpand;
        final LinearLayout layoutAttachment;
        final ImageView ivAttachment;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardAnnouncementRoot);
            tvCategory = itemView.findViewById(R.id.tvAnnouncementCategory);
            tvPinnedLabel = itemView.findViewById(R.id.tvPinnedLabel);
            tvTarget = itemView.findViewById(R.id.tvAnnouncementTarget);
            tvTitle = itemView.findViewById(R.id.tvAnnouncementTitle);
            tvBody = itemView.findViewById(R.id.tvAnnouncementBody);
            tvDate = itemView.findViewById(R.id.tvAnnouncementDate);
            tvExpand = itemView.findViewById(R.id.tvAnnouncementExpand);
            layoutAttachment = itemView.findViewById(R.id.layoutAnnouncementAttachment);
            ivAttachment = itemView.findViewById(R.id.ivAnnouncementAttachment);
        }
    }
}
