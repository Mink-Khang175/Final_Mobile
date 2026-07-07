package com.finalproject.v_league_ticket.presentation.auth;

import android.net.Uri;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.FragmentAuthLoginBinding;
import com.finalproject.v_league_ticket.presentation.admin.AdminDashboardFragment;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AuthLoginFragment extends Fragment {
    private FragmentAuthLoginBinding binding;

    public AuthLoginFragment() {
        super(R.layout.fragment_auth_login);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentAuthLoginBinding.bind(view);
        setupHeroVideo();
        bindToggleText();
        binding.btnLogin.setOnClickListener(v -> submitLogin());
        binding.tvGoRegister.setOnClickListener(v -> navigateTo(new AuthRegisterFragment()));
        binding.tvForgotPassword.setOnClickListener(v -> toast("Tính năng khôi phục mật khẩu sẽ sớm được hỗ trợ."));
        binding.btnGoogle.setOnClickListener(v -> toast("Đăng nhập bằng Google sẽ sớm được hỗ trợ."));
        binding.btnFacebook.setOnClickListener(v -> toast("Đăng nhập bằng Facebook sẽ sớm được hỗ trợ."));
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.videoAuthHero.stopPlayback();
        }
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && !binding.videoAuthHero.isPlaying()) {
            binding.videoAuthHero.start();
        }
    }

    @Override
    public void onPause() {
        if (binding != null) {
            binding.videoAuthHero.pause();
        }
        super.onPause();
    }

    private void setupHeroVideo() {
        Uri videoUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + R.raw.auth_intro);
        binding.videoAuthHero.setVideoURI(videoUri);
        binding.videoAuthHero.setOnPreparedListener(player -> {
            player.setLooping(true);
            player.setVolume(0f, 0f);
            cropHeroVideo(player.getVideoWidth(), player.getVideoHeight());
            binding.videoAuthHero.start();
        });
        binding.videoAuthHero.setOnErrorListener((player, what, extra) -> {
            binding.videoAuthHero.setVisibility(View.GONE);
            binding.imgAuthHero.setVisibility(View.VISIBLE);
            return true;
        });
    }

    private void cropHeroVideo(int videoWidth, int videoHeight) {
        binding.videoAuthHero.post(() -> {
            if (binding == null || videoWidth <= 0 || videoHeight <= 0) return;
            int viewWidth = binding.authHero.getWidth();
            int viewHeight = binding.authHero.getHeight();
            if (viewWidth <= 0 || viewHeight <= 0) return;
            float scale = Math.max(
                    viewWidth / (float) videoWidth,
                    viewHeight / (float) videoHeight
            );
            binding.videoAuthHero.setScaleX(scale * videoWidth / viewWidth);
            binding.videoAuthHero.setScaleY(scale * videoHeight / viewHeight);
        });
    }

    private void submitLogin() {
        clearErrors();
        String email = text(binding.edtEmail.getText());
        String password = text(binding.edtPassword.getText());
        boolean hasError = false;
        if (!email.contains("@")) {
            binding.tilEmail.setError("Nhập email hợp lệ");
            hasError = true;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            hasError = true;
        }
        if (hasError) return;

        binding.btnLogin.setEnabled(false);
        binding.btnLogin.setText("ĐANG ĐĂNG NHẬP...");
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    if (!task.isSuccessful() || FirebaseAuth.getInstance().getCurrentUser() == null) {
                        binding.btnLogin.setEnabled(true);
                        binding.btnLogin.setText("ĐĂNG NHẬP");
                        binding.tilPassword.setError(task.getException() == null
                                ? "Không đăng nhập được"
                                : "Email hoặc mật khẩu không đúng");
                        toast("Không đăng nhập được. Kiểm tra email hoặc mật khẩu.");
                        return;
                    }
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    loadRoleThenNavigate(user);
                });
    }

    private void loadRoleThenNavigate(FirebaseUser user) {
        Map<String, Object> loginPatch = new HashMap<>();
        loginPatch.put("email", user.getEmail());
        loginPatch.put("lastLoginAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).set(loginPatch, SetOptions.merge());
        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(roleTask -> {
                    if (binding == null) return;
                    String role = "";
                    if (roleTask.isSuccessful()) {
                        DocumentSnapshot doc = roleTask.getResult();
                        backfillJoinDate(db, user, doc);
                        String firestoreRole = doc == null ? null : doc.getString("role");
                        if (firestoreRole != null && !firestoreRole.trim().isEmpty()) {
                            role = firestoreRole.trim();
                        }
                    }
                    if (!role.isEmpty()) {
                        finishLogin(user, role);
                        return;
                    }
                    db.collection("users")
                            .whereEqualTo("email", user.getEmail())
                            .limit(1)
                            .get()
                            .addOnCompleteListener(queryTask -> {
                                if (binding == null) return;
                                String fallbackRole = "user";
                                if (queryTask.isSuccessful() && queryTask.getResult() != null && !queryTask.getResult().isEmpty()) {
                                    String firestoreRole = queryTask.getResult().getDocuments().get(0).getString("role");
                                    if (firestoreRole != null && !firestoreRole.trim().isEmpty()) {
                                        fallbackRole = firestoreRole.trim();
                                    }
                                }
                                finishLogin(user, fallbackRole);
                            });
                });
    }

    private void backfillJoinDate(FirebaseFirestore db, FirebaseUser user, DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return;
        if (doc.get("joinedAt") != null && doc.get("createdAt") != null) return;
        Map<String, Object> patch = new HashMap<>();
        if (doc.get("joinedAt") == null) patch.put("joinedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        if (doc.get("createdAt") == null) patch.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        db.collection("users").document(user.getUid()).set(patch, SetOptions.merge());
    }

    private void finishLogin(FirebaseUser user, String role) {
        AuthSession.saveFirebaseUser(requireContext(), user, role);
        binding.btnLogin.setEnabled(true);
        binding.btnLogin.setText("ĐĂNG NHẬP");
        toast(AuthSession.isAdmin(requireContext()) ? "Đăng nhập admin" : "Đăng nhập thành công");
        navigateAfterAuth();
    }

    private void navigateAfterAuth() {
        parentClearBackStack();
        Fragment fragment = AuthSession.isAdmin(requireContext()) ? new AdminDashboardFragment() : new HomepageFragment();
        parentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void bindToggleText() {
        String fullText = "Chưa có tài khoản? Đăng ký";
        String actionText = "Đăng ký";
        SpannableString spannable = new SpannableString(fullText);
        int start = fullText.indexOf(actionText);
        if (start >= 0) {
            int end = start + actionText.length();
            spannable.setSpan(new ForegroundColorSpan(requireContext().getColor(R.color.red_energy)),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        binding.tvGoRegister.setText(spannable);
    }

    private void clearErrors() {
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
    }

    private void navigateTo(Fragment fragment) {
        parentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName())
                .commit();
    }

    private void parentClearBackStack() {
        parentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private androidx.fragment.app.FragmentManager parentFragmentManager() {
        return getParentFragmentManager();
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
