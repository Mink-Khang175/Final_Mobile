package com.finalproject.v_league_ticket.presentation.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.data.remote.VietnamAddressApiClient;
import com.finalproject.v_league_ticket.databinding.DialogChangePasswordBinding;
import com.finalproject.v_league_ticket.databinding.DialogPersonalInfoBinding;
import com.finalproject.v_league_ticket.databinding.FragmentProfileBinding;
import com.finalproject.v_league_ticket.databinding.ItemProfileRowBinding;
import com.finalproject.v_league_ticket.presentation.admin.AdminDashboardFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.club.ClubProfileDirectory;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.finalproject.v_league_ticket.presentation.matches.FixturesFragment;
import com.finalproject.v_league_ticket.presentation.news.NewsFragment;
import com.finalproject.v_league_ticket.presentation.shop.ShopFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private String profileName = "";
    private String profileEmail = "";
    private String profilePhone = "";
    private String profileCity = "";
    private int profileCityCode = 0;
    private String profileDistrict = "";
    private int profileDistrictCode = 0;
    private String profileWard = "";
    private int profileWardCode = 0;
    private String profileStreet = "";
    private String profilePreferredPaymentMethod = "";
    private String profilePreferredPaymentProvider = "";

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentProfileBinding.bind(view);
        if (!AuthSession.hasToken(requireContext())) {
            getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new AuthLoginFragment()).commit();
            return;
        }
        if (AuthSession.isAdmin(requireContext())) {
            getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new AdminDashboardFragment()).commit();
            return;
        }
        setupRows();
        bindProfile();
        loadFirebaseProfile();
        loadOrderCount();
        loadTicketCount();
        loadCommerceCounts();
        setupNavigation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void bindProfile() {
        String name = AuthSession.userName(requireContext());
        String email = AuthSession.email(requireContext());
        if (name == null || name.isEmpty()) name = "Người hâm mộ V.League";
        profileName = name;
        profileEmail = email == null ? "" : email;
        binding.appHeader.tvHeaderSubtitle.setText(name);
        binding.tvUserNameClub.setText(name);
        binding.tvMembershipTier.setText(AuthSession.isAdmin(requireContext()) ? "Quản trị" : "Tân binh");
        binding.tvJoinedYear.setText("Đang cập nhật ngày tham gia");
        binding.tvPointsBalance.setText("0 điểm");
        binding.tvClubName.setText("Chưa chọn CLB yêu thích");
        binding.tvStadiumName.setText(profileEmail.isEmpty() ? "Chưa có email" : profileEmail);
        binding.tvActiveTickets.setText("--");
        binding.tvOrderStatus.setText("--");
        binding.tvPredictionAccuracy.setText("--");
        binding.tvVersion.setText("Phiên bản 1.0");
        load(binding.imgAvatar, null, R.drawable.user);
        load(binding.imgClubBackground, null, R.drawable.svd);
        load(binding.imgClubLogo, null, R.drawable.ic_logo);
        binding.rowAdminDashboard.getRoot().setVisibility(AuthSession.isAdmin(requireContext()) ? View.VISIBLE : View.GONE);
    }

    private void loadFirebaseProfile() {
        String uid = AuthSession.uid(requireContext());
        if (uid == null || uid.isEmpty()) return;
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (binding == null || !task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) return;
                    bindUserDocument(task.getResult());
                });
    }

    private void bindUserDocument(DocumentSnapshot doc) {
        String name = first(doc, "username", "displayName", "fullName", "name");
        String role = first(doc, "role");
        String email = first(doc, "email");
        String photoUrl = first(doc, "photoUrl", "avatarUrl");
        profilePhone = first(doc, "phone", "phoneNumber");
        profileCity = first(doc, "city", "province");
        profileCityCode = intField(doc, "cityCode", "provinceCode");
        profileDistrict = first(doc, "district");
        profileDistrictCode = intField(doc, "districtCode");
        profileWard = first(doc, "ward");
        profileWardCode = intField(doc, "wardCode");
        profileStreet = first(doc, "street");
        applyAddressFallback(first(doc, "shippingAddress", "address", "addressLine"));
        profilePreferredPaymentMethod = first(doc, "preferredPaymentMethod");
        profilePreferredPaymentProvider = first(doc, "preferredPaymentProvider");
        String club = first(doc, "favoriteClub", "clubName", "myClub");
        String stadium = first(doc, "stadiumName", "clubStadium");
        String clubLogo = first(doc, "clubLogoUrl", "favoriteClubLogo");
        String clubBackground = first(doc, "clubBackgroundUrl", "stadiumImageUrl");
        if (!name.isEmpty()) {
            profileName = name;
            binding.appHeader.tvHeaderSubtitle.setText(name);
            binding.tvUserNameClub.setText(name);
        }
        if (!email.isEmpty()) profileEmail = email;
        if (!role.isEmpty()) binding.tvMembershipTier.setText(role);
        bindJoinedDate(doc);
        binding.tvStadiumName.setText(stadium.isEmpty() ? (profileEmail.isEmpty() ? "Chưa có email" : profileEmail) : stadium);
        if (!club.isEmpty()) binding.tvClubName.setText(club);
        Object points = doc.get("points");
        if (points instanceof Number) applyMembership(((Number) points).longValue());
        else applyMembership(0);
        if (!photoUrl.isEmpty()) load(binding.imgAvatar, photoUrl, R.drawable.user);
        if (!clubLogo.isEmpty()) load(binding.imgClubLogo, clubLogo, R.drawable.ic_logo);
        if (!clubBackground.isEmpty()) load(binding.imgClubBackground, clubBackground, R.drawable.svd);
    }

    private void bindJoinedDate(DocumentSnapshot doc) {
        Date joined = timestampDate(doc, "joinedAt", "createdAt");
        if (joined == null) {
            joined = new Date();
            Map<String, Object> data = new HashMap<>();
            data.put("joinedAt", FieldValue.serverTimestamp());
            if (doc.get("createdAt") == null) data.put("createdAt", FieldValue.serverTimestamp());
            FirebaseFirestore.getInstance().collection("users").document(doc.getId()).set(data, SetOptions.merge());
        }
        binding.tvJoinedYear.setText("Tham gia " + new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(joined));
    }

    private Date timestampDate(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof Timestamp) return ((Timestamp) value).toDate();
            if (value instanceof Date) return (Date) value;
            if (value instanceof Number) return new Date(((Number) value).longValue());
        }
        return null;
    }

    private void loadOrderCount() {
        String uid = AuthSession.uid(requireContext());
        if (uid == null || uid.isEmpty()) return;
        FirebaseFirestore.getInstance().collection("orders").whereEqualTo("userId", uid).get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    binding.tvOrderStatus.setText(task.isSuccessful() && task.getResult() != null
                            ? String.valueOf(task.getResult().size())
                            : "--");
                });
    }

    private void loadTicketCount() {
        String uid = AuthSession.uid(requireContext());
        if (uid == null || uid.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("bookings").whereEqualTo("userId", uid).get()
                .addOnCompleteListener(firstTask -> {
                    if (binding == null) return;
                    java.util.Set<String> ids = new java.util.HashSet<>();
                    if (firstTask.isSuccessful() && firstTask.getResult() != null) {
                        firstTask.getResult().getDocuments().forEach(doc -> ids.add(doc.getId()));
                    }
                    db.collection("ticket_orders").whereEqualTo("userId", uid).get()
                            .addOnCompleteListener(secondTask -> {
                                if (binding == null) return;
                                if (secondTask.isSuccessful() && secondTask.getResult() != null) {
                                    secondTask.getResult().getDocuments().forEach(doc -> ids.add(doc.getId()));
                                }
                                binding.tvActiveTickets.setText(String.valueOf(ids.size()));
                            });
                });
    }

    private void loadCommerceCounts() {
        String uid = AuthSession.uid(requireContext());
        if (uid == null || uid.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("user_loyalty").document(uid).get().addOnCompleteListener(task -> {
            if (binding == null || !task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) return;
            Object points = task.getResult().get("points");
        if (points instanceof Number) applyMembership(((Number) points).longValue());
        });
        loadActiveBadgeCount(db, binding.rowMyBadges);
        loadRowCount(db, "user_vouchers", binding.rowVouchers);
        loadNotificationCount(db, binding.rowNotifications);
        db.collection("user_predictions").whereEqualTo("userId", uid).get().addOnCompleteListener(task -> {
            if (binding == null) return;
            binding.tvPredictionAccuracy.setText(task.isSuccessful() && task.getResult() != null
                    ? String.valueOf(task.getResult().size())
                    : "--");
        });
    }

    private void loadRowCount(FirebaseFirestore db, String collection, ItemProfileRowBinding row) {
        db.collection(collection).whereEqualTo("userId", AuthSession.uid(requireContext())).get()
                .addOnCompleteListener(task -> {
                    if (binding == null || !task.isSuccessful() || task.getResult() == null) return;
                    int count = task.getResult().size();
                    row.tvRowBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                    row.tvRowBadge.setText(String.valueOf(count));
                });
    }

    private void applyMembership(long points) {
        MembershipTier tier = membershipTier(points);
        binding.tvPointsBalance.setText(points + " điểm");
        if (!AuthSession.isAdmin(requireContext())) binding.tvMembershipTier.setText(tier.name);
        saveMembershipBadge(points, tier);
    }

    private MembershipTier membershipTier(long points) {
        if (points >= 3000) return new MembershipTier("Kim cương", "diamond", 3000);
        if (points >= 1500) return new MembershipTier("Vàng", "gold", 1500);
        if (points >= 500) return new MembershipTier("Bạc", "silver", 500);
        if (points >= 100) return new MembershipTier("Đồng", "bronze", 100);
        return new MembershipTier("Tân binh", "rookie", 0);
    }

    private void saveMembershipBadge(long points, MembershipTier tier) {
        String uid = AuthSession.uid(requireContext());
        if (uid == null || uid.isEmpty()) return;
        Map<String, Object> userPatch = new HashMap<>();
        userPatch.put("membershipTier", tier.name);
        userPatch.put("membershipTierCode", tier.code);
        userPatch.put("membershipPoints", points);
        userPatch.put("updatedAt", FieldValue.serverTimestamp());
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).set(userPatch, SetOptions.merge());

        Map<String, Object> badge = new HashMap<>();
        badge.put("userId", uid);
        badge.put("code", "member_" + tier.code);
        badge.put("title", "Hạng " + tier.name);
        badge.put("name", "Hạng " + tier.name);
        badge.put("description", "Đạt mốc " + tier.requiredPoints + " điểm thành viên");
        badge.put("pointsRequired", tier.requiredPoints);
        badge.put("type", "membership");
        badge.put("status", "active");
        badge.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("user_badges").whereEqualTo("userId", uid).whereEqualTo("type", "membership").get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (!doc.getId().equals(uid + "_member_" + tier.code)) {
                            Map<String, Object> inactive = new HashMap<>();
                            inactive.put("status", "inactive");
                            inactive.put("updatedAt", FieldValue.serverTimestamp());
                            db.collection("user_badges").document(doc.getId()).set(inactive, SetOptions.merge());
                        }
                    }
                    db.collection("user_badges").document(uid + "_member_" + tier.code).set(badge, SetOptions.merge());
                })
                .addOnFailureListener(error ->
                        db.collection("user_badges").document(uid + "_member_" + tier.code).set(badge, SetOptions.merge()));
    }

    private void loadActiveBadgeCount(FirebaseFirestore db, ItemProfileRowBinding row) {
        db.collection("user_badges")
                .whereEqualTo("userId", AuthSession.uid(requireContext()))
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(task -> {
                    if (binding == null || !task.isSuccessful() || task.getResult() == null) return;
                    int count = task.getResult().size();
                    row.tvRowBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                    row.tvRowBadge.setText(String.valueOf(count));
                });
    }

    private static class MembershipTier {
        final String name;
        final String code;
        final long requiredPoints;

        MembershipTier(String name, String code, long requiredPoints) {
            this.name = name;
            this.code = code;
            this.requiredPoints = requiredPoints;
        }
    }

    private void loadNotificationCount(FirebaseFirestore db, ItemProfileRowBinding row) {
        String uid = AuthSession.uid(requireContext());
        db.collection("notifications").get().addOnCompleteListener(task -> {
            if (binding == null || !task.isSuccessful() || task.getResult() == null) return;
            int count = 0;
            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                String userId = first(doc, "userId");
                String targetRole = first(doc, "targetRole", "target");
                boolean forMe = uid != null && uid.equals(userId);
                boolean broadcast = userId.isEmpty()
                        && (targetRole.isEmpty()
                        || "all".equalsIgnoreCase(targetRole)
                        || "user".equalsIgnoreCase(targetRole)
                        || "người dùng".equalsIgnoreCase(targetRole));
                if (forMe || broadcast) count++;
            }
            row.tvRowBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            row.tvRowBadge.setText(String.valueOf(count));
        });
    }

    private void setupRows() {
        bindRow(binding.rowLoyaltyRewards, "Điểm thành viên", R.drawable.ic_points_24);
        bindRow(binding.rowMyBadges, "Huy hiệu của tôi", R.drawable.ic_medal_24);
        bindRow(binding.rowVouchers, "Vouchers", R.drawable.ic_ticket_24);
        bindRow(binding.rowPersonalInfo, "Thông tin cá nhân", R.drawable.ic_person_24);
        bindRow(binding.rowSecurityPrivacy, "Bảo mật tài khoản", R.drawable.ic_lock_24);
        bindRow(binding.rowNotifications, "Thông báo", R.drawable.notifications_active_24);
        bindRow(binding.rowHelpCenter, "Trung tâm hỗ trợ", R.drawable.ic_email_24);
        bindRow(binding.rowAdminDashboard, "Bảng quản trị", R.drawable.ic_nav_table);
    }

    private void setupNavigation() {
        binding.bottomNav.navHome.setOnClickListener(v -> navigateTo(new HomepageFragment()));
        binding.bottomNav.navMatches.setOnClickListener(v -> navigateTo(new FixturesFragment()));
        binding.bottomNav.navShop.setOnClickListener(v -> navigateTo(new ShopFragment()));
        binding.bottomNav.navNews.setOnClickListener(v -> navigateTo(new NewsFragment()));
        binding.btnLogout.setOnClickListener(v -> {
            AuthSession.clear(requireContext());
            navigateTo(new AuthLoginFragment());
        });
        binding.btnRedeem.setOnClickListener(v -> navigateTo(ProfileHistoryFragment.loyalty()));
        binding.btnChangeClub.setOnClickListener(v -> showFavoriteClubMenu());
        binding.cardTickets.setOnClickListener(v -> navigateTo(ProfileHistoryFragment.tickets()));
        binding.cardOrders.setOnClickListener(v -> navigateTo(ProfileHistoryFragment.orders()));
        binding.cardPrediction.setOnClickListener(v -> navigateTo(ProfileHistoryFragment.predictions()));
        binding.rowLoyaltyRewards.getRoot().setOnClickListener(v -> navigateTo(ProfileHistoryFragment.loyalty()));
        binding.rowMyBadges.getRoot().setOnClickListener(v -> navigateTo(ProfileHistoryFragment.badges()));
        binding.rowVouchers.getRoot().setOnClickListener(v -> navigateTo(ProfileHistoryFragment.vouchers()));
        binding.rowNotifications.getRoot().setOnClickListener(v -> navigateTo(ProfileHistoryFragment.notifications()));
        binding.rowHelpCenter.getRoot().setOnClickListener(v -> toast("Vui lòng liên hệ support@vleague.app"));
        binding.rowPersonalInfo.getRoot().setOnClickListener(v -> showPersonalInfoDialog());
        binding.rowSecurityPrivacy.getRoot().setOnClickListener(v -> showChangePasswordDialog());
        binding.rowAdminDashboard.getRoot().setOnClickListener(v -> navigateTo(new AdminDashboardFragment()));
        binding.appHeader.btnHeaderNotification.setOnClickListener(v -> navigateTo(ProfileHistoryFragment.notifications()));
    }

    private void bindRow(ItemProfileRowBinding row, String title, int iconRes) {
        row.tvRowTitle.setText(title);
        row.imgRowIcon.setImageResource(iconRes);
    }

    private void showPersonalInfoDialog() {
        DialogPersonalInfoBinding dialog = DialogPersonalInfoBinding.inflate(getLayoutInflater());
        applyAddressFallback(profileStreet);
        dialog.edtProfileName.setText(profileName);
        dialog.edtProfilePhone.setText(profilePhone);
        dialog.dropdownCity.setText(profileCity, false);
        dialog.dropdownDistrict.setText(profileDistrict, false);
        dialog.dropdownWard.setText(profileWard, false);
        dialog.edtProfileStreet.setText(profileStreet);
        bindProfilePayment(dialog);
        setupAddressDropdowns(dialog);
        AlertDialog alert = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Thông tin cá nhân")
                .setView(dialog.getRoot())
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .show();
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> savePersonalInfo(dialog, alert));
    }

    private void setupAddressDropdowns(DialogPersonalInfoBinding dialog) {
        dialog.tilDistrict.setEnabled(false);
        dialog.dropdownDistrict.setEnabled(false);
        dialog.tilWard.setEnabled(false);
        dialog.dropdownWard.setEnabled(false);
        setLoading(dialog.dropdownCity, "");
        VietnamAddressApiClient.getInstance().fetchProvinces(new VietnamAddressApiClient.DataCallback<List<VietnamAddressApiClient.Division>>() {
            @Override
            public void onSuccess(List<VietnamAddressApiClient.Division> provinces) {
                if (binding == null) return;
                bindCityDropdown(dialog, provinces);
                if (profileCityCode == 0 && !profileCity.isEmpty()) {
                    VietnamAddressApiClient.Division matched = findDivision(provinces, profileCity);
                    if (matched != null) profileCityCode = matched.code;
                }
                if (profileCityCode > 0) {
                    loadDistricts(dialog, profileCityCode, false);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding == null) return;
                setLoading(dialog.dropdownCity, "");
                toast("Không tải được danh sách tỉnh/thành.");
            }
        });
    }

    private void bindCityDropdown(DialogPersonalInfoBinding dialog, List<VietnamAddressApiClient.Division> provinces) {
        ArrayAdapter<VietnamAddressApiClient.Division> adapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, provinces);
        dialog.dropdownCity.setAdapter(adapter);
        dialog.dropdownCity.setThreshold(0);
        dialog.dropdownCity.setOnClickListener(v -> dialog.dropdownCity.showDropDown());
        dialog.dropdownCity.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) dialog.dropdownCity.showDropDown();
        });
        dialog.dropdownCity.setOnItemClickListener((parent, view, position, id) -> {
            VietnamAddressApiClient.Division province = adapter.getItem(position);
            if (province == null) return;
            profileCityCode = province.code;
            profileDistrictCode = 0;
            profileWardCode = 0;
            dialog.dropdownDistrict.setText("", false);
            dialog.dropdownWard.setText("", false);
            loadDistricts(dialog, province.code, true);
        });
    }

    private void loadDistricts(DialogPersonalInfoBinding dialog, int provinceCode, boolean showWhenLoaded) {
        setLoading(dialog.dropdownDistrict, "");
        dialog.tilDistrict.setEnabled(true);
        dialog.dropdownDistrict.setEnabled(true);
        dialog.tilWard.setEnabled(false);
        dialog.dropdownWard.setEnabled(false);
        VietnamAddressApiClient.getInstance().fetchDistricts(provinceCode, new VietnamAddressApiClient.DataCallback<List<VietnamAddressApiClient.Division>>() {
            @Override
            public void onSuccess(List<VietnamAddressApiClient.Division> districts) {
                if (binding == null) return;
                bindDistrictDropdown(dialog, districts);
                if (profileDistrictCode == 0 && !profileDistrict.isEmpty()) {
                    VietnamAddressApiClient.Division matched = findDivision(districts, profileDistrict);
                    if (matched != null) profileDistrictCode = matched.code;
                }
                if (profileDistrictCode > 0) {
                    loadWards(dialog, profileDistrictCode, false);
                } else if (showWhenLoaded) {
                    dialog.dropdownDistrict.showDropDown();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding == null) return;
                setLoading(dialog.dropdownDistrict, "");
                toast("Không tải được danh sách quận/huyện.");
            }
        });
    }

    private void bindDistrictDropdown(DialogPersonalInfoBinding dialog, List<VietnamAddressApiClient.Division> districts) {
        ArrayAdapter<VietnamAddressApiClient.Division> adapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, districts);
        dialog.dropdownDistrict.setAdapter(adapter);
        dialog.dropdownDistrict.setThreshold(0);
        dialog.dropdownDistrict.setOnClickListener(v -> dialog.dropdownDistrict.showDropDown());
        dialog.dropdownDistrict.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) dialog.dropdownDistrict.showDropDown();
        });
        dialog.dropdownDistrict.setOnItemClickListener((parent, view, position, id) -> {
            VietnamAddressApiClient.Division district = adapter.getItem(position);
            if (district == null) return;
            profileDistrictCode = district.code;
            profileWardCode = 0;
            dialog.dropdownWard.setText("", false);
            loadWards(dialog, district.code, true);
        });
    }

    private void loadWards(DialogPersonalInfoBinding dialog, int districtCode, boolean showWhenLoaded) {
        setLoading(dialog.dropdownWard, "");
        dialog.tilWard.setEnabled(true);
        dialog.dropdownWard.setEnabled(true);
        VietnamAddressApiClient.getInstance().fetchWards(districtCode, new VietnamAddressApiClient.DataCallback<List<VietnamAddressApiClient.Division>>() {
            @Override
            public void onSuccess(List<VietnamAddressApiClient.Division> wards) {
                if (binding == null) return;
                bindWardDropdown(dialog, wards);
                if (profileWardCode == 0 && !profileWard.isEmpty()) {
                    VietnamAddressApiClient.Division matched = findDivision(wards, profileWard);
                    if (matched != null) profileWardCode = matched.code;
                }
                if (showWhenLoaded) dialog.dropdownWard.showDropDown();
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding == null) return;
                setLoading(dialog.dropdownWard, "");
                toast("Không tải được danh sách phường/xã.");
            }
        });
    }

    private void bindWardDropdown(DialogPersonalInfoBinding dialog, List<VietnamAddressApiClient.Division> wards) {
        ArrayAdapter<VietnamAddressApiClient.Division> adapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, wards);
        dialog.dropdownWard.setAdapter(adapter);
        dialog.dropdownWard.setThreshold(0);
        dialog.dropdownWard.setOnClickListener(v -> dialog.dropdownWard.showDropDown());
        dialog.dropdownWard.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) dialog.dropdownWard.showDropDown();
        });
        dialog.dropdownWard.setOnItemClickListener((parent, view, position, id) -> {
            VietnamAddressApiClient.Division ward = adapter.getItem(position);
            if (ward != null) profileWardCode = ward.code;
        });
    }

    private void setLoading(android.widget.AutoCompleteTextView view, String text) {
        // Keep the field label stable; loading text inside the input looks like saved user data.
    }

    private void applyAddressFallback(String rawAddress) {
        String value = text(rawAddress);
        if (value.isEmpty()) return;
        boolean streetLooksFull = profileStreet.contains(",") && (profileWard.isEmpty() || profileDistrict.isEmpty() || profileCity.isEmpty());
        if (!profileStreet.isEmpty() && !streetLooksFull && (!profileWard.isEmpty() || !profileDistrict.isEmpty() || !profileCity.isEmpty())) {
            return;
        }
        String[] rawParts = value.split(",");
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (String part : rawParts) {
            String clean = text(part);
            if (!clean.isEmpty()) parts.add(clean);
        }
        if (parts.isEmpty()) return;
        if (profileStreet.isEmpty() || streetLooksFull) profileStreet = parts.get(0);
        if (profileWard.isEmpty() && parts.size() > 1) profileWard = parts.get(1);
        if (profileDistrict.isEmpty() && parts.size() > 2) profileDistrict = parts.get(2);
        if (profileCity.isEmpty() && parts.size() > 3) {
            StringBuilder city = new StringBuilder();
            for (int i = 3; i < parts.size(); i++) {
                if (city.length() > 0) city.append(", ");
                city.append(parts.get(i));
            }
            profileCity = city.toString();
        }
    }

    private VietnamAddressApiClient.Division findDivision(List<VietnamAddressApiClient.Division> rows, String name) {
        if (rows == null || name == null) return null;
        String wanted = normalizeAddress(name);
        for (VietnamAddressApiClient.Division row : rows) {
            if (normalizeAddress(row.name).equals(wanted)) return row;
        }
        for (VietnamAddressApiClient.Division row : rows) {
            if (normalizeAddress(row.name).contains(wanted) || wanted.contains(normalizeAddress(row.name))) return row;
        }
        return null;
    }

    private String normalizeAddress(String value) {
        return value == null ? "" : java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }

    private void savePersonalInfo(DialogPersonalInfoBinding dialog, AlertDialog alert) {
        String name = text(dialog.edtProfileName.getText());
        if (name.length() < 2) {
            dialog.tilProfileName.setError("Nhập họ tên hợp lệ");
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("username", name);
        data.put("displayName", name);
        data.put("phone", text(dialog.edtProfilePhone.getText()));
        data.put("city", text(dialog.dropdownCity.getText()));
        data.put("cityCode", profileCityCode);
        data.put("district", text(dialog.dropdownDistrict.getText()));
        data.put("districtCode", profileDistrictCode);
        data.put("ward", text(dialog.dropdownWard.getText()));
        data.put("wardCode", profileWardCode);
        data.put("street", text(dialog.edtProfileStreet.getText()));
        data.put("address", fullAddress(text(dialog.edtProfileStreet.getText()),
                text(dialog.dropdownWard.getText()),
                text(dialog.dropdownDistrict.getText()),
                text(dialog.dropdownCity.getText())));
        data.put("shippingAddress", data.get("address"));
        data.put("preferredPaymentMethod", selectedPaymentMethod(dialog));
        data.put("preferredPaymentProvider", selectedPaymentProvider(dialog));
        data.put("updatedAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("users").document(AuthSession.uid(requireContext()))
                .set(data, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    if (task.isSuccessful()) {
                        profileName = name;
                        profilePhone = text(dialog.edtProfilePhone.getText());
                        profileCity = text(dialog.dropdownCity.getText());
                        profileCityCode = profileCity.isEmpty() ? 0 : profileCityCode;
                        profileDistrict = text(dialog.dropdownDistrict.getText());
                        profileDistrictCode = profileDistrict.isEmpty() ? 0 : profileDistrictCode;
                        profileWard = text(dialog.dropdownWard.getText());
                        profileWardCode = profileWard.isEmpty() ? 0 : profileWardCode;
                        profileStreet = text(dialog.edtProfileStreet.getText());
                        profilePreferredPaymentMethod = selectedPaymentMethod(dialog);
                        profilePreferredPaymentProvider = selectedPaymentProvider(dialog);
                        binding.tvUserNameClub.setText(name);
                        binding.appHeader.tvHeaderSubtitle.setText(name);
                        AuthSession.saveLocal(requireContext(), profileEmail, name);
                        alert.dismiss();
                        toast("Đã cập nhật hồ sơ");
                    } else {
                        toast("Không lưu được hồ sơ. Vui lòng thử lại.");
                    }
                });
    }

    private void bindProfilePayment(DialogPersonalInfoBinding dialog) {
        if ("momo".equalsIgnoreCase(profilePreferredPaymentProvider)) {
            dialog.rbProfileMomo.setChecked(true);
        } else if ("bank_transfer".equals(profilePreferredPaymentMethod)
                || "vietinbank".equalsIgnoreCase(profilePreferredPaymentProvider)) {
            dialog.rbProfileVietinBank.setChecked(true);
        } else {
            dialog.rbProfileCod.setChecked(true);
        }
    }

    private String selectedPaymentMethod(DialogPersonalInfoBinding dialog) {
        return dialog.rbProfileCod.isChecked() ? "cash_on_delivery" : "bank_transfer";
    }

    private String selectedPaymentProvider(DialogPersonalInfoBinding dialog) {
        if (dialog.rbProfileMomo.isChecked()) return "momo";
        if (dialog.rbProfileVietinBank.isChecked()) return "vietinbank";
        return "";
    }

    private String fullAddress(String street, String ward, String district, String city) {
        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, street);
        appendAddressPart(builder, ward);
        appendAddressPart(builder, district);
        appendAddressPart(builder, city);
        return builder.toString();
    }

    private void appendAddressPart(StringBuilder builder, String value) {
        value = text(value);
        if (value.isEmpty()) return;
        if (builder.length() > 0) builder.append(", ");
        builder.append(value);
    }

    private void showChangePasswordDialog() {
        DialogChangePasswordBinding dialog = DialogChangePasswordBinding.inflate(getLayoutInflater());
        AlertDialog alert = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đổi mật khẩu")
                .setView(dialog.getRoot())
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Cập nhật", null)
                .show();
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> updatePassword(dialog, alert));
    }

    private void updatePassword(DialogChangePasswordBinding dialog, AlertDialog alert) {
        String current = text(dialog.edtCurrentPassword.getText());
        String next = text(dialog.edtNewPassword.getText());
        String confirm = text(dialog.edtConfirmNewPassword.getText());
        if (current.isEmpty()) {
            dialog.tilCurrentPassword.setError("Nhập mật khẩu hiện tại");
            return;
        }
        if (next.length() < 6) {
            dialog.tilNewPassword.setError("Mật khẩu mới tối thiểu 6 ký tự");
            return;
        }
        if (!next.equals(confirm)) {
            dialog.tilConfirmNewPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user == null ? "" : user.getEmail();
        if (user == null || email == null || email.isEmpty()) {
            toast("Không tìm thấy tài khoản Firebase.");
            return;
        }
        user.reauthenticate(EmailAuthProvider.getCredential(email, current))
                .addOnSuccessListener(unused -> user.updatePassword(next)
                        .addOnSuccessListener(ok -> {
                            alert.dismiss();
                            toast("Đã đổi mật khẩu");
                        })
                        .addOnFailureListener(error -> toast("Không đổi được mật khẩu.")))
                .addOnFailureListener(error -> dialog.tilCurrentPassword.setError("Mật khẩu hiện tại không đúng"));
    }

    private void showFavoriteClubMenu() {
        PopupMenu menu = new PopupMenu(requireContext(), binding.btnChangeClub);
        List<ClubProfileDirectory.Meta> clubs = ClubProfileDirectory.all(requireContext());
        for (int i = 0; i < clubs.size(); i++) {
            menu.getMenu().add(0, i, i, clubs.get(i).name);
        }
        menu.setOnMenuItemClickListener(item -> {
            saveFavoriteClub(clubs.get(item.getItemId()));
            return true;
        });
        menu.show();
    }

    private void saveFavoriteClub(ClubProfileDirectory.Meta club) {
        Map<String, Object> data = new HashMap<>();
        data.put("favoriteClub", club.name);
        data.put("clubName", club.name);
        data.put("stadiumName", club.stadium);
        data.put("clubLogoUrl", club.logoUrl());
        data.put("updatedAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("users").document(AuthSession.uid(requireContext()))
                .set(data, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    if (task.isSuccessful()) {
                        binding.tvClubName.setText(club.name);
                        binding.tvStadiumName.setText(club.stadium);
                        load(binding.imgClubLogo, club.logoUrl(), R.drawable.ic_logo);
                        toast("Đã đổi CLB yêu thích");
                    } else {
                        toast("Không lưu được CLB yêu thích.");
                    }
                });
    }

    private void load(ImageView imageView, String url, int placeholder) {
        Glide.with(imageView).load(url == null || url.isEmpty() ? null : url)
                .placeholder(placeholder).error(placeholder).centerCrop().into(imageView);
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName()).commit();
    }

    private String first(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof String && !((String) value).trim().isEmpty()) return ((String) value).trim();
            if (value instanceof Number) return String.valueOf(((Number) value).longValue());
        }
        return "";
    }

    private int intField(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof String) {
                try {
                    return Integer.parseInt(((String) value).replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
