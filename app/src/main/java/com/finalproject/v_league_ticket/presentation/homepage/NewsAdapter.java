package com.finalproject.v_league_ticket.presentation.homepage;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.ItemHomeNewsBinding;
import com.finalproject.v_league_ticket.domain.model.NewsPost;

public class NewsAdapter extends ListAdapter<NewsPost, NewsAdapter.NewsViewHolder> {
    public NewsAdapter() {
        super(DIFF);
    }

    @Override
    public NewsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemHomeNewsBinding binding = ItemHomeNewsBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NewsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(NewsViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        private final ItemHomeNewsBinding binding;

        NewsViewHolder(ItemHomeNewsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NewsPost item) {
            binding.tvNewsCategory.setText(item.getCategory().isEmpty() ? "V.LEAGUE" : item.getCategory().toUpperCase());
            binding.tvNewsHeadline.setText(item.getTitle());
            binding.tvNewsTime.setText(item.getPublishedAt().isEmpty() ? "Latest" : item.getPublishedAt());
            Glide.with(binding.imgNewsThumbnail)
                    .load(item.getImageUrl().isEmpty() ? null : item.getImageUrl())
                    .placeholder(R.drawable.svd)
                    .error(R.drawable.svd)
                    .centerCrop()
                    .into(binding.imgNewsThumbnail);
        }
    }

    private static final DiffUtil.ItemCallback<NewsPost> DIFF = new DiffUtil.ItemCallback<NewsPost>() {
        @Override
        public boolean areItemsTheSame(NewsPost oldItem, NewsPost newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(NewsPost oldItem, NewsPost newItem) {
            return oldItem.equals(newItem);
        }
    };
}
