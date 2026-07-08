package com.finalproject.v_league_ticket.presentation.homepage;

import android.net.Uri;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.data.remote.VLeagueApiClient;
import com.finalproject.v_league_ticket.databinding.FragmentHomepageBinding;
import com.finalproject.v_league_ticket.domain.model.GoalScorer;
import com.finalproject.v_league_ticket.domain.model.Fixture;
import com.finalproject.v_league_ticket.domain.model.HomeMatch;
import com.finalproject.v_league_ticket.domain.model.NewsPost;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.booking.TicketBookingFragment;
import com.finalproject.v_league_ticket.presentation.matches.FixturesFragment;
import com.finalproject.v_league_ticket.presentation.matches.MatchCenterFragment;
import com.finalproject.v_league_ticket.presentation.matches.ResultsFragment;
import com.finalproject.v_league_ticket.presentation.matches.StandingsFragment;
import com.finalproject.v_league_ticket.presentation.matches.UpcomingMatchDetailFragment;
import com.finalproject.v_league_ticket.presentation.news.NewsFragment;
import com.finalproject.v_league_ticket.presentation.profile.HeaderNotificationRouter;
import com.finalproject.v_league_ticket.presentation.profile.ProfileFragment;
import com.finalproject.v_league_ticket.presentation.shop.ShopFragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class HomepageFragment extends Fragment {
    private FragmentHomepageBinding binding;
    private final NewsAdapter newsAdapter = new NewsAdapter();
    private final HomeFixtureAdapter fixtureAdapter = new HomeFixtureAdapter(fixture -> navigateTo(TicketBookingFragment.newInstance(fixture)));
    private HomeMatch featuredMatch;
    private MediaPlayer welcomePlayer;
    private boolean seasonVideoReady = false;
    private boolean seasonVideoActive = false;

    public HomepageFragment() {
        super(R.layout.fragment_homepage);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentHomepageBinding.bind(view);
        bindStaticCopy();
        bindSportLowerCopy();
        setupWelcomeVideo();
        setupSeasonPassVideo();
        setupLists();
        bindWelcome();
        bindFeaturedLoadingSport();
        fixtureAdapter.submitList(new ArrayList<>());
        newsAdapter.submitList(new ArrayList<>());
        setupClicks();
        loadRemoteData();
        setupVideoVisibilityControl();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHomepageVideoPlayback();
    }

    @Override
    public void onPause() {
        pauseHomepageVideos();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.videoSeasonPassBackground.stopPlayback();
        }
        releaseWelcomeVideo();
        super.onDestroyView();
        binding = null;
    }

    private void setupLists() {
        binding.rvNews.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvNews.setAdapter(newsAdapter);
        binding.rvNews.setHasFixedSize(false);
        binding.rvNews.setItemViewCacheSize(6);
        binding.rvNews.setItemAnimator(null);
        binding.rvUpcomingFixtures.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvUpcomingFixtures.setAdapter(fixtureAdapter);
        binding.rvUpcomingFixtures.setHasFixedSize(false);
        binding.rvUpcomingFixtures.setItemViewCacheSize(6);
        binding.rvUpcomingFixtures.setItemAnimator(null);
    }

    private void setupClicks() {
        binding.cardLiveMatch.setOnClickListener(v -> openFeaturedMatch());
        binding.tvLiveMatchViewAll.setOnClickListener(v -> navigateTo(new ResultsFragment()));
        binding.tvFixturesViewAll.setOnClickListener(v -> navigateTo(new FixturesFragment()));
        binding.tvNewsViewAll.setOnClickListener(v -> navigateTo(new NewsFragment()));
        binding.btnPreOrder.setOnClickListener(v -> navigateTo(new TicketBookingFragment()));
        binding.actionStandings.setOnClickListener(v -> navigateTo(new StandingsFragment()));
        binding.actionFixtures.setOnClickListener(v -> navigateTo(new FixturesFragment()));
        binding.actionShop.setOnClickListener(v -> navigateTo(new ShopFragment()));
        binding.actionFanZone.setOnClickListener(v -> navigateTo(new ResultsFragment()));
        binding.navMatches.setOnClickListener(v -> navigateTo(new FixturesFragment()));
        binding.navShop.setOnClickListener(v -> navigateTo(new ShopFragment()));
        binding.navNews.setOnClickListener(v -> navigateTo(new NewsFragment()));
        binding.navProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        binding.appHeader.btnHeaderProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        HeaderNotificationRouter.bind(this, binding.appHeader);
    }

    private void loadRemoteData() {
        VLeagueApiClient api = VLeagueApiClient.getInstance();
        api.fetchFinished(new VLeagueApiClient.DataCallback<List<Fixture>>() {
            @Override
            public void onSuccess(List<Fixture> data) {
                if (binding == null || data == null || data.isEmpty()) return;
                Fixture selectedFixture = selectFeaturedResult(data);
                if (selectedFixture == null) {
                    bindFeaturedErrorSport();
                    return;
                }
                featuredMatch = VLeagueApiClient.toHomeMatch(selectedFixture);
                bindFeaturedMatch(featuredMatch);
                api.fetchMatchIncidents(featuredMatch.getId(), new VLeagueApiClient.DataCallback<List<String>>() {
                    @Override
                    public void onSuccess(List<String> rows) {
                        if (binding == null || rows == null || rows.isEmpty()) return;
                        List<String> cleanRows = cleanIncidentRows(rows);
                        if (!cleanRows.isEmpty()) {
                            binding.tvGoalScorers.setText(String.join("\n", cleanRows));
                            bindGoalColumns(cleanRows);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        // Keep the empty real-data state for this match.
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding != null) bindFeaturedErrorSport();
            }
        });
        api.fetchUpcoming(new VLeagueApiClient.DataCallback<List<Fixture>>() {
            @Override
            public void onSuccess(List<Fixture> data) {
                if (binding == null || data == null || data.isEmpty()) return;
                fixtureAdapter.submitList(new ArrayList<>(data.subList(0, Math.min(data.size(), 10))));
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding != null) fixtureAdapter.submitList(new ArrayList<>());
            }
        });
        api.fetchNews(VLeagueApiClient.NEWS_VLEAGUE_CATEGORY_ID, new VLeagueApiClient.DataCallback<List<NewsPost>>() {
            @Override
            public void onSuccess(List<NewsPost> data) {
                if (binding == null || data == null || data.isEmpty()) return;
                newsAdapter.submitList(new ArrayList<>(data));
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding != null) newsAdapter.submitList(new ArrayList<>());
            }
        });
    }

    private void bindFeaturedLoading() {
        binding.tvMatchTournamentInfo.setText("Đang tải dữ liệu trận đấu");
            binding.tvMatchVenueStatus.setText("V.League");
            binding.tvMatchStatus.setText("ĐANG TẢI");
        binding.tvScoreBlock.setText("--");
        binding.tvHomeTeamName.setText("--");
        binding.tvAwayTeamName.setText("--");
        binding.tvMatchScore.setText("--");
        binding.tvPenaltyScore.setVisibility(View.GONE);
            binding.tvGoalScorers.setText("Đang chờ diễn biến trận đấu");
        binding.tvHomeGoalScorers.setText("Đang tải");
        binding.tvAwayGoalScorers.setText("Đang tải");
        loadLogo(binding.imgHomeTeamLogo, "", R.drawable.ic_logo);
        loadLogo(binding.imgAwayTeamLogo, "", R.drawable.ic_logo);
    }

    private void bindFeaturedError() {
        featuredMatch = null;
        binding.tvMatchTournamentInfo.setText("Không tải được dữ liệu trận đấu");
            binding.tvMatchVenueStatus.setText("Vui lòng kiểm tra kết nối mạng");
            binding.tvMatchStatus.setText("LỖI");
        binding.tvScoreBlock.setText("--");
        binding.tvHomeTeamName.setText("--");
        binding.tvAwayTeamName.setText("--");
        binding.tvMatchScore.setText("--");
        binding.tvPenaltyScore.setVisibility(View.GONE);
            binding.tvGoalScorers.setText("Chưa có dữ liệu trận đấu.");
        binding.tvHomeGoalScorers.setText("--");
        binding.tvAwayGoalScorers.setText("--");
    }    private void bindStaticCopy() {
        binding.appHeader.tvHeaderTitle.setText(getString(R.string.league_title));
        binding.appHeader.tvHeaderSubtitle.setText(getString(R.string.league_tagline));
        binding.appHeader.tvHeaderSubtitle.setVisibility(View.VISIBLE);
        binding.imgSeasonPassBackground.setImageResource(R.drawable.svd);
        binding.videoWelcomeBackground.setVisibility(View.VISIBLE);
        binding.videoSeasonPassBackground.setVisibility(View.VISIBLE);
        binding.tvActionStandings.setText("BXH");
        binding.tvActionFixtures.setText("Lịch đấu");
        binding.tvActionShop.setText("Shop");
        binding.tvActionFanZone.setText("Fan Zone");
        binding.tvLiveMatchTitle.setText("Trận nổi bật");
        binding.tvLiveMatchViewAll.setText("Tất cả");
        binding.tvMatchHubHint.setText("Đội hình & thống kê");
        binding.tvUpcomingFixturesTitle.setText("Lịch mùa 25/26");
        binding.tvFixturesViewAll.setText("Xem lịch");
        binding.tvInsidePitch.setText("Tin mới VPF");
        binding.tvNewsViewAll.setText("Tất cả");
        binding.tvSeasonPassLabel.setText("V.LEAGUE 25/26");
        binding.tvSeasonPassTitle.setText("ĐẶT VÉ MÙA GIẢI MỚI");
        binding.tvSeasonPassDesc.setText("Chọn sân, chọn trận và giữ ghế yêu thích cho lịch đấu 25/26.");
        binding.tvSeasonCountdown.setText("Đang mở bán");
        binding.btnPreOrder.setText("Đặt vé ngay");
        binding.tvNavHomeLabel.setText("Trang chủ");
        binding.tvNavMatchesLabel.setText("Trận đấu");
        binding.tvNavShopLabel.setText("Shop");
        binding.tvNavNewsLabel.setText("Tin tức");
        binding.tvNavProfileLabel.setText("Cá nhân");
    }
    private void bindSportLowerCopy() {
        binding.tvLiveMatchTitle.setText("Kết quả nổi bật");
        binding.tvLiveMatchViewAll.setText("Xem tất cả");
        binding.tvMatchHubHint.setText("Xem Match Center");
    }

    private void bindFeaturedLoadingSport() {
        binding.tvMatchTournamentInfo.setText("Đang tải dữ liệu trận đấu");
        binding.tvMatchVenueStatus.setText("V.League");
            binding.tvMatchStatus.setText("ĐANG TẢI");
        binding.tvScoreBlock.setText("FT");
        binding.tvHomeTeamName.setText("--");
        binding.tvAwayTeamName.setText("--");
        binding.tvMatchScore.setText("--");
        binding.tvPenaltyScore.setVisibility(View.GONE);
            binding.tvGoalScorers.setText("Đang chờ diễn biến trận đấu");
        loadLogo(binding.imgHomeTeamLogo, "", R.drawable.ic_logo);
        loadLogo(binding.imgAwayTeamLogo, "", R.drawable.ic_logo);
    }

    private void bindFeaturedErrorSport() {
        featuredMatch = null;
        binding.tvMatchTournamentInfo.setText("Không tải được dữ liệu trận đấu");
            binding.tvMatchVenueStatus.setText("Vui lòng kiểm tra kết nối mạng");
            binding.tvMatchStatus.setText("LỖI");
        binding.tvScoreBlock.setText("--");
        binding.tvHomeTeamName.setText("--");
        binding.tvAwayTeamName.setText("--");
        binding.tvMatchScore.setText("--");
        binding.tvPenaltyScore.setVisibility(View.GONE);
            binding.tvGoalScorers.setText("Chưa có dữ liệu trận đấu.");
    }

    private void bindWelcome() {
        boolean loggedIn = AuthSession.hasToken(requireContext());
        binding.tvWelcomeTitle.setText(loggedIn ? "Chào mừng trở lại," : "V.League trong một chạm");
        binding.tvWelcomeName.setText(AuthSession.userName(requireContext()) == null ? getString(R.string.user_name) : AuthSession.userName(requireContext()));
        binding.tvWelcomeName.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        binding.tvWelcomeSubtitle.setText(loggedIn
                ? "Theo dõi đội bóng, lịch đấu, vé và cửa hàng trong một nơi."
                : "Tỷ số, lịch đấu, vé và tin mới trong một trang chủ.");
    }

    private void setupWelcomeVideo() {
        binding.videoWelcomeBackground.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                startWelcomeVideo(surfaceTexture);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                applyWelcomeVideoCrop();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                releaseWelcomeVideo();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                // Frame updates are handled by TextureView.
            }
        });
    }

    private void setupSeasonPassVideo() {
        Uri videoUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + R.raw.videoplayback);
        binding.videoSeasonPassBackground.setVideoURI(videoUri);
        binding.videoSeasonPassBackground.setOnPreparedListener(player -> {
            player.setLooping(true);
            player.setVolume(0f, 0f);
            seasonVideoReady = true;
            updateHomepageVideoPlayback();
        });
        binding.videoSeasonPassBackground.setOnErrorListener((player, what, extra) -> {
            binding.videoSeasonPassBackground.setVisibility(View.GONE);
            binding.imgSeasonPassBackground.setVisibility(View.VISIBLE);
            return true;
        });
    }

    private void setupVideoVisibilityControl() {
        binding.getRoot().getViewTreeObserver().addOnScrollChangedListener(this::updateHomepageVideoPlayback);
        binding.getRoot().post(this::updateHomepageVideoPlayback);
    }

    private void updateHomepageVideoPlayback() {
        if (binding == null || !isResumed()) return;
        boolean seasonVisible = isMeaningfullyVisible(binding.cardSeasonPass);
        if (seasonVisible) {
            pauseWelcomeVideo();
            startSeasonVideo();
        } else {
            pauseSeasonVideo();
            startWelcomeVideoIfReady();
        }
    }

    private boolean isMeaningfullyVisible(View view) {
        if (view == null || !view.isShown()) return false;
        android.graphics.Rect rect = new android.graphics.Rect();
        boolean visible = view.getGlobalVisibleRect(rect);
        return visible && rect.height() >= Math.max(dp(80), view.getHeight() / 3);
    }

    private void pauseHomepageVideos() {
        pauseWelcomeVideo();
        pauseSeasonVideo();
    }

    private void pauseWelcomeVideo() {
        try {
            if (welcomePlayer != null && welcomePlayer.isPlaying()) welcomePlayer.pause();
        } catch (Exception ignored) {
        }
    }

    private void startWelcomeVideoIfReady() {
        try {
            if (welcomePlayer != null && !welcomePlayer.isPlaying()) welcomePlayer.start();
        } catch (Exception ignored) {
        }
    }

    private void pauseSeasonVideo() {
        if (binding == null || !seasonVideoActive) return;
        try {
            if (binding.videoSeasonPassBackground.isPlaying()) binding.videoSeasonPassBackground.pause();
        } catch (Exception ignored) {
        }
        seasonVideoActive = false;
    }

    private void startSeasonVideo() {
        if (binding == null || !seasonVideoReady || seasonVideoActive) return;
        try {
            binding.videoSeasonPassBackground.start();
            seasonVideoActive = true;
        } catch (Exception ignored) {
        }
    }

    private void startWelcomeVideo(SurfaceTexture surfaceTexture) {
        releaseWelcomeVideo();
        try {
            Uri videoUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + R.raw.welcome_background);
            welcomePlayer = MediaPlayer.create(requireContext(), videoUri);
            if (welcomePlayer == null) {
                binding.videoWelcomeBackground.setVisibility(View.GONE);
                return;
            }
            welcomePlayer.setSurface(new Surface(surfaceTexture));
            welcomePlayer.setLooping(true);
            welcomePlayer.setVolume(0f, 0f);
            welcomePlayer.setOnVideoSizeChangedListener((player, width, height) -> applyWelcomeVideoCrop());
            updateHomepageVideoPlayback();
            binding.videoWelcomeBackground.post(this::applyWelcomeVideoCrop);
        } catch (Exception ignored) {
            if (binding != null) {
                binding.videoWelcomeBackground.setVisibility(View.GONE);
            }
        }
    }

    private void applyWelcomeVideoCrop() {
        if (binding == null || welcomePlayer == null) return;
        int videoWidth = welcomePlayer.getVideoWidth();
        int videoHeight = welcomePlayer.getVideoHeight();
        int viewWidth = binding.videoWelcomeBackground.getWidth();
        int viewHeight = binding.videoWelcomeBackground.getHeight();
        if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return;

        Matrix matrix = new Matrix();
        float viewRatio = (float) viewWidth / viewHeight;
        float videoRatio = (float) videoWidth / videoHeight;
        float scaleX = 1f;
        float scaleY = 1f;

        if (videoRatio > viewRatio) {
            scaleX = videoRatio / viewRatio;
        } else {
            scaleY = viewRatio / videoRatio;
        }

        matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f);
        binding.videoWelcomeBackground.setTransform(matrix);
    }

    private void releaseWelcomeVideo() {
        if (welcomePlayer == null) return;
        try {
            welcomePlayer.stop();
        } catch (Exception ignored) {
        }
        welcomePlayer.release();
        welcomePlayer = null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void bindFeaturedMatch(HomeMatch match) {
        boolean hasPenalty = match.getHomePenaltyScore() != null && match.getAwayPenaltyScore() != null;
        binding.tvMatchTournamentInfo.setText(tournamentLine(match));
        binding.tvMatchVenueStatus.setText(venueLine(match));
        binding.tvMatchStatus.setText(hasPenalty ? "PEN" : statusBadge(match));
        binding.tvScoreBlock.setText(cleanStatusSport(match.getStatusType()));
        binding.tvHomeTeamName.setText(match.getHomeTeamName());
        binding.tvAwayTeamName.setText(match.getAwayTeamName());
        binding.tvMatchScore.setText(score(match.getHomeScore(), match.getAwayScore()));
        binding.tvPenaltyScore.setVisibility(hasPenalty ? View.VISIBLE : View.GONE);
        binding.tvPenaltyScore.setText(hasPenalty ? "PEN " + match.getHomePenaltyScore() + " - " + match.getAwayPenaltyScore() : "");
        binding.tvGoalScorers.setText(goalTextSport(match));
        bindGoalColumnsFromModel(match);
        loadLogo(binding.imgHomeTeamLogo, match.getHomeLogoUrl(), R.drawable.ic_logo);
        loadLogo(binding.imgAwayTeamLogo, match.getAwayLogoUrl(), R.drawable.ic_logo);
    }

    private Fixture selectFeaturedResult(List<Fixture> fixtures) {
        Fixture selected = null;
        for (Fixture fixture : fixtures) {
            if (fixture == null || !fixture.hasScore()) continue;
            if (selected == null || timestampOf(fixture) > timestampOf(selected)) {
                selected = fixture;
            }
        }
        return selected == null && !fixtures.isEmpty() ? fixtures.get(0) : selected;
    }

    private long timestampOf(Fixture fixture) {
        return fixture.getStartTimestamp() == null ? 0L : fixture.getStartTimestamp();
    }

    private String tournamentLine(HomeMatch match) {
        String round = match.getRoundName() == null || match.getRoundName().trim().isEmpty()
                ? ""
                : match.getRoundName().trim();
        String league = round.toLowerCase(Locale.ROOT).contains("final") ? "V.League Playoff" : match.getTournamentName();
        return round.isEmpty() ? league : league + " - " + round;
    }

    private String venueLine(HomeMatch match) {
        String venue = match.getVenue() == null || match.getVenue().trim().isEmpty()
                ? "Sân vận động V.League"
                : match.getVenue().trim();
        return venue + " · " + formatMatchTimeDate(match.getStartTimestamp());
    }

    private String formatMatchDate(Long timestamp) {
        if (timestamp == null || timestamp <= 0) return "--/--/----";
        long millis = timestamp < 100000000000L ? timestamp * 1000L : timestamp;
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(millis));
    }

    private String formatMatchTimeDate(Long timestamp) {
        if (timestamp == null || timestamp <= 0) return "--:-- · --/--/----";
        long millis = timestamp < 100000000000L ? timestamp * 1000L : timestamp;
        return new SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.getDefault()).format(new Date(millis));
    }

    private String statusBadge(HomeMatch match) {
        if (match.getStatus() != null && !match.getStatus().trim().isEmpty()) {
            String status = match.getStatus().trim().toUpperCase(Locale.ROOT);
            if (status.length() <= 4) return status;
        }
        return cleanStatusSport(match.getStatusType());
    }

    private List<String> cleanIncidentRows(List<String> rows) {
        List<String> cleanRows = new ArrayList<>();
        for (String row : rows) {
            if (row == null) continue;
            String trimmed = row.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            int separator = trimmed.indexOf(": ");
            String header = separator > 0 ? trimmed.substring(0, separator) : "";
            String body = separator > 0 ? trimmed.substring(separator + 2) : trimmed;
            String headerLower = header.toLowerCase(Locale.ROOT);
            boolean scoringRow = headerLower.contains("goal") || headerLower.contains("penalty")
                    || lower.startsWith("goal:") || lower.startsWith("penalty:");
            if (!scoringRow || lower.contains("penalty shootout")) continue;
            String minute = "";
            int minuteIndex = header.indexOf("'");
            if (minuteIndex > 0) minute = header.substring(0, minuteIndex + 1).trim();
            body = body.replaceFirst("(?i)^goal:\\s*", "")
                    .replaceFirst("(?i)^penalty:\\s*", "")
                    .trim();
            String display = (minute + " " + body).trim();
            if (!display.isEmpty()) cleanRows.add(display);
            if (cleanRows.size() == 2) break;
        }
        return cleanRows;
    }

    private String goalTextSport(HomeMatch match) {
        StringBuilder builder = new StringBuilder();
        for (GoalScorer scorer : match.getGoalScorers()) {
            if (builder.length() > 0) builder.append('\n');
            builder.append(formatGoal(scorer));
            if (builder.length() > 0 && builder.toString().split("\n").length == 2) break;
        }
        return builder.length() == 0 ? "Chưa có diễn biến bàn thắng" : builder.toString();
    }

    private String cleanStatusSport(String statusType) {
        if (statusType == null || statusType.isEmpty()) return "FT";
        if ("finished".equalsIgnoreCase(statusType)) return "FT";
        if ("inprogress".equalsIgnoreCase(statusType)) return "LIVE";
        if ("notstarted".equalsIgnoreCase(statusType)) return "NS";
        return statusType.toUpperCase(Locale.ROOT);
    }

    private void bindGoalColumns(List<String> rows) {
        List<String> homeRows = new ArrayList<>();
        List<String> awayRows = new ArrayList<>();
        for (String row : rows) {
            if (row == null || row.trim().isEmpty()) continue;
            String lower = row.toLowerCase(Locale.ROOT);
            String display = compactScorerRow(row);
            if (lower.contains("chủ nhà")) {
                homeRows.add(display);
            } else if (lower.contains("đội khách")) {
                awayRows.add(display);
            } else if (homeRows.size() <= awayRows.size()) {
                homeRows.add(display);
            } else {
                awayRows.add(display);
            }
        }
        binding.tvHomeGoalScorers.setText(homeRows.isEmpty() ? "--" : String.join("\n", homeRows));
        binding.tvAwayGoalScorers.setText(awayRows.isEmpty() ? "--" : String.join("\n", awayRows));
    }

    private void bindGoalColumnsFromModel(HomeMatch match) {
        List<String> homeRows = new ArrayList<>();
        List<String> awayRows = new ArrayList<>();
        for (GoalScorer scorer : match.getGoalScorers()) {
            String row = formatGoal(scorer)
                    .replace("Chủ nhà - ", "")
                    .replace("Đội khách - ", "")
                    .replace("Bàn thắng - ", "");
            if (Boolean.TRUE.equals(scorer.isHome())) {
                homeRows.add(row);
            } else if (Boolean.FALSE.equals(scorer.isHome())) {
                awayRows.add(row);
            }
        }
        binding.tvHomeGoalScorers.setText(homeRows.isEmpty() ? "--" : String.join("\n", homeRows));
        binding.tvAwayGoalScorers.setText(awayRows.isEmpty() ? "--" : String.join("\n", awayRows));
    }

    private String compactScorerRow(String row) {
        return row.replace("Chủ nhà - ", "")
                .replace("Đội khách - ", "")
                .replace("Goal - ", "")
                .replace("Goal: ", "")
                .replaceAll("\\s*\\(\\d+-\\d+\\)\\s*$", "")
                .trim();
    }

    private void openFeaturedMatch() {
        if (featuredMatch == null) {
        toast("Dữ liệu trận đấu chưa sẵn sàng.");
            return;
        }
        ArrayList<String> scorers = new ArrayList<>();
        for (GoalScorer scorer : featuredMatch.getGoalScorers()) {
            scorers.add(formatGoal(scorer));
        }
        navigateTo(MatchCenterFragment.newInstance(
                featuredMatch.getId(),
                featuredMatch.getHomeTeamName(),
                featuredMatch.getAwayTeamName(),
                featuredMatch.getHomeScore() == null ? 0 : featuredMatch.getHomeScore(),
                featuredMatch.getAwayScore() == null ? 0 : featuredMatch.getAwayScore(),
                featuredMatch.getHomePenaltyScore(),
                featuredMatch.getAwayPenaltyScore(),
                featuredMatch.getHomeLogoUrl(),
                featuredMatch.getAwayLogoUrl(),
                featuredMatch.getVenue(),
                featuredMatch.getTournamentName(),
                featuredMatch.getRoundName(),
                featuredMatch.getRound(),
                featuredMatch.getStartTimestamp(),
                scorers
        ));
    }

    private String goalText(HomeMatch match) {
        StringBuilder builder = new StringBuilder();
        for (GoalScorer scorer : match.getGoalScorers()) {
            if (builder.length() > 0) builder.append('\n');
            builder.append(formatGoal(scorer));
        }
        return builder.length() == 0 ? "Chưa có diễn biến bàn thắng" : builder.toString();
    }

    private String formatGoal(GoalScorer scorer) {
        String minute = scorer.getMinute() == null ? "--'" : scorer.getMinute() + "'";
        String side = scorer.isHome() == null ? "Bàn thắng" : (scorer.isHome() ? "Chủ nhà" : "Đội khách");
        String playerName = scorer.getPlayerName().replaceAll("\\s*\\(\\d+-\\d+\\)\\s*$", "").trim();
        return minute + "  " + side + " - " + playerName;
    }

    private String cleanStatus(String statusType) {
        if ("finished".equalsIgnoreCase(statusType)) return "Hết trận";
        if ("inprogress".equalsIgnoreCase(statusType)) return "Đang diễn ra";
        if ("notstarted".equalsIgnoreCase(statusType)) return "Sắp diễn ra";
        return statusType;
    }

    private String score(Integer home, Integer away) {
        return home == null || away == null ? "VS" : home + " - " + away;
    }

    private void loadLogo(ImageView view, String url, int placeholder) {
        Glide.with(view).load(url == null || url.isEmpty() ? null : url)
                .placeholder(placeholder)
                .error(placeholder)
                .fitCenter()
                .into(view);
    }

    private Fragment profileOrAuth() {
        return AuthSession.hasToken(requireContext()) ? new ProfileFragment() : new AuthLoginFragment();
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName())
                .commit();
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}

