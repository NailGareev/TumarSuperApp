package com.digitalcompany.tumarsuperapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CardPagerAdapter extends RecyclerView.Adapter<CardPagerAdapter.CardViewHolder> {

    interface OnCardClickListener {
        void onCardClick(int cardIndex);
    }

    static class CardEntry {
        int index;
        String number;
        String expiry;
        String cvv;
        String customName;
        boolean blocked;
    }

    private final List<CardEntry> cards;
    private final OnCardClickListener listener;

    CardPagerAdapter(List<CardEntry> cards, OnCardClickListener listener) {
        this.cards = cards;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_widget, parent, false);
        return new CardViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardEntry card = cards.get(position);
        String name = (card.customName == null || card.customName.isEmpty())
                ? "Tumar Карта" : card.customName;
        holder.textCustomName.setText(name);
        holder.textNumber.setText(maskCardNumber(card.number));
        holder.textExpiry.setText(card.expiry);
        holder.blockedOverlay.setVisibility(card.blocked ? View.VISIBLE : View.GONE);
        int idx = card.index;
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onCardClick(idx); });
    }

    @Override
    public int getItemCount() { return cards.size(); }

    private String maskCardNumber(String n) {
        if (n == null || n.length() != 16) return "•••• •••• •••• ----";
        return "•••• •••• •••• " + n.substring(12);
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView     textCustomName, textNumber, textExpiry;
        LinearLayout blockedOverlay;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            textCustomName = itemView.findViewById(R.id.text_card_custom_name_item);
            textNumber     = itemView.findViewById(R.id.text_card_number_item);
            textExpiry     = itemView.findViewById(R.id.text_card_expiry_item);
            blockedOverlay = itemView.findViewById(R.id.card_blocked_overlay_item);
        }
    }
}
