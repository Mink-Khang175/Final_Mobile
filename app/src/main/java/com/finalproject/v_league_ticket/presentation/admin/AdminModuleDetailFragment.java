package com.finalproject.v_league_ticket.presentation.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.FragmentAdminModuleDetailBinding;
import com.finalproject.v_league_ticket.databinding.ItemAdminDocumentBinding;
import com.finalproject.v_league_ticket.presentation.profile.LegacyFirestoreCleanup;
import com.finalproject.v_league_ticket.presentation.profile.UserEngagementManager;
import com.finalproject.v_league_ticket.presentation.shop.CartStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AdminModuleDetailFragment extends Fragment {
    private static final String ARG_MODULE_TYPE = "module_type";
    public static final String MODULE_USERS = "users";
    public static final String MODULE_ORDERS = "orders";
    public static final String MODULE_DELIVERY = "delivery";
    public static final String MODULE_PRODUCTS = "products";
    public static final String MODULE_MISSING_PRODUCTS = "missing_products";
    public static final String MODULE_BANNERS = "banners";
    public static final String MODULE_NOTIFICATIONS = "notifications";
    public static final String MODULE_SETTINGS = "settings";
    public static final String MODULE_SYNC = "sync";
    private static final int ADMIN_PAGE_SIZE = 30;

    private static final String[] EXPECTED_CLUBS = new String[]{
            "Cong An Ha Noi", "Ninh Binh", "Hong Linh Ha Tinh", "Dong A Thanh Hoa",
            "Ha Noi FC", "The Cong Viettel", "Nam Dinh", "Hai Phong",
            "Hoang Anh Gia Lai", "Song Lam Nghe An", "Becamex Binh Duong", "Becamex TP.HCM",
            "SHB Da Nang", "Quang Nam", "PVF-CAND", "Bac Ninh FC"
    };

    private FragmentAdminModuleDetailBinding binding;
    private String currentModule = MODULE_USERS;
    private final AdminDocumentAdapter adapter = new AdminDocumentAdapter();
    private final List<AdminDocumentItem> loadedRows = new ArrayList<>();
    private DocumentSnapshot lastVisibleDoc;
    private String currentSearchQuery = "";
    private boolean hasMoreRows;
    private boolean loadingPage;

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
        binding.btnLoadMore.setOnClickListener(v -> loadAdminPage(false));
        binding.edtAdminSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                applySearchFilter();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        binding.rvContent.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvContent.setAdapter(adapter);
        binding.rvContent.setHasFixedSize(true);
        binding.rvContent.setItemViewCacheSize(12);
        binding.rvContent.setItemAnimator(null);
        render(currentModule);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) binding.rvContent.setAdapter(null);
        adapter.submitList(new ArrayList<>());
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
        resetPagingState();
        showState("\u0110ang t\u1ea3i d\u1eef li\u1ec7u...");
        if (MODULE_MISSING_PRODUCTS.equals(module)) {
            renderMissingProducts();
            return;
        }
        loadAdminPage(true);
    }

    private void resetPagingState() {
        loadedRows.clear();
        lastVisibleDoc = null;
        hasMoreRows = false;
        loadingPage = false;
        currentSearchQuery = "";
        if (binding != null) binding.edtAdminSearch.setText("");
        if (binding != null) binding.btnLoadMore.setVisibility(View.GONE);
    }

    private void loadAdminPage(boolean firstPage) {
        if (binding == null || loadingPage) return;
        if (firstPage) resetPagingState();
        String module = currentModule;
        String collection = collectionFor(module);
        Query query = FirebaseFirestore.getInstance().collection(collection)
                .orderBy(FieldPath.documentId())
                .limit(ADMIN_PAGE_SIZE);
        if (!firstPage && lastVisibleDoc != null) {
            query = query.startAfter(lastVisibleDoc);
        }
        loadingPage = true;
        updateLoadMoreButton();
        query.get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    loadingPage = false;
                    if (!task.isSuccessful() || task.getResult() == null) {
                        if (loadedRows.isEmpty()) {
                            showState("Kh\u00f4ng \u0111\u1ecdc \u0111\u01b0\u1ee3c d\u1eef li\u1ec7u. Vui l\u00f2ng th\u1eed l\u1ea1i.");
                        } else {
                            toast("Kh\u00f4ng t\u1ea3i th\u00eam \u0111\u01b0\u1ee3c d\u1eef li\u1ec7u.");
                            updateLoadMoreButton();
                        }
                        return;
                    }
                    if (task.getResult().isEmpty()) {
                        hasMoreRows = false;
                        if (loadedRows.isEmpty()) showState(emptyText(module));
                        else updateLoadMoreButton();
                        return;
                    }
                    int rawCount = task.getResult().size();
                    List<DocumentSnapshot> docs = task.getResult().getDocuments();
                    lastVisibleDoc = docs.get(docs.size() - 1);
                    hasMoreRows = rawCount == ADMIN_PAGE_SIZE;
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        if ("_schema".equals(doc.getId())) continue;
                        if (MODULE_DELIVERY.equals(module) && "ticket".equalsIgnoreCase(first(doc, "type"))) continue;
                        if (MODULE_DELIVERY.equals(module) && !isDeliveryManageable(doc)) continue;
                        loadedRows.add(documentItem(module, doc));
                    }
                    if (loadedRows.isEmpty() && hasMoreRows) {
                        updateLoadMoreButton();
                        loadAdminPage(false);
                        return;
                    }
                    showRows(loadedRows, emptyText(module));
                });
    }

    private AdminDocumentItem documentItem(String module, DocumentSnapshot doc) {
        if (MODULE_ORDERS.equals(module) || MODULE_DELIVERY.equals(module)) LegacyFirestoreCleanup.normalizeOrderDocument("orders", doc);
        return new AdminDocumentItem(module, doc.getId(), docTitle(module, doc), docBody(module, doc), doc);
    }

    private void addActions(String module, DocumentSnapshot doc, LinearLayout actions) {
        if (MODULE_PRODUCTS.equals(module)) {
            addSmallButton(actions, "S\u1eeda", v -> showProductDialog(doc));
            addSmallButton(actions, Boolean.TRUE.equals(doc.getBoolean("hidden")) ? "Hi\u1ec7n" : "\u1ea8n",
                    v -> updateDoc("shop_products", doc.getId(), "hidden", !Boolean.TRUE.equals(doc.getBoolean("hidden"))));
            addDangerButton(actions, "X\u00f3a", v -> confirmDelete("shop_products", doc.getId()));
        } else if (MODULE_ORDERS.equals(module)) {
            String status = displayValue(safe(LegacyFirestoreCleanup.normalizeOrderStatus(first(doc, "status", "orderStatus")), "pending"));
            addSmallButton(actions, "\u0110\u1ed5i tr\u1ea1ng th\u00e1i: " + status, v -> showOrderStatusMenu(v, doc));
        } else if (MODULE_DELIVERY.equals(module)) {
            String deliveryStatus = LegacyFirestoreCleanup.normalizeOrderStatus(first(doc, "status", "orderStatus")).toLowerCase(Locale.ROOT);
            addSmallButton(actions, "M\u00e3 v\u1eadn \u0111\u01a1n SPX", v -> showDeliveryDialog(doc));
            if ("confirmed".equals(deliveryStatus)) {
                addSmallButton(actions, "B\u00e0n giao SPX", v -> updateDeliveryStatus(doc, "shipping"));
            } else if ("shipping".equals(deliveryStatus)) {
                addSmallButton(actions, "Ho\u00e0n t\u1ea5t", v -> updateDeliveryStatus(doc, "completed"));
            }
        } else if (MODULE_USERS.equals(module)) {
            addSmallButton(actions, "Vai tr\u00f2", v -> showStatusMenu(v, "users", doc.getId(), new String[]{"user", "admin"}));
            addSmallButton(actions, "Tr\u1ea1ng th\u00e1i", v -> showFieldMenu(v, "users", doc.getId(), "status",
                    new String[]{"active", "disabled"}));
        } else if (MODULE_NOTIFICATIONS.equals(module)) {
            addDangerButton(actions, "X\u00f3a", v -> confirmDelete("notifications", doc.getId()));
        } else if (MODULE_BANNERS.equals(module)) {
            addSmallButton(actions, Boolean.FALSE.equals(doc.getBoolean("enabled")) ? "B\u1eadt" : "T\u1eaft",
                    v -> updateDoc("app_banners", doc.getId(), "enabled", Boolean.FALSE.equals(doc.getBoolean("enabled"))));
            addSmallButton(actions, "S\u1eeda", v -> showBannerDialog(doc));
            addDangerButton(actions, "X\u00f3a", v -> confirmDelete("app_banners", doc.getId()));
        } else if (MODULE_SETTINGS.equals(module)) {
            addSmallButton(actions, "S\u1eeda", v -> showSettingsDialog(doc));
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
            String label = statuses[i].equals(current) ? "\u2713 " + displayValue(statuses[i]) : displayValue(statuses[i]);
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

    private void showDeliveryDialog(DocumentSnapshot doc) {
        LinearLayout form = form();
        TextView carrier = dialogSelect("\u0110\u01a1n v\u1ecb v\u1eadn chuy\u1ec3n: SPX");
        EditText trackingCode = input("M\u00e3 v\u1eadn \u0111\u01a1n", first(doc, "trackingCode", "trackingNumber", "shippingTrackingCode"));
        EditText note = input("Ghi ch\u00fa v\u1eadn chuy\u1ec3n", first(doc, "deliveryNote", "shippingNote"));
        form.addView(carrier);
        form.addView(trackingCode);
        form.addView(note);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("C\u1eadp nh\u1eadt v\u1eadn chuy\u1ec3n")
                .setMessage("Nh\u1eadp m\u00e3 v\u1eadn \u0111\u01a1n SPX cho \u0111\u01a1n " + safe(first(doc, "orderCode", "orderId"), doc.getId()) + ".")
                .setView(form)
                .setNegativeButton("H\u1ee7y", null)
                .setPositiveButton("L\u01b0u", (dialog, which) -> updateDelivery(doc, text(trackingCode), text(note)))
                .show();
    }

    private void showProductDialog(DocumentSnapshot doc) {
        LinearLayout form = form();
        EditText name = input("T\u00ean s\u1ea3n ph\u1ea9m", first(doc, "name", "title", "productName"));
        EditText club = input("CLB", first(doc, "clubName", "club", "teamName"));
        EditText category = input("Danh m\u1ee5c", safe(first(doc, "category"), "\u00c1o \u0111\u1ea5u"));
        EditText price = input("Gi\u00e1 hi\u1ec3n th\u1ecb", first(doc, "priceText", "formattedPrice", "price"));
        EditText image = input("\u0110\u01b0\u1eddng d\u1eabn \u1ea3nh", first(doc, "imageUrl", "image"));
        EditText url = input("\u0110\u01b0\u1eddng d\u1eabn s\u1ea3n ph\u1ea9m", first(doc, "productUrl", "url"));
        CheckBox hidden = new CheckBox(requireContext());
        hidden.setText("\u1ea8n s\u1ea3n ph\u1ea9m kh\u1ecfi shop");
        hidden.setChecked(doc != null && Boolean.TRUE.equals(doc.getBoolean("hidden")));
        form.addView(name); form.addView(club); form.addView(category); form.addView(price); form.addView(image); form.addView(url); form.addView(hidden);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(doc == null ? "Th\u00eam s\u1ea3n ph\u1ea9m" : "S\u1eeda s\u1ea3n ph\u1ea9m")
                .setView(form)
                .setNegativeButton("H\u1ee7y", null)
                .setPositiveButton("L\u01b0u", (dialog, which) -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", text(name));
                    data.put("clubName", text(club));
                    data.put("category", text(category).isEmpty() ? "\u00c1o \u0111\u1ea5u" : text(category));
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
        EditText title = input("Ti\u00eau \u0111\u1ec1", "");
        EditText body = input("N\u1ed9i dung", "");
        EditText targetUser = input("UID ng\u01b0\u1eddi nh\u1eadn (b\u1ecf tr\u1ed1ng n\u1ebfu g\u1eedi broadcast)", "");
        targetUser.setVisibility(View.GONE);
        TextView targetRole = dialogSelect("\u0110\u1ed1i t\u01b0\u1ee3ng: T\u1ea5t c\u1ea3");
        final String[] selectedTarget = new String[]{"all"};
        targetRole.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), targetRole);
            String[] values = new String[]{"all", "user", "admin", "uid"};
            String[] labels = new String[]{"T\u1ea5t c\u1ea3", "Ng\u01b0\u1eddi d\u00f9ng", "Qu\u1ea3n tr\u1ecb vi\u00ean", "UID c\u1ee5 th\u1ec3"};
            for (int i = 0; i < values.length; i++) menu.getMenu().add(0, i, i, labels[i]);
            menu.setOnMenuItemClickListener(item -> {
                selectedTarget[0] = values[item.getItemId()];
                targetRole.setText("\u0110\u1ed1i t\u01b0\u1ee3ng: " + labels[item.getItemId()]);
                targetUser.setVisibility("uid".equals(selectedTarget[0]) ? View.VISIBLE : View.GONE);
                return true;
            });
            menu.show();
        });
        form.addView(title); form.addView(body); form.addView(targetRole); form.addView(targetUser);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("T\u1ea1o th\u00f4ng b\u00e1o")
                .setView(form)
                .setNegativeButton("H\u1ee7y", null)
                .setPositiveButton("G\u1eedi", (dialog, which) -> {
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
        EditText title = input("Ti\u00eau \u0111\u1ec1", first(doc, "title", "name"));
        EditText subtitle = input("M\u00f4 t\u1ea3", first(doc, "subtitle", "description"));
        EditText image = input("\u0110\u01b0\u1eddng d\u1eabn \u1ea3nh", first(doc, "imageUrl", "image"));
        CheckBox enabled = new CheckBox(requireContext());
        enabled.setText("\u0110ang b\u1eadt");
        enabled.setChecked(doc == null || !Boolean.FALSE.equals(doc.getBoolean("enabled")));
        form.addView(title); form.addView(subtitle); form.addView(image); form.addView(enabled);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(doc == null ? "Th\u00eam banner" : "S\u1eeda banner")
                .setView(form)
                .setNegativeButton("H\u1ee7y", null)
                .setPositiveButton("L\u01b0u", (dialog, which) -> {
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
        EditText season = input("M\u00f9a gi\u1ea3i", first(doc, "season", "currentSeason"));
        EditText shipping = input("Ph\u00ed ship", first(doc, "shippingFee"));
        CheckBox ordersEnabled = new CheckBox(requireContext());
        ordersEnabled.setText("Cho ph\u00e9p \u0111\u1eb7t h\u00e0ng");
        ordersEnabled.setChecked(doc == null || !Boolean.FALSE.equals(doc.getBoolean("ordersEnabled")));
        form.addView(season); form.addView(shipping); form.addView(ordersEnabled);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("C\u00e0i \u0111\u1eb7t \u1ee9ng d\u1ee5ng")
                .setView(form)
                .setNegativeButton("H\u1ee7y", null)
                .setPositiveButton("L\u01b0u", (dialog, which) -> {
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
        button.setMinWidth(dp(112));
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(42));
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
        button.setBackgroundResource(R.drawable.bg_admin_action_danger);
        button.setMinWidth(dp(96));
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(42));
        params.setMargins(0, 0, dp(6), 0);
        parent.addView(button, params);
    }

    private void saveDoc(String collection, String id, Map<String, Object> data) {
        FirebaseFirestore.getInstance().collection(collection).document(id)
                .set(data, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) invalidateSummaryFor(collection);
                    toast(task.isSuccessful() ? "\u0110\u00e3 l\u01b0u" : "Kh\u00f4ng l\u01b0u \u0111\u01b0\u1ee3c");
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
                        toast("Kh\u00f4ng c\u1eadp nh\u1eadt \u0111\u01b0\u1ee3c tr\u1ea1ng th\u00e1i");
                        render(currentModule);
                        return;
                    }
                    AdminSummaryStore.invalidateOrders();
                    if ("ticket".equalsIgnoreCase(first(order, "type"))) {
                        db.collection("bookings").document(orderId).set(data, SetOptions.merge());
                        db.collection("ticket_orders").document(orderId).set(data, SetOptions.merge());
                    }
                    String uid = first(order, "userId");
                    if (!uid.isEmpty()) {
                        UserEngagementManager.notifyUser(uid,
                                "C\u1eadp nh\u1eadt \u0111\u01a1n h\u00e0ng",
                                "\u0110\u01a1n " + safe(first(order, "orderCode", "orderId"), orderId)
                                        + " \u0111ang \u1edf tr\u1ea1ng th\u00e1i: " + displayValue(status) + ".",
                                "order_status",
                                orderId);
                    }
                    toast("\u0110\u00e3 c\u1eadp nh\u1eadt tr\u1ea1ng th\u00e1i");
                    render(currentModule);
                });
    }

    private void updateDelivery(DocumentSnapshot order, String trackingCode, String note) {
        if (trackingCode.isEmpty()) {
            toast("Vui l\u00f2ng nh\u1eadp m\u00e3 v\u1eadn \u0111\u01a1n");
            return;
        }
        Map<String, Object> data = deliveryPatch("shipping");
        data.put("trackingCode", trackingCode);
        data.put("trackingNumber", trackingCode);
        data.put("shippingTrackingCode", trackingCode);
        data.put("deliveryNote", note);
        String body = "Order " + safe(first(order, "orderCode", "orderId"), order.getId())
                + " \u0111ang \u0111\u01b0\u1ee3c giao b\u1edfi SPX. M\u00e3 v\u1eadn \u0111\u01a1n: " + trackingCode + ".";
        saveDeliveryPatch(order, data, body, "\u0110\u00e3 c\u1eadp nh\u1eadt v\u1eadn chuy\u1ec3n");
    }

    private void updateDeliveryStatus(DocumentSnapshot order, String status) {
        Map<String, Object> data = deliveryPatch(status);
        String code = first(order, "trackingCode", "trackingNumber", "shippingTrackingCode");
        if ("shipping".equals(status) && code.isEmpty()) {
            showDeliveryDialog(order);
            toast("Nh\u1eadp m\u00e3 v\u1eadn \u0111\u01a1n SPX tr\u01b0\u1edbc khi b\u00e0n giao.");
            return;
        }
        String body = "completed".equals(status)
                ? "\u0110\u01a1n " + safe(first(order, "orderCode", "orderId"), order.getId()) + " \u0111\u00e3 giao th\u00e0nh c\u00f4ng."
                : "Order " + safe(first(order, "orderCode", "orderId"), order.getId())
                + " \u0111ang \u0111\u01b0\u1ee3c SPX giao" + (code.isEmpty() ? "." : ". M\u00e3 v\u1eadn \u0111\u01a1n: " + code + ".");
        saveDeliveryPatch(order, data, body, "\u0110\u00e3 c\u1eadp nh\u1eadt tr\u1ea1ng th\u00e1i giao h\u00e0ng");
    }

    private Map<String, Object> deliveryPatch(String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("deliveryStatus", status);
        data.put("shippingStatus", status);
        data.put("shippingCarrier", "SPX");
        data.put("carrier", "SPX");
        data.put("carrierName", "SPX");
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("deliveryUpdatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private void saveDeliveryPatch(DocumentSnapshot order, Map<String, Object> data, String notificationBody, String toastText) {
        String orderId = order.getId();
        FirebaseFirestore.getInstance().collection("orders").document(orderId).set(data, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        toast("Kh\u00f4ng c\u1eadp nh\u1eadt \u0111\u01b0\u1ee3c v\u1eadn chuy\u1ec3n");
                        render(currentModule);
                        return;
                    }
                    AdminSummaryStore.invalidateOrders();
                    String uid = first(order, "userId");
                    if (!uid.isEmpty()) {
                        UserEngagementManager.notifyUser(uid,
                                "C\u1eadp nh\u1eadt giao h\u00e0ng SPX",
                                notificationBody,
                                "delivery_update",
                                orderId);
                    }
                    toast(toastText);
                    render(currentModule);
                });
    }

    private void confirmDelete(String collection, String id) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("X\u00f3a d\u1eef li\u1ec7u?")
                .setMessage("Thao t\u00e1c n\u00e0y kh\u00f4ng th\u1ec3 ho\u00e0n t\u00e1c.")
                .setNegativeButton("H\u1ee7y", null)
                .setPositiveButton("X\u00f3a", (dialog, which) ->
                        FirebaseFirestore.getInstance().collection(collection).document(id).delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) invalidateSummaryFor(collection);
                                    toast(task.isSuccessful() ? "\u0110\u00e3 x\u00f3a" : "Kh\u00f4ng x\u00f3a \u0111\u01b0\u1ee3c");
                                    render(currentModule);
                                }))
                .show();
    }

    private void invalidateSummaryFor(String collection) {
        if ("orders".equals(collection)) {
            AdminSummaryStore.invalidateOrders();
        } else if ("shop_products".equals(collection)) {
            AdminSummaryStore.invalidateProducts();
        }
    }

    private void showState(String text) {
        adapter.submitList(new ArrayList<>());
        binding.rvContent.setVisibility(View.GONE);
        binding.btnLoadMore.setVisibility(View.GONE);
        binding.tvListState.setText(text);
        binding.tvListState.setVisibility(View.VISIBLE);
    }

    private void showRows(List<AdminDocumentItem> rows, String emptyText) {
        if (rows.isEmpty()) {
            showState(emptyText);
            return;
        }
        applySearchFilter();
    }

    private void applySearchFilter() {
        if (binding == null) return;
        if (loadedRows.isEmpty()) {
            showState(emptyText(currentModule));
            return;
        }
        List<AdminDocumentItem> rows = filteredRows();
        if (rows.isEmpty()) {
            adapter.submitList(new ArrayList<>());
            binding.rvContent.setVisibility(View.GONE);
            binding.btnLoadMore.setVisibility(View.GONE);
            binding.tvListState.setText("Không tìm thấy dữ liệu phù hợp với: " + currentSearchQuery);
            binding.tvListState.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvListState.setVisibility(View.GONE);
        binding.rvContent.setVisibility(View.VISIBLE);
        adapter.submitList(new ArrayList<>(rows));
        updateLoadMoreButton();
    }

    private List<AdminDocumentItem> filteredRows() {
        if (currentSearchQuery.isEmpty()) return new ArrayList<>(loadedRows);
        List<AdminDocumentItem> rows = new ArrayList<>();
        for (AdminDocumentItem item : loadedRows) {
            String haystack = (item.id + " " + item.title + " " + item.body).toLowerCase(Locale.ROOT);
            if (haystack.contains(currentSearchQuery)) rows.add(item);
        }
        return rows;
    }

    private void updateLoadMoreButton() {
        if (binding == null) return;
        boolean visible = !MODULE_MISSING_PRODUCTS.equals(currentModule)
                && !loadedRows.isEmpty()
                && (hasMoreRows || loadingPage);
        binding.btnLoadMore.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.btnLoadMore.setEnabled(!loadingPage);
        binding.btnLoadMore.setText(loadingPage ? "\u0110ANG T\u1ea2I..." : "T\u1ea2I TH\u00caM");
    }

    private String primaryActionText(String module) {
        switch (module) {
            case MODULE_PRODUCTS: return "TH\u00caM S\u1ea2N PH\u1ea8M";
            case MODULE_NOTIFICATIONS: return "T\u1ea0O TH\u00d4NG B\u00c1O";
            case MODULE_BANNERS: return "TH\u00caM BANNER";
            case MODULE_SETTINGS: return "C\u1eacP NH\u1eacT C\u00c0I \u0110\u1eb6T";
            default: return "L\u00c0M M\u1edaI";
        }
    }

    private String title(String module) {
        switch (module) {
            case MODULE_DELIVERY: return "Qu\u1ea3n l\u00fd v\u1eadn chuy\u1ec3n";
            case MODULE_ORDERS: return "Qu\u1ea3n l\u00fd \u0111\u01a1n h\u00e0ng";
            case MODULE_PRODUCTS: return "Qu\u1ea3n l\u00fd s\u1ea3n ph\u1ea9m";
            case MODULE_MISSING_PRODUCTS: return "CLB thi\u1ebfu s\u1ea3n ph\u1ea9m";
            case MODULE_BANNERS: return "Banner / N\u1ed9i dung trang ch\u1ee7";
            case MODULE_NOTIFICATIONS: return "Qu\u1ea3n l\u00fd th\u00f4ng b\u00e1o";
            case MODULE_SETTINGS: return "C\u00e0i \u0111\u1eb7t \u1ee9ng d\u1ee5ng";
            case MODULE_USERS:
            default: return "Qu\u1ea3n l\u00fd ng\u01b0\u1eddi d\u00f9ng";
        }
    }

    private String subtitle(String module) {
        switch (module) {
            case MODULE_USERS: return "Qu\u1ea3n l\u00fd role v\u00e0 tr\u1ea1ng th\u00e1i t\u00e0i kho\u1ea3n.";
            case MODULE_DELIVERY: return "H\u00e0ng ch\u1edd giao sau khi \u0111\u01a1n \u0111\u00e3 x\u00e1c nh\u1eadn: t\u1ea1o m\u00e3 SPX, b\u00e0n giao v\u00e0 c\u1eadp nh\u1eadt cho user.";
            case MODULE_ORDERS: return "Theo d\u00f5i v\u00e0 c\u1eadp nh\u1eadt tr\u1ea1ng th\u00e1i \u0111\u01a1n h\u00e0ng.";
            case MODULE_PRODUCTS: return "Qu\u1ea3n l\u00fd \u00e1o \u0111\u1ea5u, tr\u00e1i b\u00f3ng, v\u1edb b\u00f3ng \u0111\u00e1 v\u00e0 ph\u1ee5 ki\u1ec7n.";
            case MODULE_MISSING_PRODUCTS: return "Theo d\u00f5i CLB thi\u1ebfu s\u1ea3n ph\u1ea9m.";
            case MODULE_BANNERS: return "Qu\u1ea3n l\u00fd banner v\u00e0 n\u1ed9i dung n\u1ed5i b\u1eadt.";
            case MODULE_NOTIFICATIONS: return "T\u1ea1o v\u00e0 qu\u1ea3n l\u00fd th\u00f4ng b\u00e1o ng\u01b0\u1eddi d\u00f9ng.";
            case MODULE_SETTINGS: return "C\u1ea5u h\u00ecnh m\u00f9a gi\u1ea3i, \u0111\u1eb7t h\u00e0ng v\u00e0 ph\u00ed v\u1eadn chuy\u1ec3n.";
            default: return "";
        }
    }

    private String collectionFor(String module) {
        switch (module) {
            case MODULE_DELIVERY: return "orders";
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
            case MODULE_PRODUCTS: return "Ch\u01b0a c\u00f3 s\u1ea3n ph\u1ea9m.";
            case MODULE_DELIVERY: return "Ch\u01b0a c\u00f3 \u0111\u01a1n h\u00e0ng c\u1ea7n v\u1eadn chuy\u1ec3n.";
            case MODULE_ORDERS: return "Ch\u01b0a c\u00f3 \u0111\u01a1n h\u00e0ng.";
            case MODULE_NOTIFICATIONS: return "Ch\u01b0a c\u00f3 th\u00f4ng b\u00e1o.";
            case MODULE_BANNERS: return "Ch\u01b0a c\u00f3 banner.";
            default: return "Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u.";
        }
    }

    private String docTitle(String module, DocumentSnapshot doc) {
        switch (module) {
            case MODULE_USERS: return safe(first(doc, "email", "username", "displayName"), doc.getId());
            case MODULE_DELIVERY: return "SPX - " + safe(first(doc, "orderCode", "orderId", "code"), doc.getId());
            case MODULE_ORDERS: return "\u0110\u01a1n h\u00e0ng " + safe(first(doc, "orderCode", "orderId", "code"), doc.getId());
            case MODULE_PRODUCTS: return safe(first(doc, "name", "title", "productName"), doc.getId());
            case MODULE_BANNERS:
            case MODULE_NOTIFICATIONS: return safe(first(doc, "title", "name"), doc.getId());
            case MODULE_SETTINGS: return "C\u00e0i \u0111\u1eb7t \u1ee9ng d\u1ee5ng";
            default: return doc.getId();
        }
    }

    private String docBody(String module, DocumentSnapshot doc) {
        switch (module) {
            case MODULE_USERS:
                return "Vai tr\u00f2: " + displayValue(safe(first(doc, "role"), "user"))
                        + "\nTr\u1ea1ng th\u00e1i: " + displayValue(safe(first(doc, "status"), "active"))
                        + "\nT\u00ean: " + safe(first(doc, "username", "displayName"), "-");
            case MODULE_ORDERS:
                return "Kh\u00e1ch h\u00e0ng: " + safe(customerName(doc), "-")
                        + "\nS\u0110T: " + safe(customerPhone(doc), "-")
                        + "\n\u0110\u1ecba ch\u1ec9/\u0111i\u1ec3m nh\u1eadn: " + safe(customerAddress(doc), "-")
                        + "\nThanh to\u00e1n: " + displayValue(safe(first(doc, "paymentMethod"), "-"))
                        + "\nT\u1ed5ng ti\u1ec1n: " + CartStore.formatVnd(orderTotal(doc))
                        + "\nTr\u1ea1ng th\u00e1i: " + displayValue(safe(LegacyFirestoreCleanup.normalizeOrderStatus(first(doc, "status", "orderStatus")), "pending"));
            case MODULE_DELIVERY:
                return "Kh\u00e1ch h\u00e0ng: " + safe(customerName(doc), "-")
                        + "\nS\u0110T: " + safe(customerPhone(doc), "-")
                        + "\n\u0110\u1ecba ch\u1ec9: " + safe(customerAddress(doc), "-")
                        + "\nGiai \u0111o\u1ea1n: " + deliveryStage(doc)
                        + "\n\u0110\u01a1n v\u1ecb v\u1eadn chuy\u1ec3n: " + safe(first(doc, "shippingCarrier", "carrier", "carrierName"), "SPX")
                        + "\nM\u00e3 v\u1eadn \u0111\u01a1n: " + safe(first(doc, "trackingCode", "trackingNumber", "shippingTrackingCode"), "Ch\u01b0a nh\u1eadp")
                        + "\nTr\u1ea1ng th\u00e1i giao: " + displayValue(safe(first(doc, "deliveryStatus", "shippingStatus", "status"), "pending"));
            case MODULE_PRODUCTS:
                return "CLB: " + safe(first(doc, "clubName", "club", "teamName"), "-")
                        + "\nDanh m\u1ee5c: " + safe(first(doc, "category"), "\u00c1o \u0111\u1ea5u")
                        + "\nGi\u00e1: " + safe(first(doc, "priceText", "formattedPrice", "price"), "-")
                        + "\nCh\u1ea5t l\u01b0\u1ee3ng: " + productQuality(doc)
                        + "\nHi\u1ec3n th\u1ecb: " + (Boolean.TRUE.equals(doc.getBoolean("hidden")) ? "\u0110ang \u1ea9n" : "\u0110ang hi\u1ec7n");
            case MODULE_BANNERS:
                return "\u0110ang b\u1eadt: " + String.valueOf(!Boolean.FALSE.equals(doc.getBoolean("enabled")))
                        + "\nM\u00f4 t\u1ea3: " + safe(first(doc, "subtitle", "description"), "-");
            case MODULE_NOTIFICATIONS:
                return notificationSummary(doc);
            case MODULE_SETTINGS:
                return "M\u00f9a gi\u1ea3i: " + safe(first(doc, "season", "currentSeason"), "-")
                        + "\nPh\u00ed v\u1eadn chuy\u1ec3n: " + safe(first(doc, "shippingFee"), "0")
                        + "\n\u0110\u1eb7t h\u00e0ng: " + (Boolean.FALSE.equals(doc.getBoolean("ordersEnabled")) ? "\u0110ang t\u1eaft" : "\u0110ang b\u1eadt");
            default:
                return "D\u1eef li\u1ec7u \u0111\u00e3 s\u1eb5n s\u00e0ng.";
        }
    }

    private void renderMissingProducts() {
        FirebaseFirestore.getInstance().collection("shop_products").get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    if (!task.isSuccessful() || task.getResult() == null) {
                        showState("Kh\u00f4ng \u0111\u1ecdc \u0111\u01b0\u1ee3c d\u1eef li\u1ec7u s\u1ea3n ph\u1ea9m.");
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
                    List<AdminDocumentItem> rows = new ArrayList<>();
                    for (String club : EXPECTED_CLUBS) {
                        int count = counts.containsKey(normalize(club)) ? counts.get(normalize(club)) : 0;
                        rows.add(new AdminDocumentItem(MODULE_MISSING_PRODUCTS, normalize(club), club,
                                count + " s\u1ea3n ph\u1ea9m", null));
                    }
                    loadedRows.clear();
                    loadedRows.addAll(rows);
                    showRows(rows, emptyText(MODULE_MISSING_PRODUCTS));
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

    private String productQuality(DocumentSnapshot doc) {
        List<String> issues = new ArrayList<>();
        if (first(doc, "imageUrl", "image").isEmpty()) issues.add("thi\u1ebfu \u1ea3nh");
        if (first(doc, "priceText", "formattedPrice", "price").isEmpty()) issues.add("thi\u1ebfu gi\u00e1");
        if (first(doc, "productUrl", "url").isEmpty()) issues.add("thi\u1ebfu link");
        if (Boolean.TRUE.equals(doc.getBoolean("hidden"))) issues.add("\u0111ang \u1ea9n");
        return issues.isEmpty() ? "\u0110\u1ee7 th\u00f4ng tin c\u01a1 b\u1ea3n" : joinAddress(issues.toArray(new String[0]));
    }

    private String notificationTarget(DocumentSnapshot doc) {
        String userId = first(doc, "userId", "targetUserId");
        if (!userId.isEmpty()) return "UID: " + userId;
        return displayValue(safe(first(doc, "targetRole", "target"), "all"));
    }

    private String notificationSummary(DocumentSnapshot doc) {
        String body = safe(first(doc, "body", "message", "content"), "-");
        String type = safe(first(doc, "type", "category"), "notification");
        String refId = first(doc, "refId", "referenceId", "orderId");
        String summary = "N\u1ed9i dung: " + body
                + "\n\u0110\u1ed1i t\u01b0\u1ee3ng: " + notificationTarget(doc)
                + "\nLo\u1ea1i: " + displayValue(type);
        return refId.isEmpty() ? summary : summary + "\nLi\u00ean k\u1ebft: " + refId;
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String displayValue(String value) {
        String normalized = LegacyFirestoreCleanup.normalizeOrderStatus(value).toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "admin": return "Qu\u1ea3n tr\u1ecb";
            case "user": return "Ng\u01b0\u1eddi d\u00f9ng";
            case "active": return "\u0110ang ho\u1ea1t \u0111\u1ed9ng";
            case "disabled": return "\u0110\u00e3 kh\u00f3a";
            case "pending": return "Ch\u1edd x\u1eed l\u00fd";
            case "pending_payment": return "Ch\u1edd x\u00e1c nh\u1eadn thanh to\u00e1n";
            case "confirmed": return "\u0110\u00e3 x\u00e1c nh\u1eadn";
            case "shipping": return "\u0110ang giao";
            case "completed": return "Ho\u00e0n t\u1ea5t";
            case "cancelled": return "\u0110\u00e3 h\u1ee7y";
            case "all": return "T\u1ea5t c\u1ea3";
            case "bank_transfer": return "Chuy\u1ec3n kho\u1ea3n";
            case "cash_on_delivery": return "Thanh to\u00e1n khi nh\u1eadn h\u00e0ng";
            default: return value == null || value.trim().isEmpty() ? "-" : value.trim();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace("\u0111", "d")
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

    private final class AdminDocumentAdapter extends ListAdapter<AdminDocumentItem, AdminDocumentViewHolder> {
        AdminDocumentAdapter() {
            super(new DiffUtil.ItemCallback<AdminDocumentItem>() {
                @Override
                public boolean areItemsTheSame(AdminDocumentItem oldItem, AdminDocumentItem newItem) {
                    return oldItem.module.equals(newItem.module) && oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(AdminDocumentItem oldItem, AdminDocumentItem newItem) {
                    return oldItem.equals(newItem);
                }
            });
        }

        @Override
        public AdminDocumentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new AdminDocumentViewHolder(ItemAdminDocumentBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(AdminDocumentViewHolder holder, int position) {
            holder.bind(getItem(position));
        }
    }

    private boolean isDeliveryManageable(DocumentSnapshot doc) {
        String status = LegacyFirestoreCleanup.normalizeOrderStatus(first(doc, "status", "orderStatus")).toLowerCase(Locale.ROOT);
        return "confirmed".equals(status) || "shipping".equals(status);
    }

    private String deliveryStage(DocumentSnapshot doc) {
        String status = LegacyFirestoreCleanup.normalizeOrderStatus(first(doc, "status", "orderStatus")).toLowerCase(Locale.ROOT);
        if ("confirmed".equals(status)) {
            return first(doc, "trackingCode", "trackingNumber", "shippingTrackingCode").isEmpty()
                    ? "Ch\u1edd t\u1ea1o m\u00e3 v\u1eadn \u0111\u01a1n"
                    : "Ch\u1edd b\u00e0n giao SPX";
        }
        if ("shipping".equals(status)) return "\u0110ang giao";
        return displayValue(status);
    }

    private final class AdminDocumentViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminDocumentBinding itemBinding;

        AdminDocumentViewHolder(ItemAdminDocumentBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        void bind(AdminDocumentItem item) {
            itemBinding.tvDocumentTitle.setText(item.title);
            itemBinding.tvDocumentBody.setText(item.body);
            itemBinding.layoutActions.removeAllViews();
            if (item.document != null) {
                addActions(item.module, item.document, itemBinding.layoutActions);
            }
            itemBinding.scrollActions.setVisibility(
                    itemBinding.layoutActions.getChildCount() == 0 ? View.GONE : View.VISIBLE);
        }
    }

    private static final class AdminDocumentItem {
        final String module;
        final String id;
        final String title;
        final String body;
        final DocumentSnapshot document;

        AdminDocumentItem(String module, String id, String title, String body, DocumentSnapshot document) {
            this.module = module;
            this.id = id;
            this.title = title;
            this.body = body;
            this.document = document;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof AdminDocumentItem)) return false;
            AdminDocumentItem other = (AdminDocumentItem) obj;
            return Objects.equals(module, other.module)
                    && Objects.equals(id, other.id)
                    && Objects.equals(title, other.title)
                    && Objects.equals(body, other.body);
        }

        @Override
        public int hashCode() {
            return Objects.hash(module, id, title, body);
        }
    }

    private void navigateBack() {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new AdminDashboardFragment()).commit();
    }
}

