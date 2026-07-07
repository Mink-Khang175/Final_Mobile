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
            toast("Tài khoản này không có quyền admin.");
            AuthSession.clear(requireContext());
            navigateRoot(new AuthLoginFragment());
            return;
        }
        binding.appHeader.tvHeaderTitle.setText("ADMIN");
        binding.appHeader.tvHeaderSubtitle.setText("Bảng điều khiển quản trị");
        binding.tvAdminSubtitle.setText("Quản lý người dùng, đơn hàng, sản phẩm và cấu hình ứng dụng.");
        binding.tvAdminEmail.setText("Tài khoản: " + AuthSession.email(requireContext()));
        binding.tvTotalUsers.setText("--");
        binding.tvTotalOrders.setText("--");
        binding.tvProductCount.setText("--");
        binding.tvTotalRevenue.setText("--");
        binding.tvAdminNotes.setVisibility(View.GONE);
        binding.tvMissingProductsList.setText("Đang kiểm tra sản phẩm theo CLB...");
        binding.btnAdminLogout.setOnClickListener(v -> {
            AuthSession.clear(requireContext());
            navigateRoot(new AuthLoginFragment());
        });
        bindCard(binding.cardUsers, "Người dùng", "Tài khoản và phân quyền", R.drawable.ic_person_24, "--", AdminModuleDetailFragment.MODULE_USERS);
        bindCard(binding.cardOrders, "Đơn hàng", "Theo dõi đơn mua hàng", R.drawable.ic_bag_24, "--", AdminModuleDetailFragment.MODULE_ORDERS);
        bindCard(binding.cardProducts, "Sản phẩm", "Áo đấu, bóng, vớ, phụ kiện", R.drawable.storefront_24, "--", AdminModuleDetailFragment.MODULE_PRODUCTS);
        bindCard(binding.cardMissingProducts, "Thiếu sản phẩm", "CLB chưa có hàng", R.drawable.ic_nav_fan, "--", AdminModuleDetailFragment.MODULE_MISSING_PRODUCTS);
        bindCard(binding.cardBanners, "Banner", "Nội dung trang chủ", R.drawable.ic_bookmark_24, "trang chủ", AdminModuleDetailFragment.MODULE_BANNERS);
        bindCard(binding.cardNotifications, "Thông báo", "Cập nhật cho người dùng", R.drawable.notifications_active_24, "mới", AdminModuleDetailFragment.MODULE_NOTIFICATIONS);
        bindCard(binding.cardAppSettings, "Cài đặt", "Mùa giải và quy tắc cửa hàng", R.drawable.ic_lock_24, "25/26", AdminModuleDetailFragment.MODULE_SETTINGS);
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
        db.collection("users").get().addOnCompleteListener(task -> {
            if (binding == null) return;
            if (task.isSuccessful() && task.getResult() != null) {
                String count = String.valueOf(task.getResult().size());
                binding.tvTotalUsers.setText(count);
                binding.cardUsers.tvModuleBadge.setText(count);
            } else {
                binding.tvTotalUsers.setText("!");
                binding.cardUsers.tvModuleBadge.setText("lỗi");
            }
        });
        db.collection("orders").get().addOnCompleteListener(task -> {
            if (binding == null) return;
            if (task.isSuccessful() && task.getResult() != null) {
                int count = task.getResult().size();
                int revenue = 0;
                int pending = 0;
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    revenue += orderTotal(doc);
                    String status = firstString(doc, "status", "orderStatus").toLowerCase(Locale.ROOT);
                    if (status.contains("pending")) pending++;
                }
                binding.tvTotalOrders.setText(String.valueOf(count));
                binding.tvTotalRevenue.setText(CartStore.formatVnd(revenue));
                binding.cardOrders.tvModuleBadge.setText(pending > 0 ? pending + " chờ xử lý" : String.valueOf(count));
            } else {
                binding.tvTotalOrders.setText("!");
                binding.tvTotalRevenue.setText("!");
                binding.cardOrders.tvModuleBadge.setText("lỗi");
            }
        });
        db.collection("shop_products").get().addOnCompleteListener(task -> {
            if (binding == null) return;
            if (task.isSuccessful() && task.getResult() != null) {
                int visibleCount = 0;
                Map<String, Integer> counts = new LinkedHashMap<>();
                for (String club : EXPECTED_CLUBS) counts.put(normalize(club), 0);
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
            } else {
                binding.tvProductCount.setText("!");
                binding.cardProducts.tvModuleBadge.setText("lỗi");
                binding.tvMissingProductsList.setText("Không đọc được dữ liệu sản phẩm.");
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
                missing.append(club).append(": chưa có sản phẩm\n");
            } else if (count <= 2) {
                missing.append(club).append(": ").append(count).append(" sản phẩm - cần bổ sung\n");
            }
        }
        binding.cardMissingProducts.tvModuleBadge.setText(String.valueOf(missingCount));
        binding.tvMissingProductsList.setText(missing.length() == 0
                ? "Tất cả CLB chính đã có sản phẩm."
                : missing.toString().trim());
        if (missing.length() == 0) {
            binding.tvMissingProductsList.setText("Tất cả CLB chính đã có sản phẩm.");
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace("đ", "d")
                .replaceAll("[^a-z0-9]+", "-");
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
