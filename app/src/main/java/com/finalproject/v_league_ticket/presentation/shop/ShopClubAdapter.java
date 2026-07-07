package com.finalproject.v_league_ticket.presentation.shop;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.ItemShopClubBinding;
import com.finalproject.v_league_ticket.presentation.club.ClubProfileDirectory;

public class ShopClubAdapter extends ListAdapter<ShopClub, ShopClubAdapter.ClubViewHolder> {
    public interface OnClubClick {
        void onClick(ShopClub club);
    }

    private final OnClubClick onClubClick;

    public ShopClubAdapter(OnClubClick onClubClick) {
        super(DIFF);
        this.onClubClick = onClubClick;
    }

    @Override
    public ClubViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ClubViewHolder(ItemShopClubBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), onClubClick);
    }

    @Override
    public void onBindViewHolder(ClubViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ClubViewHolder extends RecyclerView.ViewHolder {
        private final ItemShopClubBinding binding;
        private final OnClubClick onClubClick;

        ClubViewHolder(ItemShopClubBinding binding, OnClubClick onClubClick) {
            super(binding.getRoot());
            this.binding = binding;
            this.onClubClick = onClubClick;
        }

        void bind(ShopClub item) {
            binding.getRoot().setOnClickListener(v -> onClubClick.onClick(item));
            binding.tvClubName.setText(item.getName());
            applyClubTone(item);
            load(binding.imgClub, item.getImageUrl());
        }

        private void applyClubTone(ShopClub item) {
            ClubProfileDirectory.Meta meta = ClubProfileDirectory.find(itemView.getContext(), item.getName());
            int strokeColor = meta == null ? itemView.getContext().getColor(R.color.sports_outline) : meta.primaryColorInt();
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            background.setColor(Color.WHITE);
            background.setStroke(dp(1.4f), adjustAlpha(strokeColor, 0.28f));
            binding.getRoot().setBackground(background);
        }

        private void load(ImageView imageView, String url) {
            if (url == null || url.isEmpty()) {
                Glide.with(imageView).load(R.drawable.ic_logo).centerInside().into(imageView);
                return;
            }
            Glide.with(imageView).load(url)
                    .placeholder(R.color.transparent)
                    .error(R.color.transparent)
                    .centerInside()
                    .into(imageView);
        }

        private int dp(float value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }

        private int adjustAlpha(int color, float factor) {
            return Color.argb(Math.round(Color.alpha(color) * factor),
                    Color.red(color), Color.green(color), Color.blue(color));
        }
    }

    private static final DiffUtil.ItemCallback<ShopClub> DIFF = new DiffUtil.ItemCallback<ShopClub>() {
        @Override
        public boolean areItemsTheSame(ShopClub oldItem, ShopClub newItem) {
            return oldItem.getPagePath().equals(newItem.getPagePath());
        }

        @Override
        public boolean areContentsTheSame(ShopClub oldItem, ShopClub newItem) {
            return oldItem.equals(newItem);
        }
    };
}
