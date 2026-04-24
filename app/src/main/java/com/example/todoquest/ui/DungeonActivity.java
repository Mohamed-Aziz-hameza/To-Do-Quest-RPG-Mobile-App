package com.example.todoquest.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.todoquest.R;
import com.example.todoquest.data.local.entity.PlayerProfile;
import com.example.todoquest.data.repository.TaskRepository;
import com.example.todoquest.databinding.ActivityDungeonBinding;
import com.example.todoquest.domain.XPEngine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DungeonActivity extends AppCompatActivity {

    private static final int SPECIAL_COOLDOWN = 3;

    private ActivityDungeonBinding binding;
    private TaskRepository repository;
    private final XPEngine xpEngine = new XPEngine();
    private final Random random = new Random();

    private PlayerProfile profile;
    private int playerHp;
    private Enemy enemy;
    private final List<Enemy> corridorQueue = new ArrayList<>();
    private boolean inBossRoom;

    private int totalXp;
    private int totalCoins;
    private int totalKeys;

    private int specialCooldown;
    private boolean playerDefending;
    private boolean playerStunned;
    private int cursedAttackPenalty;
    private int warCryTurns;

    private final Deque<String> combatLog = new ArrayDeque<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDungeonBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new TaskRepository(getApplicationContext());
        repository.runOnDiskIo(() -> {
            profile = repository.getOrCreateProfileSync();
            if (profile.currentHealth <= 0) {
                profile.currentHealth = profile.baseHealth;
            }
            playerHp = profile.currentHealth;
            buildCorridorEnemies();
            runOnUiThread(() -> {
                nextEnemy();
                wireActions();
            });
        });
    }

    private void wireActions() {
        binding.btnAttack.setOnClickListener(v -> playerTurn(Action.ATTACK));
        binding.btnDefend.setOnClickListener(v -> playerTurn(Action.DEFEND));
        binding.btnSpecial.setOnClickListener(v -> playerTurn(Action.SPECIAL));
        binding.btnFlee.setOnClickListener(v -> playerTurn(Action.FLEE));
    }

    private void buildCorridorEnemies() {
        List<Enemy> pool = new ArrayList<>();
        pool.add(Enemy.goblinWarrior());
        pool.add(Enemy.goblinArcher());
        pool.add(Enemy.goblinShaman());
        Collections.shuffle(pool);
        corridorQueue.add(pool.get(0));
        corridorQueue.add(pool.get(1));
        corridorQueue.add(pool.get(2));
    }

    private void nextEnemy() {
        if (corridorQueue.isEmpty() && !inBossRoom) {
            inBossRoom = true;
            addLog(getString(R.string.boss_awaits));
            binding.ivCombatBg.setImageResource(R.drawable.bossrom);
            enemy = Enemy.highGoblinKing();
        } else if (!corridorQueue.isEmpty()) {
            binding.ivCombatBg.setImageResource(R.drawable.thewaytothebossroom);
            enemy = corridorQueue.remove(0);
        }

        if (enemy == null) {
            onVictory();
            return;
        }

        warCryTurns = 0;
        updateUi();
    }

    private void playerTurn(Action action) {
        if (enemy == null || profile == null) {
            return;
        }

        if (playerStunned) {
            addLog("You are stunned and lose the turn.");
            playerStunned = false;
            enemyTurn();
            return;
        }

        playerDefending = false;
        switch (action) {
            case ATTACK:
                doAttack(false);
                break;
            case DEFEND:
                playerDefending = true;
                addLog("You brace behind your guard.");
                break;
            case SPECIAL:
                doSpecial();
                break;
            case FLEE:
                if (random.nextInt(100) < 40) {
                    addLog("You escaped the dungeon.");
                    finish();
                    return;
                }
                addLog("Flee failed. Enemy gets a free strike.");
                enemyAttack(true);
                break;
            default:
                break;
        }

        if (enemy.hp <= 0) {
            onEnemyDefeated();
            return;
        }

        enemyTurn();
    }

    private void doAttack(boolean fromEnemyTurn) {
        int attackValue = Math.max(1, profile.effectiveAttack - cursedAttackPenalty);
        int dealt = computeDamage(attackValue, enemy.defense, false);
        enemy.hp -= dealt;
        addLog("You hit " + enemy.name + " for " + dealt + ".");
        if (!fromEnemyTurn && specialCooldown > 0) {
            specialCooldown--;
        }
    }

    private void doSpecial() {
        if (specialCooldown > 0) {
            addLog("Special on cooldown: " + specialCooldown + " turn(s).");
            doAttack(false);
            return;
        }

        int reducedDefense = enemy.defense / 2;
        int damage = Math.max(1, (profile.effectiveAttack * 2) - reducedDefense + roll(-2, 2));
        enemy.hp -= damage;
        addLog("Special Strike deals " + damage + ".");
        specialCooldown = SPECIAL_COOLDOWN;
    }

    private void enemyTurn() {
        if (enemy == null || enemy.hp <= 0) {
            return;
        }

        enemy.turnCount++;
        if (cursedAttackPenalty > 0) {
            cursedAttackPenalty = 0;
        }

        switch (enemy.type) {
            case "GOBLIN_WARRIOR":
                if (enemy.turnCount % 3 == 0) {
                    enemyAttackWithMultiplier(1.5f, "Shield Bash");
                } else {
                    enemyAttack(false);
                }
                break;
            case "GOBLIN_ARCHER":
                if (playerDefending) {
                    addLog("Aimed Shot pierces your guard.");
                    enemyAttackWithMultiplier(1.0f, "Aimed Shot");
                } else {
                    enemyAttack(false);
                }
                break;
            case "GOBLIN_SHAMAN":
                if (enemy.turnCount % 2 == 0) {
                    cursedAttackPenalty = 5;
                    addLog("Hex Curse lowers your next attack by 5.");
                }
                enemyAttack(false);
                break;
            case "HIGH_GOBLIN":
                highGoblinAi();
                break;
            default:
                enemyAttack(false);
                break;
        }

        if (playerHp <= 0) {
            onDefeat();
        }

        updateUi();
    }

    private void highGoblinAi() {
        boolean enraged = enemy.hp <= 75;
        if (!enemy.usedWarCry && enemy.hp <= 100) {
            enemy.usedWarCry = true;
            warCryTurns = 2;
            addLog("High Goblin uses War Cry. Attack rises.");
        }

        if (enraged && enemy.turnCount % 3 == 0) {
            enemyAttackWithMultiplier(1.4f, "Skull Crush");
            if (random.nextBoolean()) {
                playerStunned = true;
                addLog("Skull Crush stuns you.");
            }
            return;
        }

        enemyAttack(false);
    }

    private void enemyAttack(boolean freeAttack) {
        enemyAttackWithMultiplier(1.0f, freeAttack ? "Free Attack" : "Attack");
    }

    private void enemyAttackWithMultiplier(float multiplier, String moveName) {
        int baseAttack = enemy.attack;
        if ("HIGH_GOBLIN".equals(enemy.type) && enemy.hp <= 75) {
            baseAttack = 38;
        }
        if (warCryTurns > 0) {
            baseAttack += 8;
            warCryTurns--;
        }

        int rawAttack = (int) Math.round(baseAttack * multiplier);
        int defense = profile.effectiveDefense;
        int damage = Math.max(1, rawAttack - defense + roll(-2, 2));

        if (playerDefending) {
            damage = Math.max(1, (int) Math.floor(damage * 0.5f));
            if (profile.equippedShield) {
                damage = Math.max(1, (int) Math.floor((damage - 22) * 0.5f));
            }
        }

        playerHp -= damage;
        addLog(enemy.name + " uses " + moveName + " for " + damage + ".");
    }

    private int computeDamage(int attack, int defense, boolean ignoreDefense) {
        if (ignoreDefense) {
            return Math.max(1, attack + roll(-2, 2));
        }
        return Math.max(1, attack - defense + roll(-2, 2));
    }

    private int roll(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private void onEnemyDefeated() {
        totalXp += enemy.xpReward;
        totalCoins += enemy.coinReward;

        // Immediate rewards and full heal after each enemy kill.
        int previousLevel = Math.max(1, profile.level);
        profile.xp += enemy.xpReward;
        profile.coins += enemy.coinReward;
        int newLevel = xpEngine.calculateLevel(profile.xp);
        if (newLevel > previousLevel) {
            profile.freeStatPoints += (newLevel - previousLevel) * 5;
        }
        profile.level = newLevel;
        profile.currentHealth = Math.max(1, profile.baseHealth);
        playerHp = profile.currentHealth;
        repository.updateProfile(profile);

        if ("HIGH_GOBLIN".equals(enemy.type)) {
            totalKeys += 1;
        }
        addLog(enemy.name + " defeated. +" + enemy.xpReward + " XP, +" + enemy.coinReward + " coins. You are fully healed.");
        updateUi();

        if (!inBossRoom || enemy.type.equals("HIGH_GOBLIN")) {
            if (inBossRoom && enemy.type.equals("HIGH_GOBLIN")) {
                onVictory();
                return;
            }
            nextEnemy();
        }
    }

    private void onVictory() {
        // XP/coins were already granted after each fight; only grant key/win state here.
        repository.awardDungeonVictory(0, 0, totalKeys);
        repository.addDungeonRun(totalXp, totalCoins, true);

        new AlertDialog.Builder(this)
                .setTitle(R.string.victory_title)
                .setMessage(getString(R.string.victory_body, totalXp, totalCoins, totalKeys))
                .setPositiveButton(R.string.close, (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void onDefeat() {
        repository.addDungeonRun(totalXp, totalCoins, false);
        new AlertDialog.Builder(this)
                .setTitle(R.string.defeat_title)
                .setMessage(R.string.defeat_body)
                .setPositiveButton(R.string.retry_one_key, (d, w) -> {
                    repository.consumeDungeonKey(success -> runOnUiThread(() -> {
                        if (!success) {
                            Toast.makeText(this, R.string.need_key, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        resetRun();
                    }));
                })
                .setNegativeButton(R.string.exit, (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void resetRun() {
        totalXp = 0;
        totalCoins = 0;
        totalKeys = 0;
        specialCooldown = 0;
        playerDefending = false;
        playerStunned = false;
        cursedAttackPenalty = 0;
        inBossRoom = false;
        corridorQueue.clear();
        buildCorridorEnemies();

        playerHp = Math.max(1, profile.baseHealth);
        combatLog.clear();
        addLog("You return to the corridor.");
        nextEnemy();
    }

    private void addLog(String line) {
        if (TextUtils.isEmpty(line)) {
            return;
        }
        if (combatLog.size() == 5) {
            combatLog.removeFirst();
        }
        combatLog.addLast(line);
        StringBuilder sb = new StringBuilder();
        for (String item : combatLog) {
            sb.append(item).append('\n');
        }
        binding.tvCombatLog.setText(sb.toString().trim());
    }

    private void updateUi() {
        if (enemy == null || profile == null) {
            return;
        }

        binding.ivEnemy.setImageResource(enemy.imageRes);
        binding.tvEnemyName.setText(enemy.name + "  HP " + Math.max(0, enemy.hp) + "/" + enemy.maxHp);
        binding.pbEnemyHp.setMax(enemy.maxHp);
        binding.pbEnemyHp.setProgress(Math.max(0, enemy.hp));
        binding.tvEnemyStats.setText(String.format(Locale.US, "ATK %d  DEF %d  SPD %d", enemy.attack, enemy.defense, enemy.speed));

        binding.tvPlayerName.setText((profile.heroName == null || profile.heroName.isEmpty() ? "Hero" : profile.heroName)
                + "  HP " + Math.max(0, playerHp) + "/" + profile.baseHealth);
        binding.pbPlayerHp.setMax(Math.max(1, profile.baseHealth));
        binding.pbPlayerHp.setProgress(Math.max(0, playerHp));
        binding.tvPlayerStats.setText(String.format(Locale.US, "ATK %d  DEF %d  SPD %d", profile.effectiveAttack, profile.effectiveDefense, profile.effectiveSpeed));

        binding.btnSpecial.setEnabled(specialCooldown == 0);
    }

    private enum Action {
        ATTACK,
        DEFEND,
        SPECIAL,
        FLEE
    }

    private static final class Enemy {
        String type;
        String name;
        int imageRes;
        int hp;
        int maxHp;
        int attack;
        int defense;
        int speed;
        int xpReward;
        int coinReward;
        int turnCount;
        boolean usedWarCry;

        static Enemy goblinWarrior() {
            Enemy enemy = new Enemy();
            enemy.type = "GOBLIN_WARRIOR";
            enemy.name = "Goblin Warrior";
            enemy.imageRes = R.drawable.goblin;
            enemy.hp = 45;
            enemy.maxHp = 45;
            enemy.attack = 12;
            enemy.defense = 3;
            enemy.speed = 4;
            enemy.xpReward = 15;
            enemy.coinReward = 8;
            return enemy;
        }

        static Enemy goblinArcher() {
            Enemy enemy = new Enemy();
            enemy.type = "GOBLIN_ARCHER";
            enemy.name = "Goblin Archer";
            enemy.imageRes = R.drawable.goblinarcher;
            enemy.hp = 30;
            enemy.maxHp = 30;
            enemy.attack = 18;
            enemy.defense = 1;
            enemy.speed = 8;
            enemy.xpReward = 20;
            enemy.coinReward = 10;
            return enemy;
        }

        static Enemy goblinShaman() {
            Enemy enemy = new Enemy();
            enemy.type = "GOBLIN_SHAMAN";
            enemy.name = "Goblin Shaman";
            enemy.imageRes = R.drawable.goblinshaman;
            enemy.hp = 35;
            enemy.maxHp = 35;
            enemy.attack = 10;
            enemy.defense = 2;
            enemy.speed = 5;
            enemy.xpReward = 25;
            enemy.coinReward = 15;
            return enemy;
        }

        static Enemy highGoblinKing() {
            Enemy enemy = new Enemy();
            enemy.type = "HIGH_GOBLIN";
            enemy.name = "High Goblin King";
            enemy.imageRes = R.drawable.highgoblin;
            enemy.hp = 150;
            enemy.maxHp = 150;
            enemy.attack = 28;
            enemy.defense = 10;
            enemy.speed = 6;
            enemy.xpReward = 120;
            enemy.coinReward = 80;
            return enemy;
        }
    }
}

