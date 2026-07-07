package com.finalproject.v_league_ticket.presentation.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproject.v_league_ticket.databinding.ItemProfileHistoryBinding;

public class ProfileHistoryAdapter extends ListAdapter<ProfileHistoryItem, ProfileHistoryAdapter.HistoryViewHolder> {
    public interface OnItemClick {
        void onClick(ProfileHistoryItem item);
    }

    private final OnItemClick onItemClick;

    public ProfileHistoryAdapter(OnItemClick onItemClick) {
        super(DIFF);
        this.onItemClick = onItemClick;
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new HistoryViewHolder(ItemProfileHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), onItemClick);
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemProfileHistoryBinding binding;
        private final OnItemClick onItemClick;

        HistoryViewHolder(ItemProfileHistoryBinding binding, OnItemClick onItemClick) {
            super(binding.getRoot());
            this.binding = binding;
            this.onItemClick = onItemClick;
        }

        void bind(ProfileHistoryItem item) {
            binding.tvHistoryPrimary.setText(item.getPrimary());
            binding.tvHistorySecondary.setText(item.getSecondary());
            binding.tvHistoryAmount.setText(item.getAmount());
            binding.imgHistoryIcon.setImageResource(item.getIconRes());
            binding.imgHistoryIcon.setBackgroundResource(item.getIconBackgroundRes());
            binding.imgHistoryIcon.setColorFilter(ContextCompat.getColor(
                    binding.getRoot().getContext(), item.getIconTintColorRes()));
            binding.tvHistoryAmount.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(), item.getAmountColorRes()));
            binding.historyCardContent.setBackgroundResource(item.getCardBackgroundRes());
            binding.getRoot().setOnClickListener(v -> onItemClick.onClick(item));
        }
    }

    private static final DiffUtil.ItemCallback<ProfileHistoryItem> DIFF = new DiffUtil.ItemCallback<ProfileHistoryItem>() {
        @Override
        public boolean areItemsTheSame(ProfileHistoryItem oldItem, ProfileHistoryItem newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(ProfileHistoryItem oldItem, ProfileHistoryItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}
