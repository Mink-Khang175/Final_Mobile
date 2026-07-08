package com.finalproject.v_league_ticket.presentation.matches;

import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.data.remote.VLeagueApiClient;
import com.finalproject.v_league_ticket.databinding.FragmentMatchesResultsBinding;
import com.finalproject.v_league_ticket.domain.model.Fixture;
import com.finalproject.v_league_ticket.domain.model.VLeagueSeason;
import com.finalproject.v_league_ticket.domain.model.VLeagueSeasons;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.finalproject.v_league_ticket.presentation.news.NewsFragment;
import com.finalproject.v_league_ticket.presentation.profile.HeaderNotificationRouter;
import com.finalproject.v_league_ticket.presentation.profile.ProfileFragment;
import com.finalproject.v_league_ticket.presentation.shop.ShopFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResultsFragment extends Fragment {
    private FragmentMatchesResultsBinding binding;
    private final MatchAdapter resultAdapter = new MatchAdapter(MatchAdapter.Mode.RESULTS, this::openMatchCenter);
    private final List<VLeagueSeason> seasons = new ArrayList<>();
    private VLeagueSeason selectedSeason = VLeagueSeasons.current();
    private String selectedRoundLabel = "Mới nhất";

    public ResultsFragment() {
        super(R.layout.fragment_matches_results);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentMatchesResultsBinding.bind(view);
        binding.rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvResults.setAdapter(resultAdapter);
        binding.rvResults.setHasFixedSize(false);
        binding.rvResults.setItemViewCacheSize(8);
        binding.rvResults.setItemAnimator(null);
        resultAdapter.submitList(new ArrayList<>());
        binding.progressBar.setVisibility(View.GONE);
        binding.tvEmptyState.setVisibility(View.GONE);
        setupClicks();
        binding.appHeader.tvHeaderSubtitle.setText(headerName());
        loadSeasons();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupClicks() {
        binding.tabFixtures.setOnClickListener(v -> navigateTo(new FixturesFragment()));
        binding.tabStandings.setOnClickListener(v -> navigateTo(new StandingsFragment()));
        binding.bottomNav.navHome.setOnClickListener(v -> navigateTo(new HomepageFragment()));
        binding.bottomNav.navShop.setOnClickListener(v -> navigateTo(new ShopFragment()));
        binding.bottomNav.navNews.setOnClickListener(v -> navigateTo(new NewsFragment()));
        binding.bottomNav.navProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        binding.appHeader.btnHeaderProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        HeaderNotificationRouter.bind(this, binding.appHeader);
    }

    private void loadSeasons() {
        showLoading(true);
        VLeagueApiClient.getInstance().fetchSeasons(new VLeagueApiClient.DataCallback<List<VLeagueSeason>>() {
            @Override
            public void onSuccess(List<VLeagueSeason> data) {
                if (binding == null) return;
                seasons.clear();
                if (data != null) seasons.addAll(data);
                if (seasons.isEmpty()) {
                    bindSeasonChips();
                    bindRoundChips();
                    showError("Không tải được danh sách mùa giải. Vui lòng thử lại sau.");
                    return;
                }
                selectedSeason = defaultResultsSeason();
                bindSeasonChips();
                bindRoundChips();
                loadLatest();
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding == null) return;
                seasons.clear();
                bindSeasonChips();
                bindRoundChips();
                showError("Không tải được danh sách mùa giải. Vui lòng thử lại sau.");
            }
        });
    }

    private VLeagueSeason defaultResultsSeason() {
        VLeagueSeason current = seasonByLabel(VLeagueSeasons.current().getLabel());
        if (current != null) return current;
        return seasons.isEmpty() ? VLeagueSeasons.current() : seasons.get(0);
    }

    private VLeagueSeason seasonByLabel(String label) {
        for (VLeagueSeason season : seasons) {
            if (season.getLabel().equalsIgnoreCase(label)) return season;
        }
        return null;
    }

    private void bindSeasonChips() {
        binding.seasonContainer.removeAllViews();
        binding.seasonInlineContainer.removeAllViews();
        TextView dropdown = addDropdownButton(binding.seasonInlineContainer, selectedSeason.getLabel(), 86);
        dropdown.setOnClickListener(v -> showSeasonMenu(dropdown));
    }

    private void bindRoundChips() {
        binding.roundContainer.removeAllViews();
        binding.roundInlineContainer.removeAllViews();
        TextView dropdown = addDropdownButton(binding.roundInlineContainer, roundChipLabel(), 118);
        dropdown.setOnClickListener(v -> showRoundMenu(dropdown));
    }

    private String roundChipLabel() {
        if ("Mới nhất".equals(selectedRoundLabel)) return "Vòng: Mới nhất";
        return selectedRoundLabel;
    }

    private void loadLatest() {
        selectedRoundLabel = "Mới nhất";
        bindRoundChips();
        binding.tvScreenMeta.setText("V.LEAGUE");
        showLoading(true);
        VLeagueApiClient.getInstance().fetchFinished(selectedSeason.getId(), new VLeagueApiClient.DataCallback<List<Fixture>>() {
            @Override
            public void onSuccess(List<Fixture> data) {
                showLatestResults(data, "Chưa có kết quả để hiển thị.");
            }

            @Override
            public void onError(Throwable throwable) {
                showError("Không tải được kết quả. Vui lòng thử lại sau.");
            }
        });
    }

    private void loadRound(int round, String label) {
        selectedRoundLabel = label;
        bindRoundChips();
        binding.tvScreenMeta.setText(selectedSeason.getLabel() + " / " + label.toUpperCase(Locale.ROOT));
        showLoading(true);
        VLeagueApiClient.getInstance().fetchRound(selectedSeason.getId(), round, new VLeagueApiClient.DataCallback<List<Fixture>>() {
            @Override
            public void onSuccess(List<Fixture> data) {
                showResults(data, "Chưa có trận đấu cho " + label + ".");
            }

            @Override
            public void onError(Throwable throwable) {
                showError("Không tải được dữ liệu " + label + ".");
            }
        });
    }

    private void loadFinal() {
        selectedRoundLabel = "Chung kết";
        bindRoundChips();
        binding.tvScreenMeta.setText(selectedSeason.getLabel() + " / CHUNG KẾT");
        showLoading(true);
        VLeagueApiClient.getInstance().fetchFinished(selectedSeason.getId(), new VLeagueApiClient.DataCallback<List<Fixture>>() {
            @Override
            public void onSuccess(List<Fixture> data) {
                List<Fixture> finals = new ArrayList<>();
                if (data != null) {
                    for (Fixture fixture : data) {
                        String roundName = fixture.getRoundName().toLowerCase(Locale.ROOT);
                        if (roundName.contains("final") || roundName.contains("chung kết")) finals.add(fixture);
                    }
                }
                if (!finals.isEmpty()) {
                    showResults(finals, "Chưa có dữ liệu trận chung kết.");
                    return;
                }
                loadRound(selectedSeason.getMaxRound() + 1, "Chung kết");
                selectedRoundLabel = "Chung kết";
                bindRoundChips();
            }

            @Override
            public void onError(Throwable throwable) {
                loadRound(selectedSeason.getMaxRound() + 1, "Chung kết");
                selectedRoundLabel = "Chung kết";
                bindRoundChips();
            }
        });
    }

    private void showResults(List<Fixture> data, String emptyMessage) {
        if (binding == null) return;
        showLoading(false);
        if (data == null || data.isEmpty()) {
            resultAdapter.submitList(new ArrayList<>());
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyState.setText(emptyMessage);
            return;
        }
        binding.tvEmptyState.setVisibility(View.GONE);
        resultAdapter.submitList(new ArrayList<>(data));
    }

    private void showLatestResults(List<Fixture> data, String emptyMessage) {
        if (binding == null) return;
        showLoading(false);
        if (data == null || data.isEmpty()) {
            resultAdapter.submitList(new ArrayList<>());
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyState.setText(emptyMessage);
            return;
        }

        List<Fixture> sorted = new ArrayList<>(data);
        Collections.sort(sorted, Comparator
                .comparingInt(this::roundSortValue).reversed()
                .thenComparing((Fixture fixture) -> fixture.getStartTimestamp() == null ? 0L : fixture.getStartTimestamp(), Comparator.reverseOrder()));

        Map<String, List<Fixture>> grouped = new LinkedHashMap<>();
        for (Fixture fixture : sorted) {
            String roundLabel = displayRoundLabel(fixture);
            List<Fixture> fixtures = grouped.get(roundLabel);
            if (fixtures == null) {
                fixtures = new ArrayList<>();
                grouped.put(roundLabel, fixtures);
            }
            fixtures.add(fixture);
        }

        ArrayList<Fixture> rows = new ArrayList<>();
        int headerIndex = 0;
        for (Map.Entry<String, List<Fixture>> entry : grouped.entrySet()) {
            rows.add(MatchAdapter.roundHeader(entry.getKey(), entry.getValue().size(), headerIndex++));
            rows.addAll(entry.getValue());
        }

        binding.tvEmptyState.setVisibility(View.GONE);
        resultAdapter.submitList(new ArrayList<>(rows));
    }

    private int roundSortValue(Fixture fixture) {
        if (fixture.getRound() != null) return fixture.getRound();
        String roundName = fixture.getRoundName().toLowerCase(Locale.ROOT);
        if (roundName.contains("final") || roundName.contains("chung kết")) return selectedSeason.getMaxRound() + 1;
        return 0;
    }

    private String displayRoundLabel(Fixture fixture) {
        if (!fixture.getRoundName().isEmpty()) return fixture.getRoundName();
        if (fixture.getRound() != null) return "Vòng " + fixture.getRound();
        return "Kết quả khác";
    }

    private void showError(String message) {
        if (binding == null) return;
        showLoading(false);
        boolean empty = resultAdapter.getCurrentList().isEmpty();
        binding.tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) binding.tvEmptyState.setText(message);
    }

    private void showLoading(boolean loading) {
        if (binding == null) return;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private TextView addDropdownButton(android.view.ViewGroup container, String text, int minWidthDp) {
        TextView chip = new TextView(requireContext());
        chip.setText(text + "  ▾");
        chip.setTextColor(requireContext().getColor(R.color.red_energy));
        chip.setTextSize(13f);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setPadding(30, 0, 30, 0);
        chip.setMinHeight(34);
        chip.setMinWidth(dp(minWidthDp));
        chip.setBackgroundResource(R.drawable.bg_segment_active);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 12, 0);
        container.addView(chip, params);
        return chip;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showSeasonMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        for (int index = 0; index < seasons.size(); index++) {
            VLeagueSeason season = seasons.get(index);
            menu.getMenu().add(0, index, index, season.getLabel());
        }
        menu.setOnMenuItemClickListener(item -> {
            selectedSeason = seasons.get(item.getItemId());
            selectedRoundLabel = "Mới nhất";
            bindSeasonChips();
            bindRoundChips();
            loadLatest();
            return true;
        });
        menu.show();
    }

    private void showRoundMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 0, 0, "Mới nhất");
        int maxRound = Math.max(1, selectedSeason.getMaxRound());
        for (int round = maxRound; round >= 1; round--) {
            menu.getMenu().add(0, round, maxRound - round + 1, "Vòng " + round);
        }
        if (selectedSeason.hasFinalRound()) {
            menu.getMenu().add(0, maxRound + 1, maxRound + 1, "Chung kết");
        }
        menu.setOnMenuItemClickListener(item -> {
            String label = item.getTitle().toString();
            if ("Mới nhất".equals(label)) {
                loadLatest();
            } else if ("Chung kết".equals(label)) {
                loadFinal();
            } else {
                loadRound(item.getItemId(), label);
            }
            return true;
        });
        menu.show();
    }

    private void openMatchCenter(Fixture fixture) {
        String roundName = fixture.getRoundName().isEmpty()
                ? (fixture.getRound() == null ? "" : "Vòng " + fixture.getRound())
                : fixture.getRoundName();
        navigateTo(MatchCenterFragment.newInstance(
                fixture.getId(), fixture.getHomeTeamName(), fixture.getAwayTeamName(),
                fixture.getHomeScore() == null ? 0 : fixture.getHomeScore(),
                fixture.getAwayScore() == null ? 0 : fixture.getAwayScore(),
                fixture.getHomePenaltyScore(), fixture.getAwayPenaltyScore(),
                fixture.getHomeLogoUrl(), fixture.getAwayLogoUrl(), fixture.getVenue(),
                "V.League 1", roundName,
                fixture.getRound(), fixture.getStartTimestamp(), new ArrayList<>()));
    }

    private Fragment profileOrAuth() {
        return AuthSession.hasToken(requireContext()) ? new ProfileFragment() : new AuthLoginFragment();
    }

    private String headerName() {
        String name = AuthSession.userName(requireContext());
        return name == null ? "Khách" : name;
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName()).commit();
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}

