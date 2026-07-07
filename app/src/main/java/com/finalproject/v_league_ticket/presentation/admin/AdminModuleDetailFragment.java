package com.finalproject.v_league_ticket.presentation.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.FragmentAdminModuleDetailBinding;
import com.finalproject.v_league_ticket.presentation.profile.LegacyFirestoreCleanup;
import com.finalproject.v_league_ticket.presentation.profile.UserEngagementManager;
import com.finalproject.v_league_ticket.presentation.shop.CartStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class AdminModuleDetailFragment extends Fragment {
    private static final String ARG_MODULE_TYPE = "module_type";
    public static final String MODULE_USERS = "users";
    public static final String MODULE_ORDERS = "orders";
    public static final String MODULE_PRODUCTS = "products";
    public static final String MODULE_MISSING_PRODUCTS = "missing_products";
    public static final String MODULE_BANNERS = "banners";
    public static final String MODULE_NOTIFICATIONS = "notifications";
    public static final String MODULE_SETTINGS = "settings";
    public static final String MODULE_SYNC = "sync";

    private static final String[] EXPECTED_CLUBS = new String[]{
            "Cong An Ha Noi", "Ninh Binh", "Hong Linh Ha Tinh", "Dong A Thanh Hoa",
            "Ha Noi FC", "The Cong Viettel", "Nam Dinh", "Hai Phong",
            "Hoang Anh Gia Lai", "Song Lam Nghe An", "Becamex Binh Duong", "Becamex TP.HCM",
            "SHB Da Nang", "Quang Nam", "PVF-CAND", "Bac Ninh FC"
    };

    private FragmentAdminModuleDetailBinding binding;
    private String currentModule = MODULE_USERS;

    public AdminModuleDetailFragment() {
        super(R.layout.fragment_admin_module_detail);
    }

    public static AdminModuleDetailFragment newInstance(String moduleType) {
        AdminModuleDetailFragment fragment = new AdminModuleDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODULE_TYPE, moduleType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentAdminModuleDetailBinding.bind(view);
        currentModule = requireArguments().getString(ARG_MODULE_TYPE, MODULE_USERS);
        binding.appHeader.tvHeaderTitle.setText("ADMIN");
        binding.appHeader.tvHeaderSubtitle.setText(title(currentModule));
        binding.tvModuleTitle.setText(title(currentModule));
        binding.tvModuleSubtitle.setText(subtitle(currentModule));
        binding.btnBackAdmin.setOnClickListener(v -> navigateBack());
        binding.btnRefresh.setText(primaryActionText(currentModule));
        binding.btnRefresh.setOnClickListener(v -> handlePrimaryAction());
        render(currentModule);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void handlePrimaryAction() {
        if (MODULE_PRODUCTS.equals(currentModule)) {
            showProductDialog(null);
        } else if (MODULE_NOTIFICATIONS.equals(currentModule)) {
            showNotificationDialog(null);
        } else if (MODULE_BANNERS.equals(currentModule)) {
            showBannerDialog(null);
        } else if (MODULE_SETTINGS.equals(currentModule)) {
            showSettingsDialog(null);
        } else {
            render(currentModule);
        }
    }

    private void render(String module) {
        binding.layoutContent.removeAllViews();
        addInfo("Đang tải dữ liệu...");
        if (MODULE_MISSING_PRODUCTS.equals(module)) {
            renderMissingProducts();
            return;
        }
        String collection = collectionFor(module);
        FirebaseFirestore.getInstance().collection(collection).get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    binding.layoutContent.removeAllViews();
                    if (!task.isSuccessful() || task.getResult() == null) {
                        addInfo("Không đọc được dữ liệu. Vui lòng thử lại.");
                        return;
                    }
                    if (task.getResult().isEmpty()) {
                        addInfo(emptyText(module));
                        return;
                    }
                    int visibleDocs = 0;
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        if ("_schema".equals(doc.getId())) continue;
                        visibleDocs++;
                        addDocumentCard(module, doc);
                    }
                    if (visibleDocs == 0) addInfo(emptyText(module));
                });
    }

    private void addDocumentCard(String module, DocumentSnapshot doc) {
        if (MODULE_ORDERS.equals(module)) LegacyFirestoreCleanup.normalizeOrderDocument("orders", doc);
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_admin_module_card);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);

        TextView title = new TextView(requireContext());
        title.setText(docTitle(module, doc));
        title.setTextColor(requireContext().getColor(R.color.stadium_ink));
        title.setTextSize(16f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView body = new TextView(requireContext());
        body.setText(docBody(module, doc));
        body.setTextColor(requireContext().getColor(R.color.slate_caption));
        body.setTextSize(13f);
        body.setPadding(0, dp(6), 0, dp(10));
        card.addView(body);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setBaselineAligned(false);
        card.addView(actions);
        addActions(module, doc, actions);
        binding.layoutContent.addView(card);
    }

    private void addActions(String module, DocumentSnapshot doc, LinearLayout actions) {
        if (MODULE_PRODUCTS.equals(module)) {
            addSmallButton(actions, "Sửa", v -> showProductDialog(doc));
            addSmallButton(actions, Boolean.TRUE.equals(doc.getBoolean("hidden")) ? "Hiện" : "Ẩn",
                    v -> updateDoc("shop_products", doc.getId(), "hidden", !Boolean.TRUE.equals(doc.getBoolean("hidden"))));
            addDangerButton(actions, "Xóa", v -> confirmDelete("shop_products", doc.getId()));
        } else if (MODULE_ORDERS.equals(module)) {
            String status = displayValue(safe(LegacyFirestoreCleanup.normalizeOrderStatus(first(doc, "status", "orderStatus")), "pending"));
            addSmallButton(actions, "Đổi trạng thái: " + status, v -> showOrderStatusMenu(v, doc));
        } else if (MODULE_USERS.equals(module)) {
            addSmallButton(actions, "Vai trò", v -> showStatusMenu(v, "users", doc.getId(), new String[]{"user", "admin"}));
            addSmallButton(actions, "Trạng thái", v -> showFieldMenu(v, "users", doc.getId(), "status",
                    new String[]{"active", "disabled"}));
        } else if (MODULE_NOTIFICATIONS.equals(module)) {
            addDangerButton(actions, "Xóa", v -> confirmDelete("notifications", doc.getId()));
        } else if (MODULE_BANNERS.equals(module)) {
            addSmallButton(actions, Boolean.FALSE.equals(doc.getBoolean("enabled")) ? "Bật" : "Tắt",
                    v -> updateDoc("app_banners", doc.getId(), "enabled", Boolean.FALSE.equals(doc.getBoolean("enabled"))));
            addSmallButton(actions, "Sửa", v -> showBannerDialog(doc));
            addDangerButton(actions, "Xóa", v -> confirmDelete("app_banners", doc.getId()));
        } else if (MODULE_SETTINGS.equals(module)) {
            addSmallButton(actions, "Sửa", v -> showSettingsDialog(doc));
        }
    }

    private void showStatusMenu(View anchor, String collection, String id, String[] statuses) {
        showFieldMenu(anchor, collection, id, "status", statuses);
    }

    private void showOrderStatusMenu(View anchor, DocumentSnapshot doc) {
        String[] statuses = new String[]{"pending_payment", "confirmed", "shipping", "completed", "cancelled"};
        String current = LegacyFirestoreCleanup.normalizeOrderStatus(first(doc, "status", "orderStatus"));
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        for (int i = 0; i < statuses.length; i++) {
            String label = statuses[i].equals(current) ? "✓ " + displayValue(statuses[i]) : displayValue(statuses[i]);
            menu.getMenu().add(0, i, i, label);
        }
        menu.setOnMenuItemClickListener(item -> {
            updateOrderStatus(doc, statuses[item.getItemId()]);
            return true;
        });
        menu.show();
    }

    private void showFieldMenu(View anchor, String collection, String id, String field, String[] values) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        for (int i = 0; i < values.length; i++) {
            menu.getMenu().add(0, i, i, displayValue(values[i]));
        }
        menu.setOnMenuItemClickListener(item -> {
            updateDoc(collection, id, field, values[item.getItemId()]);
            return true;
        });
        menu.show();
    }

    private void showProductDialog(DocumentSnapshot doc) {
        LinearLayout form = form();
        EditText name = input("Tên sản phẩm", first(doc, "name", "title", "productName"));
        EditText club = input("CLB", first(doc, "clubName", "club", "teamName"));
        EditText category = input("Danh mục", safe(first(doc, "category"), "Áo đấu"));
        EditText price = input("Giá hiển thị", first(doc, "priceText", "formattedPrice", "price"));
        EditText image = input("Đường dẫn ảnh", first(doc, "imageUrl", "image"));
        EditText url = input("Đường dẫn sản phẩm", first(doc, "productUrl", "url"));
        CheckBox hidden = new CheckBox(requireContext());
        hidden.setText("Ẩn sản phẩm khỏi shop");
        hidden.setChecked(doc != null && Boolean.TRUE.equals(doc.getBoolean("hidden")));
        form.addView(name); form.addView(club); form.addView(category); form.addView(price); form.addView(image); form.addView(url); form.addView(hidden);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(doc == null ? "Thêm sản phẩm" : "Sửa sản phẩm")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", text(name));
                    data.put("clubName", text(club));
                    data.put("category", text(category).isEmpty() ? "Áo đấu" : text(category));
                    data.put("priceText", text(price));
                    data.put("imageUrl", text(image));
                    data.put("productUrl", text(url));
                    data.put("hidden", hidden.isChecked());
                    data.put(doc == null ? "createdAt" : "updatedAt", FieldValue.serverTimestamp());
                    String id = doc == null ? "product_" + System.currentTimeMillis() : doc.getId();
                    saveDoc("shop_products", id, data);
                })
                .show();
    }

    private void showNotificationDialog(DocumentSnapshot ignored) {
        LinearLayout form = form();
        EditText title = input("Tiêu đề", "");
        EditText body = input("Nội dung", "");
        EditText targetUser = input("UID người nhận (bỏ trống nếu gửi broadcast)", "");
        targetUser.setVisibility(View.GONE);
        TextView targetRole = dialogSelect("Đối tượng: Tất cả");
        final String[] selectedTarget = new String[]{"all"};
        targetRole.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), targetRole);
            String[] values = new String[]{"all", "user", "admin", "uid"};
            String[] labels = new String[]{"Tất cả", "Người dùng", "Quản trị viên", "UID cụ thể"};
            for (int i = 0; i < values.length; i++) menu.getMenu().add(0, i, i, labels[i]);
            menu.setOnMenuItemClickListener(item -> {
                selectedTarget[0] = values[item.getItemId()];
                targetRole.setText("Đối tượng: " + labels[item.getItemId()]);
                targetUser.setVisibility("uid".equals(selectedTarget[0]) ? View.VISIBLE : View.GONE);
                return true;
            });
            menu.show();
        });
        form.addView(title); form.addView(body); form.addView(targetRole); form.addView(targetUser);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tạo thông báo")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", text(title));
                    data.put("body", text(body));
                    if ("uid".equals(selectedTarget[0]) && !text(targetUser).isEmpty()) data.put("userId", text(targetUser));
                    data.put("targetRole", "uid".equals(selectedTarget[0]) ? "user" : selectedTarget[0]);
                    data.put("type", "admin");
                    data.put("createdAt", FieldValue.serverTimestamp());
                    saveDoc("notifications", "notification_" + System.currentTimeMillis(), data);
                })
                .show();
    }

    private void showBannerDialog(DocumentSnapshot doc) {
        LinearLayout form = form();
        EditText title = input("Tiêu đề", first(doc, "title", "name"));
        EditText subtitle = input("Mô tả", first(doc, "subtitle", "description"));
        EditText image = input("Đường dẫn ảnh", first(doc, "imageUrl", "image"));
        CheckBox enabled = new CheckBox(requireContext());
        enabled.setText("Đang bật");
        enabled.setChecked(doc == null || !Boolean.FALSE.equals(doc.getBoolean("enabled")));
        form.addView(title); form.addView(subtitle); form.addView(image); form.addView(enabled);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(doc == null ? "Thêm banner" : "Sửa banner")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", text(title));
                    data.put("subtitle", text(subtitle));
                    data.put("imageUrl", text(image));
                    data.put("enabled", enabled.isChecked());
                    data.put(doc == null ? "createdAt" : "updatedAt", FieldValue.serverTimestamp());
                    saveDoc("app_banners", doc == null ? "banner_" + System.currentTimeMillis() : doc.getId(), data);
                })
                .show();
    }

    private void showSettingsDialog(DocumentSnapshot doc) {
        LinearLayout form = form();
        EditText season = input("Mùa giải", first(doc, "season", "currentSeason"));
        EditText shipping = input("Phí ship", first(doc, "shippingFee"));
        CheckBox ordersEnabled = new CheckBox(requireContext());
        ordersEnabled.setText("Cho phép đặt hàng");
        ordersEnabled.setChecked(doc == null || !Boolean.FALSE.equals(doc.getBoolean("ordersEnabled")));
        form.addView(season); form.addView(shipping); form.addView(ordersEnabled);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cài đặt ứng dụng")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("season", text(season));
                    data.put("shippingFee", parseInt(text(shipping)));
                    data.put("ordersEnabled", ordersEnabled.isChecked());
                    data.put("updatedAt", FieldValue.serverTimestamp());
                    saveDoc("app_settings", doc == null ? "main" : doc.getId(), data);
                })
                .show();
    }

    private LinearLayout form() {
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), 0);
        return form;
    }

    private EditText input(String hint, String value) {
        EditText edit = new EditText(requireContext());
        edit.setHint(hint);
        edit.setSingleLine(false);
        edit.setText(value == null ? "" : value);
        edit.setTextColor(requireContext().getColor(R.color.stadium_ink));
        edit.setHintTextColor(requireContext().getColor(R.color.slate_caption));
        edit.setPadding(0, dp(8), 0, dp(8));
        return edit;
    }

    private TextView dialogSelect(String value) {
        TextView view = new TextView(requireContext());
        view.setText(value);
        view.setTextColor(requireContext().getColor(R.color.stadium_ink));
        view.setTextSize(15f);
        view.setPadding(0, dp(14), 0, dp(14));
        view.setBackgroundResource(R.drawable.bg_screen_field);
        return view;
    }

    private void addSmallButton(LinearLayout parent, String label, View.OnClickListener listener) {
        TextView button = new TextView(requireContext());
        button.setText(label);
        button.setTextSize(11f);
        button.setGravity(android.view.Gravity.CENTER);
        button.setSingleLine(true);
        button.setTextColor(requireContext().getColor(R.color.stadium_ink));
        button.setTypeface(button.getTypeface(), android.graphics.Typeface.BOLD);
        button.setBackgroundResource(R.drawable.bg_order_outline_button);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMargins(0, 0, dp(6), 0);
        parent.addView(button, params);
    }

    private void addDangerButton(LinearLayout parent, String label, View.OnClickListener listener) {
        TextView button = new TextView(requireContext());
        button.setText(label);
        button.setTextSize(11f);
        button.setGravity(android.view.Gravity.CENTER);
        button.setSingleLine(true);
        button.setTextColor(requireContext().getColor(R.color.red_energy));
        button.setTypeface(button.getTypeface(), android.graphics.Typeface.BOLD);
        button.setBackgroundResource(R.drawable.bg_order_outline_button);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        parent.addView(button, params);
    }

    private void saveDoc(String collection, String id, Map<String, Object> data) {
        FirebaseFirestore.getInstance().collection(collection).document(id)
                .set(data, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    toast(task.isSuccessful() ? "Đã lưu" : "Không lưu được");
                    render(currentModule);
                });
    }

    private void updateDoc(String collection, String id, String field, Object value) {
        Map<String, Object> data = new HashMap<>();
        data.put(field, value);
        data.put("updatedAt", FieldValue.serverTimestamp());
        saveDoc(collection, id, data);
    }

    private void updateOrderStatus(DocumentSnapshot order, String status) {
        String orderId = order.getId();
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("updatedAt", FieldValue.serverTimestamp());
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").document(orderId).set(data, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        toast("Không cập nhật được trạng thái");
                        render(currentModule);
                        return;
                    }
                    if ("ticket".equalsIgnoreCase(first(order, "type"))) {
                        db.collection("bookings").document(orderId).set(data, SetOptions.merge());
                        db.collection("ticket_orders").document(orderId).set(data, SetOptions.merge());
                    }
                    String uid = first(order, "userId");
                    if (!uid.isEmpty()) {
                        UserEngagementManager.notifyUser(uid,
                                "Cập nhật đơn hàng",
                                "Đơn " + safe(first(order, "orderCode", "orderId"), orderId)
                                        + " đang ở trạng thái: " + displayValue(status) + ".",
                                "order_status",
                                orderId);
                    }
                    toast("Đã cập nhật trạng thái");
                    render(currentModule);
                });
    }

    private void confirmDelete(String collection, String id) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa dữ liệu?")
                .setMessage("Thao tác này không thể hoàn tác.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) ->
                        FirebaseFirestore.getInstance().collection(collection).document(id).delete()
                                .addOnCompleteListener(task -> {
                                    toast(task.isSuccessful() ? "Đã xóa" : "Không xóa được");
                                    render(currentModule);
                                }))
                .show();
    }

    private void addInfo(String text) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextColor(requireContext().getColor(R.color.slate_caption));
        view.setTextSize(14f);
        view.setPadding(dp(18), dp(18), dp(18), dp(18));
        view.setBackgroundResource(R.drawable.bg_admin_module_card);
        binding.layoutContent.addView(view);
    }

    private String primaryActionText(String module) {
        switch (module) {
            case MODULE_PRODUCTS: return "THÊM SẢN PHẨM";
            case MODULE_NOTIFICATIONS: return "TẠO THÔNG BÁO";
            case MODULE_BANNERS: return "THÊM BANNER";
            case MODULE_SETTINGS: return "CẬP NHẬT CÀI ĐẶT";
            default: return "LÀM MỚI";
        }
    }

    private String title(String module) {
        switch (module) {
            case MODULE_ORDERS: return "Quản lý đơn hàng";
            case MODULE_PRODUCTS: return "Quản lý sản phẩm";
            case MODULE_MISSING_PRODUCTS: return "CLB thiếu sản phẩm";
            case MODULE_BANNERS: return "Banner / Nội dung trang chủ";
            case MODULE_NOTIFICATIONS: return "Quản lý thông báo";
            case MODULE_SETTINGS: return "Cài đặt ứng dụng";
            case MODULE_USERS:
            default: return "Quản lý người dùng";
        }
    }

    private String subtitle(String module) {
        switch (module) {
            case MODULE_USERS: return "Quản lý role và trạng thái tài khoản.";
            case MODULE_ORDERS: return "Theo dõi và cập nhật trạng thái đơn hàng.";
            case MODULE_PRODUCTS: return "Quản lý áo đấu, trái bóng, vớ bóng đá và phụ kiện.";
            case MODULE_MISSING_PRODUCTS: return "Theo dõi CLB thiếu sản phẩm.";
            case MODULE_BANNERS: return "Quản lý banner và nội dung nổi bật.";
            case MODULE_NOTIFICATIONS: return "Tạo và quản lý thông báo người dùng.";
            case MODULE_SETTINGS: return "Cấu hình mùa giải, đặt hàng và phí vận chuyển.";
            default: return "";
        }
    }

    private String collectionFor(String module) {
        switch (module) {
            case MODULE_ORDERS: return "orders";
            case MODULE_PRODUCTS: return "shop_products";
            case MODULE_BANNERS: return "app_banners";
            case MODULE_NOTIFICATIONS: return "notifications";
            case MODULE_SETTINGS: return "app_settings";
            case MODULE_USERS:
            default: return "users";
        }
    }

    private String emptyText(String module) {
        switch (module) {
            case MODULE_PRODUCTS: return "Chưa có sản phẩm.";
            case MODULE_ORDERS: return "Chưa có đơn hàng.";
            case MODULE_NOTIFICATIONS: return "Chưa có thông báo.";
            case MODULE_BANNERS: return "Chưa có banner.";
            default: return "Chưa có dữ liệu.";
        }
    }

    private String docTitle(String module, DocumentSnapshot doc) {
        switch (module) {
            case MODULE_USERS: return safe(first(doc, "email", "username", "displayName"), doc.getId());
            case MODULE_ORDERS: return "Đơn hàng " + safe(first(doc, "orderCode", "orderId", "code"), doc.getId());
            case MODULE_PRODUCTS: return safe(first(doc, "name", "title", "productName"), doc.getId());
            case MODULE_BANNERS:
            case MODULE_NOTIFICATIONS: return safe(first(doc, "title", "name"), doc.getId());
            case MODULE_SETTINGS: return "Cài đặt ứng dụng";
            default: return doc.getId();
        }
    }

    private String docBody(String module, DocumentSnapshot doc) {
        switch (module) {
            case MODULE_USERS:
                return "Vai trò: " + displayValue(safe(first(doc, "role"), "user"))
                        + "\nTrạng thái: " + displayValue(safe(first(doc, "status"), "active"))
                        + "\nTên: " + safe(first(doc, "username", "displayName"), "-");
            case MODULE_ORDERS:
                return "Khách hàng: " + safe(customerName(doc), "-")
                        + "\nSĐT: " + safe(customerPhone(doc), "-")
                        + "\nĐịa chỉ/điểm nhận: " + safe(customerAddress(doc), "-")
                        + "\nThanh toán: " + displayValue(safe(first(doc, "paymentMethod"), "-"))
                        + "\nTổng tiền: " + CartStore.formatVnd(orderTotal(doc))
                        + "\nTrạng thái: " + displayValue(safe(LegacyFirestoreCleanup.normalizeOrderStatus(first(doc, "status", "orderStatus")), "pending"));
            case MODULE_PRODUCTS:
                return "CLB: " + safe(first(doc, "clubName", "club", "teamName"), "-")
                        + "\nDanh mục: " + safe(first(doc, "category"), "Áo đấu")
                        + "\nGiá: " + safe(first(doc, "priceText", "formattedPrice", "price"), "-")
                        + "\nHiển thị: " + (Boolean.TRUE.equals(doc.getBoolean("hidden")) ? "Đang ẩn" : "Đang hiện");
            case MODULE_BANNERS:
                return "Đang bật: " + String.valueOf(!Boolean.FALSE.equals(doc.getBoolean("enabled")))
                        + "\nMô tả: " + safe(first(doc, "subtitle", "description"), "-");
            case MODULE_NOTIFICATIONS:
                return safe(first(doc, "body", "message", "content"), "-")
                        + "\nĐối tượng: " + notificationTarget(doc);
            case MODULE_SETTINGS:
                return "Mùa giải: " + safe(first(doc, "season", "currentSeason"), "-")
                        + "\nPhí vận chuyển: " + safe(first(doc, "shippingFee"), "0")
                        + "\nĐặt hàng: " + (Boolean.FALSE.equals(doc.getBoolean("ordersEnabled")) ? "Đang tắt" : "Đang bật");
            default:
                return "Dữ liệu đã sẵn sàng.";
        }
    }

    private void renderMissingProducts() {
        FirebaseFirestore.getInstance().collection("shop_products").get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    binding.layoutContent.removeAllViews();
                    if (!task.isSuccessful() || task.getResult() == null) {
                        addInfo("Không đọc được dữ liệu sản phẩm.");
                        return;
                    }
                    Map<String, Integer> counts = new LinkedHashMap<>();
                    for (String club : EXPECTED_CLUBS) counts.put(normalize(club), 0);
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        if (Boolean.TRUE.equals(doc.getBoolean("hidden"))) continue;
                        String club = first(doc, "clubName", "club", "teamName");
                        String key = normalize(club);
                        if (counts.containsKey(key)) counts.put(key, counts.get(key) + 1);
                    }
                    for (String club : EXPECTED_CLUBS) {
                        int count = counts.containsKey(normalize(club)) ? counts.get(normalize(club)) : 0;
                        addInfo(club + "\n" + count + " sản phẩm");
                    }
                });
    }

    private String first(DocumentSnapshot doc, String... keys) {
        if (doc == null) return "";
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof String && !((String) value).trim().isEmpty()) return ((String) value).trim();
            if (value instanceof Number) return String.valueOf(((Number) value).longValue());
        }
        return "";
    }

    private String customerName(DocumentSnapshot doc) {
        String direct = first(doc, "recipientName", "receiverName", "customerName", "fullName", "displayName", "username", "name", "userEmail", "email");
        if (!direct.isEmpty()) return direct;
        return nestedFirst(doc, "billingInfo", "fullName", "name", "customerName", "email");
    }

    private String customerPhone(DocumentSnapshot doc) {
        String direct = first(doc, "recipientPhone", "receiverPhone", "customerPhone", "phone", "phoneNumber");
        if (!direct.isEmpty()) return direct;
        return nestedFirst(doc, "billingInfo", "phone", "phoneNumber");
    }

    private String customerAddress(DocumentSnapshot doc) {
        String direct = first(doc, "shippingAddress", "recipientAddress", "receiverAddress", "customerAddress", "address", "addressLine");
        if (!direct.isEmpty()) return direct;
        String billing = nestedFirst(doc, "billingInfo", "shippingAddress", "address", "addressLine");
        if (!billing.isEmpty()) return billing;
        if ("ticket".equalsIgnoreCase(first(doc, "type"))) return joinAddress(first(doc, "stadium"), first(doc, "city"));
        return joinAddress(first(doc, "street"), first(doc, "ward"), first(doc, "district"), first(doc, "city", "province"));
    }

    private String nestedFirst(DocumentSnapshot doc, String mapField, String... keys) {
        Object raw = doc == null ? null : doc.get(mapField);
        if (!(raw instanceof Map<?, ?>)) return "";
        Map<?, ?> map = (Map<?, ?>) raw;
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof String && !((String) value).trim().isEmpty()) return ((String) value).trim();
            if (value instanceof Number) return String.valueOf(((Number) value).longValue());
        }
        return "";
    }

    private String joinAddress(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append(", ");
            builder.append(part.trim());
        }
        return builder.toString();
    }

    private int orderTotal(DocumentSnapshot doc) {
        int total = money(doc, "totalAmount", "grandTotal", "total", "finalTotal", "totalPrice", "payableAmount", "amount");
        if (total <= 0) total = money(doc, "subtotal") + money(doc, "shippingFee", "shipping") + money(doc, "serviceFee");
        return total;
    }

    private int money(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof String) {
                int parsed = parseInt((String) value);
                if (parsed > 0) return parsed;
            }
        }
        return 0;
    }

    private String notificationTarget(DocumentSnapshot doc) {
        String userId = first(doc, "userId", "targetUserId");
        if (!userId.isEmpty()) return "UID: " + userId;
        return displayValue(safe(first(doc, "targetRole", "target"), "all"));
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String displayValue(String value) {
        String normalized = LegacyFirestoreCleanup.normalizeOrderStatus(value).toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "admin": return "Quản trị";
            case "user": return "Người dùng";
            case "active": return "Đang hoạt động";
            case "disabled": return "Đã khóa";
            case "pending": return "Chờ xử lý";
            case "pending_payment": return "Chờ xác nhận thanh toán";
            case "confirmed": return "Đã xác nhận";
            case "shipping": return "Đang giao";
            case "completed": return "Hoàn tất";
            case "cancelled": return "Đã hủy";
            case "all": return "Tất cả";
            case "bank_transfer": return "Chuyển khoản";
            case "cash_on_delivery": return "Thanh toán khi nhận hàng";
            default: return value == null || value.trim().isEmpty() ? "-" : value.trim();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace("đ", "d")
                .replaceAll("[^a-z0-9]+", "-");
    }

    private String text(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void navigateBack() {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new AdminDashboardFragment()).commit();
    }
}
