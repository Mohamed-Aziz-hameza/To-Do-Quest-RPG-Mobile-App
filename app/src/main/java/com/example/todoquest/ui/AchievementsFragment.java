package com.example.todoquest.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.todoquest.databinding.FragmentAchievementsBinding;
import com.example.todoquest.viewmodel.AchievementsViewModel;

public class AchievementsFragment extends Fragment {

    private FragmentAchievementsBinding binding;
    private AchievementAdapter achievementAdapter;

    /** Creates and returns the achievements fragment view binding root. */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAchievementsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /** Configures the achievements grid and subscribes to achievement updates. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        achievementAdapter = new AchievementAdapter();
        binding.rvAchievements.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvAchievements.setAdapter(achievementAdapter);

        AchievementsViewModel achievementsViewModel =
                new ViewModelProvider(this).get(AchievementsViewModel.class);
        achievementsViewModel.getAchievements().observe(getViewLifecycleOwner(),
                achievements -> achievementAdapter.submitList(achievements));
    }

    /** Clears the binding reference when the fragment view is destroyed. */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
