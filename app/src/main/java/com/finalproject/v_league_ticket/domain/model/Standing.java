package com.finalproject.v_league_ticket.domain.model;

import java.util.Objects;

public class Standing {
    private final int position;
    private final Long teamId;
    private final String teamName;
    private final int played;
    private final int wins;
    private final int draws;
    private final int losses;
    private final int goalsFor;
    private final int goalsAgainst;
    private final int goalDifference;
    private final int points;
    private final String logoOverrideUrl;

    public Standing(int position, Long teamId, String teamName, int played, int wins, int draws, int losses,
                    int goalsFor, int goalsAgainst, int goalDifference, int points, String logoOverrideUrl) {
        this.position = position;
        this.teamId = teamId;
        this.teamName = teamName == null ? "" : teamName;
        this.played = played;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
        this.goalDifference = goalDifference;
        this.points = points;
        this.logoOverrideUrl = logoOverrideUrl == null ? "" : logoOverrideUrl;
    }

    public int getPosition() { return position; }
    public Long getTeamId() { return teamId; }
    public String getTeamName() { return teamName; }
    public int getPlayed() { return played; }
    public int getWins() { return wins; }
    public int getDraws() { return draws; }
    public int getLosses() { return losses; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public int getGoalDifference() { return goalDifference; }
    public int getPoints() { return points; }

    public String getLogoUrl() {
        if (!logoOverrideUrl.isEmpty()) return imageUrl(logoOverrideUrl);
        return teamId == null ? "" : "https://img.sofascore.com/api/v1/team/" + teamId + "/image";
    }

    private static String imageUrl(String value) {
        return (value == null ? "" : value)
                .replace("https://api.sofascore.app/api/v1/", "https://img.sofascore.com/api/v1/")
                .replace("https://api.sofascore.com/api/v1/", "https://img.sofascore.com/api/v1/")
                .replace("https://www.sofascore.com/api/v1/", "https://img.sofascore.com/api/v1/");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Standing)) return false;
        Standing standing = (Standing) o;
        return position == standing.position
                && played == standing.played
                && goalDifference == standing.goalDifference
                && points == standing.points
                && Objects.equals(teamId, standing.teamId)
                && Objects.equals(teamName, standing.teamName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, teamId, teamName, played, goalDifference, points);
    }
}
