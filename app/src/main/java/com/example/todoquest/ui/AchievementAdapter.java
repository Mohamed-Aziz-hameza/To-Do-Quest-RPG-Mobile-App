package com.example.todoquest.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todoquest.R;
import com.example.todoquest.data.local.entity.Achievement;
import com.example.todoquest.databinding.ItemAchievementBinding;

import java.util.Objects;

public class AchievementAdapter extends ListAdapter<Achievement, AchievementAdapter.AchievementViewHolder> {

    private static final DiffUtil.ItemCallback<Achievement> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Achievement oldItem, @NonNull Achievement newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Achievement oldItem, @NonNull Achievement newItem) {
            return oldItem.unlocked == newItem.unlocked
                    && oldItem.unlockedAt == newItem.unlockedAt
                    && Objects.equals(oldItem.key, newItem.key);
        }
    };

    /** Creates an achievements adapter with diffing support. */
    public AchievementAdapter() {
        super(DIFF_CALLBACK);
    }

    /** Creates a new achievement row view holder. */
    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemAchievementBinding binding = ItemAchievementBinding.inflate(inflater, parent, false);
        return new AchievementViewHolder(binding);
    }

    /** Binds achievement data into a row view. */
    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public static final class AchievementViewHolder extends RecyclerView.ViewHolder {

        private final ItemAchievementBinding binding;

        public AchievementViewHolder(ItemAchievementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Achievement achievement) {
            String key = achievement.key == null ? "" : achievement.key;
            binding.tvBadgeName.setText(getBadgeName(key));

            if (achievement.unlocked) {
                int unlockedColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.badge_unlocked);
                binding.tvBadgeIcon.setBackgroundTintList(ColorStateList.valueOf(unlockedColor));
                binding.tvBadgeIcon.setText(getBadgeIcon(key));
                binding.tvBadgeLocked.setVisibility(android.view.View.GONE);
            } else {
                int lockedColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.badge_locked);
                binding.tvBadgeIcon.setBackgroundTintList(ColorStateList.valueOf(lockedColor));
                binding.tvBadgeIcon.setText("?");
                binding.tvBadgeLocked.setVisibility(android.view.View.VISIBLE);
                binding.tvBadgeLocked.setText(R.string.locked);
            }
        }

        private String getBadgeName(String key) {
            switch (key) {
                case "FIRST_BLOOD":
                    return "First Blood";
                case "ON_FIRE":
                    return "On Fire";
                case "PERFECTIONIST":
                    return "Perfectionist";
                case "EARLY_BIRD":
                    return "Early Bird";
                case "DAILY_CHAMPION":
                    return "Daily Champion";
                case "DUNGEON_HUNTER":
                    return "Dungeon Hunter";
                case "TREASURE_HUNTER":
                    return "Treasure Hunter";
                default:
                    return "Unknown";
            }
        }

        private String getBadgeIcon(String key) {
            switch (key) {
                case "FIRST_BLOOD":
                    return "⚔️";
                case "ON_FIRE":
                    return "🔥";
                case "PERFECTIONIST":
                    return "✓";
                case "EARLY_BIRD":
                    return "🌅";
                case "DAILY_CHAMPION":
                    return "🗝";
                case "DUNGEON_HUNTER":
                    return "👑";
                case "TREASURE_HUNTER":
                    return "🪙";
                default:
                    return "?";
            }
        }
    }
}
