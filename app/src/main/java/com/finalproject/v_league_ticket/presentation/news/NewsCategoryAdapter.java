package com.finalproject.v_league_ticket.presentation.news;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.ItemNewsCategoryChipBinding;

import java.util.Arrays;
import java.util.List;

public class NewsCategoryAdapter extends RecyclerView.Adapter<NewsCategoryAdapter.CategoryViewHolder> {
    public interface OnCategoryClick {
        void onClick(NewsCategory category);
    }

    private final OnCategoryClick onCategoryClick;
    private final List<NewsCategory> items = Arrays.asList(NewsCategory.values());
    private NewsCategory selected = NewsCategory.VLEAGUE;

    public NewsCategoryAdapter(OnCategoryClick onCategoryClick) {
        this.onCategoryClick = onCategoryClick;
    }

    @Override
    public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CategoryViewHolder(ItemNewsCategoryChipBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), onCategoryClick);
    }

    @Override
    public void onBindViewHolder(CategoryViewHolder holder, int position) {
        holder.bind(items.get(position), items.get(position) == selected);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void select(NewsCategory category) {
        selected = category;
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewsCategoryChipBinding binding;
        private final OnCategoryClick onCategoryClick;

        CategoryViewHolder(ItemNewsCategoryChipBinding binding, OnCategoryClick onCategoryClick) {
            super(binding.getRoot());
            this.binding = binding;
            this.onCategoryClick = onCategoryClick;
        }

        void bind(NewsCategory category, boolean selected) {
            binding.tvCategoryChip.setText(category.getLabel());
            binding.tvCategoryChip.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(),
                    selected ? R.color.white : R.color.dark_gray_text));
            binding.tvCategoryChip.setBackgroundResource(selected ? R.drawable.bg_news_chip_active : R.drawable.bg_news_chip_inactive);
            binding.tvCategoryChip.setOnClickListener(v -> onCategoryClick.onClick(category));
        }
    }
}
