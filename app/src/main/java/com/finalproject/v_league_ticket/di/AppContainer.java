package com.finalproject.v_league_ticket.di;

import android.content.Context;

public final class AppContainer {
    private static Context appContext;

    private AppContainer() {
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Context appContext() {
        return appContext;
    }
}
