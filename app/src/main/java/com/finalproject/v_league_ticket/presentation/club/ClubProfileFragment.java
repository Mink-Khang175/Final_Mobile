package com.finalproject.v_league_ticket.presentation.club;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.data.remote.VLeagueApiClient;
import com.finalproject.v_league_ticket.databinding.FragmentClubProfileBinding;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.finalproject.v_league_ticket.presentation.matches.FixturesFragment;
import com.finalproject.v_league_ticket.presentation.news.NewsFragment;
import com.finalproject.v_league_ticket.presentation.profile.ProfileFragment;
import com.finalproject.v_league_ticket.presentation.shop.ShopClub;
import com.finalproject.v_league_ticket.presentation.shop.ShopFragment;
import com.finalproject.v_league_ticket.presentation.shop.ShopProduct;
import com.finalproject.v_league_ticket.presentation.shop.ShopProductAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClubProfileFragment extends Fragment {
    private static final String ARG_NAME = "club_name";
    private static final Map<Long, List<String>> DETAILS_CACHE = new HashMap<>();
    private static final Map<Long, List<String>> STATS_CACHE = new HashMap<>();
    private static final Map<Long, List<String>> PLAYERS_CACHE = new HashMap<>();
    private static final Map<String, List<ShopProduct>> CLUB_SHOP_CACHE = new HashMap<>();

    private FragmentClubProfileBinding binding;
    private String clubName;
    private Long sofaTeamId;
    private ClubProfileDirectory.Meta clubMeta;
    private String activeTab = "overview";

    public ClubProfileFragment() {
        super(R.layout.fragment_club_profile);
    }

    public static ClubProfileFragment newInstance(ShopClub club) {
        ClubProfileFragment fragment = new ClubProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, club.getName());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentClubProfileBinding.bind(view);
        clubName = requireArguments().getString(ARG_NAME, "V.League Club");
        clubMeta = ClubProfileDirectory.find(requireContext(), clubName);
        bindLocalClubShell();
        Glide.with(binding.imgClubHero).load(R.drawable.svd).centerCrop().into(binding.imgClubHero);
        setupClicks();
        loadClubProfile();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupClicks() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnBuyTickets.setOnClickListener(v -> Toast.makeText(requireContext(), "Tính năng mua vé sẽ sớm được mở.", Toast.LENGTH_SHORT).show());
        binding.tabOverview.setOnClickListener(v -> loadClubProfile());
        binding.tabStatistics.setOnClickListener(v -> loadClubStatistics());
        binding.tabPlayers.setOnClickListener(v -> loadClubPlayers());
        binding.tabShop.setOnClickListener(v -> loadClubProducts());
        binding.bottomNav.navHome.setOnClickListener(v -> navigateTo(new HomepageFragment()));
        binding.bottomNav.navMatches.setOnClickListener(v -> navigateTo(new FixturesFragment()));
        binding.bottomNav.navNews.setOnClickListener(v -> navigateTo(new NewsFragment()));
        binding.bottomNav.navProfile.setOnClickListener(v -> navigateTo(AuthSession.hasToken(requireContext()) ? new ProfileFragment() : new AuthLoginFragment()));
    }

    private void loadClubProfile() {
        setActiveTab("overview");
        binding.progressClub.setVisibility(View.VISIBLE);
        renderOverview(null);
        resolveSofaTeam(() -> {
            if (binding == null) return;
            if (sofaTeamId != null) {
                loadSofaOverview(sofaTeamId);
            } else {
                loadFirestoreProfile();
            }
        });
    }

    private void loadFirestoreProfile() {
        FirebaseFirestore.getInstance().collection("club_profiles").document(normalizeKey(clubName)).get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    binding.progressClub.setVisibility(View.GONE);
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        addContent("Thông tin CLB đang được cập nhật.");
                        return;
                    }
                    bindClubDocument(task.getResult());
                });
    }

    private void resolveSofaTeam(Runnable onDone) {
        if (sofaTeamId != null) {
            onDone.run();
            return;
        }
        sofaTeamId = localTeamId();
        onDone.run();
    }

    private void loadSofaOverview(long teamId) {
        if (DETAILS_CACHE.containsKey(teamId)) {
            binding.progressClub.setVisibility(View.GONE);
            List<String> cachedRows = DETAILS_CACHE.get(teamId);
            bindSofaDetails(cachedRows);
            renderOverview(cachedRows);
            return;
        }
        VLeagueApiClient.getInstance().fetchTeamDetails(teamId, new VLeagueApiClient.DataCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> rows) {
                if (binding == null) return;
                DETAILS_CACHE.put(teamId, rows == null ? new ArrayList<>() : new ArrayList<>(rows));
                binding.progressClub.setVisibility(View.GONE);
                bindSofaDetails(rows);
                renderOverview(rows);
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding == null) return;
                loadFirestoreProfile();
            }
        });
    }

    private void loadClubPlayers() {
        setActiveTab("players");
        binding.progressClub.setVisibility(View.VISIBLE);
        addContent("");
        resolveSofaTeam(() -> {
            if (binding == null) return;
            if (sofaTeamId == null) {
                binding.progressClub.setVisibility(View.GONE);
                addContent("Danh sách cầu thủ đang được cập nhật.");
                return;
            }
            if (PLAYERS_CACHE.containsKey(sofaTeamId)) {
                binding.progressClub.setVisibility(View.GONE);
                renderPlayers(PLAYERS_CACHE.get(sofaTeamId));
                return;
            }
            VLeagueApiClient.getInstance().fetchTeamPlayers(sofaTeamId, new VLeagueApiClient.DataCallback<List<String>>() {
                @Override
                public void onSuccess(List<String> rows) {
                    if (binding == null) return;
                    PLAYERS_CACHE.put(sofaTeamId, rows == null ? new ArrayList<>() : new ArrayList<>(rows));
                    binding.progressClub.setVisibility(View.GONE);
                    renderPlayers(rows);
                }

                @Override
                public void onError(Throwable throwable) {
                    if (binding == null) return;
                    binding.progressClub.setVisibility(View.GONE);
                    addContent("Không tải được danh sách cầu thủ. Vui lòng thử lại sau.");
                }
            });
        });
    }

    private void loadClubStatistics() {
        setActiveTab("statistics");
        binding.progressClub.setVisibility(View.VISIBLE);
        addContent("");
        resolveSofaTeam(() -> {
            if (binding == null) return;
            if (sofaTeamId == null) {
                binding.progressClub.setVisibility(View.GONE);
                addContent("Thống kê CLB đang được cập nhật.");
                return;
            }
            if (STATS_CACHE.containsKey(sofaTeamId)) {
                binding.progressClub.setVisibility(View.GONE);
                renderStatistics(STATS_CACHE.get(sofaTeamId));
                return;
            }
            VLeagueApiClient.getInstance().fetchTeamStatistics(sofaTeamId, new VLeagueApiClient.DataCallback<List<String>>() {
                @Override
                public void onSuccess(List<String> rows) {
                    if (binding == null) return;
                    List<String> cleanRows = rows == null || rows.isEmpty() ? fallbackStatisticsRows() : rows;
                    STATS_CACHE.put(sofaTeamId, new ArrayList<>(cleanRows));
                    binding.progressClub.setVisibility(View.GONE);
                    renderStatistics(cleanRows);
                }

                @Override
                public void onError(Throwable throwable) {
                    if (binding == null) return;
                    binding.progressClub.setVisibility(View.GONE);
                    renderStatistics(fallbackStatisticsRows());
                }
            });
        });
    }

    private void bindClubDocument(DocumentSnapshot doc) {
        String name = first(doc, "name", "clubName");
        String badge = first(doc, "badge", "shortName", "code");
        String founded = first(doc, "founded", "foundedYear");
        String coach = first(doc, "coach", "headCoach");
        String stadium = first(doc, "stadium", "stadiumName");
        String logo = first(doc, "logoUrl", "imageUrl");
        String hero = first(doc, "heroUrl", "heroImageUrl", "stadiumImageUrl");
        String overview = first(doc, "overview", "description", "summary");
        if (!name.isEmpty()) binding.tvClubName.setText(name);
        if (!badge.isEmpty()) binding.tvClubBadge.setText(badge);
        binding.tvClubFounded.setText(founded.isEmpty() ? "Chưa cập nhật năm thành lập" : "Thành lập " + founded);
        binding.tvClubCoach.setText(coach.isEmpty() ? "Đang cập nhật HLV" : coach);
        binding.tvClubStadium.setText(stadium.isEmpty() ? "Đang cập nhật sân nhà" : stadium);
        if (!logo.isEmpty()) Glide.with(binding.imgClubLogo).load(logo).fitCenter().into(binding.imgClubLogo);
        if (!hero.isEmpty()) Glide.with(binding.imgClubHero).load(hero).centerCrop().into(binding.imgClubHero);
        addContent(overview.isEmpty() ? "Thông tin tổng quan đang được cập nhật." : overview);
    }

    private void bindSofaDetails(List<String> rows) {
        if (rows == null) return;
        for (String row : rows) {
            if (row.startsWith("Name: ") && clubMeta == null) binding.tvClubName.setText(row.substring(6));
            if (row.startsWith("Code: ")) binding.tvClubBadge.setText(row.substring(6).toUpperCase(Locale.ROOT));
            if (row.startsWith("Coach: ")) binding.tvClubCoach.setText(row.substring(7));
            if (row.startsWith("Stadium: ") && clubMeta == null) binding.tvClubStadium.setText(row.substring(9));
            if (row.startsWith("Founded: ")) binding.tvClubFounded.setText("Thành lập " + row.substring(9));
        }
    }

    private String joinRows(List<String> rows, String emptyMessage) {
        if (rows == null || rows.isEmpty()) return emptyMessage;
        StringBuilder builder = new StringBuilder();
        for (String row : rows) {
            if (builder.length() > 0) builder.append('\n');
            builder.append(row);
        }
        return builder.toString();
    }

    private void loadClubProducts() {
        setActiveTab("shop");
        binding.progressClub.setVisibility(View.VISIBLE);
        addContent("");
        String clubKey = ClubProfileDirectory.canonicalKey(clubName);
        if (CLUB_SHOP_CACHE.containsKey(clubKey)) {
            binding.progressClub.setVisibility(View.GONE);
            renderProducts(CLUB_SHOP_CACHE.get(clubKey));
            return;
        }
        VLeagueApiClient.getInstance().fetchShopProductsForClub(clubName, new VLeagueApiClient.DataCallback<List<ShopProduct>>() {
            @Override
            public void onSuccess(List<ShopProduct> products) {
                if (binding == null) return;
                binding.progressClub.setVisibility(View.GONE);
                CLUB_SHOP_CACHE.put(clubKey, products == null ? new ArrayList<>() : new ArrayList<>(products));
                renderProducts(products);
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding == null) return;
                binding.progressClub.setVisibility(View.GONE);
                renderProducts(new ArrayList<>());
            }
        });
    }

    private void addContent(String text) {
        binding.contentContainer.removeAllViews();
        if (text == null || text.isEmpty()) return;
        for (String block : text.split("\\n\\n")) {
            if (block.trim().isEmpty()) continue;
            addInfoCard(block.trim().split("\\n"));
        }
    }

    private void renderOverview(List<String> sofaRows) {
        binding.contentContainer.removeAllViews();
        addSectionTitle("Thông tin CLB");
        List<String> rows = new ArrayList<>();
        if (clubMeta != null) {
            rows.add("Club: " + clubMeta.name);
            rows.add("Code: " + clubMeta.code);
            rows.add("Home stadium: " + clubMeta.stadium);
            rows.add("City: " + clubMeta.city);
        }
        if (sofaRows != null) {
            for (String row : sofaRows) {
                if (!containsLabel(rows, row)) rows.add(row);
            }
        }
        if (rows.isEmpty()) rows.add("Trạng thái: Đang cập nhật thông tin CLB");
        addInfoCard(rows.toArray(new String[0]));
    }

    private void renderStatistics(List<String> rows) {
        binding.contentContainer.removeAllViews();
        addSectionTitle("Thống kê mùa giải");
        List<String> displayRows = cleanStatisticRows(rows);
        if (displayRows.isEmpty()) {
            addInfoCard(new String[]{"Trạng thái: Thống kê đang được cập nhật"});
            return;
        }
        LinearLayout card = sectionCard();
        for (int i = 0; i < displayRows.size(); i += 2) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, dp(4), 0, dp(4));
            card.addView(row, rowParams);
            addMetricCell(row, displayRows.get(i));
            if (i + 1 < displayRows.size()) {
                addMetricCell(row, displayRows.get(i + 1));
            } else {
                View spacer = new View(requireContext());
                row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
            }
        }
    }

    private void renderPlayers(List<String> rows) {
        binding.contentContainer.removeAllViews();
        addSectionTitle("Đội hình");
        if (rows == null || rows.isEmpty()) {
            addInfoCard(new String[]{"Trạng thái: Danh sách cầu thủ đang được cập nhật"});
            return;
        }
        LinearLayout card = sectionCard();
        for (String row : rows) {
            addPlayerRow(card, row);
        }
    }

    private void renderProducts(List<ShopProduct> products) {
        binding.contentContainer.removeAllViews();
        addSectionTitle("Cửa hàng CLB");
        if (products == null || products.isEmpty()) {
            addInfoCard(new String[]{"Trạng thái: CLB này chưa có sản phẩm"});
            return;
        }
        RecyclerView grid = new RecyclerView(requireContext());
        grid.setHasFixedSize(true);
        grid.setItemViewCacheSize(6);
        grid.setItemAnimator(null);
        grid.setNestedScrollingEnabled(false);
        grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
        grid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        ShopProductAdapter adapter = new ShopProductAdapter(product ->
                navigateTo(com.finalproject.v_league_ticket.presentation.shop.ProductDetailFragment.newInstance(product)));
        grid.setAdapter(adapter);
        adapter.submitList(new ArrayList<>(products));
        binding.contentContainer.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addSectionTitle(String title) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        binding.contentContainer.addView(row, params);

        View indicator = new View(requireContext());
        indicator.setBackgroundResource(R.drawable.bg_home_section_indicator);
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(dp(4), dp(22));
        indicatorParams.setMargins(0, 0, dp(8), 0);
        row.addView(indicator, indicatorParams);

        TextView view = new TextView(requireContext());
        view.setText(title);
        view.setTextColor(requireContext().getColor(R.color.stadium_ink));
        view.setTextSize(22f);
        view.setTypeface(null, android.graphics.Typeface.BOLD_ITALIC);
        row.addView(view, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private LinearLayout sectionCard() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_home_card_clean);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        binding.contentContainer.addView(card, params);
        return card;
    }

    private void addMetricCell(LinearLayout row, String statRow) {
        String[] parts = statRow.split(":", 2);
        String label = parts.length == 2 ? displayLabel(parts[0]) : "Metric";
        String value = parts.length == 2 ? parts[1].trim() : statRow;

        LinearLayout cell = new LinearLayout(requireContext());
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setBackgroundResource(R.drawable.bg_club_detail_item);
        cell.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        row.addView(cell, params);

        TextView valueView = new TextView(requireContext());
        valueView.setText(value);
        valueView.setTextColor(clubMeta == null ? requireContext().getColor(R.color.stadium_ink) : clubMeta.primaryColorInt());
        valueView.setTextSize(20f);
        valueView.setTypeface(null, android.graphics.Typeface.BOLD);
        cell.addView(valueView);

        TextView labelView = new TextView(requireContext());
        labelView.setText(label.toUpperCase(Locale.ROOT));
        labelView.setTextColor(requireContext().getColor(R.color.slate_caption));
        labelView.setTextSize(10f);
        labelView.setTypeface(null, android.graphics.Typeface.BOLD);
        cell.addView(labelView);
    }

    private void addPlayerRow(LinearLayout parent, String raw) {
        PlayerDisplay player = parsePlayer(raw);
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_club_detail_item);
        row.setPadding(dp(10), dp(9), dp(12), dp(9));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(5), 0, dp(5));
        parent.addView(row, params);

        TextView number = new TextView(requireContext());
        number.setText(player.number.isEmpty() ? "-" : player.number);
        number.setGravity(android.view.Gravity.CENTER);
        number.setTextColor(requireContext().getColor(R.color.white));
        number.setTextSize(12f);
        number.setTypeface(null, android.graphics.Typeface.BOLD);
        number.setBackgroundResource(R.drawable.bg_home_red_button);
        row.addView(number, new LinearLayout.LayoutParams(dp(36), dp(30)));

        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(12), 0, 0, 0);
        row.addView(texts, textParams);

        TextView name = new TextView(requireContext());
        name.setText(player.name);
        name.setTextColor(requireContext().getColor(R.color.stadium_ink));
        name.setTextSize(14f);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        texts.addView(name);

        if (!player.meta.isEmpty()) {
            TextView meta = new TextView(requireContext());
            meta.setText(player.meta);
            meta.setTextColor(requireContext().getColor(R.color.slate_caption));
            meta.setTextSize(12f);
            texts.addView(meta);
        }
    }

    private void addProductRow(LinearLayout parent, ShopProduct product) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_club_detail_item);
        row.setPadding(dp(10), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, dp(6));
        parent.addView(row, params);

        ImageView image = new ImageView(requireContext());
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        image.setBackgroundResource(R.drawable.bg_shop_product_image);
        image.setPadding(dp(6), dp(6), dp(6), dp(6));
        row.addView(image, new LinearLayout.LayoutParams(dp(62), dp(62)));
        Glide.with(image)
                .load(product.getImageUrl().isEmpty() ? null : product.getImageUrl())
                .placeholder(R.drawable.ic_logo)
                .error(R.drawable.ic_logo)
                .centerInside()
                .into(image);

        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(12), 0, 0, 0);
        row.addView(texts, textParams);

        TextView name = new TextView(requireContext());
        name.setText(product.getName());
        name.setTextColor(requireContext().getColor(R.color.stadium_ink));
        name.setTextSize(13f);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(name);

        TextView price = new TextView(requireContext());
        price.setText(product.getPrice());
        price.setTextColor(clubMeta == null ? requireContext().getColor(R.color.red_energy) : clubMeta.primaryColorInt());
        price.setTextSize(14f);
        price.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        priceParams.setMargins(0, dp(6), 0, 0);
        texts.addView(price, priceParams);
    }

    private List<String> fallbackStatisticsRows() {
        List<String> rows = new ArrayList<>();
        if (clubMeta != null) {
            rows.add("Club: " + clubMeta.name);
            rows.add("Home stadium: " + clubMeta.stadium);
            rows.add("City: " + clubMeta.city);
        }
        List<String> details = sofaTeamId == null ? null : DETAILS_CACHE.get(sofaTeamId);
        if (details != null) {
            for (String detail : details) {
                if (!containsLabel(rows, detail)) rows.add(detail);
            }
        }
        return rows;
    }

    private List<String> cleanStatisticRows(List<String> rows) {
        LinkedHashMap<String, String> cleanRows = new LinkedHashMap<>();
        if (clubMeta != null) {
            addCleanStat(cleanRows, "Club", clubMeta.name);
            addCleanStat(cleanRows, "City", clubMeta.city);
            addCleanStat(cleanRows, "Home stadium", clubMeta.stadium);
        }
        if (rows != null) {
            for (String row : rows) {
                String[] parts = row == null ? new String[0] : row.split(":", 2);
                String label = parts.length == 2 ? parts[0].trim() : "";
                String value = parts.length == 2 ? parts[1].trim() : row == null ? "" : row.trim();
                if (value.isEmpty() || shouldHideStatisticLabel(label)) continue;
                addCleanStat(cleanRows, label.isEmpty() ? "Metric" : label, value);
            }
        }
        return new ArrayList<>(cleanRows.values());
    }

    private void addCleanStat(LinkedHashMap<String, String> rows, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        String cleanLabel = label == null ? "" : label.trim();
        if (shouldHideStatisticLabel(cleanLabel)) return;
        String key = statisticKey(cleanLabel);
        String row = cleanLabel + ": " + value.trim();
        if (!rows.containsKey(key)) {
            rows.put(key, row);
        }
    }

    private boolean shouldHideStatisticLabel(String label) {
        String key = statisticKey(label);
        return "code".equals(key);
    }

    private String statisticKey(String label) {
        String key = normalizeKey(label == null ? "" : label);
        if (key.equals("name") || key.equals("club") || key.equals("team-name")) return "club";
        if (key.equals("code") || key.equals("team-code")) return "code";
        if (key.equals("home-stadium") || key.equals("stadium") || key.equals("venue")) return "stadium";
        if (key.equals("city") || key.equals("town")) return "city";
        if (key.equals("coach") || key.equals("head-coach") || key.equals("manager")) return "coach";
        if (key.equals("country")) return "country";
        if (key.equals("founded") || key.equals("founded-year")) return "founded";
        return "metric-" + key;
    }

    private boolean containsLabel(List<String> rows, String row) {
        String label = row == null ? "" : row.split(":", 2)[0].trim();
        if (label.isEmpty()) return false;
        for (String existing : rows) {
            if (existing.split(":", 2)[0].trim().equalsIgnoreCase(label)) return true;
        }
        return false;
    }

    private void setActiveTab(String tab) {
        activeTab = tab;
        styleTab(binding.tvTabOverview, binding.indicatorOverview, "overview".equals(tab));
        styleTab(binding.tvTabStatistics, binding.indicatorStatistics, "statistics".equals(tab));
        styleTab(binding.tvTabPlayers, binding.indicatorPlayers, "players".equals(tab));
        styleTab(binding.tvTabShop, binding.indicatorShop, "shop".equals(tab));
    }

    private void styleTab(TextView label, View indicator, boolean active) {
        label.setTextColor(requireContext().getColor(active ? R.color.red_energy : R.color.sports_nav_inactive));
        label.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        indicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        indicator.setAlpha(active ? 1f : 0f);
    }

    private PlayerDisplay parsePlayer(String raw) {
        String text = raw == null ? "" : raw.trim();
        String number = "";
        String name = text;
        String meta = "";
        int hash = text.indexOf('#');
        if (hash >= 0) {
            name = text.substring(0, hash).trim();
            int end = text.indexOf(" - ", hash);
            String numberPart = end >= 0 ? text.substring(hash + 1, end) : text.substring(hash + 1);
            number = numberPart.trim();
            if (end >= 0) meta = text.substring(end + 3).trim();
        } else {
            int split = text.indexOf(" - ");
            if (split >= 0) {
                name = text.substring(0, split).trim();
                meta = text.substring(split + 3).trim();
            }
        }
        return new PlayerDisplay(name.isEmpty() ? text : name, number, meta);
    }

    private static final class PlayerDisplay {
        final String name;
        final String number;
        final String meta;

        PlayerDisplay(String name, String number, String meta) {
            this.name = name == null ? "" : name;
            this.number = number == null ? "" : number;
            this.meta = meta == null ? "" : meta;
        }
    }

    private void addInfoCard(String[] rows) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_home_card_clean);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        binding.contentContainer.addView(card, cardParams);

        for (String row : rows) {
            if (row == null || row.trim().isEmpty()) continue;
            String[] parts = row.split(":", 2);
            if (parts.length == 2) {
                addLabelValueRow(card, displayLabel(parts[0]), parts[1].trim());
            } else {
                addListRow(card, row.trim());
            }
        }
    }

    private void addLabelValueRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView left = new TextView(requireContext());
        left.setText(label.toUpperCase(Locale.ROOT));
        left.setTextColor(requireContext().getColor(R.color.slate_caption));
        left.setTextSize(11f);
        left.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView right = new TextView(requireContext());
        right.setText(value);
        right.setTextColor(requireContext().getColor(R.color.stadium_ink));
        right.setTextSize(15f);
        right.setTypeface(null, android.graphics.Typeface.BOLD);
        right.setGravity(android.view.Gravity.END);
        row.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.25f));
    }

    private void addListRow(LinearLayout parent, String value) {
        TextView row = new TextView(requireContext());
        row.setText(value);
        row.setTextColor(requireContext().getColor(R.color.stadium_ink));
        row.setTextSize(14f);
        row.setTypeface(null, android.graphics.Typeface.BOLD);
        row.setBackgroundResource(R.drawable.bg_match_center_tag);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(5), 0, dp(5));
        parent.addView(row, params);
    }

    private void bindLocalClubShell() {
        String displayName = clubMeta == null ? clubName : clubMeta.name;
        binding.tvClubName.setText(displayName);
        binding.tvClubBadge.setText(clubMeta == null ? "CLB V.LEAGUE" : clubMeta.code);
        binding.tvClubFounded.setText(clubMeta == null ? "V.League 1" : clubMeta.city);
        binding.tvClubCoach.setText("V.League 1");
        binding.tvClubStadium.setText(clubMeta == null ? "Đang cập nhật sân nhà" : clubMeta.stadium);
        binding.btnBuyTickets.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                clubMeta == null ? requireContext().getColor(R.color.navy_deep) : clubMeta.primaryColorInt()));
        binding.viewClubHeroTint.setBackgroundColor(clubMeta == null ? requireContext().getColor(R.color.black_alpha_25)
                : adjustAlpha(clubMeta.primaryColorInt(), 0.28f));
        loadClubLogo(clubMeta == null ? "" : clubMeta.logoUrl());
    }

    private void loadClubLogo(String logoUrl) {
        Glide.with(binding.imgClubLogo)
                .load(logoUrl == null || logoUrl.isEmpty() ? R.drawable.ic_logo : logoUrl)
                .placeholder(R.color.transparent)
                .error(R.drawable.ic_logo)
                .fitCenter()
                .into(binding.imgClubLogo);
    }

    private String localOverviewText() {
        if (clubMeta == null) return "Thông tin CLB đang được cập nhật.";
        return clubMeta.name + "\n"
                + "Sân nhà: " + clubMeta.stadium + "\n"
                + "Thành phố: " + clubMeta.city + "\n\n"
                + "Thông tin sẽ được cập nhật khi dữ liệu sẵn sàng.";
    }

    private Long localTeamId() {
        return clubMeta == null || clubMeta.teamId <= 0 ? null : clubMeta.teamId;
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(android.graphics.Color.alpha(color) * factor);
        return android.graphics.Color.argb(alpha, android.graphics.Color.red(color),
                android.graphics.Color.green(color), android.graphics.Color.blue(color));
    }

    private String firstNonEmpty(String first, String second) {
        return first == null || first.isEmpty() ? (second == null ? "" : second) : first;
    }

    private String displayLabel(String label) {
        switch (label.trim()) {
            case "Name": return "Tên CLB";
            case "Club": return "CLB";
            case "Code": return "Mã đội";
            case "Home stadium": return "Sân nhà";
            case "City": return "Thành phố";
            case "Coach": return "HLV";
            case "Stadium": return "Sân nhà";
            case "Country": return "Quốc gia";
            case "Founded": return "Thành lập";
            case "Standing": return "Xếp hạng";
            case "Played": return "Số trận";
            case "Goals": return "Bàn thắng";
            case "Goal difference": return "Hiệu số";
            case "Matches": return "Số trận";
            case "Goals conceded": return "Thủng lưới";
            case "Assists": return "Kiến tạo";
            case "Shots": return "Dứt điểm";
            case "Shots on target": return "Trúng đích";
            case "Ball possession": return "Kiểm soát bóng";
            case "Accurate passes": return "Chuyền chính xác";
            case "Yellow cards": return "Thẻ vàng";
            case "Red cards": return "Thẻ đỏ";
            default: return label.trim();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName()).commit();
    }

    private String first(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof String && !((String) value).trim().isEmpty()) return ((String) value).trim();
            if (value instanceof Number) return String.valueOf(((Number) value).longValue());
        }
        return "";
    }

    private String normalizeKey(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
