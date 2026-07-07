package com.finalproject.v_league_ticket.presentation.shop;

import java.util.Objects;

public class CartItem {
    private final String productId;
    private final String productName;
    private final String priceText;
    private final int unitPrice;
    private final String imageUrl;
    private final String productUrl;
    private final String selectedSize;
    private final int quantity;

    public CartItem(String productId, String productName, String priceText, int unitPrice, String imageUrl,
                    String productUrl, String selectedSize, int quantity) {
        this.productId = productId == null ? "" : productId;
        this.productName = productName == null ? "" : productName;
        this.priceText = priceText == null ? "" : priceText;
        this.unitPrice = unitPrice;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.productUrl = productUrl == null ? "" : productUrl;
        this.selectedSize = selectedSize == null || selectedSize.isEmpty() ? "M" : selectedSize;
        this.quantity = Math.max(1, quantity);
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getPriceText() { return priceText; }
    public int getUnitPrice() { return unitPrice; }
    public String getImageUrl() { return imageUrl; }
    public String getProductUrl() { return productUrl; }
    public String getSelectedSize() { return selectedSize; }
    public int getQuantity() { return quantity; }
    public int getLineTotal() { return unitPrice * quantity; }

    public CartItem withQuantity(int nextQuantity) {
        return new CartItem(productId, productName, priceText, unitPrice, imageUrl, productUrl, selectedSize, nextQuantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem)) return false;
        CartItem cartItem = (CartItem) o;
        return unitPrice == cartItem.unitPrice && quantity == cartItem.quantity
                && Objects.equals(productId, cartItem.productId)
                && Objects.equals(productName, cartItem.productName)
                && Objects.equals(selectedSize, cartItem.selectedSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, productName, unitPrice, selectedSize, quantity);
    }
}
