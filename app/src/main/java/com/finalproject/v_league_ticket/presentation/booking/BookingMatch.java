package com.finalproject.v_league_ticket.presentation.booking;

public class BookingMatch {
    private final String id;
    private final long externalId;
    private final String city;
    private final String stadium;
    private final String address;
    private final String date;
    private final String time;
    private final String home;
    private final String away;
    private final String league;
    private final int basePrice;
    private final long startTimestamp;
    private final int round;

    public BookingMatch(String id, long externalId, String city, String stadium, String address,
                        String date, String time, String home, String away, String league,
                        int basePrice, long startTimestamp) {
        this(id, externalId, city, stadium, address, date, time, home, away, league, basePrice, startTimestamp, 0);
    }

    public BookingMatch(String id, long externalId, String city, String stadium, String address,
                        String date, String time, String home, String away, String league,
                        int basePrice, long startTimestamp, int round) {
        this.id = safe(id);
        this.externalId = externalId;
        this.city = safe(city);
        this.stadium = safe(stadium);
        this.address = safe(address);
        this.date = safe(date);
        this.time = safe(time);
        this.home = safe(home);
        this.away = safe(away);
        this.league = safe(league);
        this.basePrice = basePrice;
        this.startTimestamp = startTimestamp;
        this.round = Math.max(0, round);
    }

    public String getId() {
        return id;
    }

    public long getExternalId() {
        return externalId;
    }

    public String getCity() {
        return city;
    }

    public String getStadium() {
        return stadium;
    }

    public String getAddress() {
        return address;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getHome() {
        return home;
    }

    public String getAway() {
        return away;
    }

    public String getLeague() {
        return league;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public int getRound() {
        return round;
    }

    public String title() {
        return home + " vs " + away;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
