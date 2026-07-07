package com.finalproject.v_league_ticket.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeMatch {
    private final long id;
    private final Long homeTeamId;
    private final String homeTeamName;
    private final Long awayTeamId;
    private final String awayTeamName;
    private final Integer homeScore;
    private final Integer awayScore;
    private final String status;
    private final String statusType;
    private final String venue;
    private final Long startTimestamp;
    private final Integer currentMinute;
    private final Integer round;
    private final String roundName;
    private final String tournamentName;
    private final Integer homePenaltyScore;
    private final Integer awayPenaltyScore;
    private final List<GoalScorer> goalScorers;
    private final String homeLogoOverrideUrl;
    private final String awayLogoOverrideUrl;

    public HomeMatch(long id, Long homeTeamId, String homeTeamName, Long awayTeamId, String awayTeamName,
                     Integer homeScore, Integer awayScore, String status, String statusType, String venue,
                     Long startTimestamp, Integer currentMinute, Integer round, String roundName,
                     String tournamentName, Integer homePenaltyScore, Integer awayPenaltyScore,
                     List<GoalScorer> goalScorers, String homeLogoOverrideUrl, String awayLogoOverrideUrl) {
        this.id = id;
        this.homeTeamId = homeTeamId;
        this.homeTeamName = safe(homeTeamName);
        this.awayTeamId = awayTeamId;
        this.awayTeamName = safe(awayTeamName);
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = safe(status);
        this.statusType = safe(statusType);
        this.venue = safe(venue);
        this.startTimestamp = startTimestamp;
        this.currentMinute = currentMinute;
        this.round = round;
        this.roundName = safe(roundName);
        this.tournamentName = tournamentName == null || tournamentName.isEmpty() ? "V.League 1" : tournamentName;
        this.homePenaltyScore = homePenaltyScore;
        this.awayPenaltyScore = awayPenaltyScore;
        this.goalScorers = goalScorers == null ? Collections.emptyList() : new ArrayList<>(goalScorers);
        this.homeLogoOverrideUrl = safe(homeLogoOverrideUrl);
        this.awayLogoOverrideUrl = safe(awayLogoOverrideUrl);
    }

    public long getId() { return id; }
    public Long getHomeTeamId() { return homeTeamId; }
    public String getHomeTeamName() { return homeTeamName; }
    public Long getAwayTeamId() { return awayTeamId; }
    public String getAwayTeamName() { return awayTeamName; }
    public Integer getHomeScore() { return homeScore; }
    public Integer getAwayScore() { return awayScore; }
    public String getStatus() { return status; }
    public String getStatusType() { return statusType; }
    public String getVenue() { return venue; }
    public Long getStartTimestamp() { return startTimestamp; }
    public Integer getCurrentMinute() { return currentMinute; }
    public Integer getRound() { return round; }
    public String getRoundName() { return roundName; }
    public String getTournamentName() { return tournamentName; }
    public Integer getHomePenaltyScore() { return homePenaltyScore; }
    public Integer getAwayPenaltyScore() { return awayPenaltyScore; }
    public List<GoalScorer> getGoalScorers() { return Collections.unmodifiableList(goalScorers); }

    public String getHomeLogoUrl() {
        if (!homeLogoOverrideUrl.isEmpty()) return homeLogoOverrideUrl;
        return homeTeamId == null ? "" : "https://api.sofascore.app/api/v1/team/" + homeTeamId + "/image";
    }

    public String getAwayLogoUrl() {
        if (!awayLogoOverrideUrl.isEmpty()) return awayLogoOverrideUrl;
        return awayTeamId == null ? "" : "https://api.sofascore.app/api/v1/team/" + awayTeamId + "/image";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
