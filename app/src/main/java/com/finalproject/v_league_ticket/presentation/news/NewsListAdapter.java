package com.finalproject.v_league_ticket.presentation.news;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.ItemNewsCardBinding;
import com.finalproject.v_league_ticket.domain.model.NewsPost;

public class NewsListAdapter extends ListAdapter<NewsPost, NewsListAdapter.NewsViewHolder> {
    public interface OnNewsClick {
        void onClick(NewsPost post);
    }

    private final OnNewsClick onNewsClick;
    private final OnNewsClick onBookmarkClick;

    public NewsListAdapter(OnNewsClick onNewsClick, OnNewsClick onBookmarkClick) {
        super(DIFF);
        this.onNewsClick = onNewsClick;
        this.onBookmarkClick = onBookmarkClick;
    }

    @Override
    public NewsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NewsViewHolder(ItemNewsCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), onNewsClick, onBookmarkClick);
    }

    @Override
    public void onBindViewHolder(NewsViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewsCardBinding binding;
        private final OnNewsClick onNewsClick;
        private final OnNewsClick onBookmarkClick;

        NewsViewHolder(ItemNewsCardBinding binding, OnNewsClick onNewsClick, OnNewsClick onBookmarkClick) {
            super(binding.getRoot());
            this.binding = binding;
            this.onNewsClick = onNewsClick;
            this.onBookmarkClick = onBookmarkClick;
        }

        void bind(NewsPost item) {
            binding.getRoot().setOnClickListener(v -> onNewsClick.onClick(item));
            binding.btnBookmark.setOnClickListener(v -> onBookmarkClick.onClick(item));
            binding.tvNewsCategory.setText(item.getCategory().isEmpty() ? "V.LEAGUE" : item.getCategory().toUpperCase());
            binding.tvNewsTitle.setText(item.getTitle());
            binding.tvNewsExcerpt.setText(item.getExcerpt().isEmpty() ? "No excerpt returned by VPF." : item.getExcerpt());
            binding.tvNewsMeta.setText(item.getSourceName() + " - " + (item.getPublishedAt().isEmpty() ? "Latest" : item.getPublishedAt()));
            load(binding.imgNewsThumb, item.getImageUrl());
        }

        private void load(ImageView view, String url) {
            Glide.with(view).load(url == null || url.isEmpty() ? null : url)
                    .placeholder(R.drawable.svd).error(R.drawable.svd).centerCrop().into(view);
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
