package com.finalproject.v_league_ticket.presentation.shop;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.data.remote.VLeagueApiClient;
import com.finalproject.v_league_ticket.databinding.FragmentShopBinding;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.booking.TicketBookingFragment;
import com.finalproject.v_league_ticket.presentation.club.ClubProfileFragment;
import com.finalproject.v_league_ticket.presentation.club.ClubProfileDirectory;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.finalproject.v_league_ticket.presentation.matches.FixturesFragment;
import com.finalproject.v_league_ticket.presentation.news.NewsFragment;
import com.finalproject.v_league_ticket.presentation.profile.HeaderNotificationRouter;
import com.finalproject.v_league_ticket.presentation.profile.ProfileFragment;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShopFragment extends Fragment {
    private static final String CATEGORY_ALL = "Tất cả";
    private static final String CATEGORY_BALLS = "Trái bóng";
    private static final String CATEGORY_SOCKS = "Vớ bóng đá";
    private static final String CATEGORY_ACCESSORIES = "Phụ kiện";
    private static final long SHOP_SESSION_TTL_MS = 5 * 60 * 1000L;
    private static final List<ShopProduct> SESSION_PRODUCTS = new ArrayList<>();
    private static long sessionProductsLoadedAt = 0L;
    private static boolean sessionFetchInFlight = false;

    private FragmentShopBinding binding;
    private final ShopProductAdapter productAdapter =
            new ShopProductAdapter(product -> navigateTo(ProductDetailFragment.newInstance(product)));
    private final ShopClubAdapter clubAdapter =
            new ShopClubAdapter(club -> navigateTo(ClubProfileFragment.newInstance(club)));
    private final List<ShopProduct> allProducts = new ArrayList<>();
    private final List<ShopClub> allClubs = new ArrayList<>();

    public ShopFragment() {
        super(R.layout.fragment_shop);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentShopBinding.bind(view);
        binding.rvShopClubs.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvShopClubs.setAdapter(clubAdapter);
        binding.rvShopClubs.setHasFixedSize(true);
        binding.rvShopClubs.setItemViewCacheSize(8);
        binding.rvShopClubs.setItemAnimator(null);
        binding.rvShopProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvShopProducts.setAdapter(productAdapter);
        binding.rvShopProducts.setHasFixedSize(true);
        binding.rvShopProducts.setItemViewCacheSize(8);
        binding.rvShopProducts.setItemAnimator(null);
        setupHeroVideo();
        setupTicketBannerText();
        setupActions();
        loadShopData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && binding.videoHeroPromo.getVisibility() == View.VISIBLE
                && !binding.videoHeroPromo.isPlaying()) {
            binding.videoHeroPromo.start();
        }
    }

    @Override
    public void onDestroyView() {
        if (binding != null) binding.videoHeroPromo.stopPlayback();
        super.onDestroyView();
        binding = null;
    }

    private void setupHeroVideo() {
        Uri videoUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + R.raw.vleague_always_together);
        binding.videoHeroPromo.setVideoURI(videoUri);
        binding.videoHeroPromo.setOnPreparedListener(player -> {
            player.setLooping(true);
            player.setVolume(0f, 0f);
            binding.imgHeroPromo.setVisibility(View.GONE);
            binding.videoHeroPromo.setVisibility(View.VISIBLE);
            binding.videoHeroPromo.start();
        });
        binding.videoHeroPromo.setOnErrorListener((player, what, extra) -> {
            binding.videoHeroPromo.setVisibility(View.GONE);
            binding.imgHeroPromo.setVisibility(View.VISIBLE);
            return true;
        });
    }

    private void setupTicketBannerText() {
        binding.tvShopTicketSeason.setText("V.LEAGUE 25/26");
        binding.tvShopTicketStatus.setText("Đang mở bán");
        binding.tvShopTicketTitle.setText("ĐẶT VÉ MÙA GIẢI MỚI");
        binding.tvShopTicketDesc.setText("Chọn sân, chọn trận và giữ ghế yêu thích cho lịch đấu 25/26.");
        binding.btnShopTicket.setText("ĐẶT VÉ NGAY");
    }

    private void setupActions() {
        binding.bottomNav.navHome.setOnClickListener(v -> navigateTo(new HomepageFragment()));
        binding.bottomNav.navMatches.setOnClickListener(v -> navigateTo(new FixturesFragment()));
        binding.bottomNav.navNews.setOnClickListener(v -> navigateTo(new NewsFragment()));
        binding.bottomNav.navProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        binding.btnHeroShopNow.setOnClickListener(v -> binding.rvShopProducts.smoothScrollToPosition(0));
        binding.chipAll.setOnClickListener(v -> showProducts(allProducts));
        binding.chipJerseys.setOnClickListener(v -> showCategory(CATEGORY_BALLS));
        binding.chipSocks.setOnClickListener(v -> showCategory(CATEGORY_SOCKS));
        binding.chipAccessories.setOnClickListener(v -> showCategory(CATEGORY_ACCESSORIES));
        binding.actionTickets.setOnClickListener(v -> navigateTo(new TicketBookingFragment()));
        binding.cardShopTicketCta.setOnClickListener(v -> navigateTo(new TicketBookingFragment()));
        binding.actionJerseys.setOnClickListener(v -> showCategory(CATEGORY_BALLS));
        binding.actionFanGear.setOnClickListener(v -> showCategory(CATEGORY_ACCESSORIES));
        binding.appHeader.btnHeaderProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        HeaderNotificationRouter.bind(this, binding.appHeader);
    }

    private void loadShopData() {
        boolean hasSessionProducts = !SESSION_PRODUCTS.isEmpty();
        binding.progressShop.setVisibility(hasSessionProducts ? View.GONE : View.VISIBLE);
        binding.tvShopError.setVisibility(View.GONE);
        seedLeagueClubs();
        if (hasSessionProducts) {
            allProducts.clear();
            allProducts.addAll(SESSION_PRODUCTS);
            showProducts(allProducts);
            if (System.currentTimeMillis() - sessionProductsLoadedAt < SHOP_SESSION_TTL_MS || sessionFetchInFlight) {
                return;
            }
        } else {
            productAdapter.submitList(new ArrayList<>());
        }
        sessionFetchInFlight = true;
        VLeagueApiClient.getInstance().fetchShopProducts(new VLeagueApiClient.DataCallback<List<ShopProduct>>() {
            @Override
            public void onSuccess(List<ShopProduct> products) {
                sessionFetchInFlight = false;
                SESSION_PRODUCTS.clear();
                SESSION_PRODUCTS.addAll(products);
                sessionProductsLoadedAt = System.currentTimeMillis();
                if (binding == null) return;
                binding.progressShop.setVisibility(View.GONE);
                allProducts.clear();
                allProducts.addAll(products);
                showProducts(products);
                binding.tvShopError.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
                if (products.isEmpty()) {
                    binding.tvShopError.setText("Chưa có sản phẩm phù hợp để hiển thị.");
                }
            }

            @Override
            public void onError(Throwable throwable) {
                sessionFetchInFlight = false;
                if (binding == null) return;
                binding.progressShop.setVisibility(View.GONE);
                if (!allProducts.isEmpty()) return;
                allProducts.clear();
                productAdapter.submitList(new ArrayList<>());
                showShopMessage("Không tải được sản phẩm. Vui lòng kiểm tra kết nối và thử lại.");
            }
        });
    }

    private void showShopMessage(String message) {
        binding.tvShopError.setVisibility(View.VISIBLE);
        binding.tvShopError.setText(message);
    }

    private void seedLeagueClubs() {
        Map<String, ShopClub> clubsByName = new LinkedHashMap<>();
        for (ClubProfileDirectory.Meta meta : ClubProfileDirectory.all(requireContext())) {
            clubsByName.put(meta.key, new ShopClub(meta.name, meta.logoUrl(), meta.key));
        }
        allClubs.clear();
        allClubs.addAll(clubsByName.values());
        clubAdapter.submitList(new ArrayList<>(allClubs));
    }

    private void showProducts(List<ShopProduct> products) {
        setCategorySelected(CATEGORY_ALL);
        displayProducts(products);
    }

    private void displayProducts(List<ShopProduct> products) {
        productAdapter.submitList(new ArrayList<>(products));
    }

    private void showCategory(String category) {
        List<ShopProduct> filtered = new ArrayList<>();
        for (ShopProduct product : allProducts) {
            if (normalizeText(product.getCategory()).equals(normalizeText(category))) filtered.add(product);
        }
        setCategorySelected(category);
        displayProducts(filtered);
        binding.tvShopError.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        if (filtered.isEmpty()) binding.tvShopError.setText("Chưa có sản phẩm trong mục " + category + ".");
    }

    private void setCategorySelected(String selectedCategory) {
        if (binding == null) return;
        setChip(binding.chipAll, CATEGORY_ALL.equals(selectedCategory));
        setChip(binding.chipJerseys, CATEGORY_BALLS.equals(selectedCategory));
        setChip(binding.chipSocks, CATEGORY_SOCKS.equals(selectedCategory));
        setChip(binding.chipAccessories, CATEGORY_ACCESSORIES.equals(selectedCategory));
        if (CATEGORY_ALL.equals(selectedCategory)) binding.tvShopError.setVisibility(View.GONE);
    }

    private void setChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_shop_chip_selected : R.drawable.bg_shop_chip_unselected);
        chip.setTextColor(requireContext().getColor(selected ? R.color.white : R.color.stadium_ink));
    }

    private String normalizeText(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private Fragment profileOrAuth() {
        return AuthSession.hasToken(requireContext()) ? new ProfileFragment() : new AuthLoginFragment();
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
}

