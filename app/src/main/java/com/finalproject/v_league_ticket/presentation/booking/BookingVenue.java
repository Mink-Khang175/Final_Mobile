package com.finalproject.v_league_ticket.presentation.booking;

public class BookingVenue {
    private final int id;
    private final String clubCode;
    private final String clubName;
    private final String stadiumName;
    private final String city;
    private final String address;

    public BookingVenue(int id, String clubCode, String clubName, String stadiumName, String city, String address) {
        this.id = id;
        this.clubCode = safe(clubCode);
        this.clubName = safe(clubName);
        this.stadiumName = safe(stadiumName);
        this.city = safe(city);
        this.address = safe(address);
    }

    public int getId() {
        return id;
    }

    public String getClubCode() {
        return clubCode;
    }

    public String getClubName() {
        return clubName;
    }

    public String getStadiumName() {
        return stadiumName;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public String key() {
        return stadiumName + "|" + city;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
