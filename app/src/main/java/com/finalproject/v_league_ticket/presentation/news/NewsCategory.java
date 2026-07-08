package com.finalproject.v_league_ticket.presentation.news;

public enum NewsCategory {
    VLEAGUE("V-League", 44),
    HANG_NHAT("Hạng Nhất", 45),
    CUP_QUOC_GIA("Cúp Quốc gia", 46),
    VPF("VPF", 109),
    BONG_DA_VN("Bóng đá Việt Nam", 129);

    private final String label;
    private final int categoryId;

    NewsCategory(String label, int categoryId) {
        this.label = label;
        this.categoryId = categoryId;
    }

    public String getLabel() {
        return label;
    }

    public int getCategoryId() {
        return categoryId;
    }
}
