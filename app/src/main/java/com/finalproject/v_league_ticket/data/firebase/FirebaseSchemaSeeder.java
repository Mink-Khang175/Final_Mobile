package com.finalproject.v_league_ticket.data.firebase;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class FirebaseSchemaSeeder {
    private FirebaseSchemaSeeder() {
    }

    public static void ensureBaseSchemas() {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            cleanupLegacyCollections(db);
            merge(db, "app_settings", "main", appSettings());
            merge(db, "home_config", "main", homeConfig());
            merge(db, "backup_seasons", "_schema", schema("backup_seasons",
                    "backupKey", "provider", "items", "itemCount", "updatedAt"));
            merge(db, "backup_fixtures", "_schema", schema("backup_fixtures",
                    "backupKey", "provider", "items", "itemCount", "updatedAt"));
            merge(db, "backup_standings", "_schema", schema("backup_standings",
                    "backupKey", "provider", "items", "itemCount", "updatedAt"));
            merge(db, "backup_news", "_schema", schema("backup_news",
                    "backupKey", "provider", "items", "itemCount", "updatedAt"));
            merge(db, "backup_shop_products", "_schema", schema("backup_shop_products",
                    "backupKey", "provider", "items", "itemCount", "updatedAt"));
            merge(db, "backup_match_details", "_schema", schema("backup_match_details",
                    "backupKey", "provider", "items", "itemCount", "updatedAt"));
            merge(db, "backup_team_details", "_schema", schema("backup_team_details",
                    "backupKey", "provider", "items", "itemCount", "updatedAt"));
            merge(db, "sync_status", "_schema", schema("sync_status",
                    "success", "count", "message", "updatedAt"));
            merge(db, "app_banners", "_schema", schema("app_banners",
                    "title", "subtitle", "imageUrl", "targetType", "targetId", "active", "displayOrder"));
            merge(db, "club_profiles", "_schema", schema("club_profiles",
                    "sofaTeamId", "name", "shortName", "coach", "stadium", "foundedYear", "logoUrl", "heroUrl", "overview"));
            merge(db, "notifications", "_schema", schema("notifications",
                    "title", "body", "type", "targetUserId", "targetRole", "deeplink", "readBy", "createdAt"));
            merge(db, "rewards", "_schema", schema("rewards",
                    "code", "name", "description", "pointsRequired", "active", "startAt", "endAt"));
            merge(db, "badges", "_schema", schema("badges",
                    "code", "name", "description", "iconUrl", "ruleType", "ruleValue", "active"));
            merge(db, "vouchers", "_schema", schema("vouchers",
                    "code", "discountType", "discountValue", "minOrderValue", "usageLimit", "active", "startAt", "endAt"));
            merge(db, "predictions", "_schema", schema("predictions",
                    "matchId", "seasonId", "round", "homeTeam", "awayTeam", "lockedAt", "status", "result"));
            merge(db, "user_rewards", "_schema", schema("user_rewards",
                    "userId", "points", "lifetimePoints", "updatedAt"));
            merge(db, "user_badges", "_schema", schema("user_badges",
                    "userId", "badgeId", "earnedAt", "source"));
            merge(db, "user_vouchers", "_schema", schema("user_vouchers",
                    "userId", "voucherId", "code", "status", "claimedAt", "usedAt"));
            merge(db, "user_predictions", "_schema", schema("user_predictions",
                    "userId", "predictionId", "homeScore", "awayScore", "pointsAwarded", "createdAt"));
        } catch (Exception ignored) {
            // Firebase schema checks should never block the app startup.
        }
    }

    private static void cleanupLegacyCollections(FirebaseFirestore db) {
        deleteSome(db, "api_cache");
        deleteSome(db, "club_team_cache");
        deleteSome(db, "sync_logs");
    }

    private static void deleteSome(FirebaseFirestore db, String collection) {
        db.collection(collection).limit(100).get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) return;
            WriteBatch batch = db.batch();
            for (QueryDocumentSnapshot document : snapshot) {
                batch.delete(document.getReference());
            }
            batch.commit();
        });
    }

    private static Map<String, Object> appSettings() {
        Map<String, Object> data = new HashMap<>();
        data.put("currentSeason", "25/26");
        data.put("currentSeasonId", 78589);
        data.put("shopEnabled", true);
        data.put("ordersEnabled", true);
        data.put("bannerEnabled", true);
        data.put("shippingFee", 0);
        data.put("supportEmail", "");
        data.put("hotline", "");
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("schemaVersion", 1);
        return data;
    }

    private static Map<String, Object> homeConfig() {
        Map<String, Object> sections = new HashMap<>();
        sections.put("featuredMatch", true);
        sections.put("fixtures", true);
        sections.put("news", true);
        sections.put("shop", true);

        Map<String, Object> data = new HashMap<>();
        data.put("slogan", "");
        data.put("featuredMatchId", "");
        data.put("featuredProductIds", Arrays.asList());
        data.put("sections", sections);
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("schemaVersion", 1);
        return data;
    }

    private static Map<String, Object> schema(String collection, String... fields) {
        Map<String, Object> data = new HashMap<>();
        data.put("collection", collection);
        data.put("fields", Arrays.asList(fields));
        data.put("schemaVersion", 1);
        data.put("updatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private static void merge(FirebaseFirestore db, String collection, String document, Map<String, Object> data) {
        db.collection(collection).document(document).set(data, SetOptions.merge());
    }
}
