package com.finalproject.v_league_ticket.presentation.shop;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.databinding.FragmentCartBinding;
import com.finalproject.v_league_ticket.presentation.auth.AuthLoginFragment;
import com.finalproject.v_league_ticket.presentation.auth.AuthSession;

import java.util.List;

public class CartFragment extends Fragment implements CartStore.Listener {
    private FragmentCartBinding binding;
    private final CartItemAdapter cartAdapter = new CartItemAdapter((item, quantity) ->
            CartStore.updateQuantity(item.getProductId(), item.getSelectedSize(), quantity));

    public CartFragment() {
        super(R.layout.fragment_cart);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentCartBinding.bind(view);
        binding.rvCartItems.setAdapter(cartAdapter);
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnProceedToCheckout.setOnClickListener(v -> {
            if (CartStore.items().isEmpty()) toast("Giỏ hàng đang trống.");
            else if (!AuthSession.hasToken(requireContext())) navigateTo(new AuthLoginFragment());
            else navigateTo(new CheckoutFragment());
        });
        CartStore.addListener(this);
    }

    @Override
    public void onDestroyView() {
        CartStore.removeListener(this);
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onCartChanged(List<CartItem> items) {
        cartAdapter.submitList(items);
        if (binding != null) binding.tvTotalPrice.setText(CartStore.formatVnd(CartStore.subtotal()));
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName()).commit();
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
