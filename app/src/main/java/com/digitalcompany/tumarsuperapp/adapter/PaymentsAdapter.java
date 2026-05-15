package com.digitalcompany.tumarsuperapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.digitalcompany.tumarsuperapp.R;
import java.util.List;

public class PaymentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CATEGORY = 0;
    private static final int TYPE_SERVICE = 1;

    public static class Category {
        public final String name;
        public Category(String name) { this.name = name; }
    }

    public static class Service {
        public final String name;
        public final String accountLabel;
        public final String accountHint;
        public Service(String name, String accountLabel, String accountHint) {
            this.name = name;
            this.accountLabel = accountLabel;
            this.accountHint = accountHint;
        }
    }

    public interface OnServiceClickListener {
        void onServiceClick(Service service);
    }

    private final List<Object> items;
    private final OnServiceClickListener listener;

    public PaymentsAdapter(List<Object> items, OnServiceClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof Category ? TYPE_CATEGORY : TYPE_SERVICE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_CATEGORY) {
            return new CategoryVH(inflater.inflate(R.layout.item_payment_category, parent, false));
        }
        return new ServiceVH(inflater.inflate(R.layout.item_payment_service, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CategoryVH) {
            ((CategoryVH) holder).tvName.setText(((Category) items.get(position)).name);
        } else {
            Service service = (Service) items.get(position);
            ServiceVH svh = (ServiceVH) holder;
            svh.tvName.setText(service.name);
            svh.itemView.setOnClickListener(v -> { if (listener != null) listener.onServiceClick(service); });
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class CategoryVH extends RecyclerView.ViewHolder {
        TextView tvName;
        CategoryVH(View v) { super(v); tvName = v.findViewById(R.id.tv_category_name); }
    }

    static class ServiceVH extends RecyclerView.ViewHolder {
        TextView tvName;
        ServiceVH(View v) { super(v); tvName = v.findViewById(R.id.tv_service_name); }
    }
}
