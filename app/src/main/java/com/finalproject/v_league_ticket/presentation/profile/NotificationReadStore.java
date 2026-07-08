package com.finalproject.v_league_ticket.presentation.profile;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class NotificationReadStore {
    public interface UnreadCountCallback {
        void onResult(int count);
    }

    private NotificationReadStore() {
    }

    public static void loadUnreadCount(String uid, UnreadCountCallback callback) {
        if (uid == null || uid.isEmpty()) {
            if (callback != null) callback.onResult(0);
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Set<String> ids = new HashSet<>();
        java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger(2);
        collectUnreadIds(db.collection("notifications").whereEqualTo("userId", uid), uid, ids, pending, callback);
        collectUnreadIds(db.collection("notifications")
                .whereIn("targetRole", java.util.Arrays.asList("all", "user", "người dùng")), uid, ids, pending, callback);
    }

    private static void collectUnreadIds(Query query, String uid, Set<String> ids,
                                         java.util.concurrent.atomic.AtomicInteger pending,
                                         UnreadCountCallback callback) {
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    if (isUnread(doc, uid)) ids.add(doc.getId());
                }
            }
            if (pending.decrementAndGet() == 0 && callback != null) callback.onResult(ids.size());
        });
    }

    public static boolean isUnread(DocumentSnapshot doc, String uid) {
        if (doc == null || !doc.exists() || uid == null || uid.isEmpty()) return false;
        Object readBy = doc.get("readBy");
        if (readBy instanceof Map<?, ?> && Boolean.TRUE.equals(((Map<?, ?>) readBy).get(uid))) return false;
        if (Boolean.TRUE.equals(doc.getBoolean("read"))) return false;
        if (doc.get("readAt") instanceof Timestamp) return false;
        String status = string(doc.get("status")).toLowerCase();
        return status.isEmpty() || "unread".equals(status) || "new".equals(status) || "moi".equals(status);
    }

    public static void markRead(DocumentSnapshot doc, String uid) {
        if (doc == null || !doc.exists() || uid == null || uid.isEmpty() || !isUnread(doc, uid)) return;
        Map<String, Object> patch = new HashMap<>();
        patch.put("readBy." + uid, true);
        patch.put("readAtBy." + uid, FieldValue.serverTimestamp());
        if (uid.equals(string(doc.get("userId")))) {
            patch.put("status", "read");
            patch.put("read", true);
            patch.put("readAt", FieldValue.serverTimestamp());
        }
        FirebaseFirestore.getInstance().collection("notifications")
                .document(doc.getId())
                .set(patch, SetOptions.merge());
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
