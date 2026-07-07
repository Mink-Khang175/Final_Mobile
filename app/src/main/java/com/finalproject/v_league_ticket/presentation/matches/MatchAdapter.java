package com.finalproject.v_league_ticket.presentation.matches;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.domain.model.Fixture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MatchAdapter extends ListAdapter<Fixture, RecyclerView.ViewHolder> {
    public enum Mode { FIXTURES, RESULTS }

    private static final int VIEW_TYPE_MATCH = 0;
    private static final int VIEW_TYPE_ROUND_HEADER = 1;
    private static final String ROUND_HEADER_STATUS_TYPE = "ROUND_HEADER";
    private static Map<String, String> cachedStadiumsByClub;

    public interface OnMatchClick {
        void onClick(Fixture fixture);
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.forLanguageTag("vi-VN"));
    private final Mode mode;
    private final OnMatchClick onMatchClick;

    public MatchAdapter(Mode mode) {
        this(mode, fixture -> { });
    }

    public MatchAdapter(Mode mode, OnMatchClick onMatchClick) {
        super(DIFF);
        this.mode = mode;
        this.onMatchClick = onMatchClick;
    }

    @Override
    public int getItemViewType(int position) {
        return isRoundHeader(getItem(position)) ? VIEW_TYPE_ROUND_HEADER : VIEW_TYPE_MATCH;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ROUND_HEADER) {
            ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_result_round_header, parent, false);
            return new RoundHeaderViewHolder(view);
        }
        ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_card, parent, false);
        return new MatchViewHolder(view, mode, onMatchClick);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Fixture item = getItem(position);
        if (holder instanceof RoundHeaderViewHolder) {
            ((RoundHeaderViewHolder) holder).bind(item);
        } else {
            ((MatchViewHolder) holder).bind(item);
        }
    }

    public static Fixture roundHeader(String label, int matchCount, int index) {
        return new Fixture(-900_000L - index, null, null, safeLabel(label), String.valueOf(matchCount),
                null, null, null, null, "", ROUND_HEADER_STATUS_TYPE, null, "",
                null, safeLabel(label), "", "");
    }

    private static boolean isRoundHeader(Fixture fixture) {
        return ROUND_HEADER_STATUS_TYPE.equals(fixture.getStatusType());
    }

    private static String safeLabel(String label) {
        return label == null || label.trim().isEmpty() ? "Vòng đấu" : label.trim();
    }

    static class RoundHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView count;

        RoundHeaderViewHolder(ViewGroup itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvRoundTitle);
            count = itemView.findViewById(R.id.tvRoundCount);
        }

        void bind(Fixture header) {
            title.setText(header.getHomeTeamName());
            count.setText(header.getAwayTeamName() + " trận");
        }
    }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        private final Mode mode;
        private final OnMatchClick onMatchClick;
        private final ImageView homeLogo;
        private final ImageView awayLogo;
        private final TextView homeName;
        private final TextView awayName;
        private final TextView centerValue;
        private final TextView status;
        private final TextView penaltyScore;
        private final TextView venue;
        private final TextView date;
        private final TextView action;
        MatchViewHolder(ViewGroup itemView, Mode mode, OnMatchClick onMatchClick) {
            super(itemView);
            this.mode = mode;
            this.onMatchClick = onMatchClick;
            homeLogo = itemView.findViewById(R.id.imgHomeLogo);
            awayLogo = itemView.findViewById(R.id.imgAwayLogo);
            homeName = itemView.findViewById(R.id.tvHomeName);
            awayName = itemView.findViewById(R.id.tvAwayName);
            centerValue = itemView.findViewById(R.id.tvCenterValue);
            status = itemView.findViewById(R.id.tvStatus);
            penaltyScore = itemView.findViewById(R.id.tvPenaltyScore);
            venue = itemView.findViewById(R.id.tvVenue);
            date = itemView.findViewById(R.id.tvDate);
            action = itemView.findViewById(R.id.tvAction);
        }

        void bind(Fixture fixture) {
            homeName.setText(fixture.getHomeTeamName());
            awayName.setText(fixture.getAwayTeamName());
            venue.setText(displayVenue(fixture));
            date.setText(date(fixture.getStartTimestamp()));
            itemView.setOnClickListener(v -> openIfResult(fixture));
            action.setOnClickListener(v -> openIfResult(fixture));

            if (mode == Mode.RESULTS && fixture.hasScore()) {
                centerValue.setText(fixture.getHomeScore() + " : " + fixture.getAwayScore());
                status.setText(fixture.getStatus().isEmpty() ? "FT" : fixture.getStatus().toUpperCase());
                boolean hasPenalty = fixture.getHomePenaltyScore() != null && fixture.getAwayPenaltyScore() != null;
                penaltyScore.setVisibility(hasPenalty ? View.VISIBLE : View.GONE);
                penaltyScore.setText(hasPenalty ? "PEN " + fixture.getHomePenaltyScore() + " - " + fixture.getAwayPenaltyScore() : "");
                action.setText("Match Center  >");
            } else {
                centerValue.setText(time(fixture.getStartTimestamp()));
                status.setText("SẮP ĐẤU");
                penaltyScore.setVisibility(View.GONE);
                action.setText("Mua vé  >");
            }
            loadLogo(homeLogo, fixture.getHomeLogoUrl());
            loadLogo(awayLogo, fixture.getAwayLogoUrl());
        }

        private void openIfResult(Fixture fixture) {
            if (mode == Mode.RESULTS && fixture.hasScore()) onMatchClick.onClick(fixture);
            if (mode == Mode.FIXTURES) onMatchClick.onClick(fixture);
        }

        private void loadLogo(ImageView imageView, String url) {
            Glide.with(imageView).load(url == null || url.isEmpty() ? null : url)
                    .placeholder(R.drawable.ic_logo).error(R.drawable.ic_logo).centerInside().into(imageView);
        }

        private String time(Long timestamp) {
            return timestamp == null ? "TBD" : Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
        }

        private String date(Long timestamp) {
            return timestamp == null ? "Chưa có ngày" : Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
        }

        private String displayVenue(Fixture fixture) {
            if (!fixture.getVenue().isEmpty()) return fixture.getVenue();
            String stadium = stadiumFor(fixture.getHomeTeamName());
            return stadium.isEmpty() ? "Sân đang cập nhật" : stadium;
        }

        private String stadiumFor(String clubName) {
            Map<String, String> stadiumsByClub = stadiums(itemView);
            String normalized = normalizeClub(clubName);
            String exact = stadiumsByClub.get(normalized);
            if (exact != null) return exact;
            for (Map.Entry<String, String> entry : stadiumsByClub.entrySet()) {
                if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized)) {
                    return entry.getValue();
                }
            }
            return "";
        }

        private static synchronized Map<String, String> stadiums(View itemView) {
            if (cachedStadiumsByClub == null) cachedStadiumsByClub = loadStadiums(itemView);
            return cachedStadiumsByClub;
        }

        private static Map<String, String> loadStadiums(View itemView) {
            Map<String, String> map = new HashMap<>();
            try (InputStream stream = itemView.getContext().getAssets().open("clubStadiums.json")) {
                byte[] bytes = new byte[stream.available()];
                int ignored = stream.read(bytes);
                JsonArray array = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonArray();
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) continue;
                    JsonObject object = element.getAsJsonObject();
                    String stadium = string(object, "stadiumName");
                    if (stadium.isEmpty()) continue;
                    putClub(map, string(object, "clubName"), stadium);
                    putClub(map, string(object, "clubCode"), stadium);
                }
            } catch (Exception ignored) {
            }
            return map;
        }

        private static void putClub(Map<String, String> map, String club, String stadium) {
            String key = normalizeClub(club);
            if (!key.isEmpty()) map.put(key, stadium);
        }

        private static String string(JsonObject object, String key) {
            if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
            try {
                return object.get(key).getAsString();
            } catch (Exception ignored) {
                return "";
            }
        }

        private static String normalizeClub(String value) {
            if (value == null) return "";
            String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT)
                    .replace("fc", "")
                    .replaceAll("[^a-z0-9]+", " ")
                    .trim();
            if (normalized.equals("viettel")) return "the cong";
            return normalized;
        }
    }

    private static final DiffUtil.ItemCallback<Fixture> DIFF = new DiffUtil.ItemCallback<Fixture>() {
        @Override
        public boolean areItemsTheSame(Fixture oldItem, Fixture newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(Fixture oldItem, Fixture newItem) {
            return oldItem.equals(newItem);
        }
    };
}
