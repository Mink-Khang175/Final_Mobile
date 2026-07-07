package com.finalproject.v_league_ticket.presentation.news;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.data.remote.VLeagueApiClient;
import com.finalproject.v_league_ticket.databinding.FragmentNewsBinding;
import com.finalproject.v_league_ticket.domain.model.NewsPost;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.finalproject.v_league_ticket.presentation.matches.FixturesFragment;
import com.finalproject.v_league_ticket.presentation.profile.HeaderNotificationRouter;
import com.finalproject.v_league_ticket.presentation.profile.ProfileFragment;
import com.finalproject.v_league_ticket.presentation.shop.ShopFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewsFragment extends Fragment {
    private FragmentNewsBinding binding;
    private final NewsCategoryAdapter categoryAdapter = new NewsCategoryAdapter(this::selectCategory);
    private final NewsListAdapter newsAdapter = new NewsListAdapter(this::openNews, this::bookmarkNews);
    private List<NewsPost> posts;
    private NewsCategory selectedCategory = NewsCategory.VLEAGUE;

    public NewsFragment() {
        super(R.layout.fragment_news);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentNewsBinding.bind(view);
        posts = new ArrayList<>();
        setupLists();
        setupClicks();
        bindHeader();
        bindPosts(posts);
        loadNews(selectedCategory);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupLists() {
        binding.rvNewsCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvNewsCategories.setAdapter(categoryAdapter);
        binding.rvNewsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNewsList.setAdapter(newsAdapter);
    }

    private void setupClicks() {
        binding.btnRetryNews.setOnClickListener(v -> loadNews(selectedCategory));
        binding.featuredNews.cardFeaturedNews.setOnClickListener(v -> {
            if (!posts.isEmpty()) openNews(posts.get(0));
        });
        binding.bottomNav.navHome.setOnClickListener(v -> navigateTo(new HomepageFragment()));
        binding.bottomNav.navMatches.setOnClickListener(v -> navigateTo(new FixturesFragment()));
        binding.bottomNav.navShop.setOnClickListener(v -> navigateTo(new ShopFragment()));
        binding.bottomNav.navProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        binding.appHeader.btnHeaderProfile.setOnClickListener(v -> navigateTo(profileOrAuth()));
        HeaderNotificationRouter.bind(this, binding.appHeader);
    }

    private void bindHeader() {
        String name = AuthSession.userName(requireContext());
        binding.appHeader.tvHeaderSubtitle.setText(name == null ? "Khách" : name);
        binding.newsHeader.tvNewsTitle.setText("Tin tức");
        binding.newsHeader.tvNewsSubtitle.setText("Cập nhật tin nóng, lịch thi đấu và câu chuyện bên lề");
        binding.newsSearch.etNewsSearch.setHint("Tìm kiếm tin tức, CLB, cầu thủ...");
    }

    private void bindPosts(List<NewsPost> newsPosts) {
        boolean hasContent = !newsPosts.isEmpty();
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(hasContent ? View.GONE : View.VISIBLE);
        binding.layoutFeaturedHeader.setVisibility(hasContent ? View.VISIBLE : View.GONE);
        binding.featuredNews.getRoot().setVisibility(hasContent ? View.VISIBLE : View.GONE);
        binding.tvLatestNewsTitle.setVisibility(hasContent && newsPosts.size() > 1 ? View.VISIBLE : View.GONE);
        binding.rvNewsList.setVisibility(hasContent && newsPosts.size() > 1 ? View.VISIBLE : View.GONE);
        if (hasContent) {
            bindFeatured(newsPosts.get(0));
            newsAdapter.submitList(newsPosts.subList(1, newsPosts.size()));
        } else {
            newsAdapter.submitList(Collections.emptyList());
        }
    }

    private void bindFeatured(NewsPost post) {
        binding.featuredNews.tvFeaturedCategory.setText(post.getCategory().isEmpty() ? "NÓNG" : post.getCategory().toUpperCase());
        binding.featuredNews.tvFeaturedTitle.setText(post.getTitle());
        binding.featuredNews.tvFeaturedMeta.setText(post.getSourceName() + " - " + post.getPublishedAt());
        loadImage(binding.featuredNews.imgFeaturedNews, post.getImageUrl());
    }

    private void selectCategory(NewsCategory category) {
        selectedCategory = category;
        categoryAdapter.select(category);
        loadNews(category);
    }

    private void loadNews(NewsCategory category) {
        if (binding == null) return;
        binding.layoutLoading.setVisibility(View.VISIBLE);
        binding.layoutError.setVisibility(View.GONE);
        VLeagueApiClient.getInstance().fetchNews(category.getCategoryId(), new VLeagueApiClient.DataCallback<List<NewsPost>>() {
            @Override
            public void onSuccess(List<NewsPost> data) {
                if (binding == null) return;
                posts = data == null ? new ArrayList<>() : new ArrayList<>(data);
                bindPosts(posts);
            }

            @Override
            public void onError(Throwable throwable) {
                if (binding == null) return;
                binding.layoutLoading.setVisibility(View.GONE);
                if (posts == null || posts.isEmpty()) {
                    binding.layoutError.setVisibility(View.VISIBLE);
                    binding.layoutEmpty.setVisibility(View.GONE);
                    binding.layoutFeaturedHeader.setVisibility(View.GONE);
                    binding.featuredNews.getRoot().setVisibility(View.GONE);
                    binding.tvLatestNewsTitle.setVisibility(View.GONE);
                    binding.rvNewsList.setVisibility(View.GONE);
                    newsAdapter.submitList(Collections.emptyList());
                } else {
                    bindPosts(posts);
        toast("Không làm mới được tin tức lúc này.");
                }
            }
        });
    }

    private void openNews(NewsPost post) {
        if (post.getLink().isEmpty()) {
        toast("Tin này chưa có liên kết.");
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(post.getLink())));
        } catch (Exception ignored) {
        toast("Không mở được liên kết tin tức.");
        }
    }

    private void bookmarkNews(NewsPost post) {
        toast("Đã lưu tin tức.");
    }

    private void loadImage(ImageView imageView, String url) {
        Glide.with(imageView).load(url == null || url.isEmpty() ? null : url)
                .placeholder(R.drawable.svd).error(R.drawable.svd).centerCrop().into(imageView);
    }

    private Fragment profileOrAuth() {
        return AuthSession.hasToken(requireContext()) ? new ProfileFragment() : new AuthLoginFragment();
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName()).commit();
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}

