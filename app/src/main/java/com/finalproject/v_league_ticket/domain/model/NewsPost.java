package com.finalproject.v_league_ticket.domain.model;

import java.util.Objects;

public class NewsPost {
    private final long id;
    private final String title;
    private final String excerpt;
    private final String category;
    private final String imageUrl;
    private final String publishedAt;
    private final String link;
    private final String sourceName;

    public NewsPost(long id, String title, String excerpt, String category, String imageUrl,
                    String publishedAt, String link, String sourceName) {
        this.id = id;
        this.title = safe(title);
        this.excerpt = safe(excerpt);
        this.category = safe(category);
        this.imageUrl = safe(imageUrl);
        this.publishedAt = safe(publishedAt);
        this.link = safe(link);
        this.sourceName = sourceName == null || sourceName.isEmpty() ? "VPF" : sourceName;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getExcerpt() { return excerpt; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public String getPublishedAt() { return publishedAt; }
    public String getLink() { return link; }
    public String getSourceName() { return sourceName; }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NewsPost)) return false;
        NewsPost newsPost = (NewsPost) o;
        return id == newsPost.id
                && Objects.equals(title, newsPost.title)
                && Objects.equals(excerpt, newsPost.excerpt)
                && Objects.equals(category, newsPost.category)
                && Objects.equals(imageUrl, newsPost.imageUrl)
                && Objects.equals(publishedAt, newsPost.publishedAt)
                && Objects.equals(link, newsPost.link)
                && Objects.equals(sourceName, newsPost.sourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, excerpt, category, imageUrl, publishedAt, link, sourceName);
    }
}
