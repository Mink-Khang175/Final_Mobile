package com.finalproject.v_league_ticket.presentation.shop;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.ItemShopProductBinding;

public class ShopProductAdapter extends ListAdapter<ShopProduct, ShopProductAdapter.ProductViewHolder> {
    public interface OnProductClick {
        void onClick(ShopProduct product);
    }

    private final OnProductClick onProductClick;

    public ShopProductAdapter(OnProductClick onProductClick) {
        super(DIFF);
        this.onProductClick = onProductClick;
    }

    @Override
    public ProductViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ProductViewHolder(ItemShopProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), onProductClick);
    }

    @Override
    public void onBindViewHolder(ProductViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ItemShopProductBinding binding;
        private final OnProductClick onProductClick;

        ProductViewHolder(ItemShopProductBinding binding, OnProductClick onProductClick) {
            super(binding.getRoot());
            this.binding = binding;
            this.onProductClick = onProductClick;
        }

        void bind(ShopProduct item) {
            binding.getRoot().setOnClickListener(v -> onProductClick.onClick(item));
            binding.tvProductName.setText(item.getName());
            binding.tvProductPrice.setText(item.isInStock() ? item.getPrice() : item.getPrice() + " - Hết hàng");
            load(binding.imgProduct, item.getImageUrl());
        }

        private void load(ImageView imageView, String url) {
            Glide.with(imageView).load(url == null || url.isEmpty() ? null : url)
                    .placeholder(R.drawable.ic_logo).error(R.drawable.ic_logo).centerInside().into(imageView);
        }
    }

    private static final DiffUtil.ItemCallback<ShopProduct> DIFF = new DiffUtil.ItemCallback<ShopProduct>() {
        @Override
        public boolean areItemsTheSame(ShopProduct oldItem, ShopProduct newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(ShopProduct oldItem, ShopProduct newItem) {
            return oldItem.equals(newItem);
        }
    };
}
