package com.finalproject.v_league_ticket.presentation.profile;

import com.finalproject.v_league_ticket.presentation.admin.AdminSummaryStore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LegacyFirestoreCleanup {
    public static final String PENDING_PAYMENT = "pending_payment";
    private static final String[] LEGACY_STATUSES = new String[]{"paid_mock", "mock_paid", "mock_payment", "paid"};
    private static boolean normalizeAllStarted = false;

    private LegacyFirestoreCleanup() {
    }

    public static String normalizeOrderStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "paid_mock":
            case "mock_paid":
            case "mock_payment":
            case "paid":
                return PENDING_PAYMENT;
            default:
                return status == null ? "" : status.trim();
        }
    }

    public static void normalizeOrderDocument(String collection, DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return;
        String status = string(doc, "status", "orderStatus", "bookingStatus");
        String cleanStatus = normalizeOrderStatus(status);
        if (cleanStatus.equals(status)) return;
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", cleanStatus);
        patch.put("paymentStatus", "awaiting_admin_confirmation");
        patch.put("updatedAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection(collection).document(doc.getId()).set(patch, SetOptions.merge());
    }

    public static void normalizeTicketOrderEverywhere(DocumentSnapshot doc) {
        normalizeOrderDocument("orders", doc);
        normalizeOrderDocument("bookings", doc);
        normalizeOrderDocument("ticket_orders", doc);
    }

    public static synchronized void normalizeAllLegacyOrders() {
        if (normalizeAllStarted) return;
        normalizeAllStarted = true;
        normalizeCollection("orders");
        normalizeCollection("bookings");
        normalizeCollection("ticket_orders");
    }

    public static void deleteInvalidShopOrdersForUser(String uid, Runnable done) {
        if (uid == null || uid.trim().isEmpty()) {
            if (done != null) done.run();
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").whereEqualTo("userId", uid).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        if (done != null) done.run();
                        return;
                    }
                    java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger(1);
                    java.util.concurrent.atomic.AtomicBoolean deletedAny = new java.util.concurrent.atomic.AtomicBoolean(false);
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        if (!isInvalidLegacyShopOrder(doc)) continue;
                        deletedAny.set(true);
                        pending.incrementAndGet();
                        db.collection("orders").document(doc.getId()).delete()
                                .addOnCompleteListener(ignored -> finishInvalidOrderCleanup(pending, deletedAny, done));
                    }
                    finishInvalidOrderCleanup(pending, deletedAny, done);
                });
    }

    public static boolean isInvalidLegacyShopOrder(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return false;
        if ("ticket".equalsIgnoreCase(string(doc, "type"))) return false;
        Object raw = doc.get("items");
        if (raw instanceof List<?>) return !hasUsableItem((List<?>) raw);
        return !hasTopLevelProductSignal(doc);
    }

    private static void finishInvalidOrderCleanup(java.util.concurrent.atomic.AtomicInteger pending,
                                                  java.util.concurrent.atomic.AtomicBoolean deletedAny,
                                                  Runnable done) {
        if (pending.decrementAndGet() != 0) return;
        if (deletedAny.get()) AdminSummaryStore.invalidateOrders();
        if (done != null) done.run();
    }

    private static boolean hasUsableItem(List<?> items) {
        for (Object row : items) {
            if (!(row instanceof Map<?, ?>)) continue;
            Map<?, ?> item = (Map<?, ?>) row;
            String productId = string(item.get("productId"));
            String sku = string(item.get("sku"));
            String name = firstString(item, "name", "productName", "title");
            boolean genericPlaceholder = name.toLowerCase(Locale.ROOT).matches("s.*n ph.*m\\s*\\d+")
                    || name.toLowerCase(Locale.ROOT).matches("product\\s*\\d+");
            boolean hasTicketSeat = !string(item.get("seatNumber")).isEmpty();
            if (!productId.isEmpty() || !sku.isEmpty() || hasTicketSeat) return true;
            if (!name.isEmpty() && !genericPlaceholder) return true;
        }
        return false;
    }

    private static boolean hasTopLevelProductSignal(DocumentSnapshot doc) {
        return !string(doc, "productId", "sku", "productName", "productTitle").isEmpty();
    }

    private static void normalizeCollection(String collection) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (String status : LEGACY_STATUSES) {
            db.collection(collection).whereEqualTo("status", status).get()
                    .addOnSuccessListener(snapshot -> {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            normalizeOrderDocument(collection, doc);
                        }
                    });
        }
    }

    private static String string(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            String text = string(value);
            if (!text.isEmpty()) return text;
        }
        return "";
    }

    private static String firstString(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            String text = string(map.get(key));
            if (!text.isEmpty()) return text;
        }
        return "";
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
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
}
