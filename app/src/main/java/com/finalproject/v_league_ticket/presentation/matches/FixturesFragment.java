package com.finalproject.v_league_ticket.presentation.matches;

import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.data.remote.VLeagueApiClient;
import com.finalproject.v_league_ticket.databinding.FragmentMatchesFixturesBinding;
import com.finalproject.v_league_ticket.domain.model.Fixture;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.finalproject.v_league_ticket.presentation.news.NewsFragment;
import com.finalproject.v_league_ticket.presentation.profile.HeaderNotificationRouter;
import com.finalproject.v_league_ticket.presentation.profile.ProfileFragment;
import com.finalproject.v_league_ticket.presentation.shop.ShopFragment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FixturesFragment extends Fragment {
    private FragmentMatchesFixturesBinding binding;
    private final MatchAdapter fixtureAdapter =
            new MatchAdapter(MatchAdapter.Mode.FIXTURES, fixture -> navigateTo(UpcomingMatchDetailFragment.newInstance(fixture)));
    private final List<Fixture> allFixtures = new ArrayList<>();
    private String selectedRoundLabel = "Tất cả vòng";

    public FixturesFragment() {
        super(R.layout.fragment_matches_fixtures);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentMatchesFixturesBinding.bind(view);
        binding.rvFixtures.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFixtures.setAdapter(fixtureAdapter);
        binding.rvFixtures.setItemAnimator(null);
        fixtureAdapter.submitList(new ArrayList<>());
        binding.progressBar.setVisibility(View.GONE);
        binding.tvEmptyState.setVisibility(View.GONE);
        binding.tvScreenTitle.setText("Lịch mùa 25/26");
        binding.tvScreenMeta.setText("SẮP DIỄN RA");
        binding.tvScreenSubtitle.setText("Toàn bộ lịch thi đấu mùa 25/26 đang được mở để đặt vé.");
        setupClicks();
        bindRoundFilter();
        binding.appHeader.tvHeaderSubtitle.setText(headerName());
        loadFixtures();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupClicks() {
        binding.tabResults.setOnClickListener(v -> navigateTo(new ResultsFragment()));
        binding.tabStandings.setOnClickListener(v -> navigateTo(new StandingsFragment()));
        binding.bottomNav.navHome.setOnClickListener(v -> navigateTo(new HomepageFragment()));
        binding.bottomNav.navShop.setOnClickListener(v -> navigateTo(new ShopFragment()));
        binding.bottomNav.navNews.setOnClickListener(v -> navigateTo(new NewsFragment()));
        binding.bottomNav.navProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        binding.appHeader.btnHeaderProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        binding.chipRoundFilter.setOnClickListener(v -> showRoundMenu());
        HeaderNotificationRouter.bind(this, binding.appHeader);
    }

    private void loadFixtures() {
        binding.progressBar.setVisibility(View.VISIBLE);
        VLeagueApiClient.getInstance().fetchUpcoming(new VLeagueApiClient.DataCallback<List<Fixture>>() {
            @Override
            public void onSuccess(List<Fixture> data) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                if (data == null || data.isEmpty()) {
                    fixtureAdapter.submitList(new ArrayList<>());
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                    binding.tvEmptyState.setText("Chưa có lịch thi đấu để hiển thị.");
                    return;
                }
                binding.tvEmptyState.setVisibility(View.GONE);
                allFixtures.clear();
                allFixtures.addAll(data);
                selectedRoundLabel = "Tất cả vòng";
                bindRoundFilter();
                renderSelectedRound();
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                boolean empty = fixtureAdapter.getCurrentList().isEmpty();
                binding.tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (empty) binding.tvEmptyState.setText("Không tải được lịch thi đấu. Vui lòng thử lại sau.");
            }
        });
    }

    private void bindRoundFilter() {
        if (binding == null) return;
        binding.chipRoundFilter.setText(selectedRoundLabel + "  ▾");
    }

    private void showRoundMenu() {
        PopupMenu menu = new PopupMenu(requireContext(), binding.chipRoundFilter);
        menu.getMenu().add(0, 0, 0, "Tất cả vòng");
        int order = 1;
        for (String label : roundLabels(allFixtures)) {
            menu.getMenu().add(0, order++, order, label);
        }
        menu.setOnMenuItemClickListener(item -> {
            selectedRoundLabel = item.getItemId() == 0 ? "Tất cả vòng" : String.valueOf(item.getTitle());
            bindRoundFilter();
            renderSelectedRound();
            return true;
        });
        menu.show();
    }

    private List<String> roundLabels(List<Fixture> fixtures) {
        List<String> labels = new ArrayList<>();
        for (Fixture fixture : fixtures) {
            String label = roundLabel(fixture);
            if (!labels.contains(label)) labels.add(label);
        }
        return labels;
    }

    private void renderSelectedRound() {
        List<Fixture> filtered = new ArrayList<>();
        for (Fixture fixture : allFixtures) {
            if ("Tất cả vòng".equals(selectedRoundLabel) || roundLabel(fixture).equals(selectedRoundLabel)) {
                filtered.add(fixture);
            }
        }
        fixtureAdapter.submitList(groupByRound(filtered));
        binding.tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        if (filtered.isEmpty()) binding.tvEmptyState.setText("Chưa có trận đấu trong vòng này.");
    }

    private Fragment profileOrAuth() {
        return AuthSession.hasToken(requireContext()) ? new ProfileFragment() : new AuthLoginFragment();
    }

    private List<Fixture> groupByRound(List<Fixture> fixtures) {
        Map<String, List<Fixture>> grouped = new LinkedHashMap<>();
        for (Fixture fixture : fixtures) {
            String label = roundLabel(fixture);
            List<Fixture> rows = grouped.get(label);
            if (rows == null) {
                rows = new ArrayList<>();
                grouped.put(label, rows);
            }
            rows.add(fixture);
        }
        List<Fixture> output = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, List<Fixture>> entry : grouped.entrySet()) {
            output.add(MatchAdapter.roundHeader(entry.getKey(), entry.getValue().size(), index++));
            output.addAll(entry.getValue());
        }
        return output;
    }

    private String roundLabel(Fixture fixture) {
        return fixture.getRoundName().isEmpty()
                ? (fixture.getRound() == null ? "Lịch đấu" : "Vòng " + fixture.getRound())
                : fixture.getRoundName();
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

