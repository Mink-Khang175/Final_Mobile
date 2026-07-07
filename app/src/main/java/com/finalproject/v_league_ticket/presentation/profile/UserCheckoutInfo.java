package com.finalproject.v_league_ticket.presentation.profile;

import android.content.Context;

import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserCheckoutInfo {
    public interface Callback {
        void onLoaded(UserCheckoutInfo info);
    }

    public final String name;
    public final String email;
    public final String phone;
    public final String address;
    public final String paymentMethod;
    public final String paymentProvider;

    public UserCheckoutInfo(String name, String email, String phone, String address,
                            String paymentMethod, String paymentProvider) {
        this.name = safe(name);
        this.email = safe(email);
        this.phone = safe(phone);
        this.address = safe(address);
        this.paymentMethod = safe(paymentMethod);
        this.paymentProvider = safe(paymentProvider);
    }

    public static void load(Context context, Callback callback) {
        String uid = AuthSession.uid(context);
        if (uid == null || uid.isEmpty()) {
            callback.onLoaded(defaultFromSession(context));
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        callback.onLoaded(fromDocument(context, task.getResult()));
                    } else {
                        callback.onLoaded(defaultFromSession(context));
                    }
                });
    }

    public static void save(Context context, String name, String phone, String email, String address,
                            String paymentMethod, String paymentProvider) {
        String uid = AuthSession.uid(context);
        if (uid == null || uid.isEmpty()) return;
        Map<String, Object> data = new HashMap<>();
        putIfPresent(data, "username", name);
        putIfPresent(data, "displayName", name);
        putIfPresent(data, "phone", phone);
        putIfPresent(data, "email", email);
        putIfPresent(data, "address", address);
        putIfPresent(data, "shippingAddress", address);
        putIfPresent(data, "preferredPaymentMethod", paymentMethod);
        putIfPresent(data, "preferredPaymentProvider", paymentProvider);
        data.put("updatedAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("users").document(uid).set(data, SetOptions.merge());
    }

    public static UserCheckoutInfo fromDocument(Context context, DocumentSnapshot doc) {
        String name = first(doc, "username", "displayName", "fullName", "name");
        String email = first(doc, "email");
        String phone = first(doc, "phone", "phoneNumber");
        String address = first(doc, "shippingAddress", "address", "addressLine");
        if (address.isEmpty()) address = joinAddress(first(doc, "street"), first(doc, "ward"),
                first(doc, "district"), first(doc, "city", "province"));
        if (name.isEmpty()) name = AuthSession.userName(context);
        if (email.isEmpty()) email = AuthSession.email(context);
        return new UserCheckoutInfo(name, email, phone, address,
                first(doc, "preferredPaymentMethod"),
                first(doc, "preferredPaymentProvider"));
    }

    public static UserCheckoutInfo defaultFromSession(Context context) {
        return new UserCheckoutInfo(AuthSession.userName(context), AuthSession.email(context),
                "", "", "", "");
    }

    public boolean prefersBankTransfer() {
        return "bank_transfer".equals(paymentMethod) || !paymentProvider.isEmpty();
    }

    public boolean prefersMomo() {
        return "momo".equalsIgnoreCase(paymentProvider);
    }

    private static String joinAddress(String street, String ward, String district, String city) {
        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, street);
        appendAddressPart(builder, ward);
        appendAddressPart(builder, district);
        appendAddressPart(builder, city);
        return builder.toString();
    }

    private static void appendAddressPart(StringBuilder builder, String value) {
        value = safe(value);
        if (value.isEmpty()) return;
        if (builder.length() > 0) builder.append(", ");
        builder.append(value);
    }

    private static void putIfPresent(Map<String, Object> data, String key, String value) {
        value = safe(value);
        if (!value.isEmpty()) data.put(key, value);
    }

    private static String first(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof String && !((String) value).trim().isEmpty()) return ((String) value).trim();
            if (value instanceof Number) return String.valueOf(((Number) value).longValue());
        }
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
