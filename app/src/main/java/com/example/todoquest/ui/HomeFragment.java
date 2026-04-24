package com.example.todoquest.ui;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.todoquest.R;
import com.example.todoquest.data.local.entity.PlayerProfile;
import com.example.todoquest.databinding.FragmentHomeBinding;
import com.example.todoquest.domain.XPEngine;
import com.example.todoquest.viewmodel.HomeViewModel;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private final XPEngine xpEngine = new XPEngine();
    private int lastKnownKeys = -1;

    /** Creates and returns the home fragment view binding root. */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /** Subscribes to profile updates once the fragment view is created. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.refreshDailyState();
        homeViewModel.getProfile().observe(getViewLifecycleOwner(), this::renderProfile);

        binding.btnWeaponSlot.setOnClickListener(v -> askSwordToggle());
        binding.btnShieldSlot.setOnClickListener(v -> askShieldToggle());
        binding.btnAllocateStats.setOnClickListener(v -> openAllocateStatsDialog());
    }

    private void renderProfile(PlayerProfile profile) {
        if (binding == null || profile == null) {
            return;
        }

        int level = Math.max(1, xpEngine.calculateLevel(profile.xp));
        int xpInLevel = xpEngine.getXpProgressInCurrentLevel(profile.xp);
        int xpNeeded = xpEngine.getXpForNextLevel(level);
        int progress = (int) ((xpInLevel / (float) xpNeeded) * 100f);

        binding.tvHeroName.setText(profile.heroName == null || profile.heroName.trim().isEmpty()
                ? getString(R.string.default_hero_name) : profile.heroName);
        binding.tvClassBadge.setText(getString(R.string.rookie_adventurer));

        binding.ivHeroAvatar.setImageResource(mapAvatar(profile.avatarChoice));

        int maxHealth = Math.max(1, profile.baseHealth);
        int currentHealth = Math.max(0, Math.min(maxHealth, profile.currentHealth));
        binding.pbHealth.setMax(maxHealth);
        binding.pbHealth.setProgress(currentHealth);
        binding.tvHealth.setText(getString(R.string.health_line, currentHealth, maxHealth));
        binding.tvStrength.setText(getString(R.string.strength_line, profile.effectiveAttack));
        binding.tvSpeed.setText(getString(R.string.speed_line, profile.effectiveSpeed));
        binding.tvDefense.setText(getString(R.string.defense_line, profile.effectiveDefense));

        // Show in-level XP progress, not total XP, to avoid confusion on level thresholds.
        binding.tvXpLabel.setText(getString(R.string.xp_line, xpInLevel, xpNeeded, level));
        ObjectAnimator.ofInt(binding.pbXp, "progress", binding.pbXp.getProgress(), Math.max(0, Math.min(100, progress)))
                .setDuration(500)
                .start();

        binding.tvStreak.setText(getString(R.string.streak_line, profile.streakDays));
        binding.tvCoins.setText(getString(R.string.coins_line, profile.coins));
        binding.tvKeys.setText(getString(R.string.keys_line, profile.dungeonKeys));
        binding.tvFreeStats.setText(getString(R.string.free_stats_line, profile.freeStatPoints));
        binding.btnAllocateStats.setEnabled(profile.freeStatPoints > 0);

        if (lastKnownKeys >= 0 && profile.dungeonKeys > lastKnownKeys) {
            animateKeyGain();
        }
        lastKnownKeys = profile.dungeonKeys;

        binding.btnWeaponSlot.setImageResource(profile.equippedSword ? R.drawable.sword : android.R.color.transparent);
        binding.btnShieldSlot.setImageResource(profile.equippedShield ? R.drawable.shield : android.R.color.transparent);

        binding.cbDailyCreated.setChecked(profile.dailyTaskCreated);
        binding.cbDailyCompleted.setChecked(profile.dailyTaskCompleted);
        binding.cbDailyCombo.setChecked(profile.dailyTaskCreated && profile.dailyTaskCompleted);
    }

    private void askSwordToggle() {
        if (binding == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.weapon_slot)
                .setMessage(R.string.toggle_weapon_prompt)
                .setPositiveButton(R.string.yes, (dialog, which) -> homeViewModel.toggleSword(this::showResult))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void askShieldToggle() {
        if (binding == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.shield_slot)
                .setMessage(R.string.toggle_shield_prompt)
                .setPositiveButton(R.string.yes, (dialog, which) -> homeViewModel.toggleShield(this::showResult))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void openAllocateStatsDialog() {
        if (binding == null) {
            return;
        }

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * requireContext().getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);

        EditText etStrength = buildNumberInput(getString(R.string.allocate_strength_hint));
        EditText etSpeed = buildNumberInput(getString(R.string.allocate_speed_hint));
        EditText etHealth = buildNumberInput(getString(R.string.allocate_health_hint));
        container.addView(etStrength);
        container.addView(etSpeed);
        container.addView(etHealth);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.allocate_stats)
                .setMessage(R.string.allocate_stats_message)
                .setView(container)
                .setPositiveButton(R.string.apply_points, (dialog, which) -> {
                    int strength = parseNonNegative(etStrength.getText() == null ? "" : etStrength.getText().toString());
                    int speed = parseNonNegative(etSpeed.getText() == null ? "" : etSpeed.getText().toString());
                    int health = parseNonNegative(etHealth.getText() == null ? "" : etHealth.getText().toString());
                    homeViewModel.allocateStats(strength, speed, health, this::showResult);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private EditText buildNumberInput(String hint) {
        EditText editText = new EditText(requireContext());
        editText.setHint(hint);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        return editText;
    }

    private int parseNonNegative(String raw) {
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (Exception exception) {
            return 0;
        }
    }

    private void showResult(boolean success, String message) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void animateKeyGain() {
        TranslateAnimation animation = new TranslateAnimation(0f, 0f, -40f, 0f);
        animation.setDuration(350);
        binding.tvKeys.startAnimation(animation);
    }

    private int mapAvatar(int avatarChoice) {
        switch (avatarChoice) {
            case 1:
                return R.drawable.maleoption2;
            case 2:
                return R.drawable.femaleoption1;
            case 3:
                return R.drawable.femaleoption2;
            case 0:
            default:
                return R.drawable.maleoption1;
        }
    }

    /** Clears the binding reference when the view is destroyed. */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
