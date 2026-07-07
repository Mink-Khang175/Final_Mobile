package com.finalproject.v_league_ticket.data.remote;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class VietnamAddressApiClient {
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Throwable throwable);
    }

    public static final class Division {
        public final int code;
        public final String name;

        Division(int code, String name) {
            this.code = code;
            this.name = name == null ? "" : name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String BASE_URL = "https://provinces.open-api.vn/api/v1";
    private static VietnamAddressApiClient instance;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<Integer, List<Division>> districtCache = new LinkedHashMap<>();
    private final Map<Integer, List<Division>> wardCache = new LinkedHashMap<>();
    private List<Division> provinceCache;

    private VietnamAddressApiClient() {
    }

    public static synchronized VietnamAddressApiClient getInstance() {
        if (instance == null) instance = new VietnamAddressApiClient();
        return instance;
    }

    public void fetchProvinces(DataCallback<List<Division>> callback) {
        if (provinceCache != null) {
            postSuccess(callback, provinceCache);
            return;
        }
        request(BASE_URL + "/p/", body -> {
            provinceCache = parseDivisions(JsonParser.parseString(body).getAsJsonArray());
            postSuccess(callback, provinceCache);
        }, callback);
    }

    public void fetchDistricts(int provinceCode, DataCallback<List<Division>> callback) {
        if (districtCache.containsKey(provinceCode)) {
            postSuccess(callback, districtCache.get(provinceCode));
            return;
        }
        request(BASE_URL + "/p/" + provinceCode + "?depth=2", body -> {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            List<Division> rows = parseDivisions(array(root, "districts"));
            districtCache.put(provinceCode, rows);
            postSuccess(callback, rows);
        }, callback);
    }

    public void fetchWards(int districtCode, DataCallback<List<Division>> callback) {
        if (wardCache.containsKey(districtCode)) {
            postSuccess(callback, wardCache.get(districtCode));
            return;
        }
        request(BASE_URL + "/d/" + districtCode + "?depth=2", body -> {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            List<Division> rows = parseDivisions(array(root, "wards"));
            wardCache.put(districtCode, rows);
            postSuccess(callback, rows);
        }, callback);
    }

    private void request(String url, BodyConsumer consumer, DataCallback<List<Division>> callback) {
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        postError(callback, new IOException("Vietnam address API error: " + response.code()));
                        return;
                    }
                    consumer.accept(responseBody.string());
                } catch (Exception e) {
                    postError(callback, e);
                }
            }
        });
    }

    private List<Division> parseDivisions(JsonArray array) {
        if (array == null) return Collections.emptyList();
        List<Division> rows = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            rows.add(new Division(intValue(object, "code"), string(object, "name")));
        }
        return rows;
    }

    private JsonArray array(JsonObject object, String key) {
        JsonElement element = object == null ? null : object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private int intValue(JsonObject object, String key) {
        JsonElement element = object == null ? null : object.get(key);
        try {
            return element == null || element.isJsonNull() ? 0 : element.getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String string(JsonObject object, String key) {
        JsonElement element = object == null ? null : object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private void postSuccess(DataCallback<List<Division>> callback, List<Division> data) {
        mainHandler.post(() -> callback.onSuccess(data == null ? Collections.emptyList() : data));
    }

    private void postError(DataCallback<List<Division>> callback, Throwable throwable) {
        mainHandler.post(() -> callback.onError(throwable));
    }

    private interface BodyConsumer {
        void accept(String body) throws Exception;
    }
}
