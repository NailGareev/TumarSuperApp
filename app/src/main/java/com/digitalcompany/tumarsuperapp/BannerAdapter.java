package com.digitalcompany.tumarsuperapp; // Или ваш пакет adapters

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Пример адаптера для локальных drawable ресурсов
public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<Integer> bannerDrawableIds; // Список ID ресурсов drawable

    // Конструктор
    public BannerAdapter(List<Integer> bannerDrawableIds) {
        this.bannerDrawableIds = bannerDrawableIds;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        int drawableId = bannerDrawableIds.get(position);
        holder.bannerImageView.setImageResource(drawableId);
        // Здесь можно добавить обработчик кликов на баннер, если нужно
        // holder.itemView.setOnClickListener(v -> { /* обработка клика */ });
    }

    @Override
    public int getItemCount() {
        return bannerDrawableIds == null ? 0 : bannerDrawableIds.size();
    }

    // ViewHolder для хранения ссылок на View элемента списка
    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImageView;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            bannerImageView = itemView.findViewById(R.id.bannerImageView);
        }
    }
}

// Примечание: Если баннеры будут загружаться из сети, понадобится:
// 1. Изменить List<Integer> на List<String> (URL-адреса) или List<BannerModel> (класс с URL и др. полями).
// 2. Использовать библиотеку типа Glide или Picasso в onBindViewHolder для загрузки изображения по URL в ImageView.
//    Пример с Glide:
//    Glide.with(holder.itemView.getContext())
//         .load(bannerUrl) // bannerUrl - строка с URL
//         .placeholder(R.drawable.placeholder_image) // Опционально: заглушка
//         .error(R.drawable.error_image) // Опционально: картинка при ошибке
//         .into(holder.bannerImageView);
// 3. Добавить зависимость Glide/Picasso в build.gradle.kts (Module: app).