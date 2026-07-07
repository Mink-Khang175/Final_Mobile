package com.finalproject.v_league_ticket.presentation.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.FragmentOrderDetailBinding;
import com.finalproject.v_league_ticket.presentation.shop.CartStore;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailFragment extends Fragment {
    public static final String ARG_ORDER_ID = "order_id";
    private FragmentOrderDetailBinding binding;

    public OrderDetailFragment() {
        super(R.layout.fragment_order_detail);
    }

    public static OrderDetailFragment newInstance(String orderId) {
        OrderDetailFragment fragment = new OrderDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_ID, orderId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentOrderDetailBinding.bind(view);
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        String orderId = requireArguments().getString(ARG_ORDER_ID, "ORD");
        loadOrder(orderId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void addText(android.widget.LinearLayout container, String text) {
        container.removeAllViews();
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextColor(requireContext().getColor(R.color.stadium_ink));
        view.setPadding(0, 8, 0, 8);
        container.addView(view);
    }

    private void loadOrder(String orderId) {
        binding.progressOrder.setVisibility(View.VISIBLE);
        binding.tvOrderError.setVisibility(View.GONE);
        binding.contentOrder.setVisibility(View.GONE);
        FirebaseFirestore.getInstance().collection("orders").document(orderId).get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    binding.progressOrder.setVisibility(View.GONE);
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        binding.tvOrderError.setVisibility(View.VISIBLE);
                        binding.tvOrderError.setText("Không tìm thấy đơn hàng " + orderId);
                        return;
                    }
                    LegacyFirestoreCleanup.normalizeOrderDocument("orders", task.getResult());
                    bindOrder(task.getResult());
                });
    }

    private void bindOrder(DocumentSnapshot doc) {
        binding.tvOrderError.setVisibility(View.GONE);
        binding.contentOrder.setVisibility(View.VISIBLE);
        String orderCode = safe(first(doc, "orderCode", "orderId"), doc.getId());
        String status = safe(LegacyFirestoreCleanup.normalizeOrderStatus(first(doc, "status", "orderStatus")), "pending");
        int subtotal = money(doc, "subtotal");
        int shipping = money(doc, "shippingFee", "shipping");
        int serviceFee = money(doc, "serviceFee");
        int total = orderTotal(doc);
        binding.tvOrderStatus.setText(displayStatus(status));
        binding.tvOrderCode.setText("Đơn #" + orderCode);
        binding.tvOrderDate.setText("Đặt lúc " + dateText(doc));
        binding.tvOrderTotal.setText(CartStore.formatVnd(total));
        binding.tvCustomerName.setText(safe(customerName(doc), "Chưa có tên người nhận"));
        binding.tvCustomerPhone.setText("Số điện thoại: " + safe(customerPhone(doc), "-"));
        binding.tvCustomerAddress.setText("Địa chỉ: " + safe(customerAddress(doc), "-"));
        binding.tvPaymentMethod.setText("Phương thức: " + displayPayment(doc));
        binding.tvSubtotal.setText("Tạm tính: " + CartStore.formatVnd(subtotal));
        binding.tvShippingFee.setText(feeLabel(doc) + ": " + CartStore.formatVnd(shipping + serviceFee));
        binding.tvFinalTotal.setText("Tổng thanh toán: " + CartStore.formatVnd(total));
        bindTimeline(status);
        bindItems(doc);
    }

    private void bindItems(DocumentSnapshot doc) {
        binding.layoutItems.removeAllViews();
        Object raw = doc.get("items");
        if (!(raw instanceof List<?>)) {
            addRow(binding.layoutItems, "Chưa có chi tiết sản phẩm", "", false);
            return;
        }
        int index = 1;
        for (Object row : (List<?>) raw) {
            if (!(row instanceof Map<?, ?>)) continue;
            Map<?, ?> map = (Map<?, ?>) row;
            String name = safe(String.valueOf(map.get("name")), "Sản phẩm " + index);
            String seat = safe(String.valueOf(map.get("seatNumber")), "");
            String size = safe(String.valueOf(map.get("size")), "");
            String quantity = safe(String.valueOf(map.get("quantity")), "1");
            String detail = !seat.isEmpty()
                    ? "Ghế " + seat + " · " + CartStore.formatVnd(intValue(map.get("unitPrice")))
                    : (!size.isEmpty() && !"-".equals(size) ? "Size " + size + " · SL " + quantity : "Số lượng " + quantity);
            addRow(binding.layoutItems, name, detail, index > 1);
            index++;
        }
        if (index == 1) addRow(binding.layoutItems, "Chưa có chi tiết sản phẩm", "", false);
    }

    private void bindTimeline(String status) {
        binding.layoutTimeline.removeAllViews();
        String normalized = LegacyFirestoreCleanup.normalizeOrderStatus(status).toLowerCase(Locale.ROOT);
        addStep("Chờ xác nhận thanh toán", isReached(normalized, "pending_payment"), true);
        addStep("Đã xác nhận", isReached(normalized, "confirmed"), true);
        addStep("Đang giao / chờ soát vé", isReached(normalized, "shipping"), true);
        addStep("Hoàn tất", isReached(normalized, "completed"), false);
    }

    private void addStep(String title, boolean active, boolean showDivider) {
        int index = binding.layoutTimeline.getChildCount() + 1;
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView marker = new TextView(requireContext());
        marker.setText(String.valueOf(index));
        marker.setGravity(android.view.Gravity.CENTER);
        marker.setTextSize(12f);
        marker.setTypeface(marker.getTypeface(), android.graphics.Typeface.BOLD);
        marker.setTextColor(requireContext().getColor(active ? R.color.white : R.color.slate_caption));
        marker.setBackgroundResource(active ? R.drawable.bg_order_step_active : R.drawable.bg_order_step_inactive);
        row.addView(marker, new LinearLayout.LayoutParams(dp(30), dp(30)));

        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(12), 0, 0, 0);
        TextView primary = new TextView(requireContext());
        primary.setText(title);
        primary.setTextColor(requireContext().getColor(active ? R.color.stadium_ink : R.color.slate_caption));
        primary.setTextSize(14f);
        primary.setTypeface(primary.getTypeface(), active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        texts.addView(primary);
        TextView secondary = new TextView(requireContext());
        secondary.setText(active ? "Đã đạt bước này" : "Đang chờ cập nhật");
        secondary.setTextColor(requireContext().getColor(R.color.slate_caption));
        secondary.setTextSize(12f);
        secondary.setPadding(0, dp(2), 0, 0);
        texts.addView(secondary);
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        binding.layoutTimeline.addView(row);
        if (showDivider) addThinDivider(binding.layoutTimeline, dp(12), dp(15));
    }

    private boolean isReached(String status, String step) {
        int current = statusIndex(status);
        int wanted = statusIndex(step);
        return current >= wanted && wanted >= 0;
    }

    private int statusIndex(String status) {
        switch (status) {
            case "pending":
            case "pending_payment": return 0;
            case "confirmed": return 1;
            case "shipping": return 2;
            case "completed": return 3;
            default: return -1;
        }
    }

    private void addRow(LinearLayout container, String title, String subtitle, boolean divider) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_order_detail_item);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, divider ? dp(8) : 0, 0, 0);
        TextView primary = new TextView(requireContext());
        primary.setText(title);
        primary.setTextColor(requireContext().getColor(R.color.stadium_ink));
        primary.setTextSize(14f);
        primary.setTypeface(primary.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(primary);
        if (!subtitle.isEmpty()) {
            TextView secondary = new TextView(requireContext());
            secondary.setText(subtitle);
            secondary.setTextColor(requireContext().getColor(R.color.slate_caption));
            secondary.setTextSize(13f);
            secondary.setPadding(0, dp(4), 0, 0);
            card.addView(secondary);
        }
        container.addView(card, cardParams);
    }

    private void addThinDivider(LinearLayout container, int startMargin, int endMargin) {
        View line = new View(requireContext());
        line.setBackgroundColor(requireContext().getColor(R.color.colorOutline));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(1), dp(14));
        params.setMargins(startMargin, 0, endMargin, 0);
        container.addView(line, params);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String displayPayment(DocumentSnapshot doc) {
        String method = first(doc, "paymentMethod");
        String provider = first(doc, "paymentProvider");
        if ("cash_on_delivery".equals(method)) return "Thanh toán khi nhận hàng";
        if ("momo".equalsIgnoreCase(provider)) return "Chuyển khoản MoMo";
        if ("vietinbank".equalsIgnoreCase(provider)) return "Chuyển khoản VietinBank";
        if ("bank_transfer".equals(method)) return "Chuyển khoản";
        return safe(method, "Chưa cập nhật");
    }

    private String feeLabel(DocumentSnapshot doc) {
        return "ticket".equalsIgnoreCase(first(doc, "type")) ? "Phí dịch vụ" : "Phí vận chuyển";
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

    private int orderTotal(DocumentSnapshot doc) {
        int total = money(doc, "totalAmount", "grandTotal", "total", "finalTotal", "amount", "payableAmount", "totalPrice");
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

    private String customerName(DocumentSnapshot doc) {
        String direct = first(doc, "recipientName", "receiverName", "customerName", "fullName", "displayName", "username", "name", "buyerName");
        if (!direct.isEmpty()) return direct;
        return nestedFirst(doc, "billingInfo", "recipient", "receiver", "shippingInfo", "customer", "name", "fullName", "displayName", "username", "buyerName");
    }

    private String customerPhone(DocumentSnapshot doc) {
        String direct = first(doc, "recipientPhone", "receiverPhone", "customerPhone", "phone", "phoneNumber", "buyerPhone");
        if (!direct.isEmpty()) return direct;
        return nestedFirst(doc, "billingInfo", "recipient", "receiver", "shippingInfo", "customer", "phone", "phoneNumber", "customerPhone");
    }

    private String customerAddress(DocumentSnapshot doc) {
        String direct = first(doc, "shippingAddress", "recipientAddress", "receiverAddress", "customerAddress", "address", "addressLine");
        String nested = nestedFirst(doc, "billingInfo", "recipient", "receiver", "shippingInfo", "customer", "shippingAddress", "address", "addressLine");
        if (looksBrokenText(direct) && !nested.isEmpty()) return nested;
        if (!direct.isEmpty()) return direct;
        if (!nested.isEmpty()) return nested;
        if ("ticket".equalsIgnoreCase(first(doc, "type"))) {
            String venue = joinAddress(first(doc, "stadium"), first(doc, "city"), "", "");
            if (!venue.isEmpty()) return venue;
        }
        return joinAddress(first(doc, "street"), first(doc, "ward"), first(doc, "district"), first(doc, "city", "province"));
    }

    private boolean looksBrokenText(String value) {
        return value != null && value.contains("?") && value.matches(".*[A-Za-z].*");
    }

    private String nestedFirst(DocumentSnapshot doc, String field, String altField, String thirdField,
                               String fourthField, String fifthField, String... keys) {
        String value = nestedFirst(doc.get(field), keys);
        if (!value.isEmpty()) return value;
        value = nestedFirst(doc.get(altField), keys);
        if (!value.isEmpty()) return value;
        value = nestedFirst(doc.get(thirdField), keys);
        if (!value.isEmpty()) return value;
        value = nestedFirst(doc.get(fourthField), keys);
        if (!value.isEmpty()) return value;
        return nestedFirst(doc.get(fifthField), keys);
    }

    private String nestedFirst(Object raw, String... keys) {
        if (!(raw instanceof Map<?, ?>)) return "";
        Map<?, ?> map = (Map<?, ?>) raw;
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof String && !((String) value).trim().isEmpty()) return ((String) value).trim();
            if (value instanceof Number) return String.valueOf(((Number) value).longValue());
        }
        return "";
    }

    private Object firstObject(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) return value;
        }
        return null;
    }

    private String joinAddress(String street, String ward, String district, String city) {
        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, street);
        appendAddressPart(builder, ward);
        appendAddressPart(builder, district);
        appendAddressPart(builder, city);
        return builder.toString();
    }

    private void appendAddressPart(StringBuilder builder, String value) {
        value = safe(value, "");
        if (value.isEmpty()) return;
        if (builder.length() > 0) builder.append(", ");
        builder.append(value);
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) return fallback;
        return value.trim();
    }

    private String displayStatus(String value) {
        String normalized = LegacyFirestoreCleanup.normalizeOrderStatus(value).toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "pending":
            case "pending_payment": return "Chờ xác nhận thanh toán";
            case "confirmed": return "Đã xác nhận";
            case "shipping": return "Đang giao";
            case "completed": return "Hoàn tất";
            case "cancelled": return "Đã hủy";
            default: return value == null || value.trim().isEmpty() ? "-" : value.trim();
        }
    }

    private String dateText(DocumentSnapshot doc) {
        Object value = doc.get("createdAt");
        if (!(value instanceof Timestamp)) value = doc.get("updatedAt");
        Date date = value instanceof Timestamp ? ((Timestamp) value).toDate() : null;
        return date == null ? "Chưa có ngày" : new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(date);
    }
}
