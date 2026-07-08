package com.finalproject.v_league_ticket.presentation.club;

import android.content.Context;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClubProfileDirectory {
    private static List<Meta> cachedRows;

    private ClubProfileDirectory() {
    }

    public static final class Meta {
        public final String key;
        public final String code;
        public final String name;
        public final String stadium;
        public final String city;
        public final String primaryColor;
        public final String secondaryColor;
        public final long teamId;

        Meta(String key, String code, String name, String stadium, String city,
             String primaryColor, String secondaryColor, long teamId) {
            this.key = key;
            this.code = code;
            this.name = name;
            this.stadium = stadium;
            this.city = city;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.teamId = teamId;
        }

        public String logoUrl() {
            return teamId <= 0 ? "" : "https://img.sofascore.com/api/v1/team/" + teamId + "/image";
        }

        public int primaryColorInt() {
            return Color.parseColor(primaryColor);
        }

        public int secondaryColorInt() {
            return Color.parseColor(secondaryColor);
        }
    }

    public static Meta find(Context context, String name) {
        String wanted = canonicalKey(name);
        for (Meta meta : all(context)) {
            if (wanted.equals(meta.key)) return meta;
        }
        for (Meta meta : all(context)) {
            if (wanted.contains(meta.key) || meta.key.contains(wanted)) return meta;
        }
        return null;
    }

    public static synchronized List<Meta> all(Context context) {
        if (cachedRows == null) {
            Map<String, Meta> rows = defaults();
            mergeAssetCodes(context == null ? null : context.getApplicationContext(), rows);
            cachedRows = new ArrayList<>(rows.values());
        }
        return new ArrayList<>(cachedRows);
    }

    public static String canonicalKey(String value) {
        String key = normalize(value)
                .replace("-fc", "")
                .replace("fc-", "")
                .replace("-clb", "")
                .replace("clb-", "");
        if (key.equals("hagl") || key.contains("hoang-anh-gia-lai")) return "hoang-anh-gia-lai";
        if (key.equals("cahn") || key.contains("cong-an-ha-noi")) return "cong-an-ha-noi";
        if (key.equals("cahcm") || key.equals("cahcmc") || key.contains("cong-an-tp-ho-chi-minh")
                || key.contains("cong-an-tp-hcm") || key.contains("cong-an-ho-chi-minh")
                || key.contains("cong-an-hcm")) return "cong-an-tp-ho-chi-minh";
        if (key.contains("pvf-cand") || key.contains("pvf-cong-an-nhan-dan")) return "pvf-cand";
        if (key.contains("the-cong-viettel") || key.contains("the-cong") || key.equals("viettel")) return "the-cong-viettel";
        if (key.contains("thep-xanh-nam-dinh") || key.equals("nam-dinh")) return "nam-dinh";
        if (key.contains("dong-a-thanh-hoa") || key.equals("thanh-hoa")) return "thanh-hoa";
        if (key.contains("shb-da-nang") || key.equals("da-nang")) return "da-nang";
        if (key.equals("slna") || key.contains("song-lam-nghe-an")) return "song-lam-nghe-an";
        if (key.contains("hong-linh-ha-tinh") || key.equals("ha-tinh")) return "hong-linh-ha-tinh";
        if (key.contains("becamex") || key.contains("binh-duong")) return "becamex-tp-hcm";
        if (key.equals("ha-noi") || key.equals("hanoi")) return "ha-noi";
        if (key.contains("hai-phong")) return "hai-phong";
        if (key.contains("ninh-binh")) return "ninh-binh";
        if (key.contains("bac-ninh")) return "bac-ninh";
        return key;
    }

    private static Map<String, Meta> defaults() {
        Map<String, Meta> rows = new LinkedHashMap<>();
        put(rows, "cong-an-ha-noi", "CAHN", "Công An Hà Nội", "Sân vận động Hàng Đẫy", "Hà Nội", "#D71920", "#F4B400", 193616L);
        put(rows, "the-cong-viettel", "Thể Công", "Thể Công Viettel", "Sân vận động Hàng Đẫy", "Hà Nội", "#D71920", "#0B122B", 231270L);
        put(rows, "ninh-binh", "Ninh Bình", "Ninh Bình", "Sân vận động Ninh Bình", "Ninh Bình", "#A11D2D", "#F0B323", 253057L);
        put(rows, "ha-noi", "Hà Nội", "Hà Nội FC", "Sân vận động Hàng Đẫy", "Hà Nội", "#5B2DAA", "#F2C94C", 33130L);
        put(rows, "cong-an-tp-ho-chi-minh", "CAHCMC", "Công An TP.HCM", "Sân vận động Thống Nhất", "TP. Hồ Chí Minh", "#C9182B", "#102A43", 33120L);
        put(rows, "nam-dinh", "Thép Xanh Nam Định", "Thép Xanh Nam Định", "Sân vận động Thiên Trường", "Nam Định", "#0E7AC4", "#F2C94C", 33127L);
        put(rows, "hai-phong", "Hải Phòng", "Hải Phòng FC", "Sân vận động Lạch Tray", "Hải Phòng", "#D71920", "#FFFFFF", 33124L);
        put(rows, "hong-linh-ha-tinh", "Hồng Lĩnh Hà Tĩnh", "Hồng Lĩnh Hà Tĩnh", "Sân vận động Hà Tĩnh", "Hà Tĩnh", "#C62828", "#111827", 309496L);
        put(rows, "thanh-hoa", "Thanh Hóa", "Đông Á Thanh Hóa", "Sân vận động Thanh Hóa", "Thanh Hóa", "#F47C20", "#0B122B", 33030L);
        put(rows, "song-lam-nghe-an", "Sông Lam Nghệ An", "Sông Lam Nghệ An", "Sân vận động Vinh", "Nghệ An", "#F6C400", "#0B122B", 33129L);
        put(rows, "hoang-anh-gia-lai", "Hoàng Anh Gia Lai", "Hoàng Anh Gia Lai", "Sân vận động Pleiku", "Gia Lai", "#F28C28", "#5A2D82", 33125L);
        put(rows, "becamex-tp-hcm", "Becamex Bình Dương", "Becamex TP.HCM", "Sân vận động Gò Đậu", "Bình Dương", "#6D28D9", "#F97316", 33119L);
        put(rows, "da-nang", "SHB Đà Nẵng", "SHB Đà Nẵng", "Sân vận động Hòa Xuân", "Đà Nẵng", "#F97316", "#0B122B", 33121L);
        put(rows, "pvf-cand", "PVF-CAND", "PVF-CAND", "Sân vận động PVF", "Hưng Yên", "#E32227", "#F6C400", 465466L);
        put(rows, "bac-ninh", "Bắc Ninh", "Bắc Ninh", "Sân vận động Bắc Ninh", "Bắc Ninh", "#0061A8", "#F6C400", 516953L);
        return rows;
    }

    private static void put(Map<String, Meta> rows, String key, String code, String name, String stadium,
                            String city, String primaryColor, String secondaryColor, long teamId) {
        rows.put(key, new Meta(key, code, name, stadium, city, primaryColor, secondaryColor, teamId));
    }

    private static void mergeAssetCodes(Context context, Map<String, Meta> rows) {
        if (context == null) return;
        try (InputStream stream = context.getAssets().open("clubStadiums.json")) {
            byte[] bytes = new byte[stream.available()];
            int ignored = stream.read(bytes);
            JSONArray array = new JSONArray(new String(bytes));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String code = item.optString("clubCode", "");
                String key = canonicalKey(code);
                Meta current = rows.get(key);
                if (current == null) continue;
                rows.put(key, new Meta(current.key, code.isEmpty() ? current.code : code, current.name,
                        current.stadium, current.city, current.primaryColor, current.secondaryColor, current.teamId));
            }
        } catch (Exception ignored) {
            // Local defaults above keep the UI usable even when the asset is malformed.
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
