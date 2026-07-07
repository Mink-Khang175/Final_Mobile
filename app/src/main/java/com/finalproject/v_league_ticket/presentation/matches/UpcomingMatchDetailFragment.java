package com.finalproject.v_league_ticket.presentation.matches;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.FragmentUpcomingMatchDetailBinding;
import com.finalproject.v_league_ticket.domain.model.Fixture;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.booking.TicketBookingFragment;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.finalproject.v_league_ticket.presentation.news.NewsFragment;
import com.finalproject.v_league_ticket.presentation.profile.HeaderNotificationRouter;
import com.finalproject.v_league_ticket.presentation.profile.ProfileFragment;
import com.finalproject.v_league_ticket.presentation.shop.ShopFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UpcomingMatchDetailFragment extends Fragment {
    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_HOME = "home";
    private static final String ARG_AWAY = "away";
    private static final String ARG_HOME_LOGO = "home_logo";
    private static final String ARG_AWAY_LOGO = "away_logo";
    private static final String ARG_VENUE = "venue";
    private static final String ARG_ROUND = "round";
    private static final String ARG_ROUND_NAME = "round_name";
    private static final String ARG_TIMESTAMP = "timestamp";
    private FragmentUpcomingMatchDetailBinding binding;

    public UpcomingMatchDetailFragment() {
        super(R.layout.fragment_upcoming_match_detail);
    }

    public static UpcomingMatchDetailFragment newInstance(Fixture fixture) {
        UpcomingMatchDetailFragment fragment = new UpcomingMatchDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, fixture.getId());
        args.putString(ARG_HOME, fixture.getHomeTeamName());
        args.putString(ARG_AWAY, fixture.getAwayTeamName());
        args.putString(ARG_HOME_LOGO, fixture.getHomeLogoUrl());
        args.putString(ARG_AWAY_LOGO, fixture.getAwayLogoUrl());
        args.putString(ARG_VENUE, fixture.getVenue());
        if (fixture.getRound() != null) args.putInt(ARG_ROUND, fixture.getRound());
        args.putString(ARG_ROUND_NAME, fixture.getRoundName());
        if (fixture.getStartTimestamp() != null) args.putLong(ARG_TIMESTAMP, fixture.getStartTimestamp());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentUpcomingMatchDetailBinding.bind(view);
        binding.appHeader.tvHeaderSubtitle.setText("Trước trận");
        bindMatch();
        setupClicks();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void bindMatch() {
        Bundle args = requireArguments();
        String home = args.getString(ARG_HOME, "Chủ nhà");
        String away = args.getString(ARG_AWAY, "Đội khách");
        String venue = args.getString(ARG_VENUE, "Sân vận động V.League");
        String roundName = args.getString(ARG_ROUND_NAME, "");
        if (roundName == null || roundName.isEmpty()) {
            roundName = args.containsKey(ARG_ROUND) ? "Vòng " + args.getInt(ARG_ROUND) : "V.League 25/26";
        }
        long timestamp = args.getLong(ARG_TIMESTAMP, 0L);

        binding.tvRound.setText(roundName);
        binding.tvHomeName.setText(home);
        binding.tvAwayName.setText(away);
        binding.tvKickoffTime.setText(formatTime(timestamp));
        binding.tvKickoffDate.setText(formatDate(timestamp));
        binding.tvVenue.setText(venue == null || venue.trim().isEmpty() ? "Sân vận động đang cập nhật" : venue);
        binding.tvPreview.setText(home + " và " + away
                + " sẽ gặp nhau tại " + binding.tvVenue.getText()
                + ". Trận đấu thuộc mùa 25/26 và đang mở bán vé trên ứng dụng.");
        binding.tvStadiumInfo.setText(binding.tvVenue.getText()
                + "\nKhu A: VIP | Khu B/C/D: vé thường"
                + "\nNên đến sân trước 30 phút để soát vé điện tử.");
        loadLogo(binding.imgHomeLogo, args.getString(ARG_HOME_LOGO, ""));
        loadLogo(binding.imgAwayLogo, args.getString(ARG_AWAY_LOGO, ""));
    }

    private void setupClicks() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnBuyTicket.setOnClickListener(v -> navigateTo(bookingFragmentFromArgs()));
        binding.bottomNav.navHome.setOnClickListener(v -> navigateTo(new HomepageFragment()));
        binding.bottomNav.navMatches.setOnClickListener(v -> navigateTo(new FixturesFragment()));
        binding.bottomNav.navShop.setOnClickListener(v -> navigateTo(new ShopFragment()));
        binding.bottomNav.navNews.setOnClickListener(v -> navigateTo(new NewsFragment()));
        binding.bottomNav.navProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        binding.appHeader.btnHeaderProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        HeaderNotificationRouter.bind(this, binding.appHeader);
    }

    private TicketBookingFragment bookingFragmentFromArgs() {
        Bundle args = requireArguments();
        return TicketBookingFragment.newInstance(
                args.getLong(ARG_EVENT_ID, 0L),
                args.getInt(ARG_ROUND, 0),
                args.getString(ARG_HOME, ""),
                args.getString(ARG_AWAY, ""),
                args.getLong(ARG_TIMESTAMP, 0L)
        );
    }

    private Fragment profileOrAuth() {
        return AuthSession.hasToken(requireContext()) ? new ProfileFragment() : new AuthLoginFragment();
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0L) return "--/--/----";
        return new SimpleDateFormat("EEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
                .format(new Date(timestamp * 1000L));
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0L) return "--:--";
        return new SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN"))
                .format(new Date(timestamp * 1000L));
    }

    private void loadLogo(ImageView imageView, String url) {
        Glide.with(imageView)
                .load(url == null || url.isEmpty() ? null : url)
                .placeholder(R.drawable.ic_logo)
                .error(R.drawable.ic_logo)
                .centerInside()
                .into(imageView);
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

