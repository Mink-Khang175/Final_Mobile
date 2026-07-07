package com.finalproject.v_league_ticket.domain.model;

import java.util.List;

public final class VLeagueSeasons {
    public static final int SEASON_2024_25_ID = 202425;
    public static final int SEASON_2025_26_ID = 202526;
    public static final int SEASON_2023_24_ID = 202324;
    public static final int SEASON_2023_ID = 2023;
    public static final int SEASON_2022_ID = 2022;
    public static final int SEASON_2021_ID = 2021;

    private static final VLeagueSeason CURRENT =
            new VLeagueSeason(SEASON_2024_25_ID, "24/25", "2024-2025", 26, false);
    private static final VLeagueSeason NEXT =
            new VLeagueSeason(SEASON_2025_26_ID, "25/26", "2025-2026", 26, false);
    private static final VLeagueSeason PREVIOUS_2023_24 =
            new VLeagueSeason(SEASON_2023_24_ID, "23/24", "2023-2024", 26, false);
    private static final VLeagueSeason PREVIOUS_2023 =
            new VLeagueSeason(SEASON_2023_ID, "2023", "2023", 13, false);
    private static final VLeagueSeason PREVIOUS_2022 =
            new VLeagueSeason(SEASON_2022_ID, "2022", "2022", 26, false);
    private static final VLeagueSeason PREVIOUS_2021 =
            new VLeagueSeason(SEASON_2021_ID, "2021", "2021", 13, false);
    private static final List<VLeagueSeason> ALL = java.util.Arrays.asList(
            CURRENT,
            PREVIOUS_2023_24,
            PREVIOUS_2023,
            PREVIOUS_2022,
            PREVIOUS_2021,
            NEXT
    );

    private VLeagueSeasons() {
    }

    public static List<VLeagueSeason> all() {
        return ALL;
    }

    public static VLeagueSeason current() {
        return CURRENT;
    }

    public static VLeagueSeason next() {
        return NEXT;
    }

    public static int maxRoundForLabel(String label) {
        if (label == null || label.trim().isEmpty()) return 26;
        return 26;
    }

    public static String sportsDbSeason(String label) {
        if (label == null) return "";
        String value = label.trim();
        if (value.matches("\\d{2}/\\d{2}")) {
            int start = Integer.parseInt(value.substring(0, 2));
            int end = Integer.parseInt(value.substring(3, 5));
            return "20" + String.format("%02d", start) + "-20" + String.format("%02d", end);
        }
        return value;
    }
}
