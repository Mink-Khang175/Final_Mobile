package com.finalproject.v_league_ticket.presentation.profile;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public final class UserEngagementManager {
    public static final int SIGNUP_POINTS = 100;
    public static final int TICKET_POINTS = 40;
    public static final int PREDICTION_POINTS = 80;

    private UserEngagementManager() {
    }

    public static void awardSignupBonus(String uid) {
        awardPoints(uid, SIGNUP_POINTS, "signup", "Chào mừng thành viên mới", uid);
    }

    public static void awardTicketPurchase(String uid, String orderId) {
        awardPoints(uid, TICKET_POINTS, "ticket_purchase", "Đặt vé thành công", orderId);
    }

    public static void awardShopPurchase(String uid, String orderId, int totalAmount) {
        int points = Math.max(20, totalAmount / 10000);
        awardPoints(uid, points, "shop_purchase", "Mua hàng V.League Shop", orderId);
    }

    public static void awardPrediction(String uid, String predictionId) {
        awardPoints(uid, PREDICTION_POINTS, "prediction_correct", "Dự đoán đúng tỉ số", predictionId);
    }

    public static void awardPoints(String uid, int points, String type, String title, String refId) {
        if (uid == null || uid.isEmpty() || points <= 0) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> loyalty = new HashMap<>();
        loyalty.put("userId", uid);
        loyalty.put("points", FieldValue.increment(points));
        loyalty.put("lifetimePoints", FieldValue.increment(points));
        loyalty.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("user_loyalty").document(uid).set(loyalty, SetOptions.merge());

        Map<String, Object> userPatch = new HashMap<>();
        userPatch.put("points", FieldValue.increment(points));
        userPatch.put("lifetimePoints", FieldValue.increment(points));
        userPatch.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("users").document(uid).set(userPatch, SetOptions.merge());

        Map<String, Object> log = new HashMap<>();
        log.put("userId", uid);
        log.put("points", points);
        log.put("type", type);
        log.put("title", title);
        log.put("refId", refId == null ? "" : refId);
        log.put("createdAt", FieldValue.serverTimestamp());
        db.collection("loyalty_transactions")
                .document(type + "_" + uid + "_" + System.currentTimeMillis())
                .set(log);
    }

    public static void notifyUser(String uid, String title, String body, String type, String refId) {
        if (uid == null || uid.isEmpty()) return;
        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("title", title);
        data.put("body", body);
        data.put("type", type);
        data.put("refId", refId == null ? "" : refId);
        data.put("status", "unread");
        data.put("createdAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("notifications")
                .document("notification_" + uid + "_" + System.currentTimeMillis())
                .set(data);
    }
}
