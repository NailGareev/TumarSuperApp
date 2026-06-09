package com.digitalcompany.tumarsuperapp.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalcompany.tumarsuperapp.PaymentsFragment;
import com.digitalcompany.tumarsuperapp.R;

import java.util.List;

public class PaymentsAdapter extends RecyclerView.Adapter<PaymentsAdapter.ServiceVH> {

    public static class Category {
        public final String name;
        public Category(String name) { this.name = name; }
    }

    public static class Service {
        public final String name;
        public final String accountLabel;
        public final String accountHint;
        public final String icon;
        public final String category;
        public final String logoUrl;

        public Service(String name, String accountLabel, String accountHint,
                       String icon, String category, String logoUrl) {
            this.name         = name;
            this.accountLabel = accountLabel;
            this.accountHint  = accountHint;
            this.icon         = icon;
            this.category     = category;
            this.logoUrl      = logoUrl;
        }
    }

    public interface OnServiceClickListener {
        void onServiceClick(Service service);
    }

    private final List<Service> items;
    private final OnServiceClickListener listener;

    public PaymentsAdapter(List<Service> items, OnServiceClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ServiceVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_service, parent, false);
        return new ServiceVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceVH holder, int position) {
        Service svc = items.get(position);
        holder.tvName.setText(svc.name);

        // Determine subtitle (country) based on service name
        String country = getCountryLabel(svc.name, svc.category);
        if (country != null && !country.isEmpty()) {
            holder.tvSub.setText(country);
            holder.tvSub.setVisibility(View.VISIBLE);
        } else {
            holder.tvSub.setVisibility(View.GONE);
        }

        // Apply accent colors via GradientDrawable
        int accentColor  = PaymentsFragment.getAccentColor(svc.category);
        int accentLight  = (accentColor & 0x00FFFFFF) | 0x1A000000; // 10% alpha
        int accentMedium = (accentColor & 0x00FFFFFF) | 0x35000000; // 21% alpha
        int accentBorder = (accentColor & 0x00FFFFFF) | 0x47000000; // 28% alpha

        float density = holder.itemView.getResources().getDisplayMetrics().density;

        // Icon box
        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setColor(accentLight);
        iconBoxBg.setStroke((int)(1.5f * density), accentBorder);
        iconBoxBg.setCornerRadius(12 * density);
        holder.flSvcIconBox.setBackground(iconBoxBg);

        // Inner icon
        GradientDrawable innerBg = new GradientDrawable();
        innerBg.setColor(accentMedium);
        innerBg.setCornerRadius(6 * density);
        holder.viewSvcIconInner.setBackground(innerBg);

        // Divider: show for all but last item
        if (holder.divider != null) {
            holder.divider.setVisibility(position < items.size() - 1 ? View.VISIBLE : View.GONE);
        }

        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onServiceClick(svc); });
    }

    private String getCountryLabel(String name, String category) {
        if (!"Мобильная связь".equals(category)) return null;
        if (name.contains("KG") || name.contains("O!") || name.contains("MegaCom")) {
            return "Кыргызстан";
        }
        return "Казахстан";
    }

    @Override public int getItemCount() { return items.size(); }

    public static class ServiceVH extends RecyclerView.ViewHolder {
        TextView tvName, tvSub;
        FrameLayout flSvcIconBox;
        View viewSvcIconInner;
        View divider;

        ServiceVH(View v) {
            super(v);
            tvName          = v.findViewById(R.id.tv_svc_name);
            tvSub           = v.findViewById(R.id.tv_svc_sub);
            flSvcIconBox    = v.findViewById(R.id.fl_svc_icon_box);
            viewSvcIconInner = v.findViewById(R.id.view_svc_icon_inner);
            divider         = v.findViewById(R.id.view_svc_divider);
        }
    }
}
