package com.finalproject.v_league_ticket.presentation.profile;

import androidx.fragment.app.Fragment;

import android.view.View;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.ComponentAppHeaderBinding;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;

public final class HeaderNotificationRouter {
    private HeaderNotificationRouter() {
    }

    public static void bind(Fragment fragment, ComponentAppHeaderBinding header) {
        String uid = AuthSession.uid(fragment.requireContext());
        header.viewHeaderNotificationDot.setVisibility(View.GONE);
        NotificationReadStore.loadUnreadCount(uid, count -> {
            if (fragment.getView() == null) return;
            header.viewHeaderNotificationDot.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        });
        header.btnHeaderNotification.setOnClickListener(v ->
                fragment.getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, AuthSession.hasToken(fragment.requireContext())
                                ? ProfileHistoryFragment.notifications()
                                : new AuthLoginFragment())
                        .addToBackStack("Notifications")
                        .commit());
    }
}
