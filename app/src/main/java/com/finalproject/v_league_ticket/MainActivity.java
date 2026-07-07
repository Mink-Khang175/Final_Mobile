package com.finalproject.v_league_ticket;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.finalproject.v_league_ticket.data.firebase.FirebaseSchemaSeeder;
import com.finalproject.v_league_ticket.di.AppContainer;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;

public class MainActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContainer.init(getApplicationContext());
        FirebaseSchemaSeeder.ensureBaseSchemas();
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setBackgroundColor(getColor(R.color.colorBackground));
        getWindow().setStatusBarColor(getColor(R.color.colorSurface));
        getWindow().setNavigationBarColor(getColor(R.color.colorSurface));

        int flags = getWindow().getDecorView().getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new HomepageFragment())
                    .commit();
        }
    }
}
