package com.finalproject.v_league_ticket.presentation.matches;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.data.remote.VLeagueApiClient;
import com.finalproject.v_league_ticket.databinding.FragmentMatchesDetailsBinding;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.finalproject.v_league_ticket.presentation.news.NewsFragment;
import com.finalproject.v_league_ticket.presentation.profile.ProfileFragment;
import com.finalproject.v_league_ticket.presentation.shop.ShopFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MatchCenterFragment extends Fragment {
    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_HOME = "home";
    private static final String ARG_AWAY = "away";
    private static final String ARG_HOME_SCORE = "home_score";
    private static final String ARG_AWAY_SCORE = "away_score";
    private static final String ARG_HOME_PENALTY = "home_penalty";
    private static final String ARG_AWAY_PENALTY = "away_penalty";
    private static final String ARG_HOME_LOGO = "home_logo";
    private static final String ARG_AWAY_LOGO = "away_logo";
    private static final String ARG_VENUE = "venue";
    private static final String ARG_TOURNAMENT = "tournament";
    private static final String ARG_ROUND = "round";
    private static final String ARG_GOALS = "goals";
    private FragmentMatchesDetailsBinding binding;

    public MatchCenterFragment() {
        super(R.layout.fragment_matches_details);
    }

    public static MatchCenterFragment newInstance(long eventId, String homeName, String awayName, int homeScore,
                                                  int awayScore, Integer homePenaltyScore, Integer awayPenaltyScore,
                                                  String homeLogoUrl, String awayLogoUrl, String venue,
                                                  String tournamentName, String roundName, Integer round,
                                                  Long timestamp, ArrayList<String> goalScorers) {
        MatchCenterFragment fragment = new MatchCenterFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, eventId);
        args.putString(ARG_HOME, homeName);
        args.putString(ARG_AWAY, awayName);
        args.putInt(ARG_HOME_SCORE, homeScore);
        args.putInt(ARG_AWAY_SCORE, awayScore);
        if (homePenaltyScore != null) args.putInt(ARG_HOME_PENALTY, homePenaltyScore);
        if (awayPenaltyScore != null) args.putInt(ARG_AWAY_PENALTY, awayPenaltyScore);
        args.putString(ARG_HOME_LOGO, homeLogoUrl);
        args.putString(ARG_AWAY_LOGO, awayLogoUrl);
        args.putString(ARG_VENUE, venue);
        args.putString(ARG_TOURNAMENT, tournamentName);
        args.putString(ARG_ROUND, roundName == null || roundName.isEmpty() ? (round == null ? "" : "Vòng " + round) : roundName);
        args.putStringArrayList(ARG_GOALS, goalScorers);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentMatchesDetailsBinding.bind(view);
        bindHeader();
        bindContent();
        setupClicks();
        selectTab(binding.llOverview);
        loadRemoteMatchData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void bindHeader() {
        Bundle args = requireArguments();
        binding.tvHeaderTitle.setText("Trung tâm trận đấu");
        binding.tvTournament.setText(args.getString(ARG_TOURNAMENT, "V.League 1") + " - " + args.getString(ARG_ROUND, ""));
        String venue = args.getString(ARG_VENUE, "Đang cập nhật sân");
        binding.tvVenueDate.setText(venue);
        binding.tvVenueDate.setVisibility(venue == null || venue.trim().isEmpty() || "Venue TBA".equalsIgnoreCase(venue.trim())
                ? View.GONE : View.VISIBLE);
        binding.tvHomeName.setText(args.getString(ARG_HOME, "Home"));
        binding.tvAwayName.setText(args.getString(ARG_AWAY, "Away"));
        binding.tvScore.setText(args.getInt(ARG_HOME_SCORE, 0) + " - " + args.getInt(ARG_AWAY_SCORE, 0));
        boolean hasPenalty = args.containsKey(ARG_HOME_PENALTY) && args.containsKey(ARG_AWAY_PENALTY);
        if (hasPenalty) {
            binding.tvStatus.setText("PEN " + args.getInt(ARG_HOME_PENALTY) + " - " + args.getInt(ARG_AWAY_PENALTY));
        } else {
            binding.tvStatus.setText("FT");
        }
        applyScoreHeaderLayout(hasPenalty);
        loadLogo(binding.imgHomeLogo, args.getString(ARG_HOME_LOGO, ""));
        loadLogo(binding.imgAwayLogo, args.getString(ARG_AWAY_LOGO, ""));
    }

    private void applyScoreHeaderLayout(boolean hasPenalty) {
        ViewGroup.LayoutParams backdropParams = binding.scoreHeaderBackdrop.getLayoutParams();
        backdropParams.height = dp(hasPenalty ? 244 : 242);
        binding.scoreHeaderBackdrop.setLayoutParams(backdropParams);

        ViewGroup.LayoutParams cardParams = binding.cardScoreSummary.getLayoutParams();
        cardParams.height = dp(hasPenalty ? 166 : 168);
        binding.cardScoreSummary.setLayoutParams(cardParams);

        ViewGroup.LayoutParams statusParams = binding.tvStatus.getLayoutParams();
        statusParams.width = dp(hasPenalty ? 86 : 70);
        statusParams.height = dp(hasPenalty ? 30 : 28);
        if (statusParams instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) statusParams).topMargin = dp(hasPenalty ? 2 : 4);
        }
        binding.tvStatus.setLayoutParams(statusParams);
        binding.tvScore.setTextSize(hasPenalty ? 32f : 34f);
    }

    private void bindContent() {
        List<String> initialFacts = new ArrayList<>();
        initialFacts.add("Sân: " + requireArguments().getString(ARG_VENUE, "Đang cập nhật"));
        renderFacts(binding.llFacts, initialFacts);

        ArrayList<String> goals = requireArguments().getStringArrayList(ARG_GOALS);
        if (goals == null || goals.isEmpty()) {
            renderTimeline(binding.llGoalTimeline, new ArrayList<>(), true);
        } else {
            renderTimeline(binding.llGoalTimeline, goals, true);
        }

        renderStats(binding.llOverviewStats, new ArrayList<>(), 4);
        renderLineups(binding.llLineupCards, new ArrayList<>());
        renderStats(binding.llStatsRows, new ArrayList<>(), 18);
        renderTimeline(binding.llTimelineOnlyRows, new ArrayList<>(), false);
        renderRatings(binding.llRatedPlayers, new ArrayList<>(), "Đang chờ dữ liệu điểm cầu thủ.");
    }

    private void loadRemoteMatchData() {
        long eventId = requireArguments().getLong(ARG_EVENT_ID, 0L);
        if (eventId <= 0L) return;
        VLeagueApiClient api = VLeagueApiClient.getInstance();
        api.fetchMatchFacts(eventId, new VLeagueApiClient.DataCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> rows) {
                if (binding == null || rows == null || rows.isEmpty()) return;
                renderFacts(binding.llFacts, rows);
            }

            @Override
            public void onError(Throwable throwable) {
                // Keep the existing venue from the result card.
            }
        });
        api.fetchMatchIncidents(eventId, new VLeagueApiClient.DataCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> rows) {
                if (binding == null || rows == null || rows.isEmpty()) return;
                renderTimeline(binding.llGoalTimeline, rows.subList(0, Math.min(rows.size(), 5)), true);
                renderTimeline(binding.llTimelineOnlyRows, rows, false);
            }

            @Override
            public void onError(Throwable throwable) {
                // Keep the empty real-data state.
            }
        });
        api.fetchMatchStatistics(eventId, new VLeagueApiClient.DataCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> rows) {
                if (binding == null || rows == null || rows.isEmpty()) return;
                renderStats(binding.llOverviewStats, rows, 5);
                renderStats(binding.llStatsRows, rows, 18);
            }

            @Override
            public void onError(Throwable throwable) {
                // Keep the empty real-data state.
            }
        });
        api.fetchMatchLineups(eventId, new VLeagueApiClient.DataCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> rows) {
                if (binding == null || rows == null || rows.isEmpty()) return;
                renderLineups(binding.llLineupCards, rows);
            }

            @Override
            public void onError(Throwable throwable) {
                // Keep the empty real-data state.
            }
        });
        api.fetchTopRatedPlayers(eventId, new VLeagueApiClient.DataCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> rows) {
                if (binding == null || rows == null || rows.isEmpty()) return;
                renderRatings(binding.llRatedPlayers, rows, "Chưa có điểm cầu thủ.");
            }

            @Override
            public void onError(Throwable throwable) {
                // Keep the empty real-data state.
            }
        });
    }

    private void setupClicks() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.tabOverview.setOnClickListener(v -> selectTab(binding.llOverview));
        binding.tabLineups.setOnClickListener(v -> selectTab(binding.llLineups));
        binding.tabStats.setOnClickListener(v -> selectTab(binding.llStats));
        binding.tabTimeline.setOnClickListener(v -> selectTab(binding.llTimeline));
        binding.tabH2h.setOnClickListener(v -> selectTab(binding.llH2h));
        binding.bottomNav.navHome.setOnClickListener(v -> navigateTo(new HomepageFragment()));
        binding.bottomNav.navMatches.setOnClickListener(v -> navigateTo(new ResultsFragment()));
        binding.bottomNav.navShop.setOnClickListener(v -> navigateTo(new ShopFragment()));
        binding.bottomNav.navNews.setOnClickListener(v -> navigateTo(new NewsFragment()));
        binding.bottomNav.navProfile.setOnClickListener(v -> navigateTo(AuthSession.hasToken(requireContext()) ? new ProfileFragment() : new AuthLoginFragment()));
    }

    private void selectTab(View visibleView) {
        binding.llOverview.setVisibility(View.GONE);
        binding.llLineups.setVisibility(View.GONE);
        binding.llStats.setVisibility(View.GONE);
        binding.llTimeline.setVisibility(View.GONE);
        binding.llH2h.setVisibility(View.GONE);
        visibleView.setVisibility(View.VISIBLE);
        styleMatchTabs(visibleView);
    }

    private void styleMatchTabs(View visibleView) {
        TextView[] tabs = {
                binding.tabOverview,
                binding.tabLineups,
                binding.tabStats,
                binding.tabTimeline,
                binding.tabH2h
        };
        for (TextView tab : tabs) {
            tab.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tab.setTextColor(requireContext().getColor(R.color.dark_gray_text));
        }

        TextView selectedTab = binding.tabOverview;
        if (visibleView == binding.llLineups) selectedTab = binding.tabLineups;
        else if (visibleView == binding.llStats) selectedTab = binding.tabStats;
        else if (visibleView == binding.llTimeline) selectedTab = binding.tabTimeline;
        else if (visibleView == binding.llH2h) selectedTab = binding.tabH2h;

        selectedTab.setBackgroundResource(R.drawable.bg_segment_active);
        selectedTab.setTextColor(requireContext().getColor(R.color.stadium_ink));
    }

    private void renderFacts(LinearLayout container, List<String> rows) {
        container.removeAllViews();
        if (rows == null || rows.isEmpty()) {
            addEmptyState(container, "Chưa có dữ kiện trận đấu.");
            return;
        }
        for (String raw : rows) {
            KeyValue row = parseKeyValue(raw);
            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(0, dp(8), 0, dp(8));
            item.addView(text(row.title, 11f, R.color.dark_gray_text, true, true));
            item.addView(text(row.body, 14f, R.color.stadium_ink, true, false));
            container.addView(item);
        }
    }

    private void renderSimpleRows(LinearLayout container, List<String> rows, String emptyMessage) {
        container.removeAllViews();
        if (rows == null || rows.isEmpty()) {
            addEmptyState(container, emptyMessage);
            return;
        }
        for (String raw : rows) {
            KeyValue row = parseKeyValue(raw);
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackground(rounded(requireContext().getColor(R.color.white), dp(14), dp(1), requireContext().getColor(R.color.sports_outline)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(8));
            card.setLayoutParams(params);
            card.addView(text(row.title, 12f, R.color.dark_gray_text, true, true));
            card.addView(text(row.body, 14f, R.color.stadium_ink, true, false));
            container.addView(card);
        }
    }

    private void renderRatings(LinearLayout container, List<String> rows, String emptyMessage) {
        container.removeAllViews();
        if (rows == null || rows.isEmpty()) {
            addEmptyState(container, emptyMessage);
            return;
        }

        List<RatedPlayerUi> home = new ArrayList<>();
        List<RatedPlayerUi> away = new ArrayList<>();
        for (String raw : rows) {
            RatedPlayerUi player = parseRatedPlayer(raw);
            if (player == null) continue;
            if ("away".equals(player.side.toLowerCase(Locale.ROOT))) away.add(player);
            else home.add(player);
        }

        LinearLayout board = new LinearLayout(requireContext());
        board.setOrientation(LinearLayout.HORIZONTAL);
        board.setBaselineAligned(false);
        board.setPadding(dp(10), dp(12), dp(10), dp(12));
        board.setBackground(rounded(requireContext().getColor(R.color.white), dp(18), dp(1), requireContext().getColor(R.color.sports_outline)));
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        board.setLayoutParams(boardParams);

        board.addView(ratingColumn(requireArguments().getString(ARG_HOME, "Home"), home),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        View divider = new View(requireContext());
        divider.setBackgroundColor(requireContext().getColor(R.color.sports_outline));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(dp(1), ViewGroup.LayoutParams.MATCH_PARENT);
        dividerParams.setMargins(dp(8), 0, dp(8), 0);
        board.addView(divider, dividerParams);
        board.addView(ratingColumn(requireArguments().getString(ARG_AWAY, "Away"), away),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        container.addView(board);
    }

    private LinearLayout ratingColumn(String teamName, List<RatedPlayerUi> players) {
        LinearLayout column = new LinearLayout(requireContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(2), 0, dp(2), 0);

        TextView title = text(teamName, 12f, R.color.stadium_ink, true, true);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, 0, 0, dp(10));
        column.addView(title, titleParams);

        if (players.isEmpty()) {
            TextView empty = text("Chưa có điểm", 12f, R.color.dark_gray_text, false, false);
            empty.setGravity(Gravity.CENTER);
            column.addView(empty);
            return column;
        }
        for (int i = 0; i < Math.min(players.size(), 5); i++) {
            column.addView(ratingPlayerRow(players.get(i), i == 0));
        }
        return column;
    }

    private View ratingPlayerRow(RatedPlayerUi player, boolean featured) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(7), dp(7), dp(7), dp(7));
        row.setBackground(rounded(requireContext().getColor(featured ? R.color.colorSurfaceVariant : R.color.white),
                dp(14), dp(1), requireContext().getColor(R.color.sports_outline)));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowParams);

        FrameLayout avatar = new FrameLayout(requireContext());
        ImageView photo = new ImageView(requireContext());
        photo.setBackgroundResource(R.drawable.bg_player_avatar);
        photo.setClipToOutline(true);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.addView(photo, new FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER));
        TextView initials = text(initials(player.name), 11f, R.color.stadium_ink, true, false);
        initials.setGravity(Gravity.CENTER);
        initials.setBackgroundResource(R.drawable.bg_player_avatar);
        avatar.addView(initials, new FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER));
        if (!player.photoUrl().isEmpty()) {
            Glide.with(photo).load(player.photoUrl())
                    .placeholder(R.drawable.bg_player_avatar)
                    .error(R.drawable.bg_player_avatar)
                    .centerCrop()
                    .into(photo);
            initials.setVisibility(View.GONE);
        }
        row.addView(avatar, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(player.name, 11.5f, R.color.stadium_ink, true, false);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView meta = text(player.side.equalsIgnoreCase("home") ? "Chủ nhà" : "Đội khách", 9.5f, R.color.dark_gray_text, true, true);
        info.addView(name);
        info.addView(meta);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoParams.setMargins(dp(7), 0, dp(6), 0);
        row.addView(info, infoParams);

        TextView rating = text(player.rating, 11f, R.color.white, true, false);
        rating.setGravity(Gravity.CENTER);
        rating.setBackgroundResource(R.drawable.bg_match_center_status_ended);
        row.addView(rating, new LinearLayout.LayoutParams(dp(38), dp(26)));
        return row;
    }

    private RatedPlayerUi parseRatedPlayer(String raw) {
        KeyValue row = parseKeyValue(raw);
        String side = row.title;
        String body = row.body;
        if (body.contains("|name=")) {
            return new RatedPlayerUi(side, metadataValue(body, "name"), metadataValue(body, "rating"), metadataValue(body, "id"));
        }
        int separator = body.lastIndexOf(" - ");
        if (separator > 0) {
            return new RatedPlayerUi(side, body.substring(0, separator).trim(), body.substring(separator + 3).trim(), "");
        }
        return null;
    }

    private void renderTimeline(LinearLayout container, List<String> rows, boolean compact) {
        container.removeAllViews();
        if (rows == null || rows.isEmpty()) {
            addEmptyState(container, "Đang chờ diễn biến trận đấu.");
            return;
        }
        for (String raw : rows) {
            TimelineEvent event = parseTimelineEvent(raw);
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dp(12));
            row.setLayoutParams(rowParams);

            TextView minute = text(event.minute, 12f, R.color.stadium_ink, true, false);
            minute.setGravity(Gravity.CENTER);
            minute.setBackgroundResource(R.drawable.bg_timeline_minute);
            row.addView(minute, new LinearLayout.LayoutParams(dp(44), dp(44)));

            LinearLayout body = new LinearLayout(requireContext());
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(dp(12), dp(10), dp(12), dp(10));
            body.setBackground(rounded(requireContext().getColor(R.color.white), dp(16), dp(1), requireContext().getColor(R.color.sports_outline)));
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            bodyParams.setMargins(dp(12), 0, 0, 0);
            body.setLayoutParams(bodyParams);

            LinearLayout titleRow = new LinearLayout(requireContext());
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView icon = text(event.icon, 10f, R.color.white, true, false);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(rounded(requireContext().getColor(event.iconColorRes), dp(999), 0, 0));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
            iconParams.setMargins(0, 0, dp(8), 0);
            titleRow.addView(icon, iconParams);
            titleRow.addView(text(event.title, 12f, event.colorRes, true, true));
            body.addView(titleRow);
            body.addView(text(event.body, compact ? 13f : 14f, R.color.stadium_ink, true, false));
            if (!event.detail.isEmpty() && !compact) {
                body.addView(text(event.detail, 12f, R.color.dark_gray_text, false, false));
            }
            row.addView(body);
            container.addView(row);
        }
    }

    private void renderStats(LinearLayout container, List<String> rows, int limit) {
        container.removeAllViews();
        if (rows == null || rows.isEmpty()) {
            addEmptyState(container, "Đang chờ dữ liệu thống kê.");
            return;
        }
        int rendered = 0;
        for (String raw : rows) {
            StatRow stat = parseStatRow(raw);
            if (stat == null) continue;
            addStatRow(container, stat);
            rendered++;
            if (rendered >= limit) break;
        }
        if (rendered == 0) {
            addEmptyState(container, "Chưa có thống kê có thể so sánh.");
        }
    }

    private void addStatRow(LinearLayout container, StatRow stat) {
        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, dp(7), 0, dp(13));

        TextView label = text(stat.label.toUpperCase(Locale.ROOT), 11f, R.color.dark_gray_text, true, true);
        label.setGravity(Gravity.CENTER);
        wrapper.addView(label);

        LinearLayout values = new LinearLayout(requireContext());
        values.setGravity(Gravity.CENTER);
        values.setOrientation(LinearLayout.HORIZONTAL);
        values.setPadding(0, dp(8), 0, dp(7));
        TextView home = text(stat.homeText, 20f, R.color.stadium_ink, true, false);
        home.setGravity(Gravity.START);
        TextView away = text(stat.awayText, 20f, R.color.stadium_ink, true, false);
        away.setGravity(Gravity.END);
        TextView spacer = text("", 1f, R.color.transparent, false, false);
        values.addView(home, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        values.addView(spacer, new LinearLayout.LayoutParams(dp(8), 1));
        values.addView(away, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        wrapper.addView(values);

        wrapper.addView(createDualBar(stat.homeValue, stat.awayValue));
        container.addView(wrapper);
    }

    private View createDualBar(double homeValue, double awayValue) {
        double total = Math.max(1d, Math.abs(homeValue) + Math.abs(awayValue));
        float homePct = (float) Math.max(0.5d, Math.abs(homeValue) / total * 100d);
        float awayPct = (float) Math.max(0.5d, Math.abs(awayValue) / total * 100d);

        LinearLayout bar = new LinearLayout(requireContext());
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackgroundResource(R.drawable.bg_stat_track);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        barParams.setMargins(0, 0, 0, dp(2));
        bar.setLayoutParams(barParams);

        LinearLayout left = halfBar(Gravity.END, homePct, requireContext().getColor(R.color.stadium_ink));
        LinearLayout right = halfBar(Gravity.START, awayPct, requireContext().getColor(R.color.red_energy));
        bar.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        View gap = new View(requireContext());
        bar.addView(gap, new LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT));
        bar.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        return bar;
    }

    private LinearLayout halfBar(int gravity, float fillPct, int color) {
        LinearLayout half = new LinearLayout(requireContext());
        half.setOrientation(LinearLayout.HORIZONTAL);
        half.setGravity(gravity);
        float spacePct = Math.max(0.1f, 100f - fillPct);
        View fill = new View(requireContext());
        fill.setBackground(rounded(color, dp(999), 0, 0));
        View space = new View(requireContext());
        if (gravity == Gravity.END) {
            half.addView(space, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, spacePct));
            half.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, fillPct));
        } else {
            half.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, fillPct));
            half.addView(space, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, spacePct));
        }
        return half;
    }

    private void renderLineups(LinearLayout container, List<String> rows) {
        container.removeAllViews();
        if (rows == null || rows.isEmpty()) {
            addEmptyState(container, "Đang chờ dữ liệu đội hình.");
            return;
        }
        LineupData data = parseLineupData(rows);
        addPitch(container, requireArguments().getString(ARG_HOME, "Home"), data.homeFormation, data.homeXi);
        addBench(container, "Dự bị chủ nhà", data.homeSubs);
        addPitch(container, requireArguments().getString(ARG_AWAY, "Away"), data.awayFormation, data.awayXi);
        addBench(container, "Dự bị đội khách", data.awaySubs);
    }

    private void addPitch(LinearLayout container, String teamName, String formation, List<PlayerRow> players) {
        TextView title = text(teamName + (formation.isEmpty() ? "" : " · " + formation), 16f, R.color.stadium_ink, true, false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        title.setText(formation.isEmpty() ? teamName : teamName + "  " + formation);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleParams.setMargins(0, dp(10), 0, dp(10));
        title.setLayoutParams(titleParams);
        container.addView(title);

        FrameLayout pitch = new FrameLayout(requireContext());
        pitch.setBackgroundResource(R.drawable.bg_pitch_board);
        pitch.setClipToOutline(true);
        pitch.setElevation(dp(2));
        LinearLayout.LayoutParams pitchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(500));
        pitchParams.setMargins(0, 0, 0, dp(12));
        pitch.setLayoutParams(pitchParams);
        pitch.addView(new PitchLinesView(requireContext()), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (players.isEmpty()) {
            TextView empty = text("Chưa có XI ra sân.", 14f, R.color.stadium_ink, true, false);
            empty.setGravity(Gravity.CENTER);
            pitch.addView(empty, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            double[][] positions = playerPositions(formation, players.size());
            int boardWidth = Math.max(dp(280), getResources().getDisplayMetrics().widthPixels - dp(48));
            int nodeWidth = dp(68);
            int nodeHeight = dp(100);
            int boardHeight = dp(500);
            for (int i = 0; i < Math.min(players.size(), positions.length); i++) {
                View node = playerNode(players.get(i));
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(nodeWidth, nodeHeight);
                params.leftMargin = clamp((int) (boardWidth * positions[i][0]) - nodeWidth / 2, dp(10), boardWidth - nodeWidth - dp(10));
                params.topMargin = clamp((int) (boardHeight * positions[i][1]) - nodeHeight / 2, dp(12), boardHeight - nodeHeight - dp(12));
                pitch.addView(node, params);
            }
        }
        container.addView(pitch);
    }

    private void addBench(LinearLayout container, String title, List<PlayerRow> players) {
        if (players.isEmpty()) return;
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(10));
        card.setBackground(rounded(requireContext().getColor(R.color.white), dp(16), dp(1), requireContext().getColor(R.color.sports_outline)));
        card.setElevation(dp(1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(params);
        TextView label = text(title, 12f, R.color.dark_gray_text, true, true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(0, 0, 0, dp(8));
        card.addView(label, labelParams);
        int limit = Math.min(players.size(), 12);
        for (int i = 0; i < limit; i++) {
            PlayerRow player = players.get(i);
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(8), dp(10), dp(8));
            row.setBackground(rounded(requireContext().getColor(R.color.colorSurfaceVariant), dp(12), 0, 0));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dp(6));

            TextView shirt = text(player.shirt.isEmpty() ? "--" : player.shirt, 11f, R.color.white, true, false);
            shirt.setGravity(Gravity.CENTER);
            shirt.setBackground(rounded(requireContext().getColor(R.color.stadium_ink), dp(999), 0, 0));
            row.addView(shirt, new LinearLayout.LayoutParams(dp(30), dp(30)));

            LinearLayout info = new LinearLayout(requireContext());
            info.setOrientation(LinearLayout.VERTICAL);
            TextView name = text(player.name, 13f, R.color.stadium_ink, true, false);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(name);
            if (!player.position.isEmpty()) {
                info.addView(text(player.position, 11f, R.color.dark_gray_text, false, true));
            }
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            infoParams.setMargins(dp(10), 0, 0, 0);
            row.addView(info, infoParams);
            card.addView(row, rowParams);
        }
        container.addView(card);
    }

    private View playerNode(PlayerRow player) {
        LinearLayout node = new LinearLayout(requireContext());
        node.setOrientation(LinearLayout.VERTICAL);
        node.setGravity(Gravity.CENTER);

        FrameLayout avatar = new FrameLayout(requireContext());
        ImageView photo = new ImageView(requireContext());
        photo.setBackgroundResource(R.drawable.bg_player_avatar);
        photo.setClipToOutline(true);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.addView(photo, new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        TextView initials = text(initials(player.name), 12f, R.color.stadium_ink, true, false);
        initials.setGravity(Gravity.CENTER);
        initials.setBackgroundResource(R.drawable.bg_player_avatar);
        avatar.addView(initials, new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        if (!player.photoUrl().isEmpty()) {
            Glide.with(photo).load(player.photoUrl())
                    .placeholder(R.drawable.bg_player_avatar)
                    .error(R.drawable.bg_player_avatar)
                    .centerCrop()
                    .into(photo);
            initials.setVisibility(View.GONE);
        }

        TextView number = text(player.shirt.isEmpty() ? "--" : player.shirt, 10f, R.color.white, true, false);
        number.setGravity(Gravity.CENTER);
        number.setBackgroundResource(R.drawable.bg_player_number);
        FrameLayout.LayoutParams numberParams = new FrameLayout.LayoutParams(dp(21), dp(21), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        avatar.addView(number, numberParams);
        node.addView(avatar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));

        TextView name = text(player.shortName(), 10f, R.color.stadium_ink, true, false);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.setMargins(0, dp(4), 0, 0);
        node.addView(name, nameParams);
        return node;
    }

    private KeyValue parseKeyValue(String raw) {
        String clean = cleanMatchCopy(raw);
        int separator = clean.indexOf(": ");
        if (separator > 0 && separator < 34) {
            return new KeyValue(clean.substring(0, separator), clean.substring(separator + 2));
        }
        return new KeyValue("Thông tin", clean);
    }

    private TimelineEvent parseTimelineEvent(String raw) {
        String clean = cleanMatchCopy(raw);
        String header = clean;
        String body = "";
        int separator = clean.indexOf(": ");
        if (separator > 0) {
            header = clean.substring(0, separator);
            body = clean.substring(separator + 2);
        }

        String minute = "--'";
        String type = header;
        int minuteIndex = header.indexOf("'");
        if (minuteIndex > 0) {
            minute = header.substring(0, minuteIndex + 1).trim();
            if (separator > 0) {
                type = header.substring(minuteIndex + 1).trim();
            } else {
                type = "Goal";
                body = header.substring(minuteIndex + 1).trim();
            }
        } else if (clean.contains("'")) {
            int idx = clean.indexOf("'");
            minute = clean.substring(0, idx + 1).trim();
            type = "Goal";
            body = clean.substring(idx + 1).trim();
        }
        if (body.isEmpty()) body = header;

        String normalized = type.toLowerCase(Locale.ROOT);
        String normalizedBody = body.toLowerCase(Locale.ROOT);
        if (normalized.contains("period")) {
            if (normalizedBody.contains("ft")) {
                return new TimelineEvent(minute, "Kết thúc trận", "Trận đấu khép lại" + scoreSuffix(body), "", R.color.stadium_ink, "FT", R.color.stadium_ink);
            }
            if (normalizedBody.contains("ht")) {
                return new TimelineEvent(minute, "Hết hiệp 1", "Hai đội bước vào giờ nghỉ" + scoreSuffix(body), "", R.color.dark_gray_text, "HT", R.color.dark_gray_text);
            }
            return new TimelineEvent(minute, "Mốc trận đấu", readableBody(body), "", R.color.dark_gray_text, "i", R.color.dark_gray_text);
        }
        if (normalized.contains("goal")) {
            return new TimelineEvent(minute, "Bàn thắng", readableBody(body), "", R.color.stadium_ink, "G", R.color.green_turf);
        }
        if (normalized.contains("substitution")) {
            return new TimelineEvent(minute, "Thay người", readableBody(body), substitutionDetail(body), R.color.green_turf, "↔", R.color.green_turf);
        }
        if (normalized.contains("card")) {
            boolean redCard = normalized.contains("red") || normalizedBody.contains("red") || normalizedBody.contains("đỏ") || normalizedBody.contains("do");
            return new TimelineEvent(minute, redCard ? "Thẻ đỏ" : "Thẻ vàng", readableBody(body), "", redCard ? R.color.red_energy : R.color.orange_spark, redCard ? "R" : "Y", redCard ? R.color.red_energy : R.color.orange_spark);
        }
        if (normalized.contains("penalty")) {
            return new TimelineEvent(minute, "Penalty", readableBody(body), "", R.color.orange_spark, "P", R.color.orange_spark);
        }
        return new TimelineEvent(minute, "Diễn biến", readableBody(body), "", R.color.dark_gray_text, "i", R.color.dark_gray_text);
    }

    private StatRow parseStatRow(String raw) {
        KeyValue row = parseKeyValue(raw);
        String[] values = row.body.split("\\s+-\\s+", 2);
        if (values.length < 2) return null;
        double home = numericValue(values[0]);
        double away = numericValue(values[1]);
        return new StatRow(row.title, values[0].trim(), values[1].trim(), home, away);
    }

    private String scoreSuffix(String body) {
        int start = body == null ? -1 : body.indexOf('(');
        int end = body == null ? -1 : body.indexOf(')', start + 1);
        if (start >= 0 && end > start) return " · Tỷ số " + body.substring(start + 1, end);
        return "";
    }

    private String readableBody(String body) {
        String clean = cleanMatchCopy(body);
        return clean.replace("FT", "Hết trận").replace("HT", "Hết hiệp 1");
    }

    private String substitutionDetail(String body) {
        String clean = cleanMatchCopy(body).toLowerCase(Locale.ROOT);
        if (clean.contains("vào:") || clean.contains("ra:")) return "";
        return "Nguồn dữ liệu chưa có tên cầu thủ vào/ra.";
    }

    private LineupData parseLineupData(List<String> rows) {
        LineupData data = new LineupData();
        for (String raw : rows) {
            KeyValue row = parseKeyValue(raw);
            String title = row.title.toLowerCase(Locale.ROOT);
            if (title.equals("home formation")) {
                data.homeFormation = row.body;
            } else if (title.equals("away formation")) {
                data.awayFormation = row.body;
            } else if (title.equals("home xi")) {
                data.homeXi.add(parsePlayer(row.body));
            } else if (title.equals("away xi")) {
                data.awayXi.add(parsePlayer(row.body));
            } else if (title.equals("home sub")) {
                data.homeSubs.add(parsePlayer(row.body));
            } else if (title.equals("away sub")) {
                data.awaySubs.add(parsePlayer(row.body));
            }
        }
        return data;
    }

    private PlayerRow parsePlayer(String body) {
        String clean = cleanMatchCopy(body);
        if (clean.contains("|name=")) {
            String playerId = metadataValue(clean, "id");
            String shirt = metadataValue(clean, "shirt");
            String name = metadataValue(clean, "name");
            String position = metadataValue(clean, "pos");
            return new PlayerRow(shirt, name, position, playerId);
        }
        String shirt = "";
        if (clean.startsWith("#")) {
            int space = clean.indexOf(' ');
            if (space > 1) {
                shirt = clean.substring(1, space).trim();
                clean = clean.substring(space + 1).trim();
            }
        }
        String position = "";
        int separator = clean.lastIndexOf(" - ");
        if (separator > 0) {
            position = clean.substring(separator + 3).trim();
            clean = clean.substring(0, separator).trim();
        }
        return new PlayerRow(shirt, clean, position, "");
    }

    private String metadataValue(String value, String key) {
        String prefix = key + "=";
        String[] parts = value.split("\\|");
        for (String part : parts) {
            if (part.startsWith(prefix)) return part.substring(prefix.length()).trim();
        }
        return "";
    }

    private void addEmptyState(LinearLayout container, String message) {
        TextView empty = text(message, 14f, R.color.dark_gray_text, false, false);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(12), dp(18), dp(12), dp(18));
        container.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private TextView text(String value, float sizeSp, int colorRes, boolean bold, boolean allCaps) {
        TextView view = new TextView(requireContext());
        view.setText(allCaps ? cleanMatchCopy(value).toUpperCase(Locale.ROOT) : cleanMatchCopy(value));
        view.setTextColor(requireContext().getColor(colorRes));
        view.setTextSize(sizeSp);
        view.setLineSpacing(dp(2), 1f);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable rounded(int fillColor, int radius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private double numericValue(String value) {
        if (value == null) return 0d;
        String clean = value.replace("%", "").replace(",", ".").replaceAll("[^0-9.\\-]", "");
        if (clean.isEmpty() || ".".equals(clean) || "-".equals(clean)) return 0d;
        try {
            return Double.parseDouble(clean);
        } catch (Exception ignored) {
            return 0d;
        }
    }

    private double[][] playerPositions(String formation, int count) {
        List<Integer> rows = formationRows(formation, count);
        double[][] positions = new double[count][2];
        int index = 0;
        int rowCount = rows.size();
        for (int row = 0; row < rowCount && index < count; row++) {
            int playersInRow = Math.max(1, rows.get(row));
            double y = rowCount == 1 ? 0.50 : 0.08 + (0.84 * row / (rowCount - 1));
            for (int col = 0; col < playersInRow && index < count; col++) {
                positions[index][0] = xForColumn(playersInRow, col);
                positions[index][1] = y;
                index++;
            }
        }
        while (index < count) {
            positions[index][0] = xForColumn(count - index, 0);
            positions[index][1] = 0.88;
            index++;
        }
        return positions;
    }

    private List<Integer> formationRows(String formation, int count) {
        List<Integer> rows = new ArrayList<>();
        rows.add(1);
        String clean = formation == null ? "" : formation.replaceAll("[^0-9-]", "");
        String[] parts = clean.split("-");
        int outfield = 0;
        List<Integer> parsed = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            try {
                int value = Integer.parseInt(part);
                if (value > 0) {
                    parsed.add(value);
                    outfield += value;
                }
            } catch (Exception ignored) {
            }
        }
        if (outfield == count - 1 && !parsed.isEmpty()) {
            rows.addAll(parsed);
            return rows;
        }
        if (count <= 7) {
            rows.add(2);
            rows.add(Math.max(1, count - 4));
            rows.add(1);
            return rows;
        }
        rows.add(4);
        rows.add(3);
        rows.add(3);
        return rows;
    }

    private double xForColumn(int playersInRow, int col) {
        if (playersInRow <= 1) return 0.50;
        double left = playersInRow >= 4 ? 0.13 : 0.22;
        double right = playersInRow >= 4 ? 0.87 : 0.78;
        return left + ((right - left) * col / (playersInRow - 1));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String initials(String name) {
        String clean = cleanMatchCopy(name);
        if (clean.isEmpty()) return "?";
        String[] parts = clean.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (builder.length() == 2) break;
        }
        return builder.length() == 0 ? clean.substring(0, 1).toUpperCase(Locale.ROOT) : builder.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class PitchLinesView extends View {
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final float density;

        PitchLinesView(Context context) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(1.8f * density);
            linePaint.setColor(0xB3FFFFFF);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float inset = 16f * density;
            float w = getWidth();
            float h = getHeight();
            float left = inset;
            float top = inset;
            float right = w - inset;
            float bottom = h - inset;
            float midX = w / 2f;
            float midY = h / 2f;

            rect.set(left, top, right, bottom);
            canvas.drawRoundRect(rect, 18f * density, 18f * density, linePaint);
            canvas.drawLine(left, midY, right, midY, linePaint);
            canvas.drawCircle(midX, midY, 44f * density, linePaint);
            canvas.drawCircle(midX, midY, 3.5f * density, linePaint);

            float boxW = Math.min(170f * density, (right - left) * 0.62f);
            float boxH = 70f * density;
            rect.set(midX - boxW / 2f, top, midX + boxW / 2f, top + boxH);
            canvas.drawRect(rect, linePaint);
            rect.set(midX - boxW / 2f, bottom - boxH, midX + boxW / 2f, bottom);
            canvas.drawRect(rect, linePaint);

            float goalW = Math.min(82f * density, (right - left) * 0.32f);
            float goalH = 30f * density;
            rect.set(midX - goalW / 2f, top, midX + goalW / 2f, top + goalH);
            canvas.drawRect(rect, linePaint);
            rect.set(midX - goalW / 2f, bottom - goalH, midX + goalW / 2f, bottom);
            canvas.drawRect(rect, linePaint);
        }
    }

    private static final class KeyValue {
        final String title;
        final String body;

        KeyValue(String title, String body) {
            this.title = title == null || title.isEmpty() ? "Thông tin" : title;
            this.body = body == null || body.isEmpty() ? "--" : body;
        }
    }

    private static final class TimelineEvent {
        final String minute;
        final String title;
        final String body;
        final String detail;
        final int colorRes;
        final String icon;
        final int iconColorRes;

        TimelineEvent(String minute, String title, String body, String detail, int colorRes) {
            this(minute, title, body, detail, colorRes, "i", R.color.dark_gray_text);
        }

        TimelineEvent(String minute, String title, String body, String detail, int colorRes, String icon, int iconColorRes) {
            this.minute = minute == null || minute.isEmpty() ? "--'" : minute;
            this.title = title == null ? "Diễn biến" : title;
            this.body = body == null || body.isEmpty() ? "--" : body;
            this.detail = detail == null ? "" : detail;
            this.colorRes = colorRes;
            this.icon = icon == null || icon.isEmpty() ? "i" : icon;
            this.iconColorRes = iconColorRes;
        }
    }

    private static final class StatRow {
        final String label;
        final String homeText;
        final String awayText;
        final double homeValue;
        final double awayValue;

        StatRow(String label, String homeText, String awayText, double homeValue, double awayValue) {
            this.label = label;
            this.homeText = homeText;
            this.awayText = awayText;
            this.homeValue = homeValue;
            this.awayValue = awayValue;
        }
    }

    private static final class PlayerRow {
        final String shirt;
        final String name;
        final String position;
        final String playerId;

        PlayerRow(String shirt, String name, String position, String playerId) {
            this.shirt = shirt == null ? "" : shirt;
            this.name = name == null || name.isEmpty() ? "Player" : name;
            this.position = position == null ? "" : position;
            this.playerId = playerId == null ? "" : playerId;
        }

        String shortName() {
            String[] parts = name.split("\\s+");
            if (parts.length <= 2) return name;
            return parts[parts.length - 2] + " " + parts[parts.length - 1];
        }

        String displayName() {
            String prefix = shirt.isEmpty() ? "" : "#" + shirt + " ";
            String suffix = position.isEmpty() ? "" : " · " + position;
            return prefix + name + suffix;
        }

        String photoUrl() {
            return playerId.isEmpty() ? "" : "https://api.sofascore.app/api/v1/player/" + playerId + "/image";
        }
    }

    private static final class RatedPlayerUi {
        final String side;
        final String name;
        final String rating;
        final String playerId;

        RatedPlayerUi(String side, String name, String rating, String playerId) {
            this.side = side == null || side.isEmpty() ? "Home" : side;
            this.name = name == null || name.isEmpty() ? "Player" : name;
            this.rating = rating == null || rating.isEmpty() ? "--" : rating;
            this.playerId = playerId == null ? "" : playerId;
        }

        String photoUrl() {
            return playerId.isEmpty() ? "" : "https://api.sofascore.app/api/v1/player/" + playerId + "/image";
        }
    }

    private static final class LineupData {
        String homeFormation = "";
        String awayFormation = "";
        final List<PlayerRow> homeXi = new ArrayList<>();
        final List<PlayerRow> awayXi = new ArrayList<>();
        final List<PlayerRow> homeSubs = new ArrayList<>();
        final List<PlayerRow> awaySubs = new ArrayList<>();
    }

    private String cleanMatchCopy(String value) {
        if (value == null) return "";
        return value
                .replace("999' period: PEN", "Penalty shootout")
                .replace("period: PEN", "Penalty shootout")
                .replace("penaltyShootout", "Penalty")
                .replace("Status: AP", "Status: After penalties")
                .replace("Status: FT", "Status: Full time")
                .trim();
    }

    private void loadLogo(ImageView imageView, String url) {
        Glide.with(imageView).load(url == null || url.isEmpty() ? null : url)
                .placeholder(R.drawable.ic_logo).error(R.drawable.ic_logo).fitCenter().into(imageView);
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName()).commit();
    }
}
