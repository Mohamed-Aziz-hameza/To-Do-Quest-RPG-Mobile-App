package com.example.todoquest.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.todoquest.MainActivity;
import com.example.todoquest.R;
import com.example.todoquest.data.repository.TaskRepository;
import com.example.todoquest.databinding.ActivityCharacterCreationBinding;

public class CharacterCreationActivity extends AppCompatActivity {

    private ActivityCharacterCreationBinding binding;
    private TaskRepository repository;

    private boolean isMale = true;
    private int selectedSlot = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new TaskRepository(getApplicationContext());

        if (!repository.isUserSignedIn()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        repository.runOnDiskIo(() -> {
            String heroName = repository.getOrCreateProfileSync().heroName;
            boolean hasLocalUser = !TextUtils.isEmpty(heroName);
            runOnUiThread(() -> {
                if (hasLocalUser) {
                    goToMain();
                    return;
                }

                binding = ActivityCharacterCreationBinding.inflate(getLayoutInflater());
                setContentView(binding.getRoot());
                setupUi();
            });
        });
    }

    private void setupUi() {
        renderGender();
        renderAvatarOptions();

        binding.btnMale.setOnClickListener(v -> {
            isMale = true;
            selectedSlot = 0;
            renderGender();
            renderAvatarOptions();
        });

        binding.btnFemale.setOnClickListener(v -> {
            isMale = false;
            selectedSlot = 0;
            renderGender();
            renderAvatarOptions();
        });

        binding.btnAvatarOne.setOnClickListener(v -> {
            selectedSlot = 0;
            renderAvatarSelection();
        });

        binding.btnAvatarTwo.setOnClickListener(v -> {
            selectedSlot = 1;
            renderAvatarSelection();
        });

        binding.btnBegin.setOnClickListener(v -> confirmHero());
    }

    private void confirmHero() {
        String heroName = binding.etHeroName.getText() == null
                ? ""
                : binding.etHeroName.getText().toString().trim();

        if (heroName.isEmpty()) {
            Toast.makeText(this, R.string.hero_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        int avatarChoice = mapAvatarChoice();
        repository.createHeroProfile(heroName, avatarChoice, "ROOKIE_ADVENTURER");
        goToMain();
    }

    private int mapAvatarChoice() {
        if (isMale) {
            return selectedSlot == 0 ? 0 : 1;
        }
        return selectedSlot == 0 ? 2 : 3;
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void renderGender() {
        int selectedBg = getColor(R.color.amber);
        int unselectedBg = getColor(R.color.dungeon_stone_mid);
        int selectedFg = getColor(R.color.dungeon_stone_dark);
        int unselectedFg = getColor(R.color.parchment);

        binding.btnMale.setBackgroundTintList(android.content.res.ColorStateList.valueOf(isMale ? selectedBg : unselectedBg));
        binding.btnFemale.setBackgroundTintList(android.content.res.ColorStateList.valueOf(isMale ? unselectedBg : selectedBg));
        binding.btnMale.setTextColor(isMale ? selectedFg : unselectedFg);
        binding.btnFemale.setTextColor(isMale ? unselectedFg : selectedFg);
    }

    private void renderAvatarOptions() {
        if (isMale) {
            binding.btnAvatarOne.setImageResource(R.drawable.maleoption1);
            binding.btnAvatarTwo.setImageResource(R.drawable.maleoption2);
        } else {
            binding.btnAvatarOne.setImageResource(R.drawable.femaleoption1);
            binding.btnAvatarTwo.setImageResource(R.drawable.femaleoption2);
        }
        renderAvatarSelection();
    }

    private void renderAvatarSelection() {
        applyAvatarBorder(binding.btnAvatarOne, selectedSlot == 0);
        applyAvatarBorder(binding.btnAvatarTwo, selectedSlot == 1);
    }

    private void applyAvatarBorder(ImageButton button, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(getColor(R.color.dungeon_stone_mid));
        drawable.setCornerRadius(16f);
        drawable.setStroke(selected ? 6 : 2, getColor(selected ? R.color.amber : R.color.amber_dim));
        button.setBackground(drawable);
    }
}

