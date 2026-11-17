package com.barangay.bdms.ui.resident;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.barangay.bdms.R;
import com.barangay.bdms.model.DocumentRequest;
import com.barangay.bdms.ui.common.DateFormatUtils;
import com.barangay.bdms.ui.common.StatusBadge;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DocumentRequestAdapter extends RecyclerView.Adapter<DocumentRequestAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull DocumentRequest request);
    }

    private final List<DocumentRequest> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public DocumentRequestAdapter(@NonNull OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<DocumentRequest> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentRequest item = items.get(position);
        holder.tvDocumentType.setText(item.getDocumentType() != null ? item.getDocumentType() : "Document");
        holder.tvControlNumber.setText(item.getControlNumber() != null ? item.getControlNumber() : "Pending");
        holder.tvPurpose.setText(item.getPurpose() != null ? item.getPurpose() : "");
        holder.tvCreatedAt.setText(DateFormatUtils.formatDateTime(item.getCreatedAt()));

        BigDecimal fee = item.getFee() != null ? item.getFee() : BigDecimal.ZERO;
        holder.tvFee.setText(String.format(Locale.US, "PHP %.2f", fee.doubleValue()));
        holder.badgeStatus.setStatus(item.getStatus());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDocumentType, tvControlNumber, tvPurpose, tvCreatedAt, tvFee;
        final StatusBadge badgeStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDocumentType = itemView.findViewById(R.id.tvDocumentType);
            tvControlNumber = itemView.findViewById(R.id.tvControlNumber);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            tvFee = itemView.findViewById(R.id.tvFee);
            badgeStatus = itemView.findViewById(R.id.badgeStatus);
        }
    }
}
