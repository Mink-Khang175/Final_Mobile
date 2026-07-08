package com.finalproject.v_league_ticket.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.text.Html;

import com.finalproject.v_league_ticket.data.firebase.FirebaseWebBackup;
import com.finalproject.v_league_ticket.domain.model.Fixture;
import com.finalproject.v_league_ticket.domain.model.HomeMatch;
import com.finalproject.v_league_ticket.domain.model.NewsPost;
import com.finalproject.v_league_ticket.domain.model.Standing;
import com.finalproject.v_league_ticket.domain.model.VLeagueSeason;
import com.finalproject.v_league_ticket.domain.model.VLeagueSeasons;
import com.finalproject.v_league_ticket.presentation.shop.ShopProduct;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class VLeagueApiClient {
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Throwable throwable);
    }

    private interface Parser<T> {
        T parse(String body) throws Exception;
    }

    private interface BackupWriter<T> {
        void save(String key, T data);
    }

    private interface BackupReader<T> {
        void load(String key, DataCallback<T> callback);
    }

    public static final class ShopSource {
        private final String clubName;
        private final String slug;
        private final String productAlias;
        private final String baseUrl;
        private final String category;

        private ShopSource(String clubName, String slug, String productAlias, String baseUrl, String category) {
            this.clubName = clubName;
            this.slug = slug;
            this.productAlias = productAlias;
            this.baseUrl = baseUrl;
            this.category = category;
        }

        public String getClubName() {
            return clubName;
        }

        public String getSlug() {
            return slug;
        }

        public String getProductAlias() {
            return productAlias;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getCategory() {
            return category;
        }

        public boolean isCollection() {
            return productAlias.isEmpty();
        }
    }

    public static final int VLEAGUE_TOURNAMENT_ID = 626;
    public static final int VLEAGUE_CURRENT_SEASON_ID = VLeagueSeasons.SEASON_2024_25_ID;
    public static final int VLEAGUE_NEXT_SEASON_ID = VLeagueSeasons.SEASON_2025_26_ID;
    public static final int VLEAGUE_SEASON_ID = VLEAGUE_CURRENT_SEASON_ID;
    public static final int NEWS_VLEAGUE_CATEGORY_ID = 44;

    private static final String SOFASCORE_BASE = "https://www.sofascore.com/api/v1";
    private static final String VPF_NEWS_BASE = "https://vpf.vn/wp-json/wp/v2/posts";
    private static final String FIRST_FOOTBALL_BASE = "https://1stfootballstore.com";
    private static final String NEYMAR_SPORT_BASE = "https://neymarsport.com";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Android) VLeagueTicket/1.0";
    private static final long MEMORY_CACHE_TTL_MS = 60_000L;
    private static final long BACKUP_WRITE_TTL_MS = 10 * 60_000L;
    private static final VLeagueApiClient INSTANCE = new VLeagueApiClient();
    private static List<Fixture> cachedNextSeasonFixtures;
    private static final DateTimeFormatter NEWS_INPUT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    private static final DateTimeFormatter NEWS_OUTPUT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));

    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, MemoryCacheEntry> memoryCache = new HashMap<>();
    private final Map<String, Long> lastBackupWriteTimes = new HashMap<>();

    private static final List<ShopSource> SHOP_SOURCES = buildAccessoryShopSources();
    private static final List<ShopSource> CLUB_SHOP_SOURCES = buildClubShopSources();

    private VLeagueApiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public static VLeagueApiClient getInstance() {
        return INSTANCE;
    }

    public static List<ShopSource> shopSources() {
        return Collections.unmodifiableList(SHOP_SOURCES);
    }

    private <T> DataCallback<T> withFirebaseBackup(String key, BackupWriter<T> writer, BackupReader<T> reader,
                                       DataCallback<T> callback) {
        return new DataCallback<T>() {
            @Override
            public void onSuccess(T data) {
                if (shouldBackup(data)) {
                    if (shouldWriteBackup(key)) writer.save(key, data);
                    callback.onSuccess(data);
                    return;
                }
                if (data instanceof List<?>) {
                    reader.load(key, new DataCallback<T>() {
                        @Override
                        public void onSuccess(T backupData) {
                            callback.onSuccess(backupData);
                        }

                        @Override
                        public void onError(Throwable backupError) {
                            callback.onSuccess(data);
                        }
                    });
                } else {
                    callback.onSuccess(data);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                reader.load(key, new DataCallback<T>() {
                    @Override
                    public void onSuccess(T backupData) {
                        callback.onSuccess(backupData);
                    }

                    @Override
                    public void onError(Throwable backupError) {
                        callback.onError(throwable);
                    }
                });
            }
        };
    }

    private boolean shouldBackup(Object data) {
        if (data == null) return false;
        return !(data instanceof List<?>) || !((List<?>) data).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private synchronized <T> T cachedResponse(String url) {
        MemoryCacheEntry entry = memoryCache.get(url);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expiresAtMs) {
            memoryCache.remove(url);
            return null;
        }
        return (T) entry.data;
    }

    private synchronized void cacheResponse(String url, Object data) {
        if (!shouldBackup(data)) return;
        memoryCache.put(url, new MemoryCacheEntry(data, System.currentTimeMillis() + MEMORY_CACHE_TTL_MS));
    }

    private synchronized boolean shouldWriteBackup(String key) {
        if (key == null || key.trim().isEmpty()) return true;
        long now = System.currentTimeMillis();
        Long lastWrite = lastBackupWriteTimes.get(key);
        if (lastWrite != null && now - lastWrite < BACKUP_WRITE_TTL_MS) return false;
        lastBackupWriteTimes.put(key, now);
        return true;
    }

    private static List<ShopSource> buildAccessoryShopSources() {
        List<ShopSource> sources = new ArrayList<>();
        sources.add(collection("Trái bóng", "bong-da", NEYMAR_SPORT_BASE, "Trái bóng"));
        sources.add(collection("Vớ bóng đá", "vo-bong-da", NEYMAR_SPORT_BASE, "Vớ bóng đá"));
        sources.add(collection("Phụ kiện", "phu-kien-ra-san", NEYMAR_SPORT_BASE, "Phụ kiện"));
        return sources;
    }

    private static List<ShopSource> buildClubShopSources() {
        List<ShopSource> sources = new ArrayList<>();
        sources.add(collection("Ha Noi FC", "ha-noi-fc"));
        sources.add(collection("Ninh Binh", "ninh-binh"));
        sources.add(collection("Hoang Anh Gia Lai", "hoang-anh-gia-lai-fc"));
        sources.add(collection("Hong Linh Ha Tinh", "hong-linh-ha-tinh-fc"));
        sources.add(collection("Song Lam Nghe An", "song-lam-nghe-an-fc"));
        sources.add(collection("Hai Phong FC", "hai-phong-fc"));
        sources.add(collection("The Cong Viettel", "viettel-fc"));
        sources.add(collection("SHB Da Nang", "da-nang-fc"));

        sources.add(product("Cong An Ha Noi", "ao-thi-dau-clb-bong-da-cong-an-ha-noi-2025-26-mau-do-jogarbola"));
        sources.add(product("Cong An Ha Noi", "ao-thi-dau-clb-bong-da-cong-an-ha-noi-2025-26-mau-xanh-jogarbola"));
        sources.add(product("Cong An TP. Ho Chi Minh", "ao-thi-dau-clb-bong-da-cong-an-tp-ho-chi-minh-2025-26-mau-do-jogarbola"));
        sources.add(product("Becamex TP.HCM", "ao-thi-dau-clb-bong-da-becamex-tp-hcm-2025-26-mau-tim-kamito"));
        sources.add(product("Becamex TP.HCM", "ao-thi-dau-clb-bong-da-becamex-tp-hcm-2025-26-mau-trang-kamito"));
        sources.add(product("Becamex TP.HCM", "ao-thi-dau-clb-bong-da-becamex-tp-hcm-2025-26-mau-xanh-kamito"));
        sources.add(product("Thep Xanh Nam Dinh", "ao-thi-dau-clb-bong-da-thep-xanh-nam-dinh-2025-26-mau-trang-jogarbola"));
        sources.add(product("Dong A Thanh Hoa", "ao-thi-dau-clb-thanh-hoa-2021-san-nha"));
        sources.add(product("Hai Phong FC", "bo-quan-ao-clb-hai-phong-2020-jogarbola-trang"));
        sources.add(product("Hai Phong FC", "ao-thi-dau-clb-hai-phong-2021-san-nha"));
        return sources;
    }

    private static ShopSource collection(String clubName, String slug) {
        return collection(clubName, slug, FIRST_FOOTBALL_BASE, "Áo đấu");
    }

    private static ShopSource collection(String clubName, String slug, String baseUrl, String category) {
        return new ShopSource(clubName, slug, "", baseUrl, category);
    }

    private static ShopSource product(String clubName, String productAlias) {
        return new ShopSource(clubName, "", productAlias, FIRST_FOOTBALL_BASE, "Áo đấu");
    }

    public void fetchSeasons(DataCallback<List<VLeagueSeason>> callback) {
        String url = SOFASCORE_BASE + "/unique-tournament/" + VLEAGUE_TOURNAMENT_ID + "/seasons";
        String key = "sofascore_seasons";
        request(url, this::parseSeasons, key,
                withFirebaseBackup(key, FirebaseWebBackup::saveSeasons, FirebaseWebBackup::loadSeasons, callback));
    }

    public void fetchUpcoming(DataCallback<List<Fixture>> callback) {
        mainHandler.post(() -> callback.onSuccess(generatedNextSeasonFixtures()));
    }

    public void fetchUpcoming(int seasonId, DataCallback<List<Fixture>> callback) {
        if (seasonId == VLEAGUE_NEXT_SEASON_ID) {
            mainHandler.post(() -> callback.onSuccess(generatedNextSeasonFixtures()));
            return;
        }
        fetchSeasonFixturesByRounds(seasonId, 26, callback);
    }

    public void fetchFinished(DataCallback<List<Fixture>> callback) {
        fetchSeasons(new DataCallback<List<VLeagueSeason>>() {
            @Override
            public void onSuccess(List<VLeagueSeason> data) {
                VLeagueSeason season = findSeason(data, VLeagueSeasons.current().getLabel());
                if (season == null && data != null && !data.isEmpty()) season = data.get(0);
                if (season == null) {
                    callback.onError(new IOException("No V.League season available"));
                    return;
                }
                fetchFinished(season.getId(), callback);
            }

            @Override
            public void onError(Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }

    public void fetchFinished(int seasonId, DataCallback<List<Fixture>> callback) {
        if (seasonId == VLEAGUE_NEXT_SEASON_ID) {
            callback.onError(new IOException("Missing SofaScore season id for 25/26"));
            return;
        }
        String url = SOFASCORE_BASE + "/unique-tournament/" + VLEAGUE_TOURNAMENT_ID
                + "/season/" + seasonId + "/events/last/0";
        String key = "sofascore_finished_" + seasonId;
        request(url, body -> parseEvents(body, false), key,
                withFirebaseBackup(key, FirebaseWebBackup::saveFixtures, FirebaseWebBackup::loadFixtures, callback));
    }

    public void fetchRound(int seasonId, int round, DataCallback<List<Fixture>> callback) {
        if (seasonId == VLEAGUE_NEXT_SEASON_ID) {
            List<Fixture> rows = new ArrayList<>();
            for (Fixture fixture : generatedNextSeasonFixtures()) {
                if (fixture.getRound() != null && fixture.getRound() == round) rows.add(fixture);
            }
            mainHandler.post(() -> callback.onSuccess(rows));
            return;
        }
        String url = SOFASCORE_BASE + "/unique-tournament/" + VLEAGUE_TOURNAMENT_ID
                + "/season/" + seasonId + "/events/round/" + round;
        String key = "sofascore_round_" + seasonId + "_" + round;
        request(url, body -> parseEvents(body, true), key,
                withFirebaseBackup(key, FirebaseWebBackup::saveFixtures, FirebaseWebBackup::loadFixtures, callback));
    }

    private void fetchSeasonFixturesByRounds(int seasonId, int maxRound, DataCallback<List<Fixture>> callback) {
        String backupKey = "sofascore_upcoming_" + seasonId;
        AtomicInteger pending = new AtomicInteger(maxRound);
        List<Fixture> fixtures = Collections.synchronizedList(new ArrayList<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        for (int round = 1; round <= maxRound; round++) {
            String url = SOFASCORE_BASE + "/unique-tournament/" + VLEAGUE_TOURNAMENT_ID
                    + "/season/" + seasonId + "/events/round/" + round;
            int roundNumber = round;
            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    errors.add("Round " + roundNumber + ": " + e.getMessage());
                    finishSeasonFixtures(backupKey, pending, fixtures, errors, callback);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (ResponseBody responseBody = response.body()) {
                        String body = responseBody == null ? "" : responseBody.string();
                        if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
                        fixtures.addAll(parseEvents(body, true));
                    } catch (Exception e) {
                        errors.add("Round " + roundNumber + ": " + e.getMessage());
                    } finally {
                        finishSeasonFixtures(backupKey, pending, fixtures, errors, callback);
                    }
                }
            });
        }
    }

    private void finishSeasonFixtures(String backupKey, AtomicInteger pending, List<Fixture> fixtures,
                                      List<String> errors, DataCallback<List<Fixture>> callback) {
        if (pending.decrementAndGet() != 0) return;
        List<Fixture> rows;
        synchronized (fixtures) {
            rows = new ArrayList<>(fixtures);
        }
        Collections.sort(rows, Comparator.comparingLong(item ->
                item.getStartTimestamp() == null ? Long.MAX_VALUE : item.getStartTimestamp()));
        recordSync(backupKey, !rows.isEmpty(), rows.size(), errors.isEmpty() ? "" : String.join("; ", errors));
        if (!rows.isEmpty()) {
            FirebaseWebBackup.saveFixtures(backupKey, rows);
            mainHandler.post(() -> callback.onSuccess(rows));
            return;
        }
        FirebaseWebBackup.loadFixtures(backupKey, new DataCallback<List<Fixture>>() {
            @Override
            public void onSuccess(List<Fixture> backupRows) {
                mainHandler.post(() -> callback.onSuccess(backupRows));
            }

            @Override
            public void onError(Throwable throwable) {
                mainHandler.post(() -> callback.onSuccess(generatedNextSeasonFixtures()));
            }
        });
    }

    public void fetchStandings(DataCallback<List<Standing>> callback) {
        fetchSeasons(new DataCallback<List<VLeagueSeason>>() {
            @Override
            public void onSuccess(List<VLeagueSeason> data) {
                VLeagueSeason season = findSeason(data, VLeagueSeasons.current().getLabel());
                if (season == null && data != null && !data.isEmpty()) season = data.get(0);
                if (season == null) {
                    callback.onError(new IOException("No V.League season available"));
                    return;
                }
                fetchStandings(season.getId(), callback);
            }

            @Override
            public void onError(Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }

    public void fetchStandings(int seasonId, DataCallback<List<Standing>> callback) {
        if (seasonId == VLEAGUE_NEXT_SEASON_ID) {
            callback.onError(new IOException("Missing SofaScore season id for 25/26"));
            return;
        }
        String url = SOFASCORE_BASE + "/unique-tournament/" + VLEAGUE_TOURNAMENT_ID
                + "/season/" + seasonId + "/standings/total";
        String key = "sofascore_standings_" + seasonId;
        request(url, this::parseStandings, key,
                withFirebaseBackup(key, FirebaseWebBackup::saveStandings, FirebaseWebBackup::loadStandings, callback));
    }

    public void fetchTeamDetails(long teamId, DataCallback<List<String>> callback) {
        String url = SOFASCORE_BASE + "/team/" + teamId;
        String key = "sofascore_team_" + teamId + "_details";
        request(url, this::parseTeamDetails, key,
                withFirebaseBackup(key, FirebaseWebBackup::saveStrings, FirebaseWebBackup::loadStrings, callback));
    }

    public void fetchTeamPlayers(long teamId, DataCallback<List<String>> callback) {
        String url = SOFASCORE_BASE + "/team/" + teamId + "/players";
        String key = "sofascore_team_" + teamId + "_players";
        request(url, this::parseTeamPlayers, key,
                withFirebaseBackup(key, FirebaseWebBackup::saveStrings, FirebaseWebBackup::loadStrings, callback));
    }

    public void fetchTeamStatistics(long teamId, DataCallback<List<String>> callback) {
        List<String> urls = Arrays.asList(
                SOFASCORE_BASE + "/team/" + teamId
                        + "/unique-tournament/" + VLEAGUE_TOURNAMENT_ID
                        + "/season/" + VLEAGUE_SEASON_ID + "/statistics/overall",
                SOFASCORE_BASE + "/team/" + teamId
                        + "/unique-tournament/" + VLEAGUE_TOURNAMENT_ID
                        + "/season/" + VLEAGUE_SEASON_ID + "/statistics",
                SOFASCORE_BASE + "/team/" + teamId + "/statistics/overall",
                SOFASCORE_BASE + "/team/" + teamId + "/statistics"
        );
        String key = "sofascore_team_" + teamId + "_statistics";
        requestFirstSuccess(urls, this::parseTeamStatistics, key,
                withFirebaseBackup(key, FirebaseWebBackup::saveStrings, FirebaseWebBackup::loadStrings, callback));
    }

    public void fetchMatchFacts(long eventId, DataCallback<List<String>> callback) {
        String url = SOFASCORE_BASE + "/event/" + eventId;
        String key = "sofascore_event_" + eventId + "_facts";
        request(url, this::parseMatchFacts, key,
                withFirebaseBackup(key, FirebaseWebBackup::saveStrings, FirebaseWebBackup::loadStrings, callback));
    }

    public void fetchMatchIncidents(long eventId, DataCallback<List<String>> callback) {
        String url = SOFASCORE_BASE + "/event/" + eventId + "/incidents";
        String key = "sofascore_event_" + eventId + "_incidents";
        request(url, this::parseIncidents, key,
                withFirebaseBackup(key, FirebaseWebBackup::saveStrings, FirebaseWebBackup::loadStrings, callback));
    }

    public void fetchMatchStatistics(long eventId, DataCallback<List<String>> callback) {
        String url = SOFASCORE_BASE + "/event/" + eventId + "/statistics";
        String key = "sofascore_event_" + eventId + "_statistics";
        request(url, this::parseStatistics, key,
                withFirebaseBackup(key, FirebaseWebBackup::saveStrings, FirebaseWebBackup::loadStrings, callback));
    }

    public void fetchMatchLineups(long eventId, DataCallback<List<String>> callback) {
        String url = SOFASCORE_BASE + "/event/" + eventId + "/lineups";
        String key = "sofascore_event_" + eventId + "_lineups";
        request(url, this::parseLineups, key,
                withFirebaseBackup(key, FirebaseWebBackup::saveStrings, FirebaseWebBackup::loadStrings, callback));
    }

    public void fetchTopRatedPlayers(long eventId, DataCallback<List<String>> callback) {
        String url = SOFASCORE_BASE + "/event/" + eventId + "/lineups";
        String key = "sofascore_event_" + eventId + "_ratings";
        request(url, this::parseTopRatedPlayers, key,
                withFirebaseBackup(key, FirebaseWebBackup::saveStrings, FirebaseWebBackup::loadStrings, callback));
    }

    public void fetchNews(int categoryId, DataCallback<List<NewsPost>> callback) {
        String url = VPF_NEWS_BASE + "?categories=" + categoryId
                + "&per_page=12&_embed&orderby=date&order=desc";
        String key = "vpf_news_" + categoryId;
        request(url, this::parseNews, key,
                withFirebaseBackup(key, FirebaseWebBackup::saveNews, FirebaseWebBackup::loadNews, callback));
    }

    public void fetchShopProducts(DataCallback<List<ShopProduct>> callback) {
        fetchShopProductsFromSources(SHOP_SOURCES, "shop_neymarsport_accessories", callback);
    }

    public void fetchShopProductsForClub(String clubName, DataCallback<List<ShopProduct>> callback) {
        String wanted = normalizeShopClubKey(clubName);
        List<ShopSource> sources = new ArrayList<>();
        for (ShopSource source : CLUB_SHOP_SOURCES) {
            if (normalizeShopClubKey(source.getClubName()).equals(wanted)) sources.add(source);
        }
        if (sources.isEmpty()) {
            callback.onError(new IOException("No shop source configured for " + clubName));
            return;
        }
        fetchShopProductsFromSources(sources, "shop_1stfootballstore_" + wanted, callback);
    }

    private void fetchShopProductsFromSources(List<ShopSource> sources, String syncKey,
                                              DataCallback<List<ShopProduct>> callback) {
        if (sources == null || sources.isEmpty()) {
            callback.onError(new IOException("No shop sources"));
            return;
        }
        AtomicInteger pending = new AtomicInteger(sources.size());
        Map<String, ShopProduct> productsById = Collections.synchronizedMap(new LinkedHashMap<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        for (ShopSource source : sources) {
            String url = source.isCollection()
                    ? source.getBaseUrl() + "/collections/" + source.getSlug() + "/products.json"
                    : source.getBaseUrl() + "/" + source.getProductAlias() + ".js";
            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    errors.add(source.getClubName() + ": " + e.getMessage());
                    finishShopSource(syncKey, pending, productsById, errors, callback);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (ResponseBody responseBody = response.body()) {
                        String body = responseBody == null ? "" : responseBody.string();
                        if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
                        List<ShopProduct> parsed = source.isCollection()
                                ? parseShopCollection(body, source)
                                : parseShopProduct(body, source);
                        for (ShopProduct product : parsed) {
                            productsById.put(product.getId(), product);
                        }
                    } catch (Exception e) {
                        errors.add(source.getClubName() + ": " + e.getMessage());
                    } finally {
                        finishShopSource(syncKey, pending, productsById, errors, callback);
                    }
                }
            });
        }
    }

    public static HomeMatch toHomeMatch(Fixture fixture) {
        return new HomeMatch(
                fixture.getId(),
                fixture.getHomeTeamId(),
                fixture.getHomeTeamName(),
                fixture.getAwayTeamId(),
                fixture.getAwayTeamName(),
                fixture.getHomeScore(),
                fixture.getAwayScore(),
                fixture.getStatus().isEmpty() ? "FT" : fixture.getStatus(),
                fixture.getStatusType(),
                fixture.getVenue(),
                fixture.getStartTimestamp(),
                null,
                fixture.getRound(),
                fixture.getRoundName().isEmpty()
                        ? (fixture.getRound() == null ? "" : "Round " + fixture.getRound())
                        : fixture.getRoundName(),
                "V.League 1",
                fixture.getHomePenaltyScore(),
                fixture.getAwayPenaltyScore(),
                Collections.emptyList(),
                fixture.getHomeLogoUrl(),
                fixture.getAwayLogoUrl()
        );
    }

    public static boolean isGeneratedNextSeasonFixture(Fixture fixture) {
        return fixture != null && fixture.getId() >= 252600000L && fixture.getId() < 252700000L;
    }

    private static List<Fixture> generatedNextSeasonFixtures() {
        if (cachedNextSeasonFixtures != null) return new ArrayList<>(cachedNextSeasonFixtures);
        List<LeagueTeam> teams = leagueTeams();
        List<int[]> firstLeg = new ArrayList<>();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) order.add(i);
        int teamCount = order.size();
        int half = teamCount / 2;

        for (int round = 0; round < teamCount - 1; round++) {
            for (int index = 0; index < half; index++) {
                int left = order.get(index);
                int right = order.get(teamCount - 1 - index);
                boolean flip = round % 2 == 1;
                int home = flip ? right : left;
                int away = flip ? left : right;
                firstLeg.add(new int[]{round + 1, home, away, index});
            }
            Integer last = order.remove(order.size() - 1);
            order.add(1, last);
        }

        List<Fixture> fixtures = new ArrayList<>();
        for (int[] row : firstLeg) {
            fixtures.add(generatedFixture(row[0], row[3], teams.get(row[1]), teams.get(row[2]), false));
        }
        for (int[] row : firstLeg) {
            fixtures.add(generatedFixture(row[0] + 13, row[3], teams.get(row[2]), teams.get(row[1]), false));
        }
        Collections.sort(fixtures, Comparator.comparingLong(item ->
                item.getStartTimestamp() == null ? Long.MAX_VALUE : item.getStartTimestamp()));
        cachedNextSeasonFixtures = Collections.unmodifiableList(new ArrayList<>(fixtures));
        return new ArrayList<>(cachedNextSeasonFixtures);
    }

    private static List<Fixture> generatedCurrentSeasonResults() {
        List<Fixture> rows = new ArrayList<>();
        LeagueTeam namDinh = new LeagueTeam(33127L, "Thép Xanh Nam Định", "Sân vận động Thiên Trường");
        LeagueTeam haNoi = new LeagueTeam(33130L, "Hà Nội FC", "Sân vận động Hàng Đẫy");
        long timestamp = localTimestampSeconds(2025, Calendar.JUNE, 22, 19, 15);
        rows.add(new Fixture(
                242502601L,
                namDinh.id,
                haNoi.id,
                namDinh.name,
                haNoi.name,
                2,
                1,
                null,
                null,
                "FT",
                "finished",
                timestamp,
                namDinh.stadium,
                26,
                "Vòng 26 - Hạ màn 24/25",
                "",
                ""
        ));
        return rows;
    }

    private static List<Fixture> generatedHistoricalSeasonResults(int seasonId) {
        List<Fixture> rows = new ArrayList<>();
        List<Fixture> template = generatedRoundRobinSeason(seasonStartYear(seasonId), false, historicalBaseId(seasonId));
        for (Fixture fixture : template) {
            int homeGoals = (int) ((fixture.getId() + fixture.getRound()) % 4);
            int awayGoals = (int) ((fixture.getId() / 3 + fixture.getRound()) % 3);
            rows.add(new Fixture(
                    fixture.getId(),
                    fixture.getHomeTeamId(),
                    fixture.getAwayTeamId(),
                    fixture.getHomeTeamName(),
                    fixture.getAwayTeamName(),
                    homeGoals,
                    awayGoals,
                    null,
                    null,
                    "FT",
                    "finished",
                    fixture.getStartTimestamp(),
                    fixture.getVenue(),
                    fixture.getRound(),
                    fixture.getRoundName(),
                    "",
                    ""
            ));
        }
        Collections.sort(rows, Comparator.comparingLong(item ->
                item.getStartTimestamp() == null ? Long.MAX_VALUE : item.getStartTimestamp()));
        Collections.reverse(rows);
        return rows;
    }

    private static List<Standing> generatedCurrentSeasonStandings() {
        List<Standing> rows = new ArrayList<>();
        LeagueTeam[] teams = {
                new LeagueTeam(33127L, "Thép Xanh Nam Định", "Sân vận động Thiên Trường"),
                new LeagueTeam(33130L, "Hà Nội FC", "Sân vận động Hàng Đẫy"),
                new LeagueTeam(193616L, "Công An Hà Nội", "Sân vận động Hàng Đẫy"),
                new LeagueTeam(231270L, "Thể Công - Viettel", "Sân vận động Hàng Đẫy"),
                new LeagueTeam(33030L, "Đông Á Thanh Hóa", "Sân vận động Thanh Hóa"),
                new LeagueTeam(33124L, "Hải Phòng FC", "Sân vận động Lạch Tray"),
                new LeagueTeam(33129L, "Sông Lam Nghệ An", "Sân vận động Vinh"),
                new LeagueTeam(33125L, "Hoàng Anh Gia Lai", "Sân vận động Pleiku"),
                new LeagueTeam(309496L, "Hồng Lĩnh Hà Tĩnh", "Sân vận động Hà Tĩnh"),
                new LeagueTeam(33120L, "Công An TP. Hồ Chí Minh", "Sân vận động Thống Nhất"),
                new LeagueTeam(33119L, "Becamex TP. Hồ Chí Minh", "Sân vận động Gò Đậu"),
                new LeagueTeam(33121L, "SHB Đà Nẵng", "Sân vận động Chi Lăng"),
                new LeagueTeam(253057L, "Ninh Bình FC", "Sân vận động Ninh Bình"),
                new LeagueTeam(465466L, "PVF-CAND FC", "Sân vận động PVF")
        };
        int[] points = {53, 49, 46, 43, 40, 37, 35, 32, 30, 29, 27, 24, 22, 19};
        for (int i = 0; i < teams.length; i++) {
            int wins = Math.max(4, points[i] / 3 - 2);
            int draws = Math.max(1, points[i] - wins * 3);
            int losses = Math.max(0, 26 - wins - draws);
            int gf = 44 - i;
            int ga = 24 + i;
            rows.add(new Standing(i + 1, teams[i].id, teams[i].name, 26, wins, draws, losses,
                    gf, ga, gf - ga, points[i], ""));
        }
        return rows;
    }

    private static List<Standing> generatedNextSeasonStandings() {
        List<Standing> rows = new ArrayList<>();
        List<LeagueTeam> teams = leagueTeams();
        for (int i = 0; i < teams.size(); i++) {
            LeagueTeam team = teams.get(i);
            rows.add(new Standing(i + 1, team.id, team.name, 0, 0, 0, 0,
                    0, 0, 0, 0, ""));
        }
        return rows;
    }

    private static List<Standing> generatedHistoricalStandings(int seasonId) {
        List<Standing> rows = new ArrayList<>();
        List<LeagueTeam> teams = leagueTeams();
        for (int i = 0; i < teams.size(); i++) {
            LeagueTeam team = teams.get((i + seasonId) % teams.size());
            int points = Math.max(18, 52 - i * 2);
            int wins = Math.max(4, points / 3 - 2);
            int draws = Math.max(1, points - wins * 3);
            int losses = Math.max(0, 26 - wins - draws);
            int gf = 42 - i;
            int ga = 24 + i;
            rows.add(new Standing(i + 1, team.id, team.name, 26, wins, draws, losses,
                    gf, ga, gf - ga, points, ""));
        }
        return rows;
    }

    private static Fixture generatedFixture(int round, int indexInRound, LeagueTeam home, LeagueTeam away, boolean finished) {
        long timestamp = roundTimestamp(round, indexInRound);
        long id = 252600000L + (round * 100L) + indexInRound + 1L;
        return new Fixture(
                id,
                home.id,
                away.id,
                home.name,
                away.name,
                finished ? 0 : null,
                finished ? 0 : null,
                null,
                null,
                finished ? "FT" : "Chưa diễn ra",
                finished ? "finished" : "notstarted",
                timestamp,
                home.stadium,
                round,
                "Vòng " + round,
                "",
                ""
        );
    }

    private static List<Fixture> generatedRoundRobinSeason(int startYear, boolean upcoming, long baseId) {
        List<LeagueTeam> teams = leagueTeams();
        List<int[]> firstLeg = new ArrayList<>();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) order.add(i);
        int teamCount = order.size();
        int half = teamCount / 2;
        for (int round = 0; round < teamCount - 1; round++) {
            for (int index = 0; index < half; index++) {
                int left = order.get(index);
                int right = order.get(teamCount - 1 - index);
                boolean flip = round % 2 == 1;
                firstLeg.add(new int[]{round + 1, flip ? right : left, flip ? left : right, index});
            }
            Integer last = order.remove(order.size() - 1);
            order.add(1, last);
        }
        List<Fixture> fixtures = new ArrayList<>();
        for (int[] row : firstLeg) {
            fixtures.add(generatedFixtureForYear(baseId, startYear, row[0], row[3], teams.get(row[1]), teams.get(row[2]), upcoming));
        }
        for (int[] row : firstLeg) {
            fixtures.add(generatedFixtureForYear(baseId, startYear, row[0] + 13, row[3], teams.get(row[2]), teams.get(row[1]), upcoming));
        }
        return fixtures;
    }

    private static Fixture generatedFixtureForYear(long baseId, int startYear, int round, int indexInRound,
                                                   LeagueTeam home, LeagueTeam away, boolean upcoming) {
        long timestamp = roundTimestampForYear(startYear, round, indexInRound);
        long id = baseId + (round * 100L) + indexInRound + 1L;
        return new Fixture(
                id,
                home.id,
                away.id,
                home.name,
                away.name,
                upcoming ? null : 0,
                upcoming ? null : 0,
                null,
                null,
                upcoming ? "Chưa diễn ra" : "FT",
                upcoming ? "notstarted" : "finished",
                timestamp,
                home.stadium,
                round,
                "Vòng " + round,
                "",
                ""
        );
    }

    private static long roundTimestamp(int round, int indexInRound) {
        return roundTimestampForYear(2026, round, indexInRound);
    }

    private static long roundTimestampForYear(int year, int round, int indexInRound) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, Calendar.AUGUST);
        calendar.set(Calendar.DAY_OF_MONTH, 15);
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, (round - 1) * 7 + (indexInRound % 3));
        int[] hours = {17, 18, 19, 19, 18, 17, 20};
        int[] minutes = {0, 0, 15, 45, 30, 30, 15};
        int slot = indexInRound % hours.length;
        calendar.set(Calendar.HOUR_OF_DAY, hours[slot]);
        calendar.set(Calendar.MINUTE, minutes[slot]);
        return calendar.getTimeInMillis() / 1000L;
    }

    private static long localTimestampSeconds(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis() / 1000L;
    }

    private static List<LeagueTeam> leagueTeams() {
        return Arrays.asList(
                new LeagueTeam(193616L, "Công An Hà Nội", "Sân vận động Hàng Đẫy"),
                new LeagueTeam(231270L, "Thể Công - Viettel", "Sân vận động Hàng Đẫy"),
                new LeagueTeam(253057L, "Ninh Bình FC", "Sân vận động Ninh Bình"),
                new LeagueTeam(33130L, "Hà Nội FC", "Sân vận động Hàng Đẫy"),
                new LeagueTeam(33120L, "Công An TP. Hồ Chí Minh", "Sân vận động Thống Nhất"),
                new LeagueTeam(33127L, "Thép Xanh Nam Định", "Sân vận động Thiên Trường"),
                new LeagueTeam(33124L, "Hải Phòng FC", "Sân vận động Lạch Tray"),
                new LeagueTeam(309496L, "Hồng Lĩnh Hà Tĩnh", "Sân vận động Hà Tĩnh"),
                new LeagueTeam(33030L, "Đông Á Thanh Hóa", "Sân vận động Thanh Hóa"),
                new LeagueTeam(33129L, "Sông Lam Nghệ An", "Sân vận động Vinh"),
                new LeagueTeam(33125L, "Hoàng Anh Gia Lai", "Sân vận động Pleiku"),
                new LeagueTeam(33119L, "Becamex TP. Hồ Chí Minh", "Sân vận động Gò Đậu"),
                new LeagueTeam(33121L, "SHB Đà Nẵng", "Sân vận động Chi Lăng"),
                new LeagueTeam(465466L, "PVF-CAND FC", "Sân vận động PVF")
        );
    }

    private static boolean isLocalHistoricalSeason(int seasonId) {
        return seasonId == VLeagueSeasons.SEASON_2023_24_ID
                || seasonId == VLeagueSeasons.SEASON_2023_ID
                || seasonId == VLeagueSeasons.SEASON_2022_ID
                || seasonId == VLeagueSeasons.SEASON_2021_ID;
    }

    private static int seasonStartYear(int seasonId) {
        if (seasonId == VLeagueSeasons.SEASON_2023_24_ID) return 2023;
        if (seasonId == VLeagueSeasons.SEASON_2023_ID) return 2023;
        if (seasonId == VLeagueSeasons.SEASON_2022_ID) return 2022;
        if (seasonId == VLeagueSeasons.SEASON_2021_ID) return 2021;
        return 2023;
    }

    private static long historicalBaseId(int seasonId) {
        return Math.abs(seasonId) * 100000L;
    }

    private static final class LeagueTeam {
        final long id;
        final String name;
        final String stadium;

        LeagueTeam(long id, String name, String stadium) {
            this.id = id;
            this.name = name;
            this.stadium = stadium;
        }
    }

    private static final class MemoryCacheEntry {
        final Object data;
        final long expiresAtMs;

        MemoryCacheEntry(Object data, long expiresAtMs) {
            this.data = data;
            this.expiresAtMs = expiresAtMs;
        }
    }

    private <T> void request(String url, Parser<T> parser, String syncKey, DataCallback<T> callback) {
        T cached = cachedResponse(url);
        if (cached != null) {
            mainHandler.post(() -> callback.onSuccess(cached));
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                recordSync(syncKey, false, 0, e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    String body = responseBody == null ? "" : responseBody.string();
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code());
                    }
                    T parsed = parser.parse(body);
                    cacheResponse(url, parsed);
                    recordSync(syncKey, true, itemCount(parsed), "");
                    mainHandler.post(() -> callback.onSuccess(parsed));
                } catch (Exception e) {
                    recordSync(syncKey, false, 0, e.getMessage());
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    private <T> void requestFirstSuccess(List<String> urls, Parser<T> parser, String syncKey, DataCallback<T> callback) {
        requestFirstSuccess(urls, 0, parser, syncKey, callback, new ArrayList<>());
    }

    private <T> void requestFirstSuccess(List<String> urls, int index, Parser<T> parser, String syncKey,
                                         DataCallback<T> callback, List<String> errors) {
        if (urls == null || index >= urls.size()) {
            String message = errors.isEmpty() ? "No endpoint candidates" : String.join("; ", errors);
            recordSync(syncKey, false, 0, message);
            mainHandler.post(() -> callback.onError(new IOException(message)));
            return;
        }
        String url = urls.get(index);
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                errors.add(endpointName(url) + ": " + e.getMessage());
                requestFirstSuccess(urls, index + 1, parser, syncKey, callback, errors);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    String body = responseBody == null ? "" : responseBody.string();
                    if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
                    T parsed = parser.parse(body);
                    if (parsed instanceof List<?> && ((List<?>) parsed).isEmpty()) {
                        throw new IOException("empty response");
                    }
                    recordSync(syncKey, true, itemCount(parsed), endpointName(url));
                    mainHandler.post(() -> callback.onSuccess(parsed));
                } catch (Exception e) {
                    errors.add(endpointName(url) + ": " + e.getMessage());
                    requestFirstSuccess(urls, index + 1, parser, syncKey, callback, errors);
                }
            }
        });
    }

    private List<VLeagueSeason> parseSeasons(String body) {
        List<VLeagueSeason> seasons = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray rows = array(root, "seasons");
        if (rows == null) return seasons;
        for (JsonElement element : rows) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            int id = intValue(row, "id", 0);
            if (id <= 0) continue;
            String label = firstNonEmpty(string(row, "year"), string(row, "name"));
            label = cleanSeasonLabel(label);
            seasons.add(new VLeagueSeason(
                    id,
                    label,
                    VLeagueSeasons.sportsDbSeason(label),
                    VLeagueSeasons.maxRoundForLabel(label),
                    true
            ));
        }
        Collections.sort(seasons, (a, b) -> Integer.compare(b.getId(), a.getId()));
        return seasons;
    }

    private VLeagueSeason findSeason(List<VLeagueSeason> seasons, String label) {
        if (seasons == null || label == null) return null;
        for (VLeagueSeason season : seasons) {
            if (label.equalsIgnoreCase(season.getLabel())) return season;
        }
        return null;
    }

    private List<Fixture> parseEvents(String body, boolean ascending) {
        List<Fixture> fixtures = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray events = array(root, "events");
        if (events == null) return fixtures;
        for (JsonElement element : events) {
            if (!element.isJsonObject()) continue;
            fixtures.add(mapEvent(element.getAsJsonObject()));
        }
        Collections.sort(fixtures, Comparator.comparingLong(item ->
                item.getStartTimestamp() == null ? Long.MAX_VALUE : item.getStartTimestamp()));
        if (!ascending) Collections.reverse(fixtures);
        return fixtures;
    }

    private Fixture mapEvent(JsonObject event) {
        JsonObject homeTeam = object(event, "homeTeam");
        JsonObject awayTeam = object(event, "awayTeam");
        JsonObject homeScore = object(event, "homeScore");
        JsonObject awayScore = object(event, "awayScore");
        JsonObject status = object(event, "status");
        JsonObject roundInfo = object(event, "roundInfo");
        JsonObject venue = object(event, "venue");
        JsonObject tournament = object(event, "tournament");

        String venueName = firstNonEmpty(
                nestedString(venue, "stadium", "name"),
                string(venue, "name"),
                nestedString(venue, "city", "name")
        );
        String roundName = firstNonEmpty(
                string(roundInfo, "name"),
                string(roundInfo, "slug"),
                integer(roundInfo, "round") == null ? "" : "Round " + integer(roundInfo, "round")
        );
        String tournamentName = firstNonEmpty(
                nestedString(tournament, "uniqueTournament", "name"),
                string(tournament, "name"),
                "V.League 1"
        );
        long fallbackId = Math.abs((string(homeTeam, "name") + string(awayTeam, "name")
                + longObject(event, "startTimestamp")).hashCode());
        Integer homePenalty = firstInteger(homeScore, "penalties", "penaltyScore", "shootout", "currentPenalty");
        Integer awayPenalty = firstInteger(awayScore, "penalties", "penaltyScore", "shootout", "currentPenalty");
        return new Fixture(
                longValue(event, "id", fallbackId),
                longObject(homeTeam, "id"),
                longObject(awayTeam, "id"),
                firstNonEmpty(string(homeTeam, "shortName"), string(homeTeam, "name")),
                firstNonEmpty(string(awayTeam, "shortName"), string(awayTeam, "name")),
                scoreWithoutPenalties(integer(homeScore, "current"), homePenalty),
                scoreWithoutPenalties(integer(awayScore, "current"), awayPenalty),
                homePenalty,
                awayPenalty,
                string(status, "description"),
                string(status, "type"),
                longObject(event, "startTimestamp"),
                venueName,
                integer(roundInfo, "round"),
                roundName.isEmpty() ? tournamentName : roundName,
                "",
                ""
        );
    }

    private Integer scoreWithoutPenalties(Integer currentScore, Integer penaltyScore) {
        if (currentScore == null || penaltyScore == null || penaltyScore <= 0) return currentScore;
        int normalScore = currentScore - penaltyScore;
        return normalScore >= 0 ? normalScore : currentScore;
    }

    private List<Standing> parseStandings(String body) {
        List<Standing> standings = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray rootRows = array(root, "rows");
        if (rootRows != null) {
            appendStandingRows(standings, rootRows);
            return standings;
        }
        JsonArray groups = array(root, "standings");
        if (groups == null) return standings;
        for (JsonElement group : groups) {
            if (!group.isJsonObject()) continue;
            JsonArray rows = array(group.getAsJsonObject(), "rows");
            if (rows != null) appendStandingRows(standings, rows);
        }
        return standings;
    }

    private void appendStandingRows(List<Standing> standings, JsonArray rows) {
        for (JsonElement element : rows) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            JsonObject team = object(row, "team");
            int goalsFor = intValue(row, "scoresFor", 0);
            int goalsAgainst = intValue(row, "scoresAgainst", 0);
            int goalDiff = firstInt(row, goalsFor - goalsAgainst,
                    "scoreDiff", "goalDifference", "goalDiff");
            String diffText = string(row, "scoreDiffFormatted");
            if (!diffText.isEmpty()) goalDiff = parseSignedInt(diffText, goalDiff);
            Long teamId = longObject(team, "id");
            standings.add(new Standing(
                    intValue(row, "position", standings.size() + 1),
                    teamId,
                    firstNonEmpty(string(team, "shortName"), string(team, "name")),
                    firstInt(row, 0, "matches", "played"),
                    intValue(row, "wins", 0),
                    intValue(row, "draws", 0),
                    intValue(row, "losses", 0),
                    goalsFor,
                    goalsAgainst,
                    goalDiff,
                    intValue(row, "points", 0),
                    teamLogoUrl(teamId)
            ));
        }
    }

    private String teamLogoUrl(Long teamId) {
        return teamId == null || teamId <= 0
                ? ""
                : "https://img.sofascore.com/api/v1/team/" + teamId + "/image";
    }

    private List<NewsPost> parseNews(String body) {
        List<NewsPost> posts = new ArrayList<>();
        JsonElement parsed = JsonParser.parseString(body);
        if (!parsed.isJsonArray()) return posts;
        for (JsonElement element : parsed.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject post = element.getAsJsonObject();
            posts.add(new NewsPost(
                    longValue(post, "id", posts.size() + 1L),
                    cleanHtml(nestedString(post, "title", "rendered")),
                    excerpt(cleanHtml(nestedString(post, "excerpt", "rendered")), 160),
                    category(post),
                    featuredImage(post),
                    formatNewsDate(string(post, "date")),
                    string(post, "link"),
                    "VPF"
            ));
        }
        return posts;
    }

    private void finishShopSource(String syncKey, AtomicInteger pending, Map<String, ShopProduct> productsById,
                                  List<String> errors, DataCallback<List<ShopProduct>> callback) {
        if (pending.decrementAndGet() != 0) return;
        List<ShopProduct> products;
        synchronized (productsById) {
            products = new ArrayList<>(productsById.values());
        }
        Collections.sort(products, Comparator.comparing(ShopProduct::getClubName)
                .thenComparing(ShopProduct::getName));
        recordSync(syncKey, !products.isEmpty(), products.size(),
                errors.isEmpty() ? "" : String.join("; ", errors));
        if (products.isEmpty()) {
            FirebaseWebBackup.loadShopProducts(syncKey, new DataCallback<List<ShopProduct>>() {
                @Override
                public void onSuccess(List<ShopProduct> backupProducts) {
                    mainHandler.post(() -> callback.onSuccess(backupProducts));
                }

                @Override
                public void onError(Throwable throwable) {
                    mainHandler.post(() -> callback.onError(new IOException(
                            errors.isEmpty() ? "No shop products returned" : String.join("; ", errors))));
                }
            });
        } else {
            FirebaseWebBackup.saveShopProducts(syncKey, products);
            mainHandler.post(() -> callback.onSuccess(products));
        }
    }

    private static String normalizeShopClubKey(String value) {
        if (value == null) return "";
        String key = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (key.contains("cong-an-ha-noi")) return "cong-an-ha-noi";
        if (key.equals("cahcm") || key.equals("cahcmc") || key.contains("cong-an-tp-ho-chi-minh")
                || key.contains("cong-an-tp-hcm") || key.contains("cong-an-ho-chi-minh")
                || key.contains("cong-an-hcm")) return "cong-an-tp-ho-chi-minh";
        if (key.contains("the-cong") || key.equals("viettel")) return "the-cong-viettel";
        if (key.contains("thep-xanh-nam-dinh") || key.equals("nam-dinh")) return "nam-dinh";
        if (key.contains("dong-a-thanh-hoa") || key.equals("thanh-hoa")) return "thanh-hoa";
        if (key.contains("shb-da-nang") || key.equals("da-nang")) return "da-nang";
        if (key.contains("hong-linh-ha-tinh") || key.equals("ha-tinh")) return "hong-linh-ha-tinh";
        if (key.contains("song-lam-nghe-an")) return "song-lam-nghe-an";
        if (key.contains("hoang-anh-gia-lai")) return "hoang-anh-gia-lai";
        if (key.contains("becamex")) return "becamex-tp-hcm";
        if (key.equals("ha-noi") || key.equals("hanoi")) return "ha-noi";
        if (key.contains("hai-phong")) return "hai-phong";
        if (key.contains("ninh-binh")) return "ninh-binh";
        return key.replace("-fc", "").replace("fc-", "");
    }

    private List<ShopProduct> parseShopCollection(String body, ShopSource source) {
        List<ShopProduct> products = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray rows = array(root, "products");
        if (rows == null) return products;
        for (JsonElement row : rows) {
            if (!row.isJsonObject()) continue;
            ShopProduct product = mapShopProduct(row.getAsJsonObject(), source);
            if (product != null) products.add(product);
        }
        return products;
    }

    private List<ShopProduct> parseShopProduct(String body, ShopSource source) {
        JsonElement parsed = JsonParser.parseString(body);
        if (!parsed.isJsonObject()) return Collections.emptyList();
        ShopProduct product = mapShopProduct(parsed.getAsJsonObject(), source);
        return product == null ? Collections.emptyList() : Collections.singletonList(product);
    }

    private ShopProduct mapShopProduct(JsonObject product, ShopSource source) {
        List<String> availableSizes = availableSizes(product);
        List<String> allSizes = allSizes(product);
        List<String> sizes = availableSizes.isEmpty() ? allSizes : availableSizes;
        boolean productAvailable = booleanValue(product, "available", !availableSizes.isEmpty());
        if (sizes.isEmpty() && productAvailable) sizes = Collections.singletonList("Mặc định");
        if (sizes.isEmpty()) return null;
        String alias = firstNonEmpty(string(product, "alias"), string(product, "handle"),
                source.getProductAlias(), String.valueOf(longValue(product, "id", 0L)));
        String urlPath = firstNonEmpty(string(product, "url"), "/" + alias);
        if (!urlPath.startsWith("http") && NEYMAR_SPORT_BASE.equals(source.getBaseUrl()) && !urlPath.startsWith("/products/")) {
            urlPath = "/products/" + alias;
        }
        String productUrl = urlPath.startsWith("http") ? urlPath : source.getBaseUrl() + (urlPath.startsWith("/") ? urlPath : "/" + urlPath);
        List<String> images = imageArray(product, "images");
        String featured = firstNonEmpty(string(product, "featured_image"), imageSrc(product, "image"),
                images.isEmpty() ? "" : images.get(0));
        featured = fullImageUrl(featured, source.getBaseUrl());
        List<String> normalizedImages = new ArrayList<>();
        if (!featured.isEmpty()) normalizedImages.add(featured);
        for (String image : images) {
            String full = fullImageUrl(image, source.getBaseUrl());
            if (!full.isEmpty() && !normalizedImages.contains(full)) normalizedImages.add(full);
        }
        int priceValue = Math.max(0, (int) Math.round(doubleValue(product, "price_min",
                doubleValue(product, "price", minVariantPrice(product)))));
        String description = firstNonEmpty(cleanHtml(string(product, "summary")),
                excerpt(cleanHtml(firstNonEmpty(string(product, "content"), string(product, "body_html"))), 260));
        return new ShopProduct(
                (NEYMAR_SPORT_BASE.equals(source.getBaseUrl()) ? "ney-" : "1fs-") + alias,
                firstNonEmpty(string(product, "name"), string(product, "title")),
                formatVnd(priceValue),
                priceValue,
                featured,
                normalizedImages,
                sizes,
                productUrl,
                source.getClubName(),
                source.getCategory(),
                description,
                productAvailable
        );
    }

    private List<String> availableSizes(JsonObject product) {
        JsonArray variants = array(product, "variants");
        if (variants == null) return Collections.emptyList();
        LinkedHashSet<String> sizes = new LinkedHashSet<>();
        for (JsonElement element : variants) {
            if (!element.isJsonObject()) continue;
            JsonObject variant = element.getAsJsonObject();
            boolean available = booleanValue(variant, "available", false);
            int quantity = intValue(variant, "inventory_quantity", available ? 1 : 0);
            if (!available || quantity <= 0) continue;
            String size = firstNonEmpty(string(variant, "option1"), string(variant, "title"));
            if (size.contains("/")) size = size.substring(0, size.indexOf('/')).trim();
            if (!size.isEmpty()) sizes.add(size);
        }
        return new ArrayList<>(sizes);
    }

    private List<String> allSizes(JsonObject product) {
        JsonArray variants = array(product, "variants");
        if (variants == null) return Collections.emptyList();
        LinkedHashSet<String> sizes = new LinkedHashSet<>();
        for (JsonElement element : variants) {
            if (!element.isJsonObject()) continue;
            JsonObject variant = element.getAsJsonObject();
            String size = firstNonEmpty(string(variant, "option1"), string(variant, "title"));
            if (size.contains("/")) size = size.substring(0, size.indexOf('/')).trim();
            if (!size.isEmpty() && !"default title".equals(size.toLowerCase(Locale.ROOT))) sizes.add(size);
        }
        return new ArrayList<>(sizes);
    }

    private List<String> stringArray(JsonObject object, String key) {
        JsonArray array = array(object, key);
        if (array == null) return Collections.emptyList();
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element == null || element.isJsonNull()) continue;
            try {
                String value = element.getAsString();
                if (value != null && !value.trim().isEmpty()) values.add(value.trim());
            } catch (Exception ignored) {
            }
        }
        return values;
    }

    private List<String> imageArray(JsonObject object, String key) {
        JsonArray array = array(object, key);
        if (array == null) return Collections.emptyList();
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element == null || element.isJsonNull()) continue;
            String value = "";
            if (element.isJsonObject()) {
                value = firstNonEmpty(string(element.getAsJsonObject(), "src"),
                        string(element.getAsJsonObject(), "url"));
            } else {
                try {
                    value = element.getAsString();
                } catch (Exception ignored) {
                }
            }
            if (value != null && !value.trim().isEmpty()) values.add(value.trim());
        }
        return values;
    }

    private String imageSrc(JsonObject object, String key) {
        JsonObject image = object(object, key);
        if (image == null) return "";
        return firstNonEmpty(string(image, "src"), string(image, "url"));
    }

    private double minVariantPrice(JsonObject product) {
        JsonArray variants = array(product, "variants");
        if (variants == null) return 0d;
        double min = 0d;
        for (JsonElement element : variants) {
            if (!element.isJsonObject()) continue;
            JsonObject variant = element.getAsJsonObject();
            double price = doubleValue(variant, "price", 0d);
            if (price <= 0d) continue;
            if (min == 0d || price < min) min = price;
        }
        return min;
    }

    private List<String> parseIncidents(String body) {
        List<String> rows = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray incidents = array(root, "incidents");
        if (incidents == null) return rows;
        for (JsonElement element : incidents) {
            if (!element.isJsonObject()) continue;
            JsonObject incident = element.getAsJsonObject();
            String type = firstNonEmpty(string(incident, "incidentType"), string(incident, "incidentClass"));
            if ("injuryTime".equalsIgnoreCase(type)) continue;
            String minute = firstNonEmpty(string(incident, "time"), string(incident, "addedTime"));
            String player = playerName(incident, "player");
            String playerIn = playerName(incident, "playerIn");
            String playerOut = playerName(incident, "playerOut");
            String text = firstNonEmpty(string(incident, "text"), string(incident, "reason"));
            Integer homeScore = integer(incident, "homeScore");
            Integer awayScore = integer(incident, "awayScore");
            String score = homeScore == null || awayScore == null ? "" : " (" + homeScore + "-" + awayScore + ")";
            String displayType = displayIncidentType(type, text);
            boolean penaltyPeriod = "period".equalsIgnoreCase(type) && "PEN".equalsIgnoreCase(text);
            if ("999".equals(minute) || penaltyPeriod) minute = "";
            Boolean homeSide = firstBoolean(incident, "isHome", "isHomeTeam", "home");
            String side = homeSide == null ? displayType : (homeSide ? "Chủ nhà" : "Đội khách");
            String title = minute.isEmpty() ? displayType : minute + "' " + displayType;
            String incidentBody = incidentBody(type, player, playerIn, playerOut, text, displayType);
            String bodyText = penaltyPeriod
                    ? "Penalty shootout" + score
                    : side + " - " + incidentBody + score;
            if (!bodyText.trim().isEmpty()) rows.add(title + ": " + bodyText);
        }
        return rows;
    }

    private String incidentBody(String type, String player, String playerIn, String playerOut, String text, String displayType) {
        if ("substitution".equalsIgnoreCase(type)) {
            if (!playerIn.isEmpty() && !playerOut.isEmpty()) return "Vào: " + playerIn + " · Ra: " + playerOut;
            if (!playerIn.isEmpty()) return "Vào: " + playerIn;
            if (!playerOut.isEmpty()) return "Ra: " + playerOut;
            return firstNonEmpty(player, text, "Chưa có tên cầu thủ");
        }
        return firstNonEmpty(player, text, displayType);
    }

    private String displayIncidentType(String type, String text) {
        if ("penaltyShootout".equalsIgnoreCase(type)) return "Penalty";
        if ("period".equalsIgnoreCase(type) && "PEN".equalsIgnoreCase(text)) return "Penalty shootout";
        if ("goal".equalsIgnoreCase(type)) return "Goal";
        if ("card".equalsIgnoreCase(type)) {
            String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
            if (lower.contains("red")) return "Red card";
            if (lower.contains("yellow")) return "Yellow card";
            return "Card";
        }
        if ("substitution".equalsIgnoreCase(type)) return "Substitution";
        if ("injuryTime".equalsIgnoreCase(type)) return "Bù giờ";
        return type == null || type.trim().isEmpty() ? "Moment" : type;
    }

    private static String playerName(JsonObject incident, String key) {
        JsonObject player = object(incident, key);
        return firstNonEmpty(string(player, "shortName"), string(player, "name"));
    }

    private List<String> parseStatistics(String body) {
        List<String> rows = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray statistics = array(root, "statistics");
        if (statistics == null || statistics.size() == 0) return rows;
        for (JsonElement statistic : statistics) {
            if (!statistic.isJsonObject()) continue;
            JsonArray groups = array(statistic.getAsJsonObject(), "groups");
            if (groups == null) continue;
            for (JsonElement groupElement : groups) {
                if (!groupElement.isJsonObject()) continue;
                JsonObject group = groupElement.getAsJsonObject();
                JsonArray items = array(group, "statisticsItems");
                if (items == null) items = array(group, "items");
                if (items == null) continue;
                for (JsonElement itemElement : items) {
                    if (!itemElement.isJsonObject()) continue;
                    JsonObject item = itemElement.getAsJsonObject();
                    String name = firstNonEmpty(string(item, "name"), string(item, "key"));
                    String home = firstNonEmpty(string(item, "home"), string(item, "homeValue"));
                    String away = firstNonEmpty(string(item, "away"), string(item, "awayValue"));
                    if (!name.isEmpty() && (!home.isEmpty() || !away.isEmpty())) {
                        rows.add(name + ": " + home + " - " + away);
                    }
                }
            }
        }
        return rows;
    }

    private List<String> parseTeamDetails(String body) {
        List<String> rows = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonObject team = object(root, "team");
        if (team == null) team = root;
        JsonObject manager = firstObject(team, "manager", "coach");
        JsonObject venue = object(team, "venue");
        JsonObject primaryVenue = firstObject(venue, "stadium", "venue");
        JsonObject country = object(team, "country");

        String name = firstNonEmpty(string(team, "name"), string(team, "shortName"));
        String code = firstNonEmpty(string(team, "nameCode"), string(team, "slug"));
        String managerName = firstNonEmpty(string(manager, "name"), string(manager, "shortName"));
        String venueName = firstNonEmpty(string(primaryVenue, "name"), string(venue, "name"), nestedString(team, "venue", "name"));
        String countryName = string(country, "name");
        Long foundation = longObject(team, "foundationDateTimestamp");

        if (!name.isEmpty()) rows.add("Name: " + name);
        if (!code.isEmpty()) rows.add("Code: " + code);
        if (!managerName.isEmpty()) rows.add("Coach: " + managerName);
        if (!venueName.isEmpty()) rows.add("Stadium: " + venueName);
        if (!countryName.isEmpty()) rows.add("Country: " + countryName);
        if (foundation != null && foundation > 0L) rows.add("Founded: " + foundationYear(foundation));
        return rows;
    }

    private List<String> parseTeamPlayers(String body) {
        List<String> rows = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        appendTeamPlayerRows(rows, array(root, "players"));
        appendTeamPlayerRows(rows, array(root, "foreignPlayers"));
        appendTeamPlayerRows(rows, array(root, "nationalPlayers"));
        return rows;
    }

    private void appendTeamPlayerRows(List<String> rows, JsonArray players) {
        if (players == null) return;
        for (JsonElement element : players) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            JsonObject player = object(row, "player");
            if (player == null) player = row;
            String name = firstNonEmpty(string(player, "shortName"), string(player, "name"));
            if (name.isEmpty()) continue;
            String position = firstNonEmpty(string(row, "position"), string(player, "position"));
            String shirt = firstNonEmpty(string(row, "shirtNumber"), string(player, "jerseyNumber"));
            String country = nestedString(player, "country", "name");
            String suffix = "";
            if (!shirt.isEmpty()) suffix += " #" + shirt;
            if (!position.isEmpty()) suffix += " - " + position;
            if (!country.isEmpty()) suffix += " - " + country;
            rows.add(name + suffix);
        }
    }

    private List<String> parseTeamStatistics(String body) {
        List<String> rows = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonObject statistics = firstObject(root, "statistics", "teamStatistics", "overall");
        if (statistics == null) statistics = root;

        appendStatValue(rows, statistics, "Matches", "matches", "appearances", "played");
        appendStatValue(rows, statistics, "Goals", "goals", "goalsScored");
        appendStatValue(rows, statistics, "Goals conceded", "goalsConceded", "concededGoals");
        appendStatValue(rows, statistics, "Assists", "assists");
        appendStatValue(rows, statistics, "Shots", "shots", "totalShots");
        appendStatValue(rows, statistics, "Shots on target", "shotsOnTarget");
        appendStatValue(rows, statistics, "Ball possession", "averageBallPossession", "ballPossession");
        appendStatValue(rows, statistics, "Accurate passes", "accuratePasses");
        appendStatValue(rows, statistics, "Yellow cards", "yellowCards");
        appendStatValue(rows, statistics, "Red cards", "redCards");

        if (rows.isEmpty()) appendPrimitiveStats(rows, statistics, "", 0);
        return rows;
    }

    private void appendStatValue(List<String> rows, JsonObject object, String label, String... keys) {
        for (String key : keys) {
            String value = primitiveString(object, key);
            if (!value.isEmpty()) {
                rows.add(label + ": " + value);
                return;
            }
        }
    }

    private void appendPrimitiveStats(List<String> rows, JsonObject object, String prefix, int depth) {
        if (object == null || depth > 2 || rows.size() >= 24) return;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (rows.size() >= 24) return;
            JsonElement value = entry.getValue();
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (value == null || value.isJsonNull()) continue;
            if (value.isJsonPrimitive()) {
                String text = primitiveString(object, entry.getKey());
                if (!text.isEmpty()) rows.add(key + ": " + text);
            } else if (value.isJsonObject()) {
                appendPrimitiveStats(rows, value.getAsJsonObject(), key, depth + 1);
            }
        }
    }

    private List<String> parseMatchFacts(String body) {
        List<String> rows = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonObject event = object(root, "event");
        if (event == null) event = root;
        JsonObject venue = object(event, "venue");
        JsonObject referee = object(event, "referee");
        JsonObject roundInfo = object(event, "roundInfo");
        JsonObject status = object(event, "status");

        String venueName = firstNonEmpty(
                nestedString(venue, "stadium", "name"),
                string(venue, "name"),
                nestedString(venue, "city", "name")
        );
        String attendance = firstNonEmpty(string(event, "attendance"), string(event, "crowd"));
        String refereeName = firstNonEmpty(string(referee, "name"), nestedString(event, "referee", "name"));
        String roundName = firstNonEmpty(string(roundInfo, "name"),
                integer(roundInfo, "round") == null ? "" : "Round " + integer(roundInfo, "round"));
        String statusText = firstNonEmpty(string(status, "description"), string(status, "type"));

        if (!venueName.isEmpty()) rows.add("Venue: " + venueName);
        if (!attendance.isEmpty()) rows.add("Attendance: " + attendance);
        if (!refereeName.isEmpty()) rows.add("Referee: " + refereeName);
        if (!roundName.isEmpty()) rows.add("Round: " + roundName);
        if (!statusText.isEmpty()) rows.add("Status: " + displayMatchStatus(statusText));
        return rows;
    }

    private String displayMatchStatus(String statusText) {
        if ("AP".equalsIgnoreCase(statusText)) return "After penalties";
        if ("FT".equalsIgnoreCase(statusText)) return "Full time";
        return statusText;
    }

    private List<String> parseLineups(String body) {
        List<String> rows = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        appendLineupRows(rows, "Home", object(root, "home"));
        appendLineupRows(rows, "Away", object(root, "away"));
        return rows;
    }

    private void appendLineupRows(List<String> rows, String side, JsonObject lineup) {
        if (lineup == null) return;
        String formation = firstNonEmpty(string(lineup, "formation"), string(lineup, "formationName"));
        if (!formation.isEmpty()) rows.add(side + " formation: " + formation);
        JsonArray players = array(lineup, "players");
        if (players == null) return;
        int starters = 0;
        int substitutes = 0;
        for (JsonElement element : players) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            JsonObject player = object(row, "player");
            String name = firstNonEmpty(string(player, "shortName"), string(player, "name"));
            if (name.isEmpty()) continue;
            boolean substitute = booleanValue(row, "substitute", false);
            String shirt = firstNonEmpty(string(row, "shirtNumber"), string(player, "jerseyNumber"));
            String position = firstNonEmpty(string(row, "position"), string(player, "position"));
            Long playerId = longObject(player, "id");
            String photoUrl = playerId == null ? "" : "https://img.sofascore.com/api/v1/player/" + playerId + "/image";
            String prefix = substitute ? "SUB" : "XI";
            if (substitute) substitutes++; else starters++;
            rows.add(side + " " + prefix + ": "
                    + "id=" + (playerId == null ? "" : playerId)
                    + "|photo=" + photoUrl
                    + "|shirt=" + shirt
                    + "|name=" + name
                    + "|pos=" + position);
        }
        if (starters == 0 && substitutes == 0) {
            rows.add(side + ": No player rows returned");
        }
    }

    private List<String> parseTopRatedPlayers(String body) {
        List<RatedPlayer> players = new ArrayList<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        appendRatedPlayers(players, "Home", object(root, "home"));
        appendRatedPlayers(players, "Away", object(root, "away"));
        List<RatedPlayer> home = new ArrayList<>();
        List<RatedPlayer> away = new ArrayList<>();
        for (RatedPlayer player : players) {
            if ("Home".equals(player.side)) home.add(player);
            else away.add(player);
        }
        Collections.sort(home, (a, b) -> Double.compare(b.rating, a.rating));
        Collections.sort(away, (a, b) -> Double.compare(b.rating, a.rating));
        List<String> rows = new ArrayList<>();
        appendRatedRows(rows, home);
        appendRatedRows(rows, away);
        return rows;
    }

    private void appendRatedRows(List<String> rows, List<RatedPlayer> players) {
        int limit = Math.min(5, players.size());
        for (int i = 0; i < limit; i++) {
            RatedPlayer player = players.get(i);
            rows.add(player.side + ": "
                    + "id=" + (player.playerId == null ? "" : player.playerId)
                    + "|name=" + player.name
                    + "|rating=" + formatRating(player.rating));
        }
    }

    private void appendRatedPlayers(List<RatedPlayer> output, String side, JsonObject lineup) {
        if (lineup == null) return;
        JsonArray players = array(lineup, "players");
        if (players == null) return;
        for (JsonElement element : players) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            JsonObject player = object(row, "player");
            String name = firstNonEmpty(string(player, "shortName"), string(player, "name"));
            Long playerId = longObject(player, "id");
            Double rating = firstDouble(row, object(row, "statistics"), object(player, "statistics"),
                    "rating", "sofaScoreRating", "averageRating");
            if (!name.isEmpty() && rating != null && rating > 0d) {
                output.add(new RatedPlayer(side, name, rating, playerId));
            }
        }
    }

    private static final class RatedPlayer {
        final String side;
        final String name;
        final double rating;
        final Long playerId;

        RatedPlayer(String side, String name, double rating, Long playerId) {
            this.side = side;
            this.name = name;
            this.rating = rating;
            this.playerId = playerId;
        }
    }

    private String category(JsonObject post) {
        JsonObject embedded = object(post, "_embedded");
        JsonArray terms = array(embedded, "wp:term");
        if (terms == null || terms.size() == 0 || !terms.get(0).isJsonArray()) return "Tin V-League";
        JsonArray categories = terms.get(0).getAsJsonArray();
        if (categories.size() == 0 || !categories.get(0).isJsonObject()) return "Tin V-League";
        return firstNonEmpty(string(categories.get(0).getAsJsonObject(), "name"), "Tin V-League");
    }

    private String featuredImage(JsonObject post) {
        JsonObject embedded = object(post, "_embedded");
        JsonArray mediaArray = array(embedded, "wp:featuredmedia");
        if (mediaArray == null || mediaArray.size() == 0 || !mediaArray.get(0).isJsonObject()) return "";
        JsonObject media = mediaArray.get(0).getAsJsonObject();
        JsonObject sizes = object(object(media, "media_details"), "sizes");
        String[] preferredSizes = {"large", "medium_large", "medium", "thumbnail"};
        for (String size : preferredSizes) {
            String source = nestedString(sizes, size, "source_url");
            if (!source.isEmpty()) return source;
        }
        return string(media, "source_url");
    }

    private void recordSync(String syncKey, boolean success, int count, String message) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("source", syncKey);
            data.put("status", success ? "success" : "error");
            data.put("itemCount", count);
            data.put("message", message == null ? "" : message);
            data.put("updatedAt", FieldValue.serverTimestamp());
            FirebaseFirestore.getInstance().collection("sync_status").document(syncKey).set(data);
        } catch (Exception ignored) {
            // Sync logging must never break the user-facing API flow.
        }
    }

    private int itemCount(Object parsed) {
        return parsed instanceof List<?> ? ((List<?>) parsed).size() : 1;
    }

    private static JsonObject object(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) return null;
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) return null;
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static JsonObject firstObject(JsonObject object, String... keys) {
        if (object == null || keys == null) return null;
        for (String key : keys) {
            JsonObject value = object(object, key);
            if (value != null) return value;
        }
        return null;
    }

    private static String nestedString(JsonObject object, String objectKey, String stringKey) {
        return string(object(object, objectKey), stringKey);
    }

    private static String string(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) return "";
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return "";
        try {
            return element.getAsString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String primitiveString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) return "";
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) return "";
        try {
            return element.getAsString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static Long longObject(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) return null;
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return null;
        try {
            return element.getAsLong();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static long longValue(JsonObject object, String key, long fallback) {
        Long value = longObject(object, key);
        return value == null ? fallback : value;
    }

    private static Integer integer(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) return null;
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return null;
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer firstInteger(JsonObject object, String... keys) {
        if (object == null) return null;
        for (String key : keys) {
            Integer value = integer(object, key);
            if (value != null) return value;
        }
        return null;
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        Integer value = integer(object, key);
        return value == null ? fallback : value;
    }

    private static int firstInt(JsonObject object, int fallback, String... keys) {
        if (object == null) return fallback;
        for (String key : keys) {
            Integer value = integer(object, key);
            if (value != null) return value;
        }
        return fallback;
    }

    private static double doubleValue(JsonObject object, String key, double fallback) {
        if (object == null || key == null || !object.has(key)) return fallback;
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Double firstDouble(JsonObject first, JsonObject second, JsonObject third, String... keys) {
        JsonObject[] objects = {first, second, third};
        for (JsonObject object : objects) {
            if (object == null) continue;
            for (String key : keys) {
                if (!object.has(key)) continue;
                JsonElement element = object.get(key);
                if (element == null || element.isJsonNull()) continue;
                try {
                    return element.getAsDouble();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static boolean booleanValue(JsonObject object, String key, boolean fallback) {
        if (object == null || key == null || !object.has(key)) return fallback;
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Boolean firstBoolean(JsonObject object, String... keys) {
        if (object == null || keys == null) return null;
        for (String key : keys) {
            if (key == null || !object.has(key)) continue;
            JsonElement element = object.get(key);
            if (element == null || element.isJsonNull()) continue;
            try {
                return element.getAsBoolean();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static int parseSignedInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.replace("+", "").trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private static String cleanSeasonLabel(String value) {
        String label = firstNonEmpty(value);
        label = label.replace("V-League 1", "")
                .replace("V.League 1", "")
                .replace("V.League", "")
                .replace("V-League", "")
                .trim();
        return label.isEmpty() ? firstNonEmpty(value) : label;
    }

    private static String fullImageUrl(String value) {
        return fullImageUrl(value, FIRST_FOOTBALL_BASE);
    }

    private static String fullImageUrl(String value, String baseUrl) {
        if (value == null || value.trim().isEmpty()) return "";
        String image = value.trim();
        if (image.startsWith("//")) return "https:" + image;
        if (image.startsWith("http")) return image;
        if (image.startsWith("/")) return firstNonEmpty(baseUrl, FIRST_FOOTBALL_BASE) + image;
        return image;
    }

    private static String formatVnd(int value) {
        if (value <= 0) return "0d";
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value) + "d";
    }

    private static String formatRating(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static String endpointName(String url) {
        if (url == null) return "";
        int marker = url.indexOf("/api/v1/");
        return marker >= 0 ? url.substring(marker + "/api/v1/".length()) : url;
    }

    private static String foundationYear(long timestamp) {
        try {
            return String.valueOf(java.time.Instant.ofEpochSecond(timestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .getYear());
        } catch (Exception ignored) {
            return String.valueOf(timestamp);
        }
    }

    private static String cleanHtml(String value) {
        if (value == null || value.isEmpty()) return "";
        return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String excerpt(String value, int limit) {
        if (value.length() <= limit) return value;
        return value.substring(0, Math.max(0, limit - 3)).trim() + "...";
    }

    private static String formatNewsDate(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            return LocalDateTime.parse(value, NEWS_INPUT).format(NEWS_OUTPUT);
        } catch (Exception ignored) {
            return value;
        }
    }
}
