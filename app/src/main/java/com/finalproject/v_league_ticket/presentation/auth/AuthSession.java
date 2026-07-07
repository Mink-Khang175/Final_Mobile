package com.finalproject.v_league_ticket.presentation.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class AuthSession {
    private static final String PREFS = "auth_session_java";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_UID = "uid";
    private static final String KEY_ROLE = "role";
    private static final String ROLE_ADMIN = "admin";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@vleague.app";

    private AuthSession() {
    }

    public static boolean hasToken(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && !user.isAnonymous();
    }

    public static void saveLocal(Context context, String email, String name) {
        prefs(context).edit()
                .putString(KEY_EMAIL, email == null ? "" : email)
                .putString(KEY_NAME, name == null || name.isEmpty() ? nameFromEmail(email) : name)
                .apply();
    }

    public static void saveFirebaseUser(Context context, FirebaseUser user, String role) {
        if (user == null) return;
        String email = user.getEmail();
        String name = user.getDisplayName();
        SharedPreferences.Editor editor = prefs(context).edit()
                .putString(KEY_UID, user.getUid())
                .putString(KEY_EMAIL, email == null ? "" : email)
                .putString(KEY_NAME, name == null || name.isEmpty() ? nameFromEmail(email) : name);
        if (role != null && !role.isEmpty()) {
            editor.putString(KEY_ROLE, role);
        }
        editor.apply();
    }

    public static void cacheRole(Context context, String role) {
        prefs(context).edit().putString(KEY_ROLE, normalizeRole(role)).apply();
    }

    public static void clear(Context context) {
        FirebaseAuth.getInstance().signOut();
        prefs(context).edit().clear().apply();
    }

    public static String userName(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) return user.getDisplayName();
            if (user.getEmail() != null && !user.getEmail().isEmpty()) return nameFromEmail(user.getEmail());
        }
        return prefs(context).getString(KEY_NAME, null);
    }

    public static String email(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) return user.getEmail();
        return prefs(context).getString(KEY_EMAIL, null);
    }

    public static boolean isAdmin(Context context) {
        String role = prefs(context).getString(KEY_ROLE, "");
        if (ROLE_ADMIN.equalsIgnoreCase(role)) return true;
        String email = email(context);
        return email != null && email.equalsIgnoreCase(DEFAULT_ADMIN_EMAIL);
    }

    public static String role(Context context) {
        return normalizeRole(prefs(context).getString(KEY_ROLE, "user"));
    }

    public static String uid(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) return user.getUid();
        return prefs(context).getString(KEY_UID, "");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String nameFromEmail(String email) {
        if (email == null || email.isEmpty()) return "Khách";
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private static String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) return "user";
        return role.trim().toLowerCase();
    }
}
