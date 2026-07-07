package com.finalproject.v_league_ticket.presentation.auth;

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
import com.finalproject.v_league_ticket.databinding.FragmentAuthRegisterBinding;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.finalproject.v_league_ticket.presentation.profile.UserEngagementManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AuthRegisterFragment extends Fragment {
    private FragmentAuthRegisterBinding binding;

    public AuthRegisterFragment() {
        super(R.layout.fragment_auth_register);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentAuthRegisterBinding.bind(view);
        bindToggleText();
        binding.btnCreateAccount.setOnClickListener(v -> submitRegister());
        binding.tvGoLogin.setOnClickListener(v -> navigateTo(new AuthLoginFragment()));
        binding.btnGoogle.setOnClickListener(v -> toast("Đăng ký bằng Google sẽ sớm được hỗ trợ."));
        binding.btnFacebook.setOnClickListener(v -> toast("Đăng ký bằng Facebook sẽ sớm được hỗ trợ."));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void submitRegister() {
        clearErrors();
        String fullName = text(binding.edtFullName.getText());
        String email = text(binding.edtEmail.getText());
        String password = text(binding.edtPassword.getText());
        String confirm = text(binding.edtConfirmPassword.getText());
        boolean hasError = false;
        if (fullName.length() < 2) {
            binding.tilFullName.setError("Nhập họ và tên");
            hasError = true;
        }
        if (!email.contains("@")) {
            binding.tilEmail.setError("Nhập email hợp lệ");
            hasError = true;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            hasError = true;
        }
        if (!password.equals(confirm)) {
            binding.tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            hasError = true;
        }
        if (hasError) return;

        binding.btnCreateAccount.setEnabled(false);
        binding.btnCreateAccount.setText("ĐANG TẠO...");
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    if (!task.isSuccessful() || FirebaseAuth.getInstance().getCurrentUser() == null) {
                        binding.btnCreateAccount.setEnabled(true);
                        binding.btnCreateAccount.setText("TẠO TÀI KHOẢN");
                        binding.tilEmail.setError(task.getException() == null
                                ? "Không tạo được tài khoản"
                                : "Email hoặc mật khẩu chưa hợp lệ");
                        toast("Không tạo được tài khoản. Vui lòng thử lại.");
                        return;
                    }
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(fullName).build());
                    createUserDocument(user, fullName);
                });
    }

    private void createUserDocument(FirebaseUser user, String fullName) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getUid());
        data.put("email", user.getEmail());
        data.put("username", fullName);
        data.put("displayName", fullName);
        data.put("role", "user");
        data.put("status", "active");
        data.put("joinedAt", FieldValue.serverTimestamp());
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnCompleteListener(profileTask -> {
                    if (binding == null) return;
                    UserEngagementManager.awardSignupBonus(user.getUid());
                    UserEngagementManager.notifyUser(user.getUid(),
                            "Chào mừng đến với V.League",
                            "Bạn đã nhận 100 điểm thành viên đầu tiên.",
                            "signup_bonus",
                            user.getUid());
                    AuthSession.saveFirebaseUser(requireContext(), user, "user");
                    binding.btnCreateAccount.setEnabled(true);
                    binding.btnCreateAccount.setText("TẠO TÀI KHOẢN");
                    toast(profileTask.isSuccessful() ? "Tài khoản đã được tạo" : "Tài khoản đã tạo, chưa lưu được hồ sơ");
                    getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new HomepageFragment())
                            .commit();
                });
    }

    private void bindToggleText() {
        String fullText = "Đã có tài khoản? Đăng nhập";
        String actionText = "Đăng nhập";
        SpannableString spannable = new SpannableString(fullText);
        int start = fullText.indexOf(actionText);
        if (start >= 0) {
            int end = start + actionText.length();
            spannable.setSpan(new ForegroundColorSpan(requireContext().getColor(R.color.shop_dynamic_red)),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        binding.tvGoLogin.setText(spannable);
    }

    private void clearErrors() {
        binding.tilFullName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName())
                .commit();
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
