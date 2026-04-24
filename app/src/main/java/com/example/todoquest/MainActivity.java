package com.example.todoquest;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.todoquest.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNav, navController);

            // Keep bottom-nav selection in sync even when navigating to child screens.
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                int menuTargetId = destinationId;
                if (destinationId == R.id.shopFragment) {
                    menuTargetId = R.id.profileFragment;
                } else if (destinationId == R.id.addEditTaskFragment) {
                    menuTargetId = R.id.taskListFragment;
                }

                if (binding.bottomNav.getMenu().findItem(menuTargetId) != null) {
                    // Marking as checked avoids triggering a second navigation event loop.
                    binding.bottomNav.getMenu().findItem(menuTargetId).setChecked(true);
                }
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            int left = binding.bottomNav.getPaddingLeft();
            int top = binding.bottomNav.getPaddingTop();
            int right = binding.bottomNav.getPaddingRight();
            binding.bottomNav.setPadding(left, top, right, systemBars.bottom);
            return insets;
        });
    }
}