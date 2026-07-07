package com.finalproject.v_league_ticket.data.firebase;

import com.finalproject.v_league_ticket.data.remote.VLeagueApiClient;
import com.finalproject.v_league_ticket.domain.model.Fixture;
import com.finalproject.v_league_ticket.domain.model.NewsPost;
import com.finalproject.v_league_ticket.domain.model.Standing;
import com.finalproject.v_league_ticket.domain.model.VLeagueSeason;
import com.finalproject.v_league_ticket.presentation.shop.ShopProduct;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FirebaseWebBackup {
    private FirebaseWebBackup() {
    }

    public static void saveSeasons(String key, List<VLeagueSeason> seasons) {
        saveItems(collectionForKey(key), key, toSeasonMaps(seasons));
    }

    public static void loadSeasons(String key, VLeagueApiClient.DataCallback<List<VLeagueSeason>> callback) {
        loadItems(collectionForKey(key), key, maps -> {
            List<VLeagueSeason> rows = new ArrayList<>();
            for (Map<String, Object> map : maps) rows.add(seasonFromMap(map));
            callback.onSuccess(rows);
        }, callback);
    }

    public static void saveFixtures(String key, List<Fixture> fixtures) {
        saveItems(collectionForKey(key), key, toFixtureMaps(fixtures));
    }

    public static void loadFixtures(String key, VLeagueApiClient.DataCallback<List<Fixture>> callback) {
        loadItems(collectionForKey(key), key, maps -> {
            List<Fixture> rows = new ArrayList<>();
            for (Map<String, Object> map : maps) rows.add(fixtureFromMap(map));
            callback.onSuccess(rows);
        }, callback);
    }

    public static void saveStandings(String key, List<Standing> standings) {
        saveItems(collectionForKey(key), key, toStandingMaps(standings));
    }

    public static void loadStandings(String key, VLeagueApiClient.DataCallback<List<Standing>> callback) {
        loadItems(collectionForKey(key), key, maps -> {
            List<Standing> rows = new ArrayList<>();
            for (Map<String, Object> map : maps) rows.add(standingFromMap(map));
            callback.onSuccess(rows);
        }, callback);
    }

    public static void saveNews(String key, List<NewsPost> posts) {
        saveItems(collectionForKey(key), key, toNewsMaps(posts));
    }

    public static void loadNews(String key, VLeagueApiClient.DataCallback<List<NewsPost>> callback) {
        loadItems(collectionForKey(key), key, maps -> {
            List<NewsPost> rows = new ArrayList<>();
            for (Map<String, Object> map : maps) rows.add(newsFromMap(map));
            callback.onSuccess(rows);
        }, callback);
    }

    public static void saveShopProducts(String key, List<ShopProduct> products) {
        saveItems(collectionForKey(key), key, toShopMaps(products));
    }

    public static void loadShopProducts(String key, VLeagueApiClient.DataCallback<List<ShopProduct>> callback) {
        loadItems(collectionForKey(key), key, maps -> {
            List<ShopProduct> rows = new ArrayList<>();
            for (Map<String, Object> map : maps) rows.add(shopFromMap(map));
            callback.onSuccess(rows);
        }, callback);
    }

    public static void saveStrings(String key, List<String> rows) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (rows != null) {
            for (String row : rows) {
                Map<String, Object> map = new HashMap<>();
                map.put("value", row == null ? "" : row);
                items.add(map);
            }
        }
        saveItems(collectionForKey(key), key, items);
    }

    public static void loadStrings(String key, VLeagueApiClient.DataCallback<List<String>> callback) {
        loadItems(collectionForKey(key), key, maps -> {
            List<String> rows = new ArrayList<>();
            for (Map<String, Object> map : maps) rows.add(str(map, "value"));
            callback.onSuccess(rows);
        }, callback);
    }

    private interface MapLoader {
        void onLoaded(List<Map<String, Object>> maps);
    }

    private static String collectionForKey(String key) {
        if (key == null) return "web_backups";
        if (key.startsWith("sofascore_seasons")) return "backup_seasons";
        if (key.startsWith("sofascore_finished_")
                || key.startsWith("sofascore_upcoming_")
                || key.startsWith("sofascore_round_")) return "backup_fixtures";
        if (key.startsWith("sofascore_standings_")) return "backup_standings";
        if (key.startsWith("vpf_news_")) return "backup_news";
        if (key.startsWith("shop_")) return "backup_shop_products";
        if (key.startsWith("sofascore_event_")) return "backup_match_details";
        if (key.startsWith("sofascore_team_")) return "backup_team_details";
        return "web_backups";
    }

    private static void saveItems(String collection, String key, List<Map<String, Object>> items) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("backupKey", key);
            data.put("items", items == null ? new ArrayList<>() : items);
            data.put("itemCount", items == null ? 0 : items.size());
            data.put("provider", providerForKey(key));
            data.put("updatedAt", FieldValue.serverTimestamp());
            FirebaseFirestore.getInstance().collection(collection).document(key).set(data, SetOptions.merge());
        } catch (Exception ignored) {
            // Backup writes are best-effort and must not block live API rendering.
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void loadItems(String collection, String key, MapLoader loader, VLeagueApiClient.DataCallback<T> callback) {
        FirebaseFirestore.getInstance().collection(collection).document(key).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError(new IOException("No Firebase backup for " + key));
                        return;
                    }
                    Object value = document.get("items");
                    if (!(value instanceof List<?>)) {
                        callback.onError(new IOException("Invalid Firebase backup for " + key));
                        return;
                    }
                    List<Map<String, Object>> maps = new ArrayList<>();
                    for (Object row : (List<?>) value) {
                        if (row instanceof Map<?, ?>) maps.add((Map<String, Object>) row);
                    }
                    if (maps.isEmpty()) {
                        callback.onError(new IOException("Empty Firebase backup for " + key));
                        return;
                    }
                    loader.onLoaded(maps);
                })
                .addOnFailureListener(callback::onError);
    }

    private static String providerForKey(String key) {
        if (key == null) return "";
        if (key.startsWith("sofascore_")) return "SofaScore";
        if (key.startsWith("vpf_news_")) return "VPF";
        if (key.startsWith("shop_neymarsport")) return "NeymarSport";
        if (key.startsWith("shop_")) return "1stFootballStore";
        return "Web";
    }

    private static List<Map<String, Object>> toSeasonMaps(List<VLeagueSeason> seasons) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (seasons == null) return rows;
        for (VLeagueSeason season : seasons) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", season.getId());
            map.put("label", season.getLabel());
            map.put("sportsDbSeason", season.getSportsDbSeason());
            map.put("maxRound", season.getMaxRound());
            map.put("hasFinalRound", season.hasFinalRound());
            rows.add(map);
        }
        return rows;
    }

    private static VLeagueSeason seasonFromMap(Map<String, Object> map) {
        return new VLeagueSeason(
                intValue(map, "id", 0),
                str(map, "label"),
                str(map, "sportsDbSeason"),
                intValue(map, "maxRound", 26),
                boolValue(map, "hasFinalRound", false)
        );
    }

    private static List<Map<String, Object>> toFixtureMaps(List<Fixture> fixtures) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (fixtures == null) return rows;
        for (Fixture item : fixtures) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("homeTeamId", item.getHomeTeamId());
            map.put("awayTeamId", item.getAwayTeamId());
            map.put("homeTeamName", item.getHomeTeamName());
            map.put("awayTeamName", item.getAwayTeamName());
            map.put("homeScore", item.getHomeScore());
            map.put("awayScore", item.getAwayScore());
            map.put("homePenaltyScore", item.getHomePenaltyScore());
            map.put("awayPenaltyScore", item.getAwayPenaltyScore());
            map.put("status", item.getStatus());
            map.put("statusType", item.getStatusType());
            map.put("startTimestamp", item.getStartTimestamp());
            map.put("venue", item.getVenue());
            map.put("round", item.getRound());
            map.put("roundName", item.getRoundName());
            map.put("homeLogoUrl", item.getHomeLogoUrl());
            map.put("awayLogoUrl", item.getAwayLogoUrl());
            rows.add(map);
        }
        return rows;
    }

    private static Fixture fixtureFromMap(Map<String, Object> map) {
        return new Fixture(
                longValue(map, "id", 0L),
                nullableLong(map, "homeTeamId"),
                nullableLong(map, "awayTeamId"),
                str(map, "homeTeamName"),
                str(map, "awayTeamName"),
                nullableInt(map, "homeScore"),
                nullableInt(map, "awayScore"),
                nullableInt(map, "homePenaltyScore"),
                nullableInt(map, "awayPenaltyScore"),
                str(map, "status"),
                str(map, "statusType"),
                nullableLong(map, "startTimestamp"),
                str(map, "venue"),
                nullableInt(map, "round"),
                str(map, "roundName"),
                str(map, "homeLogoUrl"),
                str(map, "awayLogoUrl")
        );
    }

    private static List<Map<String, Object>> toStandingMaps(List<Standing> standings) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (standings == null) return rows;
        for (Standing item : standings) {
            Map<String, Object> map = new HashMap<>();
            map.put("position", item.getPosition());
            map.put("teamId", item.getTeamId());
            map.put("teamName", item.getTeamName());
            map.put("played", item.getPlayed());
            map.put("wins", item.getWins());
            map.put("draws", item.getDraws());
            map.put("losses", item.getLosses());
            map.put("goalsFor", item.getGoalsFor());
            map.put("goalsAgainst", item.getGoalsAgainst());
            map.put("goalDifference", item.getGoalDifference());
            map.put("points", item.getPoints());
            map.put("logoUrl", item.getLogoUrl());
            rows.add(map);
        }
        return rows;
    }

    private static Standing standingFromMap(Map<String, Object> map) {
        return new Standing(
                intValue(map, "position", 0),
                nullableLong(map, "teamId"),
                str(map, "teamName"),
                intValue(map, "played", 0),
                intValue(map, "wins", 0),
                intValue(map, "draws", 0),
                intValue(map, "losses", 0),
                intValue(map, "goalsFor", 0),
                intValue(map, "goalsAgainst", 0),
                intValue(map, "goalDifference", 0),
                intValue(map, "points", 0),
                str(map, "logoUrl")
        );
    }

    private static List<Map<String, Object>> toNewsMaps(List<NewsPost> posts) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (posts == null) return rows;
        for (NewsPost item : posts) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("title", item.getTitle());
            map.put("excerpt", item.getExcerpt());
            map.put("category", item.getCategory());
            map.put("imageUrl", item.getImageUrl());
            map.put("publishedAt", item.getPublishedAt());
            map.put("link", item.getLink());
            map.put("sourceName", item.getSourceName());
            rows.add(map);
        }
        return rows;
    }

    private static NewsPost newsFromMap(Map<String, Object> map) {
        return new NewsPost(longValue(map, "id", 0L), str(map, "title"), str(map, "excerpt"),
                str(map, "category"), str(map, "imageUrl"), str(map, "publishedAt"),
                str(map, "link"), str(map, "sourceName"));
    }

    private static List<Map<String, Object>> toShopMaps(List<ShopProduct> products) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (products == null) return rows;
        for (ShopProduct item : products) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("name", item.getName());
            map.put("price", item.getPrice());
            map.put("priceValue", item.getPriceValue());
            map.put("imageUrl", item.getImageUrl());
            map.put("imageUrls", item.getImageUrls());
            map.put("availableSizes", item.getAvailableSizes());
            map.put("productUrl", item.getProductUrl());
            map.put("clubName", item.getClubName());
            map.put("category", item.getCategory());
            map.put("description", item.getDescription());
            map.put("inStock", item.isInStock());
            rows.add(map);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private static ShopProduct shopFromMap(Map<String, Object> map) {
        return new ShopProduct(str(map, "id"), str(map, "name"), str(map, "price"),
                intValue(map, "priceValue", 0), str(map, "imageUrl"),
                stringList(map.get("imageUrls")), stringList(map.get("availableSizes")),
                str(map, "productUrl"), str(map, "clubName"),
                str(map, "category").isEmpty() ? str(map, "clubName") : str(map, "category"),
                str(map, "description"),
                boolValue(map, "inStock", true));
    }

    private static List<String> stringList(Object value) {
        List<String> rows = new ArrayList<>();
        if (!(value instanceof List<?>)) return rows;
        for (Object item : (List<?>) value) {
            if (item != null) rows.add(String.valueOf(item));
        }
        return rows;
    }

    private static String str(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static Long nullableLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String && !((String) value).isEmpty()) {
            try { return Long.parseLong((String) value); } catch (Exception ignored) {}
        }
        return null;
    }

    private static long longValue(Map<String, Object> map, String key, long fallback) {
        Long value = nullableLong(map, key);
        return value == null ? fallback : value;
    }

    private static Integer nullableInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String && !((String) value).isEmpty()) {
            try { return Integer.parseInt((String) value); } catch (Exception ignored) {}
        }
        return null;
    }

    private static int intValue(Map<String, Object> map, String key, int fallback) {
        Integer value = nullableInt(map, key);
        return value == null ? fallback : value;
    }

    private static boolean boolValue(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }
}
