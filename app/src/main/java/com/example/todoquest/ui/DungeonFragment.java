package com.example.todoquest.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.todoquest.R;
import com.example.todoquest.data.repository.TaskRepository;
import com.example.todoquest.databinding.FragmentDungeonBinding;
import com.example.todoquest.viewmodel.HomeViewModel;

public class DungeonFragment extends Fragment {

    private FragmentDungeonBinding binding;
    private HomeViewModel homeViewModel;
    private int currentKeys;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDungeonBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        homeViewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) {
                return;
            }
            currentKeys = profile.dungeonKeys;
            binding.tvKeyCounter.setText(getString(R.string.keys_caps_line, currentKeys));
            boolean canEnter = currentKeys > 0;
            binding.btnEnterDungeon.setEnabled(canEnter);
            binding.btnEnterDungeon.setText(canEnter
                    ? getString(R.string.enter_dungeon)
                    : getString(R.string.need_key));
            binding.btnEnterDungeon.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(canEnter ? R.color.amber : R.color.dungeon_brown)
                    )
            );
        });

        binding.btnEnterDungeon.setOnClickListener(v -> consumeKeyAndEnter());
    }

    private void consumeKeyAndEnter() {
        TaskRepository repository = new TaskRepository(requireContext().getApplicationContext());
        repository.consumeDungeonKey(success -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!success) {
                    Toast.makeText(requireContext(), R.string.need_key, Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(new Intent(requireContext(), DungeonActivity.class));
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

