package com.finalproject.v_league_ticket.presentation.news;

public enum NewsCategory {
    VLEAGUE("V-League", 44),
    HANG_NHAT("Hang Nhat", 45),
    CUP_QUOC_GIA("Cup QG", 46),
    VPF("VPF", 109),
    BONG_DA_VN("Bong da VN", 129);

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
