package com.finalproject.v_league_ticket.presentation.profile;

public class UserProfileState {
    private final String userName;
    private final String email;

    public UserProfileState(String userName, String email) {
        this.userName = userName;
        this.email = email;
    }

    public String getUserName() { return userName; }
    public String getEmail() { return email; }
}
