package com.example.todoquest.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.todoquest.R;
import com.example.todoquest.data.local.entity.PlayerProfile;
import com.example.todoquest.databinding.FragmentShopBinding;
import com.example.todoquest.viewmodel.HomeViewModel;

public class ShopFragment extends Fragment {

    private FragmentShopBinding binding;
    private HomeViewModel homeViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentShopBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        homeViewModel.getProfile().observe(getViewLifecycleOwner(), this::render);

        binding.btnSwordAction.setOnClickListener(v -> homeViewModel.toggleSword(this::onActionResult));
        binding.btnShieldAction.setOnClickListener(v -> homeViewModel.toggleShield(this::onActionResult));
    }

    private void onActionResult(boolean success, String message) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void render(PlayerProfile profile) {
        if (binding == null || profile == null) {
            return;
        }
        binding.tvShopCoins.setText(getString(R.string.coins_line, profile.coins));
        binding.btnSwordAction.setText(resolveSwordState(profile));
        binding.btnShieldAction.setText(resolveShieldState(profile));
    }

    private String resolveSwordState(PlayerProfile profile) {
        if (!profile.ownsSword) {
            return getString(R.string.buy);
        }
        return profile.equippedSword ? getString(R.string.unequip) : getString(R.string.equip);
    }

    private String resolveShieldState(PlayerProfile profile) {
        if (!profile.ownsShield) {
            return getString(R.string.buy);
        }
        return profile.equippedShield ? getString(R.string.unequip) : getString(R.string.equip);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
