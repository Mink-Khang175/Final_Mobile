package com.finalproject.v_league_ticket.presentation.shop;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.ItemCartItemBinding;

public class CartItemAdapter extends ListAdapter<CartItem, CartItemAdapter.CartItemViewHolder> {
    public interface OnQuantityChange {
        void onChange(CartItem item, int quantity);
    }

    private final OnQuantityChange onQuantityChange;

    public CartItemAdapter(OnQuantityChange onQuantityChange) {
        super(DIFF);
        this.onQuantityChange = onQuantityChange;
    }

    @Override
    public CartItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CartItemViewHolder(ItemCartItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), onQuantityChange);
    }

    @Override
    public void onBindViewHolder(CartItemViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class CartItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemCartItemBinding binding;
        private final OnQuantityChange onQuantityChange;

        CartItemViewHolder(ItemCartItemBinding binding, OnQuantityChange onQuantityChange) {
            super(binding.getRoot());
            this.binding = binding;
            this.onQuantityChange = onQuantityChange;
        }

        void bind(CartItem item) {
            binding.tvCartProductName.setText(item.getProductName());
            binding.tvCartProductPrice.setText(CartStore.formatVnd(item.getLineTotal()));
            binding.tvCartProductSize.setText("Cỡ " + item.getSelectedSize());
            binding.tvCartQuantity.setText(String.valueOf(item.getQuantity()));
            binding.btnCartDecrease.setOnClickListener(v -> onQuantityChange.onChange(item, item.getQuantity() - 1));
            binding.btnCartIncrease.setOnClickListener(v -> onQuantityChange.onChange(item, item.getQuantity() + 1));
            Glide.with(binding.imgCartProduct).load(item.getImageUrl().isEmpty() ? null : item.getImageUrl())
                    .placeholder(R.drawable.ic_logo).error(R.drawable.ic_logo).into(binding.imgCartProduct);
        }
    }

    private static final DiffUtil.ItemCallback<CartItem> DIFF = new DiffUtil.ItemCallback<CartItem>() {
        @Override
        public boolean areItemsTheSame(CartItem oldItem, CartItem newItem) {
            return oldItem.getProductId().equals(newItem.getProductId())
                    && oldItem.getSelectedSize().equals(newItem.getSelectedSize());
        }

        @Override
        public boolean areContentsTheSame(CartItem oldItem, CartItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}
