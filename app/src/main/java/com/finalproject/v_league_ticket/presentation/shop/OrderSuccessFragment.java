package com.finalproject.v_league_ticket.presentation.shop;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.FragmentOrderSuccessBinding;
import com.finalproject.v_league_ticket.presentation.homepage.HomepageFragment;
import com.finalproject.v_league_ticket.presentation.profile.ProfileHistoryFragment;

public class OrderSuccessFragment extends Fragment {
    private static final String ARG_ORDER_ID = "order_id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_HEADLINE = "headline";
    private static final String ARG_SUBTITLE = "subtitle";
    private static final String ARG_MODE = "mode";
    private static final String MODE_ORDER = "order";
    private static final String MODE_TICKET = "ticket";
    private FragmentOrderSuccessBinding binding;

    public OrderSuccessFragment() {
        super(R.layout.fragment_order_success);
    }

    public static OrderSuccessFragment newInstance(String orderId) {
        OrderSuccessFragment fragment = new OrderSuccessFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_ID, orderId);
        args.putString(ARG_MODE, MODE_ORDER);
        args.putString(ARG_TITLE, "Đặt hàng");
        args.putString(ARG_HEADLINE, "Chờ xác nhận");
        args.putString(ARG_SUBTITLE, "Đơn hàng đã được ghi nhận và đang chờ admin xác nhận thanh toán.");
        fragment.setArguments(args);
        return fragment;
    }

    public static OrderSuccessFragment newTicketInstance(String orderId) {
        OrderSuccessFragment fragment = new OrderSuccessFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_ID, orderId);
        args.putString(ARG_MODE, MODE_TICKET);
        args.putString(ARG_TITLE, "Đặt vé");
        args.putString(ARG_HEADLINE, "Chờ xác nhận");
        args.putString(ARG_SUBTITLE, "Vé đã được giữ theo đơn đặt và đang chờ admin xác nhận thanh toán.");
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentOrderSuccessBinding.bind(view);
        Bundle args = requireArguments();
        String mode = args.getString(ARG_MODE, MODE_ORDER);
        binding.tvOrderId.setText(args.getString(ARG_ORDER_ID, ""));
        binding.tvOrderSuccessTitle.setText(args.getString(ARG_TITLE, "Đặt hàng"));
        binding.tvOrderSuccessHeadline.setText(args.getString(ARG_HEADLINE, "Thành công"));
        binding.tvOrderSuccessSubtitle.setText(args.getString(ARG_SUBTITLE, "Đơn hàng của bạn đã được ghi nhận."));
        binding.tvOrderLabel.setText(MODE_TICKET.equals(mode) ? "Mã vé" : "Mã đơn");
        binding.btnTrackOrder.setText(MODE_TICKET.equals(mode) ? "XEM VÉ CỦA TÔI" : "XEM ĐƠN HÀNG");
        binding.btnBackToHome.setText("VỀ TRANG CHỦ");
        binding.btnTrackOrder.setOnClickListener(v -> navigateTo(MODE_TICKET.equals(mode)
                ? ProfileHistoryFragment.tickets()
                : ProfileHistoryFragment.orders()));
        binding.btnBackToHome.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new HomepageFragment()).commit());
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName())
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
