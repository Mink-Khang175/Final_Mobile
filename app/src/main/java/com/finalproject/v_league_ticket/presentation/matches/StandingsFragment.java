package com.finalproject.v_league_ticket.presentation.matches;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.data.remote.VLeagueApiClient;
import com.finalproject.v_league_ticket.databinding.FragmentMatchesStandingsBinding;
import com.finalproject.v_league_ticket.domain.model.Standing;
import com.finalproject.v_league_ticket.domain.model.VLeagueSeason;
import com.finalproject.v_league_ticket.domain.model.VLeagueSeasons;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.finalproject.v_league_ticket.presentation.news.NewsFragment;
import com.finalproject.v_league_ticket.presentation.profile.HeaderNotificationRouter;
import com.finalproject.v_league_ticket.presentation.profile.ProfileFragment;
import com.finalproject.v_league_ticket.presentation.shop.ShopFragment;
import com.finalproject.v_league_ticket.presentation.standings.StandingsAdapter;

import java.util.ArrayList;
import java.util.List;

public class StandingsFragment extends Fragment {
    private FragmentMatchesStandingsBinding binding;
    private final StandingsAdapter standingsAdapter = new StandingsAdapter();
    private VLeagueSeason selectedSeason = VLeagueSeasons.current();

    public StandingsFragment() {
        super(R.layout.fragment_matches_standings);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentMatchesStandingsBinding.bind(view);
        binding.rvStandings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStandings.setAdapter(standingsAdapter);
        binding.rvStandings.setHasFixedSize(false);
        binding.rvStandings.setItemViewCacheSize(12);
        binding.rvStandings.setItemAnimator(null);
        standingsAdapter.submitList(new ArrayList<>());
        binding.progressBar.setVisibility(View.GONE);
        binding.tableContainer.setVisibility(View.GONE);
        binding.tvEmptyState.setVisibility(View.GONE);
        setupClicks();
        binding.appHeader.tvHeaderSubtitle.setText(headerName());
        loadSeasonsAndStandings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadSeasonsAndStandings() {
        binding.progressBar.setVisibility(View.VISIBLE);
        VLeagueApiClient.getInstance().fetchSeasons(new VLeagueApiClient.DataCallback<List<VLeagueSeason>>() {
            @Override
            public void onSuccess(List<VLeagueSeason> data) {
                if (binding == null) return;
                if (data == null || data.isEmpty()) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tableContainer.setVisibility(View.GONE);
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                    binding.tvEmptyState.setText("Không tải được danh sách mùa giải. Vui lòng thử lại sau.");
                    return;
                }
                selectedSeason = currentSeasonFrom(data);
                bindSeasonChip();
                loadStandings();
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.tableContainer.setVisibility(View.GONE);
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.tvEmptyState.setText("Không tải được danh sách mùa giải. Vui lòng thử lại sau.");
            }
        });
    }

    private VLeagueSeason currentSeasonFrom(List<VLeagueSeason> seasons) {
        if (seasons != null) {
            for (VLeagueSeason season : seasons) {
                if (VLeagueSeasons.current().getLabel().equalsIgnoreCase(season.getLabel())) return season;
            }
            if (!seasons.isEmpty()) return seasons.get(0);
        }
        return VLeagueSeasons.current();
    }

    private void bindSeasonChip() {
        binding.tvScreenMeta.setText(selectedSeason.getLabel());
        binding.seasonContainer.removeAllViews();
        TextView chip = new TextView(requireContext());
        chip.setText(selectedSeason.getLabel());
        chip.setTextColor(requireContext().getColor(R.color.red_energy));
        chip.setTextSize(14f);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setPadding(24, 0, 24, 0);
        chip.setBackgroundResource(R.drawable.bg_segment_active);
        binding.seasonContainer.addView(chip);
    }

    private void setupClicks() {
        binding.tabFixtures.setOnClickListener(v -> navigateTo(new FixturesFragment()));
        binding.tabResults.setOnClickListener(v -> navigateTo(new ResultsFragment()));
        binding.bottomNav.navHome.setOnClickListener(v -> navigateTo(new HomepageFragment()));
        binding.bottomNav.navShop.setOnClickListener(v -> navigateTo(new ShopFragment()));
        binding.bottomNav.navNews.setOnClickListener(v -> navigateTo(new NewsFragment()));
        binding.bottomNav.navProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        binding.appHeader.btnHeaderProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        HeaderNotificationRouter.bind(this, binding.appHeader);
    }

    private void loadStandings() {
        binding.progressBar.setVisibility(View.VISIBLE);
        VLeagueApiClient.getInstance().fetchStandings(selectedSeason.getId(), new VLeagueApiClient.DataCallback<List<Standing>>() {
            @Override
            public void onSuccess(List<Standing> data) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                if (data == null || data.isEmpty()) {
                    binding.tableContainer.setVisibility(View.GONE);
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                    binding.tvEmptyState.setText("Chưa có bảng xếp hạng để hiển thị.");
                    return;
                }
                binding.tableContainer.setVisibility(View.VISIBLE);
                binding.tvEmptyState.setVisibility(View.GONE);
                standingsAdapter.submitList(new ArrayList<>(data));
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                boolean empty = standingsAdapter.getCurrentList().isEmpty();
                binding.tableContainer.setVisibility(empty ? View.GONE : View.VISIBLE);
                binding.tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (empty) binding.tvEmptyState.setText("Không tải được bảng xếp hạng. Vui lòng thử lại sau.");
            }
        });
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

