package com.finalproject.v_league_ticket.presentation.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductDetail {
    private final String id;
    private final String name;
    private final String price;
    private final int priceValue;
    private final List<String> imageUrls;
    private final List<String> availableSizes;
    private final String productUrl;
    private final String description;

    public ProductDetail(String id, String name, String price, int priceValue, List<String> imageUrls,
                         String productUrl, String description) {
        this(id, name, price, priceValue, imageUrls, Collections.emptyList(), productUrl, description);
    }

    public ProductDetail(String id, String name, String price, int priceValue, List<String> imageUrls,
                         List<String> availableSizes, String productUrl, String description) {
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
        this.price = price == null ? "" : price;
        this.priceValue = priceValue;
        this.imageUrls = imageUrls == null ? Collections.emptyList() : new ArrayList<>(imageUrls);
        this.availableSizes = availableSizes == null ? Collections.emptyList() : new ArrayList<>(availableSizes);
        this.productUrl = productUrl == null ? "" : productUrl;
        this.description = description == null ? "" : description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public int getPriceValue() { return priceValue; }
    public List<String> getImageUrls() { return Collections.unmodifiableList(imageUrls); }
    public List<String> getAvailableSizes() { return Collections.unmodifiableList(availableSizes); }
    public String getProductUrl() { return productUrl; }
    public String getDescription() { return description; }
}
