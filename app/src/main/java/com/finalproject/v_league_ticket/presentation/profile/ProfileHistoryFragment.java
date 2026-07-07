package com.finalproject.v_league_ticket.presentation.profile;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.FragmentProfileHistoryBinding;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.shop.CartStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProfileHistoryFragment extends Fragment {
    private static final String ARG_MODE = "mode";
    private static final String MODE_ORDERS = "orders";
    private static final String MODE_TICKETS = "tickets";
    private static final String MODE_LOYALTY = "loyalty";
    private static final String MODE_BADGES = "badges";
    private static final String MODE_VOUCHERS = "vouchers";
    private static final String MODE_NOTIFICATIONS = "notifications";
    private static final String MODE_PREDICTIONS = "predictions";

    private FragmentProfileHistoryBinding binding;
    private String mode = MODE_ORDERS;
    private final ProfileHistoryAdapter adapter = new ProfileHistoryAdapter(item -> {
        if (MODE_ORDERS.equals(mode)) {
            navigateTo(OrderDetailFragment.newInstance(item.getId()));
        } else if (MODE_PREDICTIONS.equals(mode)) {
            showPredictionDialog(item);
        }
    });

    public ProfileHistoryFragment() {
        super(R.layout.fragment_profile_history);
    }

    public static ProfileHistoryFragment orders() {
        return newInstance(MODE_ORDERS);
    }

    public static ProfileHistoryFragment tickets() {
        return newInstance(MODE_TICKETS);
    }

    public static ProfileHistoryFragment loyalty() {
        return newInstance(MODE_LOYALTY);
    }

    public static ProfileHistoryFragment badges() {
        return newInstance(MODE_BADGES);
    }

    public static ProfileHistoryFragment vouchers() {
        return newInstance(MODE_VOUCHERS);
    }

    public static ProfileHistoryFragment notifications() {
        return newInstance(MODE_NOTIFICATIONS);
    }

    public static ProfileHistoryFragment predictions() {
        return newInstance(MODE_PREDICTIONS);
    }

    private static ProfileHistoryFragment newInstance(String mode) {
        ProfileHistoryFragment fragment = new ProfileHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentProfileHistoryBinding.bind(view);
        mode = requireArguments().getString(ARG_MODE, MODE_ORDERS);
        binding.rvHistory.setAdapter(adapter);
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.tvHistoryTitle.setText(titleFor(mode));
        loadMode();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName()).commit();
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadMode() {
        if (MODE_ORDERS.equals(mode)) {
            loadOrders();
        } else if (MODE_TICKETS.equals(mode)) {
            loadTickets();
        } else if (MODE_LOYALTY.equals(mode)) {
            loadLoyalty();
        } else if (MODE_BADGES.equals(mode)) {
            loadActiveBadges();
        } else if (MODE_VOUCHERS.equals(mode)) {
            loadCollection("user_vouchers", emptyFor(mode), doc -> genericItem(doc, "Voucher"));
        } else if (MODE_NOTIFICATIONS.equals(mode)) {
            loadNotifications();
        } else if (MODE_PREDICTIONS.equals(mode)) {
            loadPredictionTickets();
        }
    }

    private void loadTickets() {
        showLoading();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<ProfileHistoryItem> items = new ArrayList<>();
        db.collection("bookings").whereEqualTo("userId", AuthSession.uid(requireContext())).get()
                .addOnCompleteListener(firstTask -> {
                    if (binding == null) return;
                    if (firstTask.isSuccessful() && firstTask.getResult() != null) {
                        for (DocumentSnapshot doc : firstTask.getResult().getDocuments()) {
                            LegacyFirestoreCleanup.normalizeTicketOrderEverywhere(doc);
                            items.add(ticketItem(doc));
                        }
                    }
                    db.collection("ticket_orders").whereEqualTo("userId", AuthSession.uid(requireContext())).get()
                            .addOnCompleteListener(secondTask -> {
                                if (binding == null) return;
                                hideLoading();
                                Set<String> ids = new HashSet<>();
                                for (ProfileHistoryItem item : items) ids.add(item.getId());
                                if (secondTask.isSuccessful() && secondTask.getResult() != null) {
                                    for (DocumentSnapshot doc : secondTask.getResult().getDocuments()) {
                                        LegacyFirestoreCleanup.normalizeTicketOrderEverywhere(doc);
                                        if (!ids.contains(doc.getId())) items.add(ticketItem(doc));
                                    }
                                }
                                showItems(items, emptyFor(mode));
                            });
                });
    }

    private void loadNotifications() {
        showLoading();
        String uid = AuthSession.uid(requireContext());
        FirebaseFirestore.getInstance().collection("notifications").get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    hideLoading();
                    if (!task.isSuccessful() || task.getResult() == null) {
                        showEmpty("Không đọc được thông báo.");
                        return;
                    }
                    List<ProfileHistoryItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        String userId = first(doc, "userId");
                        String targetRole = first(doc, "targetRole", "target");
                        boolean forMe = uid != null && uid.equals(userId);
                        boolean broadcast = userId.isEmpty()
                                && (targetRole.isEmpty()
                                || "all".equalsIgnoreCase(targetRole)
                                || "user".equalsIgnoreCase(targetRole)
                                || "người dùng".equalsIgnoreCase(targetRole));
                        if (forMe || broadcast) items.add(notificationItem(doc));
                    }
                    showItems(items, emptyFor(mode));
                });
    }

    private void loadPredictionTickets() {
        showLoading();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = AuthSession.uid(requireContext());
        db.collection("user_predictions").whereEqualTo("userId", uid).get()
                .addOnCompleteListener(predTask -> {
                    if (binding == null) return;
                    Map<String, DocumentSnapshot> predictions = new HashMap<>();
                    if (predTask.isSuccessful() && predTask.getResult() != null) {
                        for (DocumentSnapshot doc : predTask.getResult().getDocuments()) {
                            String bookingId = first(doc, "bookingId", "orderId");
                            if (!bookingId.isEmpty()) predictions.put(bookingId, doc);
                        }
                    }
                    List<ProfileHistoryItem> items = new ArrayList<>();
                    db.collection("bookings").whereEqualTo("userId", uid).get()
                            .addOnCompleteListener(ticketTask -> {
                                if (binding == null) return;
                                Set<String> ids = new HashSet<>();
                                if (ticketTask.isSuccessful() && ticketTask.getResult() != null) {
                                    for (DocumentSnapshot doc : ticketTask.getResult().getDocuments()) {
                                        LegacyFirestoreCleanup.normalizeTicketOrderEverywhere(doc);
                                        ids.add(doc.getId());
                                        items.add(predictionTicketItem(doc, predictions.get(doc.getId())));
                                    }
                                }
                                db.collection("ticket_orders").whereEqualTo("userId", uid).get()
                                        .addOnCompleteListener(oldTicketTask -> {
                                            if (binding == null) return;
                                            hideLoading();
                                            if (oldTicketTask.isSuccessful() && oldTicketTask.getResult() != null) {
                                                for (DocumentSnapshot doc : oldTicketTask.getResult().getDocuments()) {
                                                    LegacyFirestoreCleanup.normalizeTicketOrderEverywhere(doc);
                                                    if (ids.contains(doc.getId())) continue;
                                                    items.add(predictionTicketItem(doc, predictions.get(doc.getId())));
                                                }
                                            }
                                            showItems(items, "Bạn chưa có vé nào để dự đoán.");
                                        });
                            });
                });
    }

    private void loadOrders() {
        showLoading();
        FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("userId", AuthSession.uid(requireContext()))
                .get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    hideLoading();
                    if (!task.isSuccessful() || task.getResult() == null) {
                        showEmpty("Không đọc được đơn hàng.");
                        return;
                    }
                    List<ProfileHistoryItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        LegacyFirestoreCleanup.normalizeOrderDocument("orders", doc);
                        String orderCode = first(doc, "orderCode", "orderId");
                        String status = LegacyFirestoreCleanup.normalizeOrderStatus(first(doc, "status", "orderStatus"));
                        int total = orderTotal(doc);
                        items.add(new ProfileHistoryItem(
                                doc.getId(),
                                "Đơn hàng #" + safe(orderCode, doc.getId()),
                                displayStatus(safe(status, "pending")) + " · " + dateText(doc),
                                CartStore.formatVnd(total),
                                R.drawable.ic_receipt_24,
                                R.drawable.bg_history_icon_order,
                                R.color.red_energy,
                                R.drawable.bg_profile_panel_dark,
                                R.color.red_energy));
                    }
                    showItems(items, "Bạn chưa có đơn hàng.");
                });
    }

    private void loadLoyalty() {
        showLoading();
        FirebaseFirestore.getInstance().collection("user_loyalty").document(AuthSession.uid(requireContext()))
                .get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    hideLoading();
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        showEmpty("Bạn chưa có dữ liệu điểm thưởng.");
                        return;
                    }
                    DocumentSnapshot doc = task.getResult();
                    List<ProfileHistoryItem> items = new ArrayList<>();
                    items.add(new ProfileHistoryItem(
                            doc.getId(),
                "Điểm thành viên",
                "Tích lũy: " + number(doc, "lifetimePoints") + " điểm · " + dateText(doc),
                number(doc, "points") + " điểm",
                R.drawable.ic_points_24,
                R.drawable.bg_history_icon_points,
                R.color.premium_gold,
                R.drawable.bg_badge_gold_card,
                R.color.premium_gold));
                    showItems(items, emptyFor(mode));
                });
    }

    private void loadCollection(String collection, String emptyText, Mapper mapper) {
        showLoading();
        FirebaseFirestore.getInstance().collection(collection)
                .whereEqualTo("userId", AuthSession.uid(requireContext()))
                .get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    hideLoading();
                    if (!task.isSuccessful() || task.getResult() == null) {
                        showEmpty("Không đọc được dữ liệu " + titleFor(mode).toLowerCase(Locale.ROOT) + ".");
                        return;
                    }
                    List<ProfileHistoryItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        items.add(mapper.map(doc));
                    }
                    showItems(items, emptyText);
                });
    }

    private void loadActiveBadges() {
        showLoading();
        FirebaseFirestore.getInstance().collection("user_badges")
                .whereEqualTo("userId", AuthSession.uid(requireContext()))
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    hideLoading();
                    if (!task.isSuccessful() || task.getResult() == null) {
                        showEmpty("Không đọc được dữ liệu huy hiệu.");
                        return;
                    }
                    List<ProfileHistoryItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        items.add(badgeItem(doc));
                    }
                    showItems(items, emptyFor(mode));
                });
    }

    private ProfileHistoryItem ticketItem(DocumentSnapshot doc) {
        String match = first(doc, "matchName", "matchTitle", "fixtureName");
        String status = LegacyFirestoreCleanup.normalizeOrderStatus(first(doc, "status", "bookingStatus"));
        int total = orderTotal(doc, "totalPrice");
        return new ProfileHistoryItem(
                doc.getId(),
                match.isEmpty() ? "Đặt vé " + doc.getId() : match,
                displayStatus(safe(status, "pending")) + " · " + dateText(doc),
                CartStore.formatVnd(total),
                R.drawable.ic_ticket_24,
                R.drawable.bg_history_icon_ticket,
                R.color.badge_platinum,
                R.drawable.bg_profile_panel_dark,
                R.color.red_energy);
    }

    private ProfileHistoryItem predictionItem(DocumentSnapshot doc) {
        String match = first(doc, "matchName", "fixtureName", "predictionId");
        String score = first(doc, "score", "predictionScore");
        String points = first(doc, "pointsAwarded", "points");
        return new ProfileHistoryItem(
                doc.getId(),
                match.isEmpty() ? "Dự đoán " + doc.getId() : match,
                (score.isEmpty() ? "Đã gửi dự đoán" : score) + " · " + dateText(doc),
                points.isEmpty() ? "--" : points + " điểm",
                R.drawable.ic_trend_24,
                R.drawable.bg_history_icon_prediction,
                R.color.green_turf,
                R.drawable.bg_profile_panel_dark,
                R.color.green_turf);
    }

    private ProfileHistoryItem predictionTicketItem(DocumentSnapshot ticket, DocumentSnapshot prediction) {
        String match = first(ticket, "matchName", "matchTitle", "fixtureName");
        String date = first(ticket, "date");
        String time = first(ticket, "time");
        if (prediction != null) {
            String score = first(prediction, "score", "predictionScore");
            String status = displayPredictionStatus(first(prediction, "status"));
            String points = first(prediction, "pointsAwarded", "points");
            return new ProfileHistoryItem(
                    ticket.getId(),
                    match.isEmpty() ? "Dự đoán " + ticket.getId() : match,
                    score + " · " + status + " · " + dateText(prediction),
                    points.isEmpty() || "0".equals(points) ? "Đã dự đoán" : points + " điểm",
                    R.drawable.ic_trend_24,
                    R.drawable.bg_history_icon_prediction,
                    R.color.green_turf,
                    R.drawable.bg_profile_panel_dark,
                    R.color.green_turf);
        }
        return new ProfileHistoryItem(
                ticket.getId(),
                match.isEmpty() ? "Dự đoán " + ticket.getId() : match,
                "Mở dự đoán cho vé đã đặt" + ((date + time).trim().isEmpty() ? "" : " · " + date + " " + time),
                "Chọn tỉ số",
                R.drawable.ic_trend_24,
                R.drawable.bg_history_icon_prediction,
                R.color.green_turf,
                R.drawable.bg_profile_panel_dark,
                R.color.green_turf);
    }

    private ProfileHistoryItem notificationItem(DocumentSnapshot doc) {
        String title = first(doc, "title", "name", "message");
        String body = first(doc, "body", "description", "content");
        return new ProfileHistoryItem(
                doc.getId(),
                title.isEmpty() ? "Thông báo" : title,
                (body.isEmpty() ? displayStatus(first(doc, "type", "status")) : body) + " · " + dateText(doc),
                first(doc, "status").equals("unread") ? "Mới" : "",
                R.drawable.notifications_active_24,
                R.drawable.bg_history_icon_ticket,
                R.color.badge_platinum,
                R.drawable.bg_profile_panel_dark,
                R.color.red_energy);
    }

    private ProfileHistoryItem genericItem(DocumentSnapshot doc, String fallback) {
        String title = first(doc, "title", "name", "code", "message", "description");
        String status = first(doc, "status", "type", "category");
        String amount = first(doc, "points", "value", "discount", "amount");
        return new ProfileHistoryItem(
                doc.getId(),
                title.isEmpty() ? fallback + " " + doc.getId() : title,
                displayStatus(status.isEmpty() ? "active" : status) + " · " + dateText(doc),
                amount.isEmpty() ? "" : amount,
                R.drawable.ic_ticket_24,
                R.drawable.bg_history_icon_points,
                R.color.premium_gold,
                R.drawable.bg_profile_panel_dark,
                R.color.premium_gold);
    }

    private ProfileHistoryItem badgeItem(DocumentSnapshot doc) {
        String title = first(doc, "title", "name", "code");
        String description = first(doc, "description");
        String required = first(doc, "pointsRequired");
        BadgeStyle style = badgeStyle(title + " " + first(doc, "code"));
        return new ProfileHistoryItem(
                doc.getId(),
                title.isEmpty() ? "Huy hiệu" : title,
                (description.isEmpty() ? "Hạng thành viên hiện tại" : description) + " · " + dateText(doc),
                required.isEmpty() ? "Đang dùng" : required + " điểm",
                R.drawable.ic_medal_24,
                style.iconBackground,
                style.tint,
                style.cardBackground,
                style.tint);
    }

    private BadgeStyle badgeStyle(String raw) {
        String value = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        if (value.contains("kim cương") || value.contains("diamond") || value.contains("đại sứ")) {
            return new BadgeStyle(R.drawable.bg_history_icon_ticket, R.color.badge_diamond, R.drawable.bg_badge_diamond_card);
        }
        if (value.contains("bạch kim") || value.contains("platinum")) {
            return new BadgeStyle(R.drawable.bg_history_icon_ticket, R.color.badge_platinum, R.drawable.bg_badge_platinum_card);
        }
        if (value.contains("vàng") || value.contains("gold")) {
            return new BadgeStyle(R.drawable.bg_history_icon_points, R.color.badge_gold, R.drawable.bg_badge_gold_card);
        }
        if (value.contains("bạc") || value.contains("silver")) {
            return new BadgeStyle(R.drawable.bg_history_icon_ticket, R.color.badge_silver, R.drawable.bg_badge_silver_card);
        }
        return new BadgeStyle(R.drawable.bg_history_icon_points, R.color.badge_bronze, R.drawable.bg_badge_bronze_card);
    }

    private static class BadgeStyle {
        final int iconBackground;
        final int tint;
        final int cardBackground;

        BadgeStyle(int iconBackground, int tint, int cardBackground) {
            this.iconBackground = iconBackground;
            this.tint = tint;
            this.cardBackground = cardBackground;
        }
    }

    private void showPredictionDialog(ProfileHistoryItem item) {
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(32, 12, 32, 0);
        EditText homeScore = predictionInput("Tỉ số đội nhà");
        EditText awayScore = predictionInput("Tỉ số đội khách");
        form.addView(homeScore);
        form.addView(awayScore);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.getPrimary())
                .setMessage("Nhập tỉ số bạn dự đoán cho trận đã đặt vé.")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu dự đoán", null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Integer home = parseScore(homeScore.getText());
            Integer away = parseScore(awayScore.getText());
            if (home == null || away == null) {
                toast("Nhập tỉ số hợp lệ.");
                return;
            }
            savePrediction(item, home, away, dialog);
        });
    }

    private EditText predictionInput(String hint) {
        EditText input = new EditText(requireContext());
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setTextSize(16f);
        input.setPadding(0, 12, 0, 12);
        return input;
    }

    private void savePrediction(ProfileHistoryItem item, int home, int away, AlertDialog dialog) {
        String uid = AuthSession.uid(requireContext());
        String predictionId = uid + "_" + item.getId();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.DocumentReference predictionRef =
                db.collection("user_predictions").document(predictionId);
        predictionRef.get().addOnCompleteListener(existingTask -> {
            if (binding == null) return;
            boolean alreadyAwarded = existingTask.isSuccessful()
                    && existingTask.getResult() != null
                    && number(existingTask.getResult(), "pointsAwarded") > 0;
            db.collection("bookings").document(item.getId()).get().addOnCompleteListener(ticketTask -> {
                if (binding == null) return;
                DocumentSnapshot ticket = ticketTask.isSuccessful() ? ticketTask.getResult() : null;
                Integer actualHome = ticket == null ? null : nullableInt(ticket, "homeScore", "homeGoals", "homeFullTimeScore");
                Integer actualAway = ticket == null ? null : nullableInt(ticket, "awayScore", "awayGoals", "awayFullTimeScore");
                boolean hasResult = actualHome != null && actualAway != null;
                boolean correct = hasResult && actualHome == home && actualAway == away;

                Map<String, Object> data = new HashMap<>();
                data.put("userId", uid);
                data.put("bookingId", item.getId());
                data.put("orderId", item.getId());
                data.put("matchName", item.getPrimary());
                data.put("homeScore", home);
                data.put("awayScore", away);
                data.put("score", home + " - " + away);
                data.put("status", hasResult ? (correct ? "correct" : "wrong") : "pending_result");
                data.put("pointsAwarded", correct || alreadyAwarded ? UserEngagementManager.PREDICTION_POINTS : 0);
                data.put("updatedAt", FieldValue.serverTimestamp());
                data.put("createdAt", FieldValue.serverTimestamp());
                predictionRef.set(data, SetOptions.merge())
                        .addOnCompleteListener(task -> {
                            if (binding == null) return;
                            if (!task.isSuccessful()) {
                                toast("Không lưu được dự đoán.");
                                return;
                            }
                            if (correct && !alreadyAwarded) {
                                UserEngagementManager.awardPrediction(uid, predictionId);
                                UserEngagementManager.notifyUser(uid,
                                        "Dự đoán chính xác",
                                        "Bạn nhận " + UserEngagementManager.PREDICTION_POINTS + " điểm cho trận " + item.getPrimary() + ".",
                                        "prediction_correct",
                                        predictionId);
                            }
                            dialog.dismiss();
                            toast(correct && !alreadyAwarded ? "Dự đoán đúng, đã cộng điểm." : "Đã lưu dự đoán.");
                            loadMode();
                        });
            });
        });
    }

    private void showLoading() {
        binding.progressHistory.setVisibility(View.VISIBLE);
        binding.tvHistoryEmpty.setVisibility(View.GONE);
        adapter.submitList(new ArrayList<>());
    }

    private void hideLoading() {
        binding.progressHistory.setVisibility(View.GONE);
    }

    private void showItems(List<ProfileHistoryItem> items, String emptyText) {
        adapter.submitList(items);
        binding.tvHistoryEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        if (items.isEmpty()) binding.tvHistoryEmpty.setText(emptyText);
    }

    private void showEmpty(String text) {
        adapter.submitList(new ArrayList<>());
        binding.tvHistoryEmpty.setText(text);
        binding.tvHistoryEmpty.setVisibility(View.VISIBLE);
    }

    private String titleFor(String mode) {
        switch (mode) {
            case MODE_TICKETS: return "Vé của tôi";
            case MODE_LOYALTY: return "Điểm thành viên";
            case MODE_BADGES: return "Huy hiệu của tôi";
            case MODE_VOUCHERS: return "Vouchers";
            case MODE_NOTIFICATIONS: return "Thông báo";
            case MODE_PREDICTIONS: return "Dự đoán";
            default: return "Đơn hàng của tôi";
        }
    }

    private String emptyFor(String mode) {
        switch (mode) {
            case MODE_TICKETS: return "Bạn chưa có vé nào.";
            case MODE_LOYALTY: return "Bạn chưa có dữ liệu điểm thưởng.";
            case MODE_BADGES: return "Bạn chưa có huy hiệu nào.";
            case MODE_VOUCHERS: return "Bạn chưa có voucher nào.";
            case MODE_NOTIFICATIONS: return "Bạn chưa có thông báo nào.";
            case MODE_PREDICTIONS: return "Bạn chưa có dự đoán nào.";
            default: return "Bạn chưa có đơn hàng.";
        }
    }

    private String first(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof String && !((String) value).trim().isEmpty()) return ((String) value).trim();
            if (value instanceof Number) return String.valueOf(((Number) value).longValue());
        }
        return "";
    }

    private int money(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof String) {
                String digits = ((String) value).replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    try {
                        return Integer.parseInt(digits);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return 0;
    }

    private int orderTotal(DocumentSnapshot doc, String... priorityKeys) {
        int total = money(doc, priorityKeys);
        if (total <= 0) total = money(doc, "totalAmount", "grandTotal", "total", "finalTotal", "amount", "payableAmount");
        if (total <= 0) total = money(doc, "subtotal") + money(doc, "shippingFee", "shipping") + money(doc, "serviceFee");
        if (total <= 0) total = itemsTotal(doc);
        return total;
    }

    private int itemsTotal(DocumentSnapshot doc) {
        Object raw = doc.get("items");
        if (!(raw instanceof List<?>)) return 0;
        int total = 0;
        for (Object row : (List<?>) raw) {
            if (!(row instanceof Map<?, ?>)) continue;
            Map<?, ?> item = (Map<?, ?>) row;
            int lineTotal = intValue(item.get("lineTotal"));
            if (lineTotal > 0) {
                total += lineTotal;
                continue;
            }
            int unitPrice = intValue(firstObject(item, "unitPrice", "price", "amount"));
            int quantity = intValue(firstObject(item, "quantity", "qty"));
            total += unitPrice * Math.max(1, quantity);
        }
        return total;
    }

    private Object firstObject(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) return value;
        }
        return null;
    }

    private int intValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            String digits = ((String) value).replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    return Integer.parseInt(digits);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private Integer nullableInt(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof String) {
                String digits = ((String) value).replaceAll("[^0-9-]", "");
                if (!digits.isEmpty()) {
                    try {
                        return Integer.parseInt(digits);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }

    private Integer parseScore(CharSequence value) {
        String text = value == null ? "" : value.toString().trim();
        if (text.isEmpty()) return null;
        try {
            int score = Integer.parseInt(text);
            return score < 0 || score > 30 ? null : score;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private long number(DocumentSnapshot doc, String key) {
        Object value = doc.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private String displayStatus(String value) {
        String normalized = LegacyFirestoreCleanup.normalizeOrderStatus(value).toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "pending": return "Chờ xử lý";
            case "pending_payment": return "Chờ xác nhận thanh toán";
            case "confirmed": return "Đã xác nhận";
            case "shipping": return "Đang giao";
            case "completed": return "Hoàn tất";
            case "cancelled": return "Đã hủy";
            case "active": return "Đang dùng";
            default: return value == null || value.trim().isEmpty() ? "-" : value.trim();
        }
    }

    private String displayPredictionStatus(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "correct": return "Đúng tỉ số";
            case "wrong": return "Chưa chính xác";
            case "pending_result": return "Chờ kết quả";
            default: return value == null || value.trim().isEmpty() ? "Chờ kết quả" : value.trim();
        }
    }

    private String dateText(DocumentSnapshot doc) {
        Object value = doc.get("createdAt");
        if (!(value instanceof Timestamp)) value = doc.get("updatedAt");
        Date date = value instanceof Timestamp ? ((Timestamp) value).toDate() : null;
        return date == null ? "Chưa có ngày" : new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private interface Mapper {
        ProfileHistoryItem map(DocumentSnapshot doc);
    }
}
