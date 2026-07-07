package com.finalproject.v_league_ticket.presentation.profile;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LegacyFirestoreCleanup {
    public static final String PENDING_PAYMENT = "pending_payment";
    private static final String[] LEGACY_STATUSES = new String[]{"paid_mock", "mock_paid", "mock_payment", "paid"};

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

    public static void normalizeAllLegacyOrders() {
        normalizeCollection("orders");
        normalizeCollection("bookings");
        normalizeCollection("ticket_orders");
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
            if (value instanceof String && !((String) value).trim().isEmpty()) return ((String) value).trim();
        }
        return "";
    }
}
