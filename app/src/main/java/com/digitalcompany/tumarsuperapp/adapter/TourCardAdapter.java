package com.digitalcompany.tumarsuperapp.adapter;

import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.digitalcompany.tumarsuperapp.R;
import com.digitalcompany.tumarsuperapp.network.models.Tour;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TourCardAdapter extends RecyclerView.Adapter<TourCardAdapter.VH> {

    public static class TourCard {
        public final String location;
        public final String hotelName;
        public final int stars;
        public final long price;
        public final long monthlyPrice;
        public final int months;
        public final int discountPercent;
        public final long originalPrice;
        public final String imageUrl;
        public final boolean isHot;
        public final String bookingUrl;

        public TourCard(String location, String hotelName, int stars,
                        long price, long monthlyPrice, int months,
                        int discountPercent, long originalPrice,
                        String imageUrl, boolean isHot, String bookingUrl) {
            this.location = location;
            this.hotelName = hotelName;
            this.stars = stars;
            this.price = price;
            this.monthlyPrice = monthlyPrice;
            this.months = months;
            this.discountPercent = discountPercent;
            this.originalPrice = originalPrice;
            this.imageUrl = imageUrl;
            this.isHot = isHot;
            this.bookingUrl = bookingUrl;
        }

        public static TourCard from(Tour t) {
            return new TourCard(
                    t.location,
                    t.hotelName,
                    t.stars,
                    t.price,
                    t.getMonthlyPrice(),
                    t.months,
                    t.discountPercent,
                    t.originalPrice,
                    t.imageUrl,
                    t.isHot == 1,
                    ""
            );
        }
    }

    public interface OnCardClickListener {
        void onCardClick(TourCard card);
    }

    private final List<TourCard> items = new ArrayList<>();
    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("ru"));
    private OnCardClickListener clickListener;

    public void setOnCardClickListener(OnCardClickListener listener) {
        this.clickListener = listener;
    }

    public void setItems(List<TourCard> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        TourCard card = items.get(pos);

        Glide.with(h.ivTour.getContext())
                .load(card.imageUrl)
                .placeholder(R.drawable.placeholder_tour)
                .error(R.drawable.placeholder_tour)
                .centerCrop()
                .into(h.ivTour);

        h.tvLocation.setText(card.location);
        h.tvHotelName.setText(card.hotelName + "  " + stars(card.stars));
        h.tvPrice.setText(fmt.format(card.price) + " ₸");
        h.tvMonthly.setText(fmt.format(card.monthlyPrice) + " ₸");
        h.tvXMonths.setText("x" + card.months);
        h.tvDiscount.setText("-" + card.discountPercent + "%");
        h.tvOriginalPrice.setText(fmt.format(card.originalPrice) + " ₸");
        h.tvOriginalPrice.setPaintFlags(h.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        GradientDrawable yellowBg = new GradientDrawable();
        yellowBg.setColor(0xFFFFC107);
        yellowBg.setCornerRadius(dp(h, 4));
        h.tvMonthly.setBackground(yellowBg);

        GradientDrawable redBg = new GradientDrawable();
        redBg.setColor(0xFFE53935);
        redBg.setCornerRadius(dp(h, 4));
        h.tvDiscount.setBackground(redBg);

        GradientDrawable installBg = new GradientDrawable();
        installBg.setColor(0xFFFFC107);
        installBg.setCornerRadius(dp(h, 4));
        h.tvInstallmentBadge.setBackground(installBg);

        GradientDrawable darkBg = new GradientDrawable();
        darkBg.setColor(0xCC000000);
        darkBg.setCornerRadius(dp(h, 8));
        h.tvLocation.setBackground(darkBg);

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCardClick(card);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private static String stars(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append('★');
        return sb.toString();
    }

    private static float dp(VH h, int dp) {
        return dp * h.itemView.getContext().getResources().getDisplayMetrics().density;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivTour;
        TextView tvInstallmentBadge, tvLocation, tvHotelName,
                 tvPrice, tvMonthly, tvXMonths, tvDiscount, tvOriginalPrice;

        VH(View v) {
            super(v);
            ivTour             = v.findViewById(R.id.iv_tour);
            tvInstallmentBadge = v.findViewById(R.id.tv_installment_badge);
            tvLocation         = v.findViewById(R.id.tv_location);
            tvHotelName        = v.findViewById(R.id.tv_hotel_name);
            tvPrice            = v.findViewById(R.id.tv_price);
            tvMonthly          = v.findViewById(R.id.tv_monthly);
            tvXMonths          = v.findViewById(R.id.tv_x_months);
            tvDiscount         = v.findViewById(R.id.tv_discount);
            tvOriginalPrice    = v.findViewById(R.id.tv_original_price);
        }
    }
}
