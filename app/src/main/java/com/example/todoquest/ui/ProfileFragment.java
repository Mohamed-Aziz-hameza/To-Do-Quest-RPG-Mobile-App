package com.example.todoquest.ui;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.todoquest.R;
import com.example.todoquest.data.local.entity.PlayerProfile;
import com.example.todoquest.data.repository.TaskRepository;
import com.example.todoquest.databinding.FragmentProfileBinding;
import com.example.todoquest.domain.XPEngine;
import com.example.todoquest.viewmodel.HomeViewModel;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private HomeViewModel homeViewModel;
    private TaskRepository repository;
    private final XPEngine xpEngine = new XPEngine();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new TaskRepository(requireContext().getApplicationContext());
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        homeViewModel.getProfile().observe(getViewLifecycleOwner(), this::renderProfile);

        binding.btnOpenShop.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.action_profileFragment_to_shopFragment));
    }

    private void renderProfile(PlayerProfile profile) {
        if (binding == null || profile == null) {
            return;
        }

        binding.ivProfileAvatar.setImageResource(mapAvatar(profile.avatarChoice));
        binding.tvProfileName.setText(profile.heroName == null || profile.heroName.isEmpty()
                ? getString(R.string.default_hero_name)
                : profile.heroName);
        binding.tvProfileClass.setText(getString(R.string.rookie_adventurer));

        int level = xpEngine.calculateLevel(profile.xp);
        binding.tvProfileLevel.setText(getString(R.string.profile_level, level));
        binding.tvProfileXpNext.setText(getString(R.string.profile_xp_to_next,
                xpEngine.getXpForNextLevel(level) - xpEngine.getXpProgressInCurrentLevel(profile.xp)));
        binding.tvProfileWins.setText(getString(R.string.profile_dungeon_wins, profile.dungeonWins));
        binding.tvProfileStreak.setText(getString(R.string.profile_streak, profile.streakDays));

        repository.runOnDiskIo(() -> {
            int completed = repository.getCompletedTaskCountSync();
            int bossWins = repository.getDungeonBossWinsSync();
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                binding.tvProfileTasks.setText(getString(R.string.profile_tasks_completed, completed));
                boolean badgeOneOn = completed >= 1;
                boolean badgeTwoOn = profile.dungeonWins >= 3;
                boolean badgeThreeOn = bossWins >= 1;
                tintBadge(binding.badgeOne, badgeOneOn);
                tintBadge(binding.badgeTwo, badgeTwoOn);
                tintBadge(binding.badgeThree, badgeThreeOn);
            });
        });

        binding.ivProfileWeapon.setVisibility(profile.equippedSword ? View.VISIBLE : View.INVISIBLE);
        binding.ivProfileShield.setVisibility(profile.equippedShield ? View.VISIBLE : View.INVISIBLE);
    }

    private void tintBadge(View badge, boolean on) {
        badge.getBackground();
        if (badge instanceof android.widget.ImageView) {
            ((android.widget.ImageView) badge).setColorFilter(
                    requireContext().getColor(on ? R.color.amber : R.color.dungeon_brown),
                    PorterDuff.Mode.SRC_IN
            );
        }
    }

    private int mapAvatar(int avatarChoice) {
        switch (avatarChoice) {
            case 1:
                return R.drawable.maleoption2;
            case 2:
                return R.drawable.femaleoption1;
            case 3:
                return R.drawable.femaleoption2;
            default:
                return R.drawable.maleoption1;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

