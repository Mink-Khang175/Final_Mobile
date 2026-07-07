package com.finalproject.v_league_ticket.domain.model;

public class VLeagueSeason {
    private final int id;
    private final String label;
    private final String sportsDbSeason;
    private final int maxRound;
    private final boolean hasFinalRound;

    public VLeagueSeason(int id, String label, String sportsDbSeason, int maxRound) {
        this(id, label, sportsDbSeason, maxRound, true);
    }

    public VLeagueSeason(int id, String label, String sportsDbSeason, int maxRound, boolean hasFinalRound) {
        this.id = id;
        this.label = label == null ? "" : label;
        this.sportsDbSeason = sportsDbSeason == null ? "" : sportsDbSeason;
        this.maxRound = maxRound;
        this.hasFinalRound = hasFinalRound;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }
    public String getSportsDbSeason() { return sportsDbSeason; }
    public int getMaxRound() { return maxRound; }
    public boolean hasFinalRound() { return hasFinalRound; }
}
