package com.digitalcompany.tumarsuperapp.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
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
        holder.tvIcon.setText(svc.icon);

        if (svc.logoUrl != null) {
            Glide.with(holder.itemView)
                    .load(svc.logoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            holder.ivLogo.setVisibility(View.GONE);
                            holder.flEmojiBg.setVisibility(View.VISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            holder.flEmojiBg.setVisibility(View.GONE);
                            holder.ivLogo.setVisibility(View.VISIBLE);
                            return false;
                        }
                    })
                    .into(holder.ivLogo);
        } else {
            holder.ivLogo.setVisibility(View.GONE);
            holder.flEmojiBg.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onServiceClick(svc); });
    }

    @Override public int getItemCount() { return items.size(); }

    public static class ServiceVH extends RecyclerView.ViewHolder {
        TextView tvName, tvIcon;
        ImageView ivLogo;
        FrameLayout flEmojiBg;

        ServiceVH(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tv_service_name);
            tvIcon    = v.findViewById(R.id.tv_service_icon);
            ivLogo    = v.findViewById(R.id.iv_service_logo);
            flEmojiBg = v.findViewById(R.id.fl_emoji_bg);
        }
    }
}
