package com.finalproject.v_league_ticket.presentation.booking;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.data.remote.VLeagueApiClient;
import com.finalproject.v_league_ticket.databinding.FragmentTicketBookingBinding;
import com.finalproject.v_league_ticket.domain.model.Fixture;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.profile.HeaderNotificationRouter;
import com.finalproject.v_league_ticket.presentation.profile.ProfileFragment;
import com.finalproject.v_league_ticket.presentation.profile.UserCheckoutInfo;
import com.finalproject.v_league_ticket.presentation.profile.UserEngagementManager;
import com.finalproject.v_league_ticket.presentation.shop.OrderSuccessFragment;
import com.finalproject.v_league_ticket.presentation.shop.ShopFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TicketBookingFragment extends Fragment {
    private static final String ARG_EXTERNAL_ID = "external_id";
    private static final String ARG_ROUND = "round";
    private static final String ARG_HOME = "home";
    private static final String ARG_AWAY = "away";
    private static final String ARG_TIMESTAMP = "timestamp";
    private static final int SERVICE_FEE = 10000;
    private static final int MAX_SEATS = 8;
    private static final long LOCK_DURATION_MS = 10 * 60 * 1000L;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NumberFormat vndFormatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final List<BookingVenue> venues = new ArrayList<>();
    private final List<BookingMatch> apiMatches = new ArrayList<>();
    private final List<TicketSeat> seats = new ArrayList<>();
    private final Set<String> selectedSeatIds = new LinkedHashSet<>();
    private FragmentTicketBookingBinding binding;
    private BookingVenue selectedVenue;
    private String selectedCity = "";
    private BookingMatch selectedMatch;
    private long requestedExternalId;
    private int requestedRound;
    private int selectedRound;
    private boolean requestedSelectionApplied;
    private ListenerRegistration seatRegistration;
    private boolean checkoutComplete;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateSummary();
            timerHandler.postDelayed(this, 1000L);
        }
    };

    public TicketBookingFragment() {
        super(R.layout.fragment_ticket_booking);
    }

    public static TicketBookingFragment newInstance(Fixture fixture) {
        TicketBookingFragment fragment = new TicketBookingFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EXTERNAL_ID, fixture.getId());
        if (fixture.getRound() != null) args.putInt(ARG_ROUND, fixture.getRound());
        args.putString(ARG_HOME, fixture.getHomeTeamName());
        args.putString(ARG_AWAY, fixture.getAwayTeamName());
        if (fixture.getStartTimestamp() != null) args.putLong(ARG_TIMESTAMP, fixture.getStartTimestamp());
        fragment.setArguments(args);
        return fragment;
    }

    public static TicketBookingFragment newInstance(long externalId, int round, String home, String away, long timestamp) {
        TicketBookingFragment fragment = new TicketBookingFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EXTERNAL_ID, externalId);
        args.putInt(ARG_ROUND, round);
        args.putString(ARG_HOME, home);
        args.putString(ARG_AWAY, away);
        args.putLong(ARG_TIMESTAMP, timestamp);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentTicketBookingBinding.bind(view);
        readRequestedMatch();
        binding.appHeader.tvHeaderSubtitle.setText("Đặt vé");
        setupActions();
        prefillCustomer();
        loadVenues();
        loadMatches();
        timerHandler.post(timerRunnable);
    }

    private void readRequestedMatch() {
        Bundle args = getArguments();
        if (args == null) return;
        requestedExternalId = args.getLong(ARG_EXTERNAL_ID, 0L);
        requestedRound = args.getInt(ARG_ROUND, 0);
        selectedRound = requestedRound;
    }

    @Override
    public void onDestroyView() {
        if (!checkoutComplete) releaseSelectedSeats(new ArrayList<>(selectedSeatIds));
        if (seatRegistration != null) seatRegistration.remove();
        timerHandler.removeCallbacks(timerRunnable);
        super.onDestroyView();
        binding = null;
    }

    private void setupActions() {
        binding.btnBackToShop.setOnClickListener(v -> navigateTo(new ShopFragment()));
        binding.appHeader.btnHeaderProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        HeaderNotificationRouter.bind(this, binding.appHeader);
        binding.seatMapView.setOnSeatClickListener(this::handleSeatTap);
        binding.seekSeatZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int percent = 80 + progress;
                binding.seatMapView.setZoomFactor(percent / 100f);
                binding.tvSeatZoomValue.setText(percent + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        binding.rgTicketPayment.setOnCheckedChangeListener((group, checkedId) ->
                binding.imgTicketQr.setImageResource(checkedId == R.id.rbTicketMomo
                        ? R.drawable.qr_momo : R.drawable.qr_vietinbank));
        binding.btnConfirmTicketOrder.setOnClickListener(v -> placeTicketOrder());
    }

    private void prefillCustomer() {
        String name = AuthSession.userName(requireContext());
        String email = AuthSession.email(requireContext());
        if (name != null) binding.edtTicketName.setText(name);
        if (email != null) binding.edtTicketEmail.setText(email);
        UserCheckoutInfo.load(requireContext(), info -> {
            if (binding == null) return;
            if (!info.name.isEmpty()) binding.edtTicketName.setText(info.name);
            if (!info.email.isEmpty()) binding.edtTicketEmail.setText(info.email);
            if (!info.phone.isEmpty()) binding.edtTicketPhone.setText(info.phone);
            if (info.prefersMomo()) {
                binding.rbTicketMomo.setChecked(true);
                binding.imgTicketQr.setImageResource(R.drawable.qr_momo);
            } else if ("vietinbank".equalsIgnoreCase(info.paymentProvider) || info.prefersBankTransfer()) {
                binding.rbTicketVietinBank.setChecked(true);
                binding.imgTicketQr.setImageResource(R.drawable.qr_vietinbank);
            }
        });
    }

    private void loadVenues() {
        venues.clear();
        try (InputStream stream = requireContext().getAssets().open("clubStadiums.json");
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                venues.add(new BookingVenue(
                        intValue(object, "id"),
                        string(object, "clubCode"),
                        string(object, "clubName"),
                        string(object, "stadiumName"),
                        string(object, "city"),
                        string(object, "address")
                ));
            }
        } catch (Exception ignored) {
            seedFallbackVenues();
        }
        if (venues.isEmpty()) seedFallbackVenues();
        renderCityChips();
    }

    private void seedFallbackVenues() {
        venues.add(new BookingVenue(1, "Hanoi", "Hà Nội FC", "Sân vận động Hàng Đẫy", "Hà Nội",
                "Trịnh Hoài Đức, Cát Linh, Đống Đa, Hà Nội"));
        venues.add(new BookingVenue(2, "CAHCMC", "Công An TP.HCM", "Sân vận động Thống Nhất", "TP. Hồ Chí Minh",
                "138 Đào Duy Từ, Quận 10, TP. HCM"));
        venues.add(new BookingVenue(3, "Nam Dinh", "Thép Xanh Nam Định", "Sân vận động Thiên Trường", "Nam Định",
                "Đặng Xuân Bảng, Vị Hoàng, TP. Nam Định"));
        venues.add(new BookingVenue(4, "Hai Phong", "Hải Phòng FC", "Sân vận động Lạch Tray", "Hải Phòng",
                "Đường Lạch Tray, Ngô Quyền, Hải Phòng"));
    }

    private void loadMatches() {
        binding.progressMatches.setVisibility(View.VISIBLE);
        VLeagueApiClient.getInstance().fetchUpcoming(new VLeagueApiClient.DataCallback<List<Fixture>>() {
            @Override
            public void onSuccess(List<Fixture> data) {
                if (binding == null) return;
                binding.progressMatches.setVisibility(View.GONE);
                apiMatches.clear();
                if (data != null) {
                    for (Fixture fixture : data) {
                        BookingMatch match = toBookingMatch(fixture);
                        if (match != null) apiMatches.add(match);
                    }
                }
                applyRequestedSelection();
                renderCityChips();
                renderMatches();
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding == null) return;
                binding.progressMatches.setVisibility(View.GONE);
                apiMatches.clear();
                renderMatches();
            }
        });
    }

    private BookingMatch toBookingMatch(Fixture fixture) {
        BookingVenue venue = matchClubByHomeTeam(fixture.getHomeTeamName());
        if (venue == null) return null;
        long timestamp = fixture.getStartTimestamp() == null ? 0L : fixture.getStartTimestamp();
        return new BookingMatch(
                "sofa-" + fixture.getId(),
                fixture.getId(),
                venue.getCity(),
                venue.getStadiumName(),
                venue.getAddress(),
                formatDate(timestamp),
                formatTime(timestamp),
                fixture.getHomeTeamName().isEmpty() ? venue.getClubName() : fixture.getHomeTeamName(),
                fixture.getAwayTeamName().isEmpty() ? "Đội khách" : fixture.getAwayTeamName(),
                "V.League 1",
                180000,
                timestamp,
                fixture.getRound() == null ? 0 : fixture.getRound()
        );
    }

    private void applyRequestedSelection() {
        if (requestedSelectionApplied || requestedExternalId <= 0L) return;
        for (BookingMatch match : apiMatches) {
            if (match.getExternalId() != requestedExternalId) continue;
            selectedCity = match.getCity();
            selectedVenue = venueForMatch(match);
            selectedRound = match.getRound();
            selectedMatch = match;
            requestedSelectionApplied = true;
            return;
        }
        if (requestedRound > 0) selectedRound = requestedRound;
    }

    private BookingVenue venueForMatch(BookingMatch match) {
        for (BookingVenue venue : venues) {
            if (sameVenue(match, venue)) return venue;
        }
        return selectedVenue;
    }

    private BookingVenue matchClubByHomeTeam(String homeTeamName) {
        String home = normalize(homeTeamName);
        if (home.isEmpty()) return null;
        for (BookingVenue venue : venues) {
            String club = normalize(venue.getClubName());
            String code = normalize(venue.getClubCode());
            if ((!club.isEmpty() && (home.contains(club) || club.contains(home)))
                    || (!code.isEmpty() && (home.contains(code) || code.contains(home)))) {
                return venue;
            }
        }
        return null;
    }

    private void renderCityChips() {
        if (binding == null) return;
        binding.layoutCityChips.removeAllViews();
        LinkedHashSet<String> cities = new LinkedHashSet<>();
        for (BookingVenue venue : venues) cities.add(venue.getCity());
        if (selectedCity.isEmpty() && !cities.isEmpty()) selectedCity = cities.iterator().next();
        for (String city : cities) {
            TextView chip = chip(city, city.equals(selectedCity));
            chip.setOnClickListener(v -> selectCity(city));
            binding.layoutCityChips.addView(chip);
        }
        if (selectedVenue == null) {
            for (BookingVenue venue : venues) {
                if (venue.getCity().equals(selectedCity)) {
                    selectedVenue = venue;
                    break;
                }
            }
        }
        renderVenues();
    }

    private void selectCity(String city) {
        releaseSelectedSeats(new ArrayList<>(selectedSeatIds));
        selectedCity = city;
        selectedVenue = null;
        selectedMatch = null;
        selectedRound = 0;
        clearSeatState();
        renderCityChips();
        renderMatches();
    }

    private void renderVenues() {
        binding.layoutVenueChips.removeAllViews();
        LinkedHashMap<String, BookingVenue> byStadium = new LinkedHashMap<>();
        for (BookingVenue venue : venues) {
            if (venue.getCity().equals(selectedCity)) byStadium.put(venue.key(), venue);
        }
        if (selectedVenue == null && !byStadium.isEmpty()) selectedVenue = byStadium.values().iterator().next();
        for (BookingVenue venue : byStadium.values()) {
            TextView chip = fullWidthChip(venue.getStadiumName(), selectedVenue != null && venue.key().equals(selectedVenue.key()));
            chip.setOnClickListener(v -> selectVenue(venue));
            binding.layoutVenueChips.addView(chip);
        }
        binding.tvVenueAddress.setText(selectedVenue == null
                ? "Chọn sân vận động để xem lịch thi đấu."
                : selectedVenue.getAddress());
    }

    private void selectVenue(BookingVenue venue) {
        releaseSelectedSeats(new ArrayList<>(selectedSeatIds));
        selectedVenue = venue;
        selectedMatch = null;
        selectedRound = 0;
        clearSeatState();
        renderVenues();
        renderMatches();
    }

    private void renderMatches() {
        if (binding == null || selectedVenue == null) return;
        binding.layoutMatchCards.removeAllViews();
        List<BookingMatch> allMatches = matchesForVenue();
        ensureSelectedRound(allMatches);
        renderRoundChips(allMatches);
        List<BookingMatch> matches = matchesForSelectedRound(allMatches);
        binding.tvMatchEmpty.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvMatchEmpty.setText(matches.isEmpty()
                ? "Chưa có trận đấu phù hợp với vòng này."
                : "");
        if (!matches.isEmpty() && (selectedMatch == null || !containsMatch(matches, selectedMatch))) {
            selectedMatch = matches.get(0);
            prepareSeats();
        }
        for (BookingMatch match : matches) {
            View card = matchCard(match, selectedMatch != null && selectedMatch.getId().equals(match.getId()));
            binding.layoutMatchCards.addView(card);
        }
        if (selectedMatch != null && seats.isEmpty()) {
            prepareSeats();
        }
    }

    private void ensureSelectedRound(List<BookingMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            selectedRound = 0;
            return;
        }
        if (selectedRound > 0) {
            for (BookingMatch match : matches) {
                if (match.getRound() == selectedRound) return;
            }
        }
        selectedRound = matches.get(0).getRound();
    }

    private void renderRoundChips(List<BookingMatch> matches) {
        binding.layoutRoundChips.removeAllViews();
        LinkedHashSet<Integer> rounds = new LinkedHashSet<>();
        for (BookingMatch match : matches) {
            if (match.getRound() > 0) rounds.add(match.getRound());
        }
        if (rounds.isEmpty()) {
            binding.layoutRoundChips.setVisibility(View.GONE);
            return;
        }
        binding.layoutRoundChips.setVisibility(View.VISIBLE);
        for (Integer round : rounds) {
            TextView chip = chip("Vòng " + round, round == selectedRound);
            chip.setOnClickListener(v -> {
                releaseSelectedSeats(new ArrayList<>(selectedSeatIds));
                selectedRound = round;
                selectedMatch = null;
                clearSeatState();
                renderMatches();
            });
            binding.layoutRoundChips.addView(chip);
        }
    }

    private List<BookingMatch> matchesForSelectedRound(List<BookingMatch> matches) {
        if (selectedRound <= 0) return matches;
        List<BookingMatch> filtered = new ArrayList<>();
        for (BookingMatch match : matches) {
            if (match.getRound() == selectedRound) filtered.add(match);
        }
        return filtered;
    }

    private List<BookingMatch> matchesForVenue() {
        if (selectedVenue == null) return Collections.emptyList();
        List<BookingMatch> filtered = new ArrayList<>();
        for (BookingMatch match : apiMatches) {
            if (sameVenue(match, selectedVenue)) filtered.add(match);
        }
        if (!filtered.isEmpty()) return filtered;
        return fallbackMatchesForVenue(selectedVenue);
    }

    private boolean containsMatch(List<BookingMatch> matches, BookingMatch selected) {
        if (selected == null) return false;
        for (BookingMatch match : matches) {
            if (match.getId().equals(selected.getId())) return true;
        }
        return false;
    }

    private boolean sameVenue(BookingMatch match, BookingVenue venue) {
        return normalize(match.getCity()).equals(normalize(venue.getCity()))
                && normalize(match.getStadium()).equals(normalize(venue.getStadiumName()));
    }

    private List<BookingMatch> fallbackMatchesForVenue(BookingVenue venue) {
        List<BookingMatch> fallback = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.AUGUST, 8, 18, 0, 0);
        String[] opponents = {"Hà Nội FC", "Thép Xanh Nam Định", "Hải Phòng FC"};
        for (int i = 0; i < opponents.length; i++) {
            Calendar item = (Calendar) calendar.clone();
            item.add(Calendar.DATE, i * 7);
            item.add(Calendar.MINUTE, i * 30);
            fallback.add(new BookingMatch(
                    "demo-" + venue.getId() + "-" + (i + 1),
                    venue.getId() * 1000L + i,
                    venue.getCity(),
                    venue.getStadiumName(),
                    venue.getAddress(),
                    new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(item.getTime()),
                    new SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")).format(item.getTime()),
                    venue.getClubName(),
                    opponents[i],
                    "V.League 1",
                    180000 + i * 20000,
                    item.getTimeInMillis() / 1000L,
                    i + 1
            ));
        }
        return fallback;
    }

    private View matchCard(BookingMatch match, boolean selected) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackgroundResource(selected ? R.drawable.bg_product_size_selected : R.drawable.bg_shop_action_simple);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dp(230), LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dp(10), 0);
        card.setLayoutParams(params);

        TextView title = new TextView(requireContext());
        title.setText(match.title());
        title.setTextColor(getColor(selected ? R.color.white : R.color.dark_icon));
        title.setTextSize(14f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(title);

        TextView meta = new TextView(requireContext());
        meta.setText(match.getDate() + " • " + match.getTime()
                + "\n" + match.getStadium()
                + "\nTừ " + formatVnd(match.getBasePrice()));
        meta.setTextColor(getColor(selected ? R.color.white_alpha_80 : R.color.dark_gray_text));
        meta.setTextSize(12f);
        meta.setMaxLines(3);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        meta.setPadding(0, dp(5), 0, 0);
        card.addView(meta);

        card.setOnClickListener(v -> selectMatch(match));
        return card;
    }

    private void selectMatch(BookingMatch match) {
        releaseSelectedSeats(new ArrayList<>(selectedSeatIds));
        selectedMatch = match;
        renderMatches();
        prepareSeats();
    }

    private void prepareSeats() {
        clearSeatState();
        if (selectedMatch == null) return;
        seats.addAll(buildSeatLayout(selectedMatch.getBasePrice()));
        binding.seatMapView.setCurrentUid(AuthSession.uid(requireContext()));
        binding.seatMapView.setSeats(seats);
        binding.tvSelectedMatch.setText(selectedMatch.title() + "\n"
                + selectedMatch.getDate() + " - " + selectedMatch.getTime() + " | " + selectedMatch.getStadium()
                + "\nChạm vào ô ghế trên sơ đồ để giữ chỗ. Có thể chọn tối đa " + MAX_SEATS + " ghế.");
        saveMatchMetadata();
        listenToSeats();
        updateSummary();
    }

    private List<TicketSeat> buildSeatLayout(int basePrice) {
        List<TicketSeat> output = new ArrayList<>();
        appendStand(output, "A", "VIP", 14, 8, basePrice + 140000);
        appendStand(output, "B", "REGULAR", 14, 8, basePrice);
        appendStand(output, "C", "REGULAR", 4, 18, basePrice);
        appendStand(output, "D", "REGULAR", 4, 18, basePrice);
        return output;
    }

    private void appendStand(List<TicketSeat> output, String stand, String type, int rows, int cols, int price) {
        for (int row = 1; row <= rows; row++) {
            for (int col = 1; col <= cols; col++) {
                int index = (row - 1) * cols + col;
                String number = stand + String.format(Locale.US, "%02d", index);
                output.add(new TicketSeat(number, stand, number, type, row, col, price));
            }
        }
    }

    private void clearSeatState() {
        if (seatRegistration != null) {
            seatRegistration.remove();
            seatRegistration = null;
        }
        selectedSeatIds.clear();
        seats.clear();
        if (binding != null) {
            binding.seatMapView.setSeats(seats);
            binding.seatMapView.setSelectedIds(selectedSeatIds);
            binding.tvSelectedMatch.setText("Chọn sân và trận đấu ở bước 2-3. Sau đó chạm vào ô ghế trên sơ đồ để giữ chỗ.");
            updateSummary();
        }
    }

    private void saveMatchMetadata() {
        if (selectedMatch == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("matchId", selectedMatch.getId());
        data.put("externalId", selectedMatch.getExternalId());
        data.put("home", selectedMatch.getHome());
        data.put("away", selectedMatch.getAway());
        data.put("city", selectedMatch.getCity());
        data.put("stadium", selectedMatch.getStadium());
        data.put("address", selectedMatch.getAddress());
        data.put("date", selectedMatch.getDate());
        data.put("time", selectedMatch.getTime());
        data.put("league", selectedMatch.getLeague());
        data.put("basePrice", selectedMatch.getBasePrice());
        data.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("ticket_matches").document(selectedMatch.getId()).set(data, SetOptions.merge());
    }

    private void listenToSeats() {
        if (selectedMatch == null) return;
        if (seatRegistration != null) seatRegistration.remove();
        seatRegistration = seatCollection().addSnapshotListener((snapshot, error) -> {
            if (binding == null) return;
            if (error != null || snapshot == null) {
                toast("Không đồng bộ được trạng thái ghế.");
                return;
            }
            long now = System.currentTimeMillis();
            for (TicketSeat seat : seats) seat.markAvailable();
            selectedSeatIds.clear();
            Map<String, TicketSeat> byId = new HashMap<>();
            for (TicketSeat seat : seats) byId.put(seat.getId(), seat);
            String uid = AuthSession.uid(requireContext());
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                TicketSeat seat = byId.get(doc.getId());
                if (seat == null) continue;
                String status = doc.getString("status");
                String owner = doc.getString("ownerId");
                long lockUntil = longValue(doc, "lockUntilMs");
                if (TicketSeat.STATUS_LOCKED.equals(status) && lockUntil <= now) {
                    seat.markAvailable();
                    continue;
                }
                seat.applyRemoteState(status, owner, lockUntil);
                if (seat.isLockedBy(uid, now)) selectedSeatIds.add(seat.getId());
            }
            binding.seatMapView.setSelectedIds(selectedSeatIds);
            binding.seatMapView.invalidate();
            updateSummary();
        });
    }

    private CollectionReference seatCollection() {
        return db.collection("ticket_matches").document(selectedMatch.getId()).collection("seats");
    }

    private void handleSeatTap(TicketSeat seat) {
        if (selectedMatch == null) {
            toast("Hãy chọn trận đấu trước.");
            return;
        }
        if (!AuthSession.hasToken(requireContext())) {
            toast("Vui lòng đăng nhập để giữ ghế.");
            navigateTo(new AuthLoginFragment());
            return;
        }
        long now = System.currentTimeMillis();
        String uid = AuthSession.uid(requireContext());
        if (selectedSeatIds.contains(seat.getId()) || seat.isLockedBy(uid, now)) {
            releaseSeat(seat.getId());
            return;
        }
        if (seat.isSold()) {
            toast("Ghế này đã bán.");
            return;
        }
        if (seat.isLockedByOther(uid, now)) {
            toast("Ghế này đang được người khác giữ.");
            return;
        }
        if (selectedSeatIds.size() >= MAX_SEATS) {
            toast("Chỉ được chọn tối đa " + MAX_SEATS + " vé mỗi giao dịch.");
            return;
        }
        lockSeat(seat);
    }

    private void lockSeat(TicketSeat seat) {
        String uid = AuthSession.uid(requireContext());
        long now = System.currentTimeMillis();
        long lockUntil = now + LOCK_DURATION_MS;
        DocumentReference ref = seatCollection().document(seat.getId());
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(ref);
            String status = snap.getString("status");
            String owner = snap.getString("ownerId");
            long remoteLockUntil = longValue(snap, "lockUntilMs");
            if (TicketSeat.STATUS_SOLD.equals(status)) {
                throw new FirebaseFirestoreException("Ghế đã bán.", FirebaseFirestoreException.Code.ABORTED);
            }
            if (TicketSeat.STATUS_LOCKED.equals(status)
                    && remoteLockUntil > now
                    && owner != null
                    && !owner.equals(uid)) {
                throw new FirebaseFirestoreException("Ghế đang được giữ.", FirebaseFirestoreException.Code.ABORTED);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("seatId", seat.getId());
            data.put("stand", seat.getStand());
            data.put("seatNumber", seat.getSeatNumber());
            data.put("type", seat.getType());
            data.put("price", seat.getPrice());
            data.put("status", TicketSeat.STATUS_LOCKED);
            data.put("ownerId", uid);
            data.put("lockUntilMs", lockUntil);
            data.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(ref, data, SetOptions.merge());
            return null;
        }).addOnSuccessListener(result -> {
            selectedSeatIds.add(seat.getId());
            seat.applyRemoteState(TicketSeat.STATUS_LOCKED, uid, lockUntil);
            if (binding != null) {
                binding.seatMapView.setSelectedIds(selectedSeatIds);
                updateSummary();
            }
        }).addOnFailureListener(error -> toast(error.getMessage() == null ? "Không thể giữ ghế." : error.getMessage()));
    }

    private void releaseSeat(String seatId) {
        releaseSelectedSeats(Collections.singletonList(seatId));
        selectedSeatIds.remove(seatId);
        for (TicketSeat seat : seats) {
            if (seat.getId().equals(seatId)) seat.markAvailable();
        }
        if (binding != null) {
            binding.seatMapView.setSelectedIds(selectedSeatIds);
            updateSummary();
        }
    }

    private void releaseSelectedSeats(List<String> seatIds) {
        if (selectedMatch == null || seatIds == null || seatIds.isEmpty()) return;
        String uid = AuthSession.uid(requireContext());
        for (String seatId : seatIds) {
            DocumentReference ref = seatCollection().document(seatId);
            db.runTransaction(transaction -> {
                DocumentSnapshot snap = transaction.get(ref);
                String status = snap.getString("status");
                String owner = snap.getString("ownerId");
                if (TicketSeat.STATUS_LOCKED.equals(status) && uid.equals(owner)) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", TicketSeat.STATUS_AVAILABLE);
                    data.put("ownerId", "");
                    data.put("lockUntilMs", 0L);
                    data.put("updatedAt", FieldValue.serverTimestamp());
                    transaction.set(ref, data, SetOptions.merge());
                }
                return null;
            });
        }
    }

    private void placeTicketOrder() {
        if (!AuthSession.hasToken(requireContext())) {
            toast("Vui lòng đăng nhập trước khi đặt vé.");
            navigateTo(new AuthLoginFragment());
            return;
        }
        if (selectedMatch == null) {
            toast("Vui lòng chọn trận đấu.");
            return;
        }
        List<TicketSeat> selected = selectedSeats();
        if (selected.isEmpty()) {
            toast("Vui lòng chọn ít nhất một ghế.");
            return;
        }
        if (text(binding.edtTicketName.getText()).isEmpty()
                || text(binding.edtTicketPhone.getText()).isEmpty()
                || text(binding.edtTicketEmail.getText()).isEmpty()) {
            toast("Vui lòng nhập đầy đủ thông tin nhận vé.");
            return;
        }
        if (!binding.cbTicketTerms.isChecked()) {
            toast("Vui lòng đồng ý quy định đặt vé.");
            return;
        }
        String orderId = "TICKET" + System.currentTimeMillis();
        String uid = AuthSession.uid(requireContext());
        String provider = binding.rbTicketMomo.isChecked() ? "momo" : "vietinbank";
        int total = totalPrice(selected);
        List<DocumentReference> seatRefs = new ArrayList<>();
        for (TicketSeat seat : selected) seatRefs.add(seatCollection().document(seat.getId()));
        Map<String, Object> orderData = ticketOrderData(orderId, provider, selected, total);
        binding.btnConfirmTicketOrder.setEnabled(false);
        binding.btnConfirmTicketOrder.setText("Đang đặt vé...");
        db.runTransaction(transaction -> {
            long now = System.currentTimeMillis();
            List<DocumentSnapshot> snapshots = new ArrayList<>();
            for (DocumentReference seatRef : seatRefs) snapshots.add(transaction.get(seatRef));
            for (int i = 0; i < selected.size(); i++) {
                TicketSeat seat = selected.get(i);
                DocumentReference seatRef = seatRefs.get(i);
                DocumentSnapshot snap = snapshots.get(i);
                String status = snap.getString("status");
                String owner = snap.getString("ownerId");
                long lockUntil = longValue(snap, "lockUntilMs");
                if (TicketSeat.STATUS_SOLD.equals(status)) {
                    throw new FirebaseFirestoreException("Có ghế đã được bán.", FirebaseFirestoreException.Code.ABORTED);
                }
                if (!TicketSeat.STATUS_LOCKED.equals(status) || !uid.equals(owner) || lockUntil <= now) {
                    throw new FirebaseFirestoreException("Thời gian giữ ghế đã hết. Hãy chọn lại ghế.", FirebaseFirestoreException.Code.ABORTED);
                }
                Map<String, Object> seatData = new HashMap<>();
                seatData.put("status", TicketSeat.STATUS_SOLD);
                seatData.put("ownerId", uid);
                seatData.put("orderId", orderId);
                seatData.put("lockUntilMs", 0L);
                seatData.put("soldAt", FieldValue.serverTimestamp());
                seatData.put("updatedAt", FieldValue.serverTimestamp());
                transaction.set(seatRef, seatData, SetOptions.merge());
            }
            transaction.set(db.collection("ticket_orders").document(orderId), orderData);
            transaction.set(db.collection("orders").document(orderId), orderData);
            transaction.set(db.collection("bookings").document(orderId), orderData);
            return null;
        }).addOnCompleteListener(task -> {
            if (binding == null) return;
            binding.btnConfirmTicketOrder.setEnabled(true);
            binding.btnConfirmTicketOrder.setText("Đặt vé");
            if (!task.isSuccessful()) {
                toast(task.getException() == null ? "Đặt vé thất bại." : task.getException().getMessage());
                return;
            }
            rememberTicketCheckoutInfo(provider);
            UserEngagementManager.awardTicketPurchase(uid, orderId);
            UserEngagementManager.notifyUser(uid,
                    "Đặt vé thành công",
                    "Vé " + selectedMatch.title() + " đã được ghi nhận. Bạn có thể xem trong mục Vé của tôi.",
                    "ticket_success",
                    orderId);
            checkoutComplete = true;
            selectedSeatIds.clear();
            navigateTo(OrderSuccessFragment.newTicketInstance(orderId));
        });
    }

    private void rememberTicketCheckoutInfo(String provider) {
        UserCheckoutInfo.save(requireContext(),
                text(binding.edtTicketName.getText()),
                text(binding.edtTicketPhone.getText()),
                text(binding.edtTicketEmail.getText()),
                "",
                "bank_transfer",
                provider);
    }

    private Map<String, Object> ticketOrderData(String orderId, String provider, List<TicketSeat> selected, int total) {
        String name = text(binding.edtTicketName.getText());
        String phone = text(binding.edtTicketPhone.getText());
        String email = text(binding.edtTicketEmail.getText());
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("orderCode", orderId);
        data.put("type", "ticket");
        data.put("userId", AuthSession.uid(requireContext()));
        data.put("userEmail", AuthSession.email(requireContext()));
        data.put("customerName", name);
        data.put("recipientName", name);
        data.put("receiverName", name);
        data.put("phone", phone);
        data.put("customerPhone", phone);
        data.put("recipientPhone", phone);
        data.put("receiverPhone", phone);
        data.put("email", email);
        data.put("address", selectedMatch.getStadium());
        data.put("shippingAddress", selectedMatch.getStadium());
        data.put("customerAddress", selectedMatch.getStadium());
        data.put("recipientAddress", selectedMatch.getStadium());
        data.put("receiverAddress", selectedMatch.getStadium());
        data.put("matchId", selectedMatch.getId());
        data.put("matchTitle", selectedMatch.title());
        data.put("home", selectedMatch.getHome());
        data.put("away", selectedMatch.getAway());
        data.put("stadium", selectedMatch.getStadium());
        data.put("city", selectedMatch.getCity());
        data.put("date", selectedMatch.getDate());
        data.put("time", selectedMatch.getTime());
        data.put("subtotal", subtotal(selected));
        data.put("serviceFee", SERVICE_FEE);
        data.put("shippingFee", 0);
        data.put("totalAmount", total);
        data.put("grandTotal", total);
        data.put("total", total);
        data.put("status", "pending_payment");
        data.put("paymentMethod", "bank_transfer");
        data.put("paymentProvider", provider);
        data.put("items", orderItems(selected));
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private List<Map<String, Object>> orderItems(List<TicketSeat> selected) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (TicketSeat seat : selected) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", seat.getId());
            item.put("seatId", seat.getId());
            item.put("name", "Vé " + selectedMatch.title() + " - " + seat.getSeatNumber());
            item.put("stand", seat.getStand());
            item.put("seatNumber", seat.getSeatNumber());
            item.put("type", seat.getType());
            item.put("unitPrice", seat.getPrice());
            item.put("quantity", 1);
            item.put("lineTotal", seat.getPrice());
            item.put("priceText", formatVnd(seat.getPrice()));
            items.add(item);
        }
        return items;
    }

    private void updateSummary() {
        if (binding == null) return;
        List<TicketSeat> selected = selectedSeats();
        int vip = 0;
        List<String> labels = new ArrayList<>();
        long holdUntil = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        for (TicketSeat seat : selected) {
            if (seat.isVip()) vip++;
            labels.add(seat.getSeatNumber() + " (" + seat.getType() + ")");
            if (seat.getLockUntilMs() > now) holdUntil = Math.min(holdUntil, seat.getLockUntilMs());
        }
        if (selected.isEmpty()) {
            binding.tvSelectedSeats.setText("Chưa chọn ghế.");
            binding.tvTicketTotal.setText("--");
            binding.tvSeatHoldTimer.setText("Giữ chỗ: --:--");
            return;
        }
        int regular = selected.size() - vip;
        binding.tvSelectedSeats.setText("Đã chọn: " + TextUtils.join(", ", labels)
                + "\nVIP " + vip + " | Thường " + regular
                + "\nPhí dịch vụ: " + formatVnd(SERVICE_FEE));
        binding.tvTicketTotal.setText(formatVnd(totalPrice(selected)));
        if (holdUntil == Long.MAX_VALUE) {
            binding.tvSeatHoldTimer.setText("Giữ chỗ: --:--");
        } else {
            binding.tvSeatHoldTimer.setText("Giữ chỗ: " + formatRemaining(Math.max(0L, holdUntil - now)));
        }
    }

    private List<TicketSeat> selectedSeats() {
        List<TicketSeat> selected = new ArrayList<>();
        for (TicketSeat seat : seats) {
            if (selectedSeatIds.contains(seat.getId())) selected.add(seat);
        }
        return selected;
    }

    private int subtotal(List<TicketSeat> selected) {
        int subtotal = 0;
        for (TicketSeat seat : selected) subtotal += seat.getPrice();
        return subtotal;
    }

    private int totalPrice(List<TicketSeat> selected) {
        return selected.isEmpty() ? 0 : subtotal(selected) + SERVICE_FEE;
    }

    private TextView chip(String text, boolean selected) {
        TextView chip = baseChip(text, selected);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(38));
        params.setMargins(0, 0, dp(10), 0);
        chip.setLayoutParams(params);
        chip.setMinWidth(dp(86));
        return chip;
    }

    private TextView fullWidthChip(String text, boolean selected) {
        TextView chip = baseChip(text, selected);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        params.setMargins(0, 0, 0, dp(8));
        chip.setLayoutParams(params);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(14), 0, dp(14), 0);
        return chip;
    }

    private TextView baseChip(String text, boolean selected) {
        TextView chip = new TextView(requireContext());
        chip.setText(text);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setGravity(Gravity.CENTER);
        chip.setTextSize(13f);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setTextColor(getColor(selected ? R.color.white : R.color.stadium_ink));
        chip.setBackgroundResource(selected ? R.drawable.bg_product_size_selected : R.drawable.bg_product_size_unselected);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setPadding(dp(14), 0, dp(14), 0);
        return chip;
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

    private int getColor(int colorRes) {
        return requireContext().getColor(colorRes);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatVnd(int value) {
        if (value <= 0) return "0đ";
        return vndFormatter.format(value) + "đ";
    }

    private String formatRemaining(long remainingMs) {
        long totalSeconds = remainingMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private String formatDate(long timestampSeconds) {
        if (timestampSeconds <= 0L) return "--/--";
        return new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
                .format(timestampSeconds * 1000L);
    }

    private String formatTime(long timestampSeconds) {
        if (timestampSeconds <= 0L) return "--:--";
        return new SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN"))
                .format(timestampSeconds * 1000L);
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String text = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .trim();
        return text.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static String string(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int intValue(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) return 0;
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static long longValue(DocumentSnapshot snapshot, String key) {
        if (snapshot == null || key == null || !snapshot.contains(key)) return 0L;
        Object value = snapshot.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void toast(String message) {
        if (getContext() == null) return;
        Toast.makeText(requireContext(), message == null ? "" : message, Toast.LENGTH_SHORT).show();
    }
}

