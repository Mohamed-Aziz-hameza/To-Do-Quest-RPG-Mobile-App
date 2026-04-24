package com.example.todoquest.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todoquest.data.local.entity.Task;
import com.example.todoquest.R;
import com.example.todoquest.databinding.ItemTaskBinding;
import com.example.todoquest.domain.XPEngine;

import java.util.Objects;
import java.util.function.Consumer;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private static final String COLOR_EASY = "#8A8A8A";
    private static final String COLOR_MEDIUM = "#3B82F6";
    private static final String COLOR_HARD = "#8B5CF6";
    private static final String COLOR_BOSS = "#E8A838";

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.completed == newItem.completed
                    && Objects.equals(oldItem.title, newItem.title)
                    && Objects.equals(oldItem.priority, newItem.priority);
        }
    };

    private final Consumer<Task> onComplete;
    private final Consumer<Task> onDelete;
    private final XPEngine xpEngine = new XPEngine();

    /**
     * Creates an adapter with completion and delete callbacks.
     *
     * @param onComplete callback for completion actions
     * @param onDelete callback for delete actions
     */
    public TaskAdapter(Consumer<Task> onComplete, Consumer<Task> onDelete) {
        super(DIFF_CALLBACK);
        this.onComplete = onComplete;
        this.onDelete = onDelete;
    }

    /**
     * Creates a new task row view holder.
     *
     * @param parent parent RecyclerView
     * @param viewType view type id
     * @return task view holder
     */
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemTaskBinding binding = ItemTaskBinding.inflate(inflater, parent, false);
        return new TaskViewHolder(binding, onComplete, onDelete, xpEngine);
    }

    /**
     * Binds task content into a row.
     *
     * @param holder target view holder
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public static final class TaskViewHolder extends RecyclerView.ViewHolder {

        private final ItemTaskBinding binding;
        private final Consumer<Task> onComplete;
        private final Consumer<Task> onDelete;
        private final XPEngine xpEngine;

        public TaskViewHolder(ItemTaskBinding binding,
                              Consumer<Task> onComplete,
                              Consumer<Task> onDelete,
                              XPEngine xpEngine) {
            super(binding.getRoot());
            this.binding = binding;
            this.onComplete = onComplete;
            this.onDelete = onDelete;
            this.xpEngine = xpEngine;
        }

        void bind(Task task) {
            String priority = task.priority == null ? "EASY" : task.priority;
            binding.tvTaskTitle.setText(task.title == null ? "" : task.title);
            binding.tvTaskPriority.setText(priority);

            int xp = xpEngine.getXpForPriority(priority);
            int coins = xpEngine.getCoinsForPriority(priority);
            binding.tvTaskXp.setText("+" + xp + " XP  +" + coins + " coins");

            GradientDrawable gem = new GradientDrawable();
            gem.setShape(GradientDrawable.OVAL);
            gem.setColor(getPriorityColor(priority));
            binding.viewPriorityStripe.setBackground(gem);

            if (task.raidTask) {
                binding.layoutRaidHeader.setVisibility(android.view.View.VISIBLE);
                binding.tvRaidHp.setVisibility(android.view.View.VISIBLE);
                binding.tvRaidRoom.setVisibility(android.view.View.VISIBLE);
                binding.tvRaidAssigned.setVisibility(android.view.View.VISIBLE);

                int hpRemaining = Math.max(0, task.bossHpRemaining);
                int hpTotal = Math.max(hpRemaining, task.bossHpTotal);
                binding.tvRaidHp.setText(binding.getRoot().getContext().getString(R.string.raid_hp_line, hpRemaining, hpTotal));

                String room = task.roomCode == null ? "-" : task.roomCode;
                binding.tvRaidRoom.setText(binding.getRoot().getContext().getString(R.string.raid_room_line, room));

                String assigned = task.assignedTo == null || task.assignedTo.trim().isEmpty() ? "-" : task.assignedTo;
                binding.tvRaidAssigned.setText(binding.getRoot().getContext().getString(R.string.raid_assigned_line, assigned));
            } else {
                binding.layoutRaidHeader.setVisibility(android.view.View.GONE);
                binding.tvRaidHp.setVisibility(android.view.View.GONE);
                binding.tvRaidRoom.setVisibility(android.view.View.GONE);
                binding.tvRaidAssigned.setVisibility(android.view.View.GONE);
            }

            binding.cbTaskComplete.setOnCheckedChangeListener(null);
            binding.cbTaskComplete.setChecked(task.completed);
            binding.cbTaskComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && !task.completed && onComplete != null) {
                    onComplete.accept(task);
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (onDelete != null) {
                    onDelete.accept(task);
                    return true;
                }
                return false;
            });
        }

        private static int getPriorityColor(String priority) {
            if (priority == null) {
                return Color.parseColor(COLOR_EASY);
            }
            switch (priority) {
                case "MEDIUM":
                    return Color.parseColor(COLOR_MEDIUM);
                case "HARD":
                    return Color.parseColor(COLOR_HARD);
                case "BOSS":
                    return Color.parseColor(COLOR_BOSS);
                case "EASY":
                default:
                    return Color.parseColor(COLOR_EASY);
            }
        }
    }
}
