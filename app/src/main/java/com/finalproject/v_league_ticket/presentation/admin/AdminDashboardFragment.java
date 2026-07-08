package com.finalproject.v_league_ticket.presentation.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.FragmentAdminDashboardBinding;
import com.finalproject.v_league_ticket.databinding.ItemAdminModuleCardBinding;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.profile.LegacyFirestoreCleanup;
import com.finalproject.v_league_ticket.presentation.shop.CartStore;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardFragment extends Fragment {
    private static final String[] EXPECTED_CLUBS = new String[]{
            "Cong An Ha Noi", "Ninh Binh", "Hong Linh Ha Tinh", "Dong A Thanh Hoa",
            "Ha Noi FC", "The Cong Viettel", "Nam Dinh", "Hai Phong",
            "Hoang Anh Gia Lai", "Song Lam Nghe An", "Becamex Binh Duong", "Becamex TP.HCM",
            "SHB Da Nang", "Quang Nam", "PVF-CAND", "Bac Ninh FC"
    };
    private FragmentAdminDashboardBinding binding;

    public AdminDashboardFragment() {
        super(R.layout.fragment_admin_dashboard);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentAdminDashboardBinding.bind(view);
        if (!AuthSession.hasToken(requireContext())) {
            navigateRoot(new AuthLoginFragment());
            return;
        }
        if (!AuthSession.isAdmin(requireContext())) {
            toast("T\u00e0i kho\u1ea3n n\u00e0y kh\u00f4ng c\u00f3 quy\u1ec1n admin.");
            AuthSession.clear(requireContext());
            navigateRoot(new AuthLoginFragment());
            return;
        }
        binding.appHeader.tvHeaderTitle.setText("ADMIN");
        binding.appHeader.tvHeaderSubtitle.setText("B\u1ea3ng \u0111i\u1ec1u khi\u1ec3n qu\u1ea3n tr\u1ecb");
        binding.tvAdminSubtitle.setText("Qu\u1ea3n l\u00fd ng\u01b0\u1eddi d\u00f9ng, \u0111\u01a1n h\u00e0ng, s\u1ea3n ph\u1ea9m v\u00e0 c\u1ea5u h\u00ecnh \u1ee9ng d\u1ee5ng.");
        binding.tvAdminEmail.setText("T\u00e0i kho\u1ea3n: " + AuthSession.email(requireContext()));
        binding.tvTotalUsers.setText("--");
        binding.tvTotalOrders.setText("--");
        binding.tvProductCount.setText("--");
        binding.tvTotalRevenue.setText("--");
        binding.tvAdminNotes.setVisibility(View.GONE);
        binding.tvMissingProductsList.setText("\u0110ang ki\u1ec3m tra s\u1ea3n ph\u1ea9m theo CLB...");
        binding.btnAdminLogout.setOnClickListener(v -> {
            AuthSession.clear(requireContext());
            navigateRoot(new AuthLoginFragment());
        });
        bindCard(binding.cardUsers, "Ng\u01b0\u1eddi d\u00f9ng", "T\u00e0i kho\u1ea3n v\u00e0 ph\u00e2n quy\u1ec1n", R.drawable.ic_person_24, "--", AdminModuleDetailFragment.MODULE_USERS);
        bindCard(binding.cardOrders, "\u0110\u01a1n h\u00e0ng", "Theo d\u00f5i \u0111\u01a1n mua h\u00e0ng", R.drawable.ic_bag_24, "--", AdminModuleDetailFragment.MODULE_ORDERS);
        bindCard(binding.cardProducts, "S\u1ea3n ph\u1ea9m", "\u00c1o \u0111\u1ea5u, b\u00f3ng, v\u1edb, ph\u1ee5 ki\u1ec7n", R.drawable.storefront_24, "--", AdminModuleDetailFragment.MODULE_PRODUCTS);
        bindCard(binding.cardDelivery, "V\u1eadn chuy\u1ec3n", "Sau x\u00e1c nh\u1eadn: m\u00e3 SPX, b\u00e0n giao, ho\u00e0n t\u1ea5t", R.drawable.ic_transfer_24, "--", AdminModuleDetailFragment.MODULE_DELIVERY);
        bindCard(binding.cardMissingProducts, "Thi\u1ebfu s\u1ea3n ph\u1ea9m", "CLB ch\u01b0a c\u00f3 h\u00e0ng", R.drawable.ic_nav_fan, "--", AdminModuleDetailFragment.MODULE_MISSING_PRODUCTS);
        bindCard(binding.cardBanners, "Banner", "N\u1ed9i dung trang ch\u1ee7", R.drawable.ic_bookmark_24, "trang ch\u1ee7", AdminModuleDetailFragment.MODULE_BANNERS);
        bindCard(binding.cardNotifications, "Th\u00f4ng b\u00e1o", "C\u1eadp nh\u1eadt cho ng\u01b0\u1eddi d\u00f9ng", R.drawable.notifications_active_24, "m\u1edbi", AdminModuleDetailFragment.MODULE_NOTIFICATIONS);
        bindCard(binding.cardAppSettings, "C\u00e0i \u0111\u1eb7t", "M\u00f9a gi\u1ea3i v\u00e0 quy t\u1eafc c\u1eeda h\u00e0ng", R.drawable.ic_lock_24, "25/26", AdminModuleDetailFragment.MODULE_SETTINGS);
        binding.cardDataSync.getRoot().setVisibility(View.GONE);
        binding.layoutMissingProducts.setOnClickListener(v -> navigateTo(AdminModuleDetailFragment.newInstance(AdminModuleDetailFragment.MODULE_MISSING_PRODUCTS)));
        LegacyFirestoreCleanup.normalizeAllLegacyOrders();
        loadLiveDashboard();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void bindCard(ItemAdminModuleCardBinding card, String title, String desc, int icon, String badge, String module) {
        card.tvModuleTitle.setText(title);
        card.tvModuleDescription.setText(desc);
        card.tvModuleBadge.setText(badge);
        card.imgModuleIcon.setImageResource(icon);
        card.getRoot().setOnClickListener(v -> navigateTo(AdminModuleDetailFragment.newInstance(module)));
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName()).commit();
    }

    private void navigateRoot(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
    }

    private void loadLiveDashboard() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").count().get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if (binding == null) return;
            if (task.isSuccessful() && task.getResult() != null) {
                String count = String.valueOf(task.getResult().getCount());
                binding.tvTotalUsers.setText(count);
                binding.cardUsers.tvModuleBadge.setText(count);
            } else {
                binding.tvTotalUsers.setText("!");
                binding.cardUsers.tvModuleBadge.setText("l\u1ed7i");
            }
        });
        AdminSummaryStore.ordersRef().get().addOnCompleteListener(summaryTask -> {
            if (binding == null) return;
            if (summaryTask.isSuccessful() && isCompleteSummary(summaryTask.getResult())) {
                bindOrderSummary(
                        intValue(summaryTask.getResult().get("totalOrders")),
                        intValue(summaryTask.getResult().get("totalRevenue")),
                        intValue(summaryTask.getResult().get("pendingOrders")),
                        intValue(summaryTask.getResult().get("deliveryQueue")));
                return;
            }
            loadOrderSummaryFromCollections(db);
        });
        AdminSummaryStore.productsRef().get().addOnCompleteListener(summaryTask -> {
            if (binding == null) return;
            if (summaryTask.isSuccessful() && isCompleteSummary(summaryTask.getResult())) {
                int visibleCount = intValue(summaryTask.getResult().get("visibleCount"));
                binding.tvProductCount.setText(String.valueOf(visibleCount));
                binding.cardProducts.tvModuleBadge.setText(String.valueOf(visibleCount));
                renderMissingProducts(productCounts(summaryTask.getResult().get("clubCounts")));
                return;
            }
            loadProductSummaryFromCollections(db);
        });
    }

    private void loadOrderSummaryFromCollections(FirebaseFirestore db) {
        db.collection("orders").get().addOnCompleteListener(task -> {
            if (binding == null) return;
            if (task.isSuccessful() && task.getResult() != null) {
                int count = task.getResult().size();
                int revenue = 0;
                int pending = 0;
                int deliveryQueue = 0;
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    revenue += orderTotal(doc);
                    String status = firstString(doc, "status", "orderStatus").toLowerCase(Locale.ROOT);
                    if (status.contains("pending")) pending++;
                    boolean ticket = "ticket".equalsIgnoreCase(firstString(doc, "type"));
                    boolean needsDelivery = !ticket && ("confirmed".equals(status) || "shipping".equals(status));
                    if (needsDelivery) deliveryQueue++;
                }
                bindOrderSummary(count, revenue, pending, deliveryQueue);
                AdminSummaryStore.seedOrders(count, revenue, pending, deliveryQueue);
            } else {
                binding.tvTotalOrders.setText("!");
                binding.tvTotalRevenue.setText("!");
                binding.cardOrders.tvModuleBadge.setText("l\u1ed7i");
            }
        });
    }

    private void bindOrderSummary(int count, int revenue, int pending, int deliveryQueue) {
        binding.tvTotalOrders.setText(String.valueOf(count));
        binding.tvTotalRevenue.setText(CartStore.formatVnd(revenue));
        binding.cardOrders.tvModuleBadge.setText(pending > 0 ? pending + " ch\u1edd x\u1eed l\u00fd" : String.valueOf(count));
        binding.cardDelivery.tvModuleBadge.setText(deliveryQueue > 0 ? deliveryQueue + " \u0111\u01a1n" : "OK");
        binding.tvAdminNotes.setVisibility(View.VISIBLE);
        binding.tvAdminNotes.setText("Vi\u1ec7c c\u1ea7n x\u1eed l\u00fd: "
                + pending + " \u0111\u01a1n ch\u1edd x\u00e1c nh\u1eadn thanh to\u00e1n, "
                + deliveryQueue + " \u0111\u01a1n s\u1eb5n s\u00e0ng v\u1eadn chuy\u1ec3n.");
    }

    private void loadProductSummaryFromCollections(FirebaseFirestore db) {
        db.collection("shop_products").get().addOnCompleteListener(task -> {
            if (binding == null) return;
            if (task.isSuccessful() && task.getResult() != null) {
                int visibleCount = 0;
                Map<String, Integer> counts = emptyProductCounts();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Boolean hidden = doc.getBoolean("hidden");
                    if (hidden != null && hidden) continue;
                    visibleCount++;
                    String clubName = firstString(doc, "clubName", "club", "teamName");
                    String key = normalize(clubName);
                    if (counts.containsKey(key)) counts.put(key, counts.get(key) + 1);
                }
                binding.tvProductCount.setText(String.valueOf(visibleCount));
                binding.cardProducts.tvModuleBadge.setText(String.valueOf(visibleCount));
                renderMissingProducts(counts);
                AdminSummaryStore.seedProducts(visibleCount, counts);
            } else {
                binding.tvProductCount.setText("!");
                binding.cardProducts.tvModuleBadge.setText("l\u1ed7i");
                binding.tvMissingProductsList.setText("Kh\u00f4ng \u0111\u1ecdc \u0111\u01b0\u1ee3c d\u1eef li\u1ec7u s\u1ea3n ph\u1ea9m.");
            }
        });
    }

    private void renderMissingProducts(Map<String, Integer> counts) {
        StringBuilder missing = new StringBuilder();
        int missingCount = 0;
        for (String club : EXPECTED_CLUBS) {
            int count = counts.containsKey(normalize(club)) ? counts.get(normalize(club)) : 0;
            if (count == 0) {
                missingCount++;
                missing.append(club).append(": ch\u01b0a c\u00f3 s\u1ea3n ph\u1ea9m\\n");
            } else if (count <= 2) {
                missing.append(club).append(": ").append(count).append(" s\u1ea3n ph\u1ea9m - c\u1ea7n b\u1ed5 sung\\n");
            }
        }
        binding.cardMissingProducts.tvModuleBadge.setText(String.valueOf(missingCount));
        binding.tvMissingProductsList.setText(missing.length() == 0
                ? "T\u1ea5t c\u1ea3 CLB ch\u00ednh \u0111\u00e3 c\u00f3 s\u1ea3n ph\u1ea9m."
                : missing.toString().trim());
        if (missing.length() == 0) {
            binding.tvMissingProductsList.setText("T\u1ea5t c\u1ea3 CLB ch\u00ednh \u0111\u00e3 c\u00f3 s\u1ea3n ph\u1ea9m.");
        }
    }

    private int money(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof String) {
                int parsed = parseMoney((String) value);
                if (parsed > 0) return parsed;
            }
        }
        return 0;
    }

    private int orderTotal(DocumentSnapshot doc) {
        int total = money(doc, "totalAmount", "grandTotal", "total", "finalTotal", "totalPrice", "payableAmount", "amount");
        if (total <= 0) total = money(doc, "subtotal") + money(doc, "shippingFee", "shipping") + money(doc, "serviceFee");
        return total;
    }

    private int parseMoney(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String firstString(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof String && !((String) value).trim().isEmpty()) return ((String) value).trim();
        }
        return "";
    }

    private boolean isCompleteSummary(DocumentSnapshot doc) {
        return doc != null && doc.exists() && Boolean.TRUE.equals(doc.getBoolean("complete"));
    }

    private Map<String, Integer> emptyProductCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String club : EXPECTED_CLUBS) counts.put(normalize(club), 0);
        return counts;
    }

    private Map<String, Integer> productCounts(Object raw) {
        Map<String, Integer> counts = emptyProductCounts();
        if (raw instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
                String key = normalize(String.valueOf(entry.getKey()));
                if (counts.containsKey(key)) counts.put(key, intValue(entry.getValue()));
            }
        }
        return counts;
    }

    private int intValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value == null) return 0;
        try {
            return Integer.parseInt(String.valueOf(value).replaceAll("[^0-9-]", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace("\u0111", "d")
                .replaceAll("[^a-z0-9]+", "-");
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}


