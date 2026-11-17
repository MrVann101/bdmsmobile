package com.barangay.bdms.ui.resident;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.barangay.bdms.R;
import com.barangay.bdms.model.Notification;
import com.barangay.bdms.ui.common.DateFormatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(@NonNull Notification notification);
    }

    private final List<Notification> items = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(@NonNull OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<Notification> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification item = items.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "Notification");
        holder.tvMessage.setText(item.getMessage() != null ? item.getMessage() : "");
        holder.tvDate.setText(DateFormatUtils.formatDateTime(item.getCreatedAt()));

        boolean unread = !item.isRead();
        holder.tvTitle.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
        holder.viewUnreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);

        String type = item.getType() != null ? item.getType().toLowerCase(Locale.US) : "";
        int tintColor;
        int iconRes;
        switch (type) {
            case "emergency":
                iconRes = android.R.drawable.ic_dialog_alert;
                tintColor = R.color.statusRejectedText;
                break;
            case "complaint":
                iconRes = android.R.drawable.ic_menu_edit;
                tintColor = R.color.statusPendingText;
                break;
            case "request_update":
                iconRes = android.R.drawable.ic_menu_agenda;
                tintColor = R.color.colorPrimary;
                break;
            case "announcement":
            default:
                iconRes = R.drawable.ic_notifications_bell;
                tintColor = R.color.statusProcessingText;
                break;
        }
        holder.ivIcon.setImageResource(iconRes);
        ImageViewCompat.setImageTintList(holder.ivIcon, ContextCompat.getColorStateList(ctx, tintColor));

        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvTitle, tvMessage, tvDate;
        final View viewUnreadDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivNotifTypeIcon);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvDate = itemView.findViewById(R.id.tvNotifDate);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }
    }
}
