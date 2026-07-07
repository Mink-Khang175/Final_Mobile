package com.finalproject.v_league_ticket.presentation.shop;

import java.util.Objects;

public class ShopClub {
    private final String name;
    private final String imageUrl;
    private final String pagePath;

    public ShopClub(String name, String imageUrl, String pagePath) {
        this.name = name == null ? "" : name;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.pagePath = pagePath == null ? "" : pagePath;
    }

    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public String getPagePath() { return pagePath; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShopClub)) return false;
        ShopClub shopClub = (ShopClub) o;
        return Objects.equals(name, shopClub.name)
                && Objects.equals(imageUrl, shopClub.imageUrl)
                && Objects.equals(pagePath, shopClub.pagePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, imageUrl, pagePath);
    }
}
