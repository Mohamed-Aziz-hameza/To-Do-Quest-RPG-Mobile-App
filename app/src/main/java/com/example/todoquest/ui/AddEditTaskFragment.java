package com.example.todoquest.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.todoquest.R;
import com.example.todoquest.data.local.entity.Task;
import com.example.todoquest.databinding.FragmentAddEditTaskBinding;
import com.example.todoquest.viewmodel.TaskViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class AddEditTaskFragment extends Fragment {

    private static final String[] PRIORITIES = {"EASY", "MEDIUM", "HARD", "BOSS"};

    private FragmentAddEditTaskBinding binding;
    private TaskViewModel taskViewModel;

    /** Creates and returns the add/edit task fragment view binding root. */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /** Configures form visibility rules and save action handlers. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                PRIORITIES
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                stylePriorityItem(view, getItem(position));
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                stylePriorityItem(view, getItem(position));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spPriority.setAdapter(adapter);
        binding.spPriority.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
                if (selectedView instanceof TextView) {
                    stylePriorityItem((TextView) selectedView, PRIORITIES[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });

        binding.cbRaidMode.setOnCheckedChangeListener((buttonView, isChecked) -> toggleRaidInputs(isChecked));
        binding.btnRaidGenerate.setOnClickListener(v -> {
            String generated = taskViewModel.generateRaidRoomCode();
            binding.etRaidRoomCode.setText(generated);
        });
        binding.btnRaidHost.setOnClickListener(v -> performHostCreate());
        binding.btnRaidJoin.setOnClickListener(v -> performJoinRaid());

        binding.btnSaveTask.setOnClickListener(v -> saveTask(v));
    }

    /** Clears the binding reference when the fragment view is destroyed. */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void saveTask(View anchorView) {
        String title = binding.etTaskTitle.getText() == null
                ? ""
                : binding.etTaskTitle.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            binding.etTaskTitle.setError(getString(R.string.title_required));
            return;
        }

        Task task = new Task();
        task.title = title;
        task.description = "";
        task.priority = String.valueOf(binding.spPriority.getSelectedItem());
        task.completed = false;
        task.createdAt = System.currentTimeMillis();
        task.dueDate = 0L;
        task.subTasks = "";
        task.bossHpTotal = 0;
        task.bossHpRemaining = 0;
        task.remoteId = null;
        task.ownerUid = null;
        task.updatedAt = System.currentTimeMillis();

        boolean raidMode = binding.cbRaidMode.isChecked();
        task.raidTask = raidMode;
        if (raidMode) {
            String roomCode = readRoomCode();
            if (TextUtils.isEmpty(roomCode)) {
                binding.etRaidRoomCode.setError(getString(R.string.raid_room_required));
                return;
            }
            task.roomCode = roomCode;
            task.assignedTo = textOrEmpty(binding.etRaidAssignedTo.getText() == null
                    ? null
                    : binding.etRaidAssignedTo.getText().toString());
            task.raidBossHpContribution = parseHpContribution();

            taskViewModel.addRaidTask(task, (success, message) -> {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        Navigation.findNavController(anchorView).navigateUp();
                    } else {
                        showMessage(anchorView, TextUtils.isEmpty(message) ? getString(R.string.raid_failed) : message);
                    }
                });
            });
            return;
        }

        task.roomCode = "";
        task.assignedTo = "";
        task.raidBossHpContribution = 0;

        taskViewModel.addTask(task);
        Navigation.findNavController(anchorView).navigateUp();
    }

    private void performHostCreate() {
        String roomCode = readRoomCode();
        if (TextUtils.isEmpty(roomCode)) {
            binding.etRaidRoomCode.setError(getString(R.string.raid_room_required));
            return;
        }
        taskViewModel.createRaidRoom(roomCode, "Fire Raid Boss", (success, message) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> showMessage(
                    binding.getRoot(),
                    success ? getString(R.string.raid_created) : (TextUtils.isEmpty(message) ? getString(R.string.raid_failed) : message)
            ));
        });
    }

    private void performJoinRaid() {
        String roomCode = readRoomCode();
        if (TextUtils.isEmpty(roomCode)) {
            binding.etRaidRoomCode.setError(getString(R.string.raid_room_required));
            return;
        }
        taskViewModel.joinRaidRoom(roomCode, (success, message) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> showMessage(
                    binding.getRoot(),
                    success ? getString(R.string.raid_joined) : (TextUtils.isEmpty(message) ? getString(R.string.raid_failed) : message)
            ));
        });
    }

    private void toggleRaidInputs(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        binding.tilRaidRoom.setVisibility(visibility);
        binding.layoutRaidActions.setVisibility(visibility);
        binding.tilRaidAssign.setVisibility(visibility);
        binding.tilRaidHp.setVisibility(visibility);
    }

    private String readRoomCode() {
        if (binding.etRaidRoomCode.getText() == null) {
            return "";
        }
        return binding.etRaidRoomCode.getText().toString().trim().toUpperCase(Locale.US);
    }

    private int parseHpContribution() {
        String raw = binding.etRaidHp.getText() == null ? "" : binding.etRaidHp.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private void showMessage(View anchor, String message) {
        Snackbar.make(anchor, message, Snackbar.LENGTH_LONG).show();
    }

    private void stylePriorityItem(TextView view, String priority) {
        if (view == null) {
            return;
        }
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(24, 16, 24, 16);
        view.setText(priority);

        int colorRes;
        switch (priority) {
            case "MEDIUM":
                colorRes = R.color.priority_medium;
                break;
            case "HARD":
                colorRes = R.color.priority_hard;
                break;
            case "BOSS":
                colorRes = R.color.priority_boss;
                break;
            case "EASY":
            default:
                colorRes = R.color.parchment;
                break;
        }
        view.setTextColor(requireContext().getColor(colorRes));
    }
}
