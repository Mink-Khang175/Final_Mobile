package com.finalproject.v_league_ticket.domain.model;

public class GoalScorer {
    private final Integer minute;
    private final String playerName;
    private final Boolean home;
    private final String scoreAfterGoal;

    public GoalScorer(Integer minute, String playerName, Boolean home, String scoreAfterGoal) {
        this.minute = minute;
        this.playerName = playerName == null ? "" : playerName;
        this.home = home;
        this.scoreAfterGoal = scoreAfterGoal == null ? "" : scoreAfterGoal;
    }

    public Integer getMinute() { return minute; }
    public String getPlayerName() { return playerName; }
    public Boolean isHome() { return home; }
    public String getScoreAfterGoal() { return scoreAfterGoal; }
}
