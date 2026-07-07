package com.finalproject.v_league_ticket.presentation.profile;

import com.finalproject.v_league_ticket.R;

import java.util.Objects;

public class ProfileHistoryItem {
    private final String id;
    private final String primary;
    private final String secondary;
    private final String amount;
    private final int iconRes;
    private final int iconBackgroundRes;
    private final int iconTintColorRes;
    private final int cardBackgroundRes;
    private final int amountColorRes;

    public ProfileHistoryItem(String id, String primary, String secondary, String amount) {
        this(id, primary, secondary, amount,
                R.drawable.ic_badge_24,
                R.drawable.bg_profile_icon_chip_gold,
                R.color.premium_gold,
                R.drawable.bg_profile_panel_dark,
                R.color.red_energy);
    }

    public ProfileHistoryItem(String id, String primary, String secondary, String amount,
                              int iconRes, int iconBackgroundRes, int iconTintColorRes,
                              int cardBackgroundRes, int amountColorRes) {
        this.id = id == null ? "" : id;
        this.primary = primary == null ? "" : primary;
        this.secondary = secondary == null ? "" : secondary;
        this.amount = amount == null ? "" : amount;
        this.iconRes = iconRes;
        this.iconBackgroundRes = iconBackgroundRes;
        this.iconTintColorRes = iconTintColorRes;
        this.cardBackgroundRes = cardBackgroundRes;
        this.amountColorRes = amountColorRes;
    }

    public String getId() { return id; }
    public String getPrimary() { return primary; }
    public String getSecondary() { return secondary; }
    public String getAmount() { return amount; }
    public int getIconRes() { return iconRes; }
    public int getIconBackgroundRes() { return iconBackgroundRes; }
    public int getIconTintColorRes() { return iconTintColorRes; }
    public int getCardBackgroundRes() { return cardBackgroundRes; }
    public int getAmountColorRes() { return amountColorRes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProfileHistoryItem)) return false;
        ProfileHistoryItem that = (ProfileHistoryItem) o;
        return Objects.equals(id, that.id) && Objects.equals(primary, that.primary)
                && Objects.equals(secondary, that.secondary) && Objects.equals(amount, that.amount)
                && iconRes == that.iconRes
                && iconBackgroundRes == that.iconBackgroundRes
                && iconTintColorRes == that.iconTintColorRes
                && cardBackgroundRes == that.cardBackgroundRes
                && amountColorRes == that.amountColorRes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, primary, secondary, amount, iconRes, iconBackgroundRes,
                iconTintColorRes, cardBackgroundRes, amountColorRes);
    }
}
