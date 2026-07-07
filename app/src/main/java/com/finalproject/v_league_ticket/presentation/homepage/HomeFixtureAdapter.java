package com.finalproject.v_league_ticket.presentation.homepage;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.ItemHomeFixtureBinding;
import com.finalproject.v_league_ticket.domain.model.Fixture;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class HomeFixtureAdapter extends ListAdapter<Fixture, HomeFixtureAdapter.FixtureViewHolder> {
    public interface OnFixtureClick {
        void onClick(Fixture fixture);
    }

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("vi-VN"));
    private static final Map<String, String> HOME_STADIUMS = new LinkedHashMap<>();

    static {
        HOME_STADIUMS.put("cong an ha noi", "Sân vận động Hàng Đẫy");
        HOME_STADIUMS.put("cahn", "Sân vận động Hàng Đẫy");
        HOME_STADIUMS.put("ha noi", "Sân vận động Hàng Đẫy");
        HOME_STADIUMS.put("hanoi", "Sân vận động Hàng Đẫy");
        HOME_STADIUMS.put("viettel", "Sân vận động Hàng Đẫy");
        HOME_STADIUMS.put("the cong", "Sân vận động Hàng Đẫy");
        HOME_STADIUMS.put("nam dinh", "Sân vận động Thiên Trường");
        HOME_STADIUMS.put("hai phong", "Sân vận động Lạch Tray");
        HOME_STADIUMS.put("hoang anh gia lai", "Sân vận động Pleiku");
        HOME_STADIUMS.put("hagl", "Sân vận động Pleiku");
        HOME_STADIUMS.put("song lam nghe an", "Sân vận động Vinh");
        HOME_STADIUMS.put("slna", "Sân vận động Vinh");
        HOME_STADIUMS.put("thanh hoa", "Sân vận động Thanh Hóa");
        HOME_STADIUMS.put("hong linh ha tinh", "Sân vận động Hà Tĩnh");
        HOME_STADIUMS.put("ha tinh", "Sân vận động Hà Tĩnh");
        HOME_STADIUMS.put("ninh binh", "Sân vận động Ninh Bình");
        HOME_STADIUMS.put("cong an tp.hcm", "Sân vận động Thống Nhất");
        HOME_STADIUMS.put("ca tp.hcm", "Sân vận động Thống Nhất");
        HOME_STADIUMS.put("tp.hcm", "Sân vận động Thống Nhất");
        HOME_STADIUMS.put("becamex binh duong", "Sân vận động Gò Đậu");
        HOME_STADIUMS.put("binh duong", "Sân vận động Gò Đậu");
        HOME_STADIUMS.put("pvf", "Sân vận động PVF");
        HOME_STADIUMS.put("da nang", "Sân vận động Hòa Xuân");
        HOME_STADIUMS.put("quang nam", "Sân vận động Tam Kỳ");
        HOME_STADIUMS.put("quy nhon binh dinh", "Sân vận động Quy Nhơn");
        HOME_STADIUMS.put("binh dinh", "Sân vận động Quy Nhơn");
    }

    private final OnFixtureClick onFixtureClick;

    public HomeFixtureAdapter(OnFixtureClick onFixtureClick) {
        super(DIFF);
        this.onFixtureClick = onFixtureClick;
    }

    @Override
    public FixtureViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemHomeFixtureBinding binding = ItemHomeFixtureBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FixtureViewHolder(binding, onFixtureClick);
    }

    @Override
    public void onBindViewHolder(FixtureViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class FixtureViewHolder extends RecyclerView.ViewHolder {
        private final ItemHomeFixtureBinding binding;
        private final OnFixtureClick onFixtureClick;

        FixtureViewHolder(ItemHomeFixtureBinding binding, OnFixtureClick onFixtureClick) {
            super(binding.getRoot());
            this.binding = binding;
            this.onFixtureClick = onFixtureClick;
        }

        void bind(Fixture item) {
            binding.getRoot().setOnClickListener(v -> onFixtureClick.onClick(item));
            binding.tvFixtureRound.setText(item.getRound() == null ? "V.League" : "Round " + item.getRound());
            binding.tvFixtureDate.setText(date(item.getStartTimestamp()));
            binding.tvFixtureTime.setText(time(item.getStartTimestamp()));
            binding.tvFixtureHomeTeam.setText(item.getHomeTeamName());
            binding.tvFixtureAwayTeam.setText(item.getAwayTeamName());
            binding.tvFixtureVenue.setText(displayVenue(item));
            loadLogo(binding.imgFixtureHomeLogo, item.getHomeLogoUrl());
            loadLogo(binding.imgFixtureAwayLogo, item.getAwayLogoUrl());
        }

        private String displayVenue(Fixture item) {
            if (item.getVenue() != null && !item.getVenue().trim().isEmpty()) {
                return item.getVenue().trim();
            }
            String home = normalize(item.getHomeTeamName());
            for (Map.Entry<String, String> entry : HOME_STADIUMS.entrySet()) {
                if (home.contains(entry.getKey()) || entry.getKey().contains(home)) {
                    return entry.getValue();
                }
            }
            return "Sân đang cập nhật";
        }

        private void loadLogo(ImageView view, String url) {
            Glide.with(view).load(url == null || url.isEmpty() ? null : url)
                    .placeholder(R.drawable.ic_logo)
                    .error(R.drawable.ic_logo)
                    .centerInside()
                    .into(view);
        }

        private String date(Long timestamp) {
            return timestamp == null ? "TBA" : Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
        }

        private String time(Long timestamp) {
            return timestamp == null ? "TBA" : Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
        }

        private String normalize(String value) {
            if (value == null) return "";
            String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                    .replace("đ", "d")
                    .replace("Đ", "D")
                    .toLowerCase(Locale.ROOT);
            return normalized.replaceAll("[^a-z0-9.]+", " ").trim();
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
