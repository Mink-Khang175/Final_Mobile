package com.finalproject.v_league_ticket.presentation.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ShopProduct {
    private final String id;
    private final String name;
    private final String price;
    private final int priceValue;
    private final String imageUrl;
    private final List<String> imageUrls;
    private final List<String> availableSizes;
    private final String productUrl;
    private final String clubName;
    private final String category;
    private final String description;
    private final boolean inStock;

    public ShopProduct(String id, String name, String price, String imageUrl, String productUrl, String clubName) {
        this(id, name, price, priceValue(price), imageUrl, imageUrl == null || imageUrl.isEmpty()
                ? Collections.emptyList()
                : Collections.singletonList(imageUrl), Collections.emptyList(), productUrl, clubName, "", true);
    }

    public ShopProduct(String id, String name, String price, int priceValue, String imageUrl, List<String> imageUrls,
                       String productUrl, String clubName, String description) {
        this(id, name, price, priceValue, imageUrl, imageUrls, Collections.emptyList(), productUrl, clubName, description, true);
    }

    public ShopProduct(String id, String name, String price, int priceValue, String imageUrl, List<String> imageUrls,
                       List<String> availableSizes, String productUrl, String clubName, String description) {
        this(id, name, price, priceValue, imageUrl, imageUrls, availableSizes, productUrl, clubName, description, true);
    }

    public ShopProduct(String id, String name, String price, int priceValue, String imageUrl, List<String> imageUrls,
                       List<String> availableSizes, String productUrl, String clubName, String description,
                       boolean inStock) {
        this(id, name, price, priceValue, imageUrl, imageUrls, availableSizes, productUrl, clubName, clubName,
                description, inStock);
    }

    public ShopProduct(String id, String name, String price, int priceValue, String imageUrl, List<String> imageUrls,
                       List<String> availableSizes, String productUrl, String clubName, String category,
                       String description, boolean inStock) {
        this.id = safe(id);
        this.name = safe(name);
        this.price = safe(price);
        this.priceValue = priceValue;
        this.imageUrl = safe(imageUrl);
        this.imageUrls = imageUrls == null ? Collections.emptyList() : new ArrayList<>(imageUrls);
        this.availableSizes = availableSizes == null ? Collections.emptyList() : new ArrayList<>(availableSizes);
        this.productUrl = safe(productUrl);
        this.clubName = safe(clubName);
        this.category = safe(category);
        this.description = safe(description);
        this.inStock = inStock;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public int getPriceValue() { return priceValue; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getImageUrls() { return Collections.unmodifiableList(imageUrls); }
    public List<String> getAvailableSizes() { return Collections.unmodifiableList(availableSizes); }
    public String getProductUrl() { return productUrl; }
    public String getClubName() { return clubName; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public boolean isInStock() { return inStock; }

    private static String safe(String value) { return value == null ? "" : value; }

    private static int priceValue(String priceText) {
        String digits = priceText == null ? "" : priceText.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShopProduct)) return false;
        ShopProduct that = (ShopProduct) o;
        return priceValue == that.priceValue && Objects.equals(id, that.id) && Objects.equals(name, that.name)
                && Objects.equals(price, that.price) && Objects.equals(imageUrl, that.imageUrl)
                && Objects.equals(imageUrls, that.imageUrls) && Objects.equals(availableSizes, that.availableSizes)
                && Objects.equals(productUrl, that.productUrl)
                && Objects.equals(clubName, that.clubName) && Objects.equals(category, that.category)
                && Objects.equals(description, that.description)
                && inStock == that.inStock;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, priceValue, imageUrl, imageUrls, availableSizes,
                productUrl, clubName, category, description, inStock);
    }
}
