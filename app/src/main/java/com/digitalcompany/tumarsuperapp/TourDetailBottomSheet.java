package com.digitalcompany.tumarsuperapp;

import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.digitalcompany.tumarsuperapp.adapter.TourCardAdapter.TourCard;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.Locale;

public class TourDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String KEY_LOCATION   = "location";
    private static final String KEY_HOTEL      = "hotel";
    private static final String KEY_STARS      = "stars";
    private static final String KEY_PRICE      = "price";
    private static final String KEY_MONTHLY    = "monthly";
    private static final String KEY_MONTHS     = "months";
    private static final String KEY_DISCOUNT   = "discount";
    private static final String KEY_ORIG_PRICE = "orig_price";
    private static final String KEY_IMAGE_URL  = "image_url";

    public static TourDetailBottomSheet newInstance(TourCard card) {
        TourDetailBottomSheet sheet = new TourDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(KEY_LOCATION,   card.location);
        args.putString(KEY_HOTEL,      card.hotelName);
        args.putInt(KEY_STARS,         card.stars);
        args.putLong(KEY_PRICE,        card.price);
        args.putLong(KEY_MONTHLY,      card.monthlyPrice);
        args.putInt(KEY_MONTHS,        card.months);
        args.putInt(KEY_DISCOUNT,      card.discountPercent);
        args.putLong(KEY_ORIG_PRICE,   card.originalPrice);
        args.putString(KEY_IMAGE_URL,  card.imageUrl);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tour_detail_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) { dismiss(); return; }

        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("ru"));

        ImageView ivPhoto = view.findViewById(R.id.iv_detail_photo);
        Glide.with(this)
                .load(args.getString(KEY_IMAGE_URL))
                .placeholder(R.drawable.placeholder_tour)
                .error(R.drawable.placeholder_tour)
                .centerCrop()
                .into(ivPhoto);

        view.<TextView>findViewById(R.id.tv_detail_location)
                .setText(args.getString(KEY_LOCATION));
        view.<TextView>findViewById(R.id.tv_detail_stars)
                .setText(buildStars(args.getInt(KEY_STARS)));
        view.<TextView>findViewById(R.id.tv_detail_hotel)
                .setText(args.getString(KEY_HOTEL));
        view.<TextView>findViewById(R.id.tv_detail_price)
                .setText(fmt.format(args.getLong(KEY_PRICE)) + " ₸");

        TextView tvOrig = view.findViewById(R.id.tv_detail_original);
        tvOrig.setText(fmt.format(args.getLong(KEY_ORIG_PRICE)) + " ₸");
        tvOrig.setPaintFlags(tvOrig.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        TextView tvDiscount = view.findViewById(R.id.tv_detail_discount);
        tvDiscount.setText("-" + args.getInt(KEY_DISCOUNT) + "%");
        GradientDrawable redBg = new GradientDrawable();
        redBg.setColor(0xFFE53935);
        redBg.setCornerRadius(8 * getResources().getDisplayMetrics().density);
        tvDiscount.setBackground(redBg);

        TextView tvMonthly = view.findViewById(R.id.tv_detail_monthly);
        tvMonthly.setText(fmt.format(args.getLong(KEY_MONTHLY)) + " ₸");
        GradientDrawable yellowBg = new GradientDrawable();
        yellowBg.setColor(0xFFFFC107);
        yellowBg.setCornerRadius(6 * getResources().getDisplayMetrics().density);
        tvMonthly.setBackground(yellowBg);

        view.<TextView>findViewById(R.id.tv_detail_installment)
                .setText("× " + args.getInt(KEY_MONTHS) + " мес  (0·0·" + args.getInt(KEY_MONTHS) + ")");

        view.<MaterialButton>findViewById(R.id.btn_book).setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Бронирование будет доступно в полной версии",
                    Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    private static String buildStars(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append('★');
        return sb.toString();
    }
}
