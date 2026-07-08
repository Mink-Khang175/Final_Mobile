package com.finalproject.v_league_ticket.presentation.shop;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.FragmentCheckoutBinding;
import com.finalproject.v_league_ticket.presentation.admin.AdminSummaryStore;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.profile.UserCheckoutInfo;
import com.finalproject.v_league_ticket.presentation.profile.UserEngagementManager;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutFragment extends Fragment {
    private static final int SHIPPING_FEE = 30000;
    private FragmentCheckoutBinding binding;

    public CheckoutFragment() {
        super(R.layout.fragment_checkout);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentCheckoutBinding.bind(view);
        bindSummary();
        binding.rbCashOnDelivery.setChecked(true);
        binding.rbVietinBank.setChecked(true);
        setupPaymentMethods();
        binding.btnPlaceOrder.setOnClickListener(v -> placeOrder());
        String name = AuthSession.userName(requireContext());
        if (name != null) binding.edtFullName.setText(name);
        loadSavedCheckoutInfo();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void bindSummary() {
        int subtotal = CartStore.subtotal();
        binding.tvSubtotal.setText(CartStore.formatVnd(subtotal));
        binding.tvShippingFee.setText(CartStore.formatVnd(SHIPPING_FEE));
        binding.tvFinalTotal.setText(CartStore.formatVnd(subtotal + SHIPPING_FEE));
    }

    private void setupPaymentMethods() {
        binding.rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            boolean online = checkedId == R.id.rbBankTransfer;
            binding.layoutOnlinePayment.setVisibility(online ? View.VISIBLE : View.GONE);
            if (online && !binding.rbVietinBank.isChecked() && !binding.rbMomo.isChecked()) {
                binding.rbVietinBank.setChecked(true);
            }
        });
        binding.rgOnlinePayment.setOnCheckedChangeListener((group, checkedId) ->
                binding.imgPaymentQr.setImageResource(checkedId == R.id.rbMomo ? R.drawable.qr_momo : R.drawable.qr_vietinbank));
    }

    private void placeOrder() {
        if (!AuthSession.hasToken(requireContext())) {
            toast("Vui lòng đăng nhập trước khi thanh toán.");
            navigateTo(new AuthLoginFragment());
            return;
        }
        if (CartStore.items().isEmpty()) {
            toast("Giỏ hàng đang trống.");
            return;
        }
        if (text(binding.edtFullName.getText()).isEmpty()
                || text(binding.edtPhoneNumber.getText()).isEmpty()
                || text(binding.edtDetailedAddress.getText()).isEmpty()) {
            toast("Vui lòng nhập đầy đủ thông tin giao hàng.");
            return;
        }
        String orderId = "ORD" + System.currentTimeMillis();
        Map<String, Object> orderData = orderData(orderId);
        rememberCheckoutInfo();
        binding.btnPlaceOrder.setEnabled(false);
        binding.btnPlaceOrder.setText("ĐANG ĐẶT HÀNG...");
        FirebaseFirestore.getInstance().collection("orders").document(orderId)
                .set(orderData)
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    binding.btnPlaceOrder.setEnabled(true);
                    binding.btnPlaceOrder.setText("ĐẶT HÀNG");
                    if (!task.isSuccessful()) {
                        toast("Không lưu được đơn hàng. Vui lòng thử lại.");
                        return;
                    }
                    AdminSummaryStore.recordOrderCreated(orderData);
                    String uid = AuthSession.uid(requireContext());
                    UserEngagementManager.awardShopPurchase(uid, orderId, CartStore.subtotal() + SHIPPING_FEE);
                    UserEngagementManager.notifyUser(uid,
                            "Đặt hàng thành công",
                            "Đơn hàng #" + orderId + " đã được ghi nhận và đang chờ xử lý.",
                            "order_success",
                            orderId);
                    CartStore.clear();
                    navigateTo(OrderSuccessFragment.newInstance(orderId));
                });
    }

    private Map<String, Object> orderData(String orderId) {
        int subtotal = CartStore.subtotal();
        int total = subtotal + SHIPPING_FEE;
        String name = text(binding.edtFullName.getText());
        String phone = text(binding.edtPhoneNumber.getText());
        String address = text(binding.edtDetailedAddress.getText());
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("orderCode", orderId);
        data.put("type", "shop");
        data.put("userId", AuthSession.uid(requireContext()));
        data.put("userEmail", AuthSession.email(requireContext()));
        data.put("customerName", name);
        data.put("recipientName", name);
        data.put("receiverName", name);
        data.put("phone", phone);
        data.put("customerPhone", phone);
        data.put("recipientPhone", phone);
        data.put("receiverPhone", phone);
        data.put("address", address);
        data.put("shippingAddress", address);
        data.put("customerAddress", address);
        data.put("recipientAddress", address);
        data.put("receiverAddress", address);
        data.put("subtotal", subtotal);
        data.put("shippingFee", SHIPPING_FEE);
        data.put("totalAmount", total);
        data.put("grandTotal", total);
        data.put("total", total);
        data.put("status", "pending_payment");
        boolean bankTransfer = binding.rbBankTransfer.isChecked();
        data.put("paymentMethod", bankTransfer ? "bank_transfer" : "cash_on_delivery");
        data.put("paymentProvider", bankTransfer ? (binding.rbMomo.isChecked() ? "momo" : "vietinbank") : "");
        data.put("items", orderItems());
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private void loadSavedCheckoutInfo() {
        UserCheckoutInfo.load(requireContext(), info -> {
            if (binding == null) return;
            if (!info.name.isEmpty()) binding.edtFullName.setText(info.name);
            if (!info.phone.isEmpty()) binding.edtPhoneNumber.setText(info.phone);
            if (!info.address.isEmpty()) binding.edtDetailedAddress.setText(info.address);
            if (info.prefersBankTransfer()) {
                binding.rbBankTransfer.setChecked(true);
                binding.layoutOnlinePayment.setVisibility(View.VISIBLE);
                if (info.prefersMomo()) {
                    binding.rbMomo.setChecked(true);
                    binding.imgPaymentQr.setImageResource(R.drawable.qr_momo);
                } else {
                    binding.rbVietinBank.setChecked(true);
                    binding.imgPaymentQr.setImageResource(R.drawable.qr_vietinbank);
                }
            } else if ("cash_on_delivery".equals(info.paymentMethod)) {
                binding.rbCashOnDelivery.setChecked(true);
                binding.layoutOnlinePayment.setVisibility(View.GONE);
            }
        });
    }

    private void rememberCheckoutInfo() {
        boolean bankTransfer = binding.rbBankTransfer.isChecked();
        UserCheckoutInfo.save(requireContext(),
                text(binding.edtFullName.getText()),
                text(binding.edtPhoneNumber.getText()),
                AuthSession.email(requireContext()),
                text(binding.edtDetailedAddress.getText()),
                bankTransfer ? "bank_transfer" : "cash_on_delivery",
                bankTransfer ? (binding.rbMomo.isChecked() ? "momo" : "vietinbank") : "");
    }

    private List<Map<String, Object>> orderItems() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (CartItem item : CartStore.items()) {
            Map<String, Object> row = new HashMap<>();
            row.put("productId", item.getProductId());
            row.put("name", item.getProductName());
            row.put("priceText", item.getPriceText());
            row.put("unitPrice", item.getUnitPrice());
            row.put("quantity", item.getQuantity());
            row.put("size", item.getSelectedSize());
            row.put("imageUrl", item.getImageUrl());
            row.put("productUrl", item.getProductUrl());
            row.put("lineTotal", item.getLineTotal());
            items.add(row);
        }
        return items;
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName()).commit();
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
