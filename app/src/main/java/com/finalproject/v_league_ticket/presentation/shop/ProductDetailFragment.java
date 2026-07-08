package com.finalproject.v_league_ticket.presentation.shop;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.FragmentProductDetailsBinding;

import java.util.Arrays;
import java.util.List;

public class ProductDetailFragment extends Fragment {
    private static final String ARG_ID = "product_id";
    private static final String ARG_NAME = "product_name";
    private static final String ARG_PRICE = "product_price";
    private static final String ARG_PRICE_VALUE = "product_price_value";
    private static final String ARG_IMAGE = "product_image";
    private static final String ARG_IMAGES = "product_images";
    private static final String ARG_SIZES = "product_sizes";
    private static final String ARG_IN_STOCK = "product_in_stock";
    private static final String ARG_URL = "product_url";
    private static final String ARG_DESCRIPTION = "product_description";
    private FragmentProductDetailsBinding binding;
    private final ProductImageAdapter imageAdapter = new ProductImageAdapter();
    private ProductDetail detail;
    private int quantity = 1;
    private String selectedSize = "M";
    private boolean inStock = true;

    public ProductDetailFragment() {
        super(R.layout.fragment_product_details);
    }

    public static ProductDetailFragment newInstance(ShopProduct product) {
        ProductDetailFragment fragment = new ProductDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, product.getId());
        args.putString(ARG_NAME, product.getName());
        args.putString(ARG_PRICE, product.getPrice());
        args.putInt(ARG_PRICE_VALUE, product.getPriceValue());
        args.putString(ARG_IMAGE, product.getImageUrl());
        args.putStringArrayList(ARG_IMAGES, new java.util.ArrayList<>(product.getImageUrls()));
        args.putStringArrayList(ARG_SIZES, new java.util.ArrayList<>(product.getAvailableSizes()));
        args.putBoolean(ARG_IN_STOCK, product.isInStock());
        args.putString(ARG_URL, product.getProductUrl());
        args.putString(ARG_DESCRIPTION, product.getDescription());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentProductDetailsBinding.bind(view);
        java.util.ArrayList<String> images = requireArguments().getStringArrayList(ARG_IMAGES);
        if (images == null || images.isEmpty()) {
            String image = requireArguments().getString(ARG_IMAGE, "");
            images = image.isEmpty() ? new java.util.ArrayList<>() : new java.util.ArrayList<>(java.util.Collections.singletonList(image));
        }
        java.util.ArrayList<String> sizes = requireArguments().getStringArrayList(ARG_SIZES);
        if (sizes == null) sizes = new java.util.ArrayList<>();
        if (!sizes.isEmpty()) selectedSize = sizes.get(0);
        String description = requireArguments().getString(ARG_DESCRIPTION,
                "");
        detail = new ProductDetail(
                requireArguments().getString(ARG_ID, ""),
                requireArguments().getString(ARG_NAME, "Product"),
                requireArguments().getString(ARG_PRICE, "0d"),
                requireArguments().getInt(ARG_PRICE_VALUE, priceValue(requireArguments().getString(ARG_PRICE, "0d"))),
                images,
                sizes,
                requireArguments().getString(ARG_URL, ""),
                description == null ? "" : description);
        inStock = requireArguments().getBoolean(ARG_IN_STOCK, true);
        binding.rvProductImages.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvProductImages.setAdapter(imageAdapter);
        binding.rvProductImages.setHasFixedSize(true);
        binding.rvProductImages.setItemViewCacheSize(4);
        binding.rvProductImages.setItemAnimator(null);
        bindProduct();
        setupActions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupActions() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnCart.setOnClickListener(v -> navigateTo(new CartFragment()));
        binding.btnDecreaseQuantity.setOnClickListener(v -> {
            quantity = Math.max(1, quantity - 1);
            binding.tvQuantity.setText(String.valueOf(quantity));
        });
        binding.btnIncreaseQuantity.setOnClickListener(v -> {
            quantity += 1;
            binding.tvQuantity.setText(String.valueOf(quantity));
        });
        List<TextView> sizes = Arrays.asList(binding.sizeS, binding.sizeM, binding.sizeL, binding.sizeXl, binding.sizeXxl);
        List<String> availableSizes = detail.getAvailableSizes();
        for (int i = 0; i < sizes.size(); i++) {
            TextView sizeView = sizes.get(i);
            if (i >= availableSizes.size()) {
                sizeView.setVisibility(View.GONE);
                continue;
            }
            sizeView.setVisibility(View.VISIBLE);
            sizeView.setText(availableSizes.get(i));
            sizeView.setOnClickListener(v -> {
                selectedSize = ((TextView) v).getText().toString();
                renderSizes(sizes);
            });
        }
        binding.layoutSizeSelector.setVisibility(availableSizes.isEmpty() ? View.GONE : View.VISIBLE);
        renderSizes(sizes);
        binding.btnAddToCart.setOnClickListener(v -> {
            if (!inStock) {
                Toast.makeText(requireContext(), "Sản phẩm này đã hết hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            CartStore.add(detail, quantity, selectedSize);
            Toast.makeText(requireContext(), "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
        });
        binding.btnBuyNow.setOnClickListener(v -> {
            if (!inStock) {
                Toast.makeText(requireContext(), "Sản phẩm này đã hết hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            CartStore.add(detail, quantity, selectedSize);
            navigateTo(new CartFragment());
        });
    }

    private void bindProduct() {
        binding.progressProductDetail.setVisibility(View.GONE);
        binding.tvProductError.setVisibility(View.GONE);
        binding.tvProductTopTitle.setText("Cửa hàng chính hãng");
        binding.tvProductName.setText(detail.getName());
        binding.tvProductPrice.setText(detail.getPrice());
        binding.tvProductDescription.setText(detail.getDescription().isEmpty() ? "Không có mô tả từ nguồn sản phẩm." : detail.getDescription());
        binding.tvStockStatus.setVisibility(inStock ? View.GONE : View.VISIBLE);
        binding.layoutQuantity.setVisibility(inStock ? View.VISIBLE : View.GONE);
        binding.btnAddToCart.setEnabled(inStock);
        binding.btnBuyNow.setEnabled(inStock);
        binding.btnAddToCart.setText(inStock ? "THÊM VÀO GIỎ" : "HẾT HÀNG");
        binding.btnBuyNow.setText(inStock ? "MUA NGAY" : "HẾT HÀNG");
        binding.btnAddToCart.setAlpha(inStock ? 1f : 0.72f);
        binding.btnBuyNow.setAlpha(inStock ? 1f : 0.72f);
        imageAdapter.submitList(new java.util.ArrayList<>(detail.getImageUrls()));
    }

    private void renderSizes(List<TextView> sizes) {
        for (TextView sizeView : sizes) {
            boolean selected = selectedSize.equals(sizeView.getText().toString());
            sizeView.setBackgroundResource(selected ? R.drawable.bg_product_size_selected : R.drawable.bg_product_size_unselected);
            sizeView.setTextColor(requireContext().getColor(selected ? R.color.white : R.color.colorOnSurface));
            sizeView.setEnabled(inStock);
            sizeView.setAlpha(inStock ? 1f : 0.45f);
        }
        binding.tvSizeHint.setText(!inStock ? "Đã hết hàng" :
                (selectedSize.isEmpty() ? "Chưa có dữ liệu size" : "Đã chọn size " + selectedSize));
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName()).commit();
    }

    private static int priceValue(String priceText) {
        String digits = priceText == null ? "" : priceText.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
