package com.finalproject.v_league_ticket.presentation.shop;

import android.content.Context;
import android.content.SharedPreferences;

import com.finalproject.v_league_ticket.di.AppContainer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class CartStore {
    public interface Listener {
        void onCartChanged(List<CartItem> items);
    }

    private static final List<CartItem> ITEMS = new ArrayList<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();
    private static final String PREFS_NAME = "cart_store";
    private static final String KEY_ITEMS = "items";
    private static boolean loaded;

    private CartStore() {
    }

    public static void add(ProductDetail detail, int quantity, String selectedSize) {
        ensureLoaded();
        int safeQuantity = Math.max(1, quantity);
        String safeSize = selectedSize == null || selectedSize.isEmpty() ? "M" : selectedSize;
        for (int i = 0; i < ITEMS.size(); i++) {
            CartItem item = ITEMS.get(i);
            if (item.getProductId().equals(detail.getId()) && item.getSelectedSize().equals(safeSize)) {
                ITEMS.set(i, item.withQuantity(item.getQuantity() + safeQuantity));
                persist();
                notifyChanged();
                return;
            }
        }
        String image = detail.getImageUrls().isEmpty() ? "" : detail.getImageUrls().get(0);
        ITEMS.add(new CartItem(detail.getId(), detail.getName(), detail.getPrice(), detail.getPriceValue(),
                image, detail.getProductUrl(), safeSize, safeQuantity));
        persist();
        notifyChanged();
    }

    public static void updateQuantity(String productId, String selectedSize, int quantity) {
        ensureLoaded();
        for (int i = 0; i < ITEMS.size(); i++) {
            CartItem item = ITEMS.get(i);
            if (item.getProductId().equals(productId) && item.getSelectedSize().equals(selectedSize)) {
                if (quantity <= 0) ITEMS.remove(i);
                else ITEMS.set(i, item.withQuantity(quantity));
                persist();
                notifyChanged();
                return;
            }
        }
    }

    public static List<CartItem> items() {
        ensureLoaded();
        return Collections.unmodifiableList(new ArrayList<>(ITEMS));
    }

    public static int subtotal() {
        ensureLoaded();
        int total = 0;
        for (CartItem item : ITEMS) total += item.getLineTotal();
        return total;
    }

    public static void clear() {
        ensureLoaded();
        ITEMS.clear();
        persist();
        notifyChanged();
    }

    public static void addListener(Listener listener) {
        ensureLoaded();
        if (!LISTENERS.contains(listener)) LISTENERS.add(listener);
        listener.onCartChanged(items());
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    private static void notifyChanged() {
        List<CartItem> snapshot = items();
        for (Listener listener : new ArrayList<>(LISTENERS)) listener.onCartChanged(snapshot);
    }

    public static String formatVnd(int value) {
        if (value <= 0) return "0d";
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value) + "d";
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Context context = AppContainer.appContext();
        if (context == null) return;
        String raw = prefs(context).getString(KEY_ITEMS, "");
        if (raw == null || raw.trim().isEmpty()) return;
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonArray()) return;
            ITEMS.clear();
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                ITEMS.add(new CartItem(
                        string(object, "productId"),
                        string(object, "productName"),
                        string(object, "priceText"),
                        intValue(object, "unitPrice"),
                        string(object, "imageUrl"),
                        string(object, "productUrl"),
                        string(object, "selectedSize"),
                        Math.max(1, intValue(object, "quantity"))
                ));
            }
        } catch (Exception ignored) {
            ITEMS.clear();
        }
    }

    private static void persist() {
        Context context = AppContainer.appContext();
        if (context == null) return;
        JsonArray array = new JsonArray();
        for (CartItem item : ITEMS) {
            JsonObject object = new JsonObject();
            object.addProperty("productId", item.getProductId());
            object.addProperty("productName", item.getProductName());
            object.addProperty("priceText", item.getPriceText());
            object.addProperty("unitPrice", item.getUnitPrice());
            object.addProperty("imageUrl", item.getImageUrl());
            object.addProperty("productUrl", item.getProductUrl());
            object.addProperty("selectedSize", item.getSelectedSize());
            object.addProperty("quantity", item.getQuantity());
            array.add(object);
        }
        prefs(context).edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String string(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int intValue(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) return 0;
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }
}
