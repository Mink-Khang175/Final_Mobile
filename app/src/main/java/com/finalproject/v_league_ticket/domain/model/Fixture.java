package com.finalproject.v_league_ticket.domain.model;

import java.util.Objects;

public class Fixture {
    private final long id;
    private final Long homeTeamId;
    private final Long awayTeamId;
    private final String homeTeamName;
    private final String awayTeamName;
    private final Integer homeScore;
    private final Integer awayScore;
    private final Integer homePenaltyScore;
    private final Integer awayPenaltyScore;
    private final String status;
    private final String statusType;
    private final Long startTimestamp;
    private final String venue;
    private final Integer round;
    private final String roundName;
    private final String homeLogoOverrideUrl;
    private final String awayLogoOverrideUrl;

    public Fixture(long id, Long homeTeamId, Long awayTeamId, String homeTeamName, String awayTeamName,
                   Integer homeScore, Integer awayScore, Integer homePenaltyScore, Integer awayPenaltyScore,
                   String status, String statusType, Long startTimestamp, String venue, Integer round,
                   String homeLogoOverrideUrl, String awayLogoOverrideUrl) {
        this(id, homeTeamId, awayTeamId, homeTeamName, awayTeamName, homeScore, awayScore, homePenaltyScore,
                awayPenaltyScore, status, statusType, startTimestamp, venue, round, "", homeLogoOverrideUrl,
                awayLogoOverrideUrl);
    }

    public Fixture(long id, Long homeTeamId, Long awayTeamId, String homeTeamName, String awayTeamName,
                   Integer homeScore, Integer awayScore, Integer homePenaltyScore, Integer awayPenaltyScore,
                   String status, String statusType, Long startTimestamp, String venue, Integer round,
                   String roundName, String homeLogoOverrideUrl, String awayLogoOverrideUrl) {
        this.id = id;
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
        this.homeTeamName = safe(homeTeamName);
        this.awayTeamName = safe(awayTeamName);
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.homePenaltyScore = homePenaltyScore;
        this.awayPenaltyScore = awayPenaltyScore;
        this.status = safe(status);
        this.statusType = safe(statusType);
        this.startTimestamp = startTimestamp;
        this.venue = safe(venue);
        this.round = round;
        this.roundName = safe(roundName);
        this.homeLogoOverrideUrl = safe(homeLogoOverrideUrl);
        this.awayLogoOverrideUrl = safe(awayLogoOverrideUrl);
    }

    public long getId() { return id; }
    public Long getHomeTeamId() { return homeTeamId; }
    public Long getAwayTeamId() { return awayTeamId; }
    public String getHomeTeamName() { return homeTeamName; }
    public String getAwayTeamName() { return awayTeamName; }
    public Integer getHomeScore() { return homeScore; }
    public Integer getAwayScore() { return awayScore; }
    public Integer getHomePenaltyScore() { return homePenaltyScore; }
    public Integer getAwayPenaltyScore() { return awayPenaltyScore; }
    public String getStatus() { return status; }
    public String getStatusType() { return statusType; }
    public Long getStartTimestamp() { return startTimestamp; }
    public String getVenue() { return venue; }
    public Integer getRound() { return round; }
    public String getRoundName() { return roundName; }

    public String getHomeLogoUrl() {
        if (!homeLogoOverrideUrl.isEmpty()) return homeLogoOverrideUrl;
        return homeTeamId == null ? "" : "https://api.sofascore.app/api/v1/team/" + homeTeamId + "/image";
    }

    public String getAwayLogoUrl() {
        if (!awayLogoOverrideUrl.isEmpty()) return awayLogoOverrideUrl;
        return awayTeamId == null ? "" : "https://api.sofascore.app/api/v1/team/" + awayTeamId + "/image";
    }

    public boolean hasScore() {
        return homeScore != null && awayScore != null;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fixture)) return false;
        Fixture fixture = (Fixture) o;
        return id == fixture.id
                && Objects.equals(homeTeamId, fixture.homeTeamId)
                && Objects.equals(awayTeamId, fixture.awayTeamId)
                && Objects.equals(homeTeamName, fixture.homeTeamName)
                && Objects.equals(awayTeamName, fixture.awayTeamName)
                && Objects.equals(homeScore, fixture.homeScore)
                && Objects.equals(awayScore, fixture.awayScore)
                && Objects.equals(status, fixture.status)
                && Objects.equals(statusType, fixture.statusType)
                && Objects.equals(startTimestamp, fixture.startTimestamp)
                && Objects.equals(venue, fixture.venue)
                && Objects.equals(round, fixture.round)
                && Objects.equals(roundName, fixture.roundName)
                && Objects.equals(homePenaltyScore, fixture.homePenaltyScore)
                && Objects.equals(awayPenaltyScore, fixture.awayPenaltyScore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, homeTeamId, awayTeamId, homeTeamName, awayTeamName, homeScore, awayScore,
                homePenaltyScore, awayPenaltyScore, status, statusType, startTimestamp, venue, round, roundName);
    }
}
