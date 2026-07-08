package com.finalproject.v_league_ticket.presentation.admin;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public final class AdminSummaryStore {
    public static final String COLLECTION = "admin_summaries";
    public static final String DOC_ORDERS = "orders";
    public static final String DOC_PRODUCTS = "products";

    private AdminSummaryStore() {
    }

    public static DocumentReference ordersRef() {
        return FirebaseFirestore.getInstance().collection(COLLECTION).document(DOC_ORDERS);
    }

    public static DocumentReference productsRef() {
        return FirebaseFirestore.getInstance().collection(COLLECTION).document(DOC_PRODUCTS);
    }

    public static void seedOrders(int count, int revenue, int pending, int deliveryQueue) {
        Map<String, Object> data = new HashMap<>();
        data.put("totalOrders", count);
        data.put("totalRevenue", revenue);
        data.put("pendingOrders", pending);
        data.put("deliveryQueue", deliveryQueue);
        data.put("complete", true);
        data.put("updatedAt", FieldValue.serverTimestamp());
        ordersRef().set(data, SetOptions.merge());
    }

    public static void recordOrderCreated(Map<String, Object> orderData) {
        if (orderData == null) return;
        ordersRef().get().addOnSuccessListener(snapshot -> {
            if (snapshot == null || !snapshot.exists() || !Boolean.TRUE.equals(snapshot.getBoolean("complete"))) return;
            String status = string(orderData.get("status")).toLowerCase();
            boolean ticket = "ticket".equalsIgnoreCase(string(orderData.get("type")));
            Map<String, Object> data = new HashMap<>();
            data.put("totalOrders", FieldValue.increment(1));
            data.put("totalRevenue", FieldValue.increment(orderTotal(orderData)));
            if (status.contains("pending")) data.put("pendingOrders", FieldValue.increment(1));
            if (!ticket && ("confirmed".equals(status) || "shipping".equals(status))) {
                data.put("deliveryQueue", FieldValue.increment(1));
            }
            data.put("updatedAt", FieldValue.serverTimestamp());
            ordersRef().set(data, SetOptions.merge());
        });
    }

    public static void seedProducts(int visibleCount, Map<String, Integer> clubCounts) {
        Map<String, Object> data = new HashMap<>();
        data.put("visibleCount", visibleCount);
        data.put("clubCounts", clubCounts == null ? new HashMap<>() : new HashMap<>(clubCounts));
        data.put("complete", true);
        data.put("updatedAt", FieldValue.serverTimestamp());
        productsRef().set(data, SetOptions.merge());
    }

    public static void invalidateOrders() {
        ordersRef().delete();
    }

    public static void invalidateProducts() {
        productsRef().delete();
    }

    private static int orderTotal(Map<String, Object> orderData) {
        int total = intValue(orderData.get("totalAmount"));
        if (total <= 0) total = intValue(orderData.get("grandTotal"));
        if (total <= 0) total = intValue(orderData.get("total"));
        return Math.max(0, total);
    }

    private static int intValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value == null) return 0;
        try {
            return Integer.parseInt(String.valueOf(value).replaceAll("[^0-9-]", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
