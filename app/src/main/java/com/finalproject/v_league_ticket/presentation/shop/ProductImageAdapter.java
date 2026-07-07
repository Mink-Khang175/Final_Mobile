package com.finalproject.v_league_ticket.presentation.shop;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.ItemProductDetailImageBinding;

public class ProductImageAdapter extends ListAdapter<String, ProductImageAdapter.ImageViewHolder> {
    public ProductImageAdapter() {
        super(DIFF);
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ImageViewHolder(ItemProductDetailImageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductDetailImageBinding binding;

        ImageViewHolder(ItemProductDetailImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String url) {
            Glide.with(binding.imgProductDetail)
                    .load(url == null || url.isEmpty() ? null : url)
                    .placeholder(R.drawable.ic_logo)
                    .error(R.drawable.ic_logo)
                    .centerInside()
                    .into(binding.imgProductDetail);
        }
    }

    private static final DiffUtil.ItemCallback<String> DIFF = new DiffUtil.ItemCallback<String>() {
        @Override
        public boolean areItemsTheSame(String oldItem, String newItem) { return oldItem.equals(newItem); }
        @Override
        public boolean areContentsTheSame(String oldItem, String newItem) { return oldItem.equals(newItem); }
    };
}
