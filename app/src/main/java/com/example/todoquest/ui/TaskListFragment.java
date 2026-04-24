package com.example.todoquest.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todoquest.R;
import com.example.todoquest.data.local.entity.Task;
import com.example.todoquest.databinding.FragmentTaskListBinding;
import com.example.todoquest.viewmodel.TaskViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class TaskListFragment extends Fragment {

    private FragmentTaskListBinding binding;
    private TaskViewModel taskViewModel;
    private TaskAdapter taskAdapter;
    private List<Task> latestTasks = new ArrayList<>();

    /** Creates and returns the task list fragment view binding root. */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /** Configures list rendering, interactions, and observers for the task list screen. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        taskAdapter = new TaskAdapter(
                task -> taskViewModel.completeTask(task),
                task -> taskViewModel.deleteTask(task)
        );

        binding.rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTasks.setAdapter(taskAdapter);

        taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
            List<Task> safeTasks = tasks == null ? new ArrayList<>() : tasks;
            latestTasks = new ArrayList<>(safeTasks);
            taskAdapter.submitList(new ArrayList<>(safeTasks));
            binding.tvEmptyState.setVisibility(safeTasks.isEmpty() ? View.VISIBLE : View.GONE);
        });

        ItemTouchHelper swipeHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int adapterPosition = viewHolder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= latestTasks.size()) {
                    taskAdapter.notifyDataSetChanged();
                    return;
                }

                Task deletedTask = latestTasks.get(adapterPosition);
                taskViewModel.deleteTask(deletedTask);

                Snackbar.make(binding.getRoot(), R.string.task_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, v -> taskViewModel.addTask(deletedTask))
                        .show();
            }
        });
        swipeHelper.attachToRecyclerView(binding.rvTasks);

        binding.fabAddTask.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.action_taskListFragment_to_addEditTaskFragment));
    }

    /** Clears the binding reference when the fragment view is destroyed. */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
