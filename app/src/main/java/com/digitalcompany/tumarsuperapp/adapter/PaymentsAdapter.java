package com.digitalcompany.tumarsuperapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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

        public Service(String name, String accountLabel, String accountHint, String icon, String category) {
            this.name         = name;
            this.accountLabel = accountLabel;
            this.accountHint  = accountHint;
            this.icon         = icon;
            this.category     = category;
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
        holder.tvIcon.setText(svc.icon);
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onServiceClick(svc); });
    }

    @Override public int getItemCount() { return items.size(); }

    public static class ServiceVH extends RecyclerView.ViewHolder {
        TextView tvName, tvIcon;
        ServiceVH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_service_name);
            tvIcon = v.findViewById(R.id.tv_service_icon);
        }
    }
}
