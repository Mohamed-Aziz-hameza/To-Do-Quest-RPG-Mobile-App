package com.example.todoquest.data.remote;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.todoquest.data.local.dao.PlayerDao;
import com.example.todoquest.data.local.dao.TaskDao;
import com.example.todoquest.data.local.entity.PlayerProfile;
import com.example.todoquest.data.local.entity.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class FirebaseSyncManager {

    private static final String USERS_NODE = "users";
    private static final String RAIDS_NODE = "raids";

    private static volatile FirebaseSyncManager instance;

    private final FirebaseAuth auth;
    private final DatabaseReference root;

    private final Set<String> attachedRaidRooms = new HashSet<>();
    private ValueEventListener joinedRaidsListener;
    private ValueEventListener tasksListener;
    private ValueEventListener profileListener;
    private String syncUid;

    private FirebaseSyncManager(@Nullable Context context) {
        this.auth = FirebaseAuth.getInstance();
        this.root = databaseFromConfig(context).getReference();
    }

    public static FirebaseSyncManager getInstance(@Nullable Context context) {
        if (instance == null) {
            synchronized (FirebaseSyncManager.class) {
                if (instance == null) {
                    Context appContext = context == null ? null : context.getApplicationContext();
                    instance = new FirebaseSyncManager(appContext);
                }
            }
        }
        return instance;
    }

    public static FirebaseSyncManager getInstance() {
        return getInstance(null);
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    public void signOut() {
        auth.signOut();
    }

    public void loginWithEmail(String email, String password, ResultCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onResult(true, null);
                    } else {
                        callback.onResult(false, safeError(task.getException()));
                    }
                });
    }

    public void registerWithEmail(String email, String password, ResultCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onResult(true, null);
                    } else {
                        callback.onResult(false, safeError(task.getException()));
                    }
                });
    }

    public void loginWithGoogleIdToken(String idToken, ResultCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onResult(true, null);
                    } else {
                        callback.onResult(false, safeError(task.getException()));
                    }
                });
    }

    public void startSync(TaskDao taskDao, PlayerDao playerDao, ExecutorService diskExecutor) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String uid = currentUser.getUid();
        if (uid.equals(syncUid)) {
            return;
        }

        detachListeners();
        syncUid = uid;

        DatabaseReference userRoot = root.child(USERS_NODE).child(uid);
        userRoot.child("meta").child("email").setValue(currentUser.getEmail());

        attachProfileListener(userRoot, playerDao, diskExecutor);
        attachTaskListener(userRoot, taskDao, diskExecutor);
        attachJoinedRaidsListener(userRoot, taskDao, diskExecutor);
        migrateLocalDataIfNeeded(userRoot, taskDao, playerDao, diskExecutor);
    }

    private void attachProfileListener(DatabaseReference userRoot, PlayerDao playerDao, ExecutorService diskExecutor) {
        profileListener = userRoot.child("profile").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    return;
                }
                diskExecutor.execute(() -> {
                    PlayerProfile local = playerDao.getProfileSync();
                    if (local == null) {
                        local = new PlayerProfile();
                        local.id = 1;
                    }

                    local.heroName = stringValue(snapshot.child("heroName"), local.heroName);
                    local.avatarChoice = intValue(snapshot.child("avatarChoice"), local.avatarChoice);
                    local.characterClass = stringValue(snapshot.child("characterClass"), local.characterClass);
                    local.xp = intValue(snapshot.child("xp"), local.xp);
                    local.level = intValue(snapshot.child("level"), local.level);
                    local.coins = intValue(snapshot.child("coins"), local.coins);
                    local.streakDays = intValue(snapshot.child("streakDays"), local.streakDays);
                    local.lastCompletedDate = longValue(snapshot.child("lastCompletedDate"), local.lastCompletedDate);
                    local.tasksCompletedToday = intValue(snapshot.child("tasksCompletedToday"), local.tasksCompletedToday);
                    local.todayDate = longValue(snapshot.child("todayDate"), local.todayDate);
                    local.baseStrength = intValue(snapshot.child("baseStrength"), local.baseStrength);
                    local.baseSpeed = intValue(snapshot.child("baseSpeed"), local.baseSpeed);
                    local.baseHealth = intValue(snapshot.child("baseHealth"), local.baseHealth);
                    local.currentHealth = intValue(snapshot.child("currentHealth"), local.currentHealth);
                    local.ownsSword = boolValue(snapshot.child("ownsSword"), local.ownsSword);
                    local.ownsShield = boolValue(snapshot.child("ownsShield"), local.ownsShield);
                    local.equippedSword = boolValue(snapshot.child("equippedSword"), local.equippedSword);
                    local.equippedShield = boolValue(snapshot.child("equippedShield"), local.equippedShield);
                    local.dungeonKeys = intValue(snapshot.child("dungeonKeys"), local.dungeonKeys);
                    local.lastDailyReset = longValue(snapshot.child("lastDailyReset"), local.lastDailyReset);
                    local.dailyTaskCreated = boolValue(snapshot.child("dailyTaskCreated"), local.dailyTaskCreated);
                    local.dailyTaskCompleted = boolValue(snapshot.child("dailyTaskCompleted"), local.dailyTaskCompleted);
                    local.dungeonWins = intValue(snapshot.child("dungeonWins"), local.dungeonWins);
                    local.freeStatPoints = intValue(snapshot.child("freeStatPoints"), local.freeStatPoints);
                    local.recalculateStats();
                    playerDao.insertProfile(local);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // No-op
            }
        });
    }

    private void attachTaskListener(DatabaseReference userRoot, TaskDao taskDao, ExecutorService diskExecutor) {
        tasksListener = userRoot.child("tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DataSnapshot> children = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    children.add(child);
                }
                diskExecutor.execute(() -> {
                    for (DataSnapshot child : children) {
                        upsertTaskFromSnapshot(taskDao, child, null);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // No-op
            }
        });
    }

    private void attachJoinedRaidsListener(DatabaseReference userRoot, TaskDao taskDao, ExecutorService diskExecutor) {
        joinedRaidsListener = userRoot.child("joinedRaids").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String roomCode = child.getKey();
                    if (!TextUtils.isEmpty(roomCode)) {
                        attachRaidTaskListener(roomCode, taskDao, diskExecutor);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // No-op
            }
        });
    }

    private void attachRaidTaskListener(String roomCode, TaskDao taskDao, ExecutorService diskExecutor) {
        String normalizedRoom = normalizeRoomCode(roomCode);
        if (TextUtils.isEmpty(normalizedRoom) || attachedRaidRooms.contains(normalizedRoom)) {
            return;
        }
        attachedRaidRooms.add(normalizedRoom);

        root.child(RAIDS_NODE).child(normalizedRoom).child("tasks")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<DataSnapshot> children = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            children.add(child);
                        }
                        diskExecutor.execute(() -> {
                            for (DataSnapshot child : children) {
                                upsertTaskFromSnapshot(taskDao, child, normalizedRoom);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // No-op
                    }
                });
    }

    private void upsertTaskFromSnapshot(TaskDao taskDao, DataSnapshot snapshot, @Nullable String roomCodeFallback) {
        String remoteId = snapshot.getKey();
        if (TextUtils.isEmpty(remoteId)) {
            return;
        }

        Task existing = taskDao.getTaskByRemoteIdSync(remoteId);
        Task target = existing == null ? new Task() : existing;

        target.remoteId = remoteId;
        target.title = stringValue(snapshot.child("title"), target.title);
        target.description = stringValue(snapshot.child("description"), target.description);
        target.priority = stringValue(snapshot.child("priority"), target.priority == null ? "EASY" : target.priority);
        target.completed = boolValue(snapshot.child("completed"), target.completed);
        target.createdAt = longValue(snapshot.child("createdAt"), target.createdAt);
        target.dueDate = longValue(snapshot.child("dueDate"), target.dueDate);
        target.subTasks = stringValue(snapshot.child("subTasks"), target.subTasks);
        target.bossHpTotal = intValue(snapshot.child("bossHpTotal"), target.bossHpTotal);
        target.bossHpRemaining = intValue(snapshot.child("bossHpRemaining"), target.bossHpRemaining);
        target.ownerUid = stringValue(snapshot.child("ownerUid"), target.ownerUid);
        target.assignedTo = stringValue(snapshot.child("assignedTo"), target.assignedTo);
        target.roomCode = stringValue(snapshot.child("roomCode"), roomCodeFallback == null ? target.roomCode : roomCodeFallback);
        target.raidTask = boolValue(snapshot.child("raidTask"), target.raidTask);
        target.raidBossHpContribution = intValue(snapshot.child("raidBossHpContribution"), target.raidBossHpContribution <= 0 ? 1 : target.raidBossHpContribution);
        target.updatedAt = longValue(snapshot.child("updatedAt"), target.updatedAt);

        if (existing == null) {
            taskDao.insertTask(target);
        } else {
            taskDao.updateTask(target);
        }
    }

    public void migrateLocalDataIfNeeded(DatabaseReference userRoot,
                                         TaskDao taskDao,
                                         PlayerDao playerDao,
                                         ExecutorService diskExecutor) {
        userRoot.child("migrationDone").get().addOnSuccessListener(snapshot -> {
            Boolean done = snapshot.getValue(Boolean.class);
            if (Boolean.TRUE.equals(done)) {
                return;
            }

            diskExecutor.execute(() -> {
                PlayerProfile profile = playerDao.getProfileSync();
                if (profile != null) {
                    pushProfile(profile);
                }

                List<Task> tasks = taskDao.getAllTasksSync();
                for (Task task : tasks) {
                    pushTask(task, taskDao);
                }
                userRoot.child("migrationDone").setValue(true);
            });
        });
    }

    public void pushProfile(PlayerProfile profile) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || profile == null) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("heroName", profile.heroName);
        data.put("avatarChoice", profile.avatarChoice);
        data.put("characterClass", profile.characterClass);
        data.put("xp", profile.xp);
        data.put("level", profile.level);
        data.put("coins", profile.coins);
        data.put("streakDays", profile.streakDays);
        data.put("lastCompletedDate", profile.lastCompletedDate);
        data.put("tasksCompletedToday", profile.tasksCompletedToday);
        data.put("todayDate", profile.todayDate);
        data.put("baseStrength", profile.baseStrength);
        data.put("baseSpeed", profile.baseSpeed);
        data.put("baseHealth", profile.baseHealth);
        data.put("currentHealth", profile.currentHealth);
        data.put("ownsSword", profile.ownsSword);
        data.put("ownsShield", profile.ownsShield);
        data.put("equippedSword", profile.equippedSword);
        data.put("equippedShield", profile.equippedShield);
        data.put("dungeonKeys", profile.dungeonKeys);
        data.put("lastDailyReset", profile.lastDailyReset);
        data.put("dailyTaskCreated", profile.dailyTaskCreated);
        data.put("dailyTaskCompleted", profile.dailyTaskCompleted);
        data.put("dungeonWins", profile.dungeonWins);
        data.put("freeStatPoints", profile.freeStatPoints);

        root.child(USERS_NODE).child(user.getUid()).child("profile").updateChildren(data);
    }

    public void pushTask(Task task, @Nullable TaskDao taskDao) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || task == null) {
            return;
        }

        if (TextUtils.isEmpty(task.remoteId)) {
            task.remoteId = root.child(USERS_NODE).child(user.getUid()).child("tasks").push().getKey();
            if (taskDao != null) {
                taskDao.updateTask(task);
            }
        }

        if (TextUtils.isEmpty(task.remoteId)) {
            return;
        }

        task.ownerUid = user.getUid();
        task.updatedAt = System.currentTimeMillis();

        Map<String, Object> data = toTaskMap(task);
        if (task.raidTask && !TextUtils.isEmpty(task.roomCode)) {
            String roomCode = normalizeRoomCode(task.roomCode);
            root.child(RAIDS_NODE).child(roomCode).child("tasks").child(task.remoteId).setValue(data);
            root.child(USERS_NODE).child(user.getUid()).child("joinedRaids").child(roomCode).setValue(true);
        } else {
            root.child(USERS_NODE).child(user.getUid()).child("tasks").child(task.remoteId).setValue(data);
        }
    }

    public void removeTask(Task task) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || task == null || TextUtils.isEmpty(task.remoteId)) {
            return;
        }

        if (task.raidTask && !TextUtils.isEmpty(task.roomCode)) {
            String roomCode = normalizeRoomCode(task.roomCode);
            root.child(RAIDS_NODE).child(roomCode).child("tasks").child(task.remoteId).removeValue();
        } else {
            root.child(USERS_NODE).child(user.getUid()).child("tasks").child(task.remoteId).removeValue();
        }
    }

    public void createRaidRoomIfAbsent(String roomCode, String bossTitle, ResultCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        String normalizedRoom = normalizeRoomCode(roomCode);
        if (user == null || TextUtils.isEmpty(normalizedRoom)) {
            callback.onResult(false, "Invalid room or user");
            return;
        }

        DatabaseReference raidRoot = root.child(RAIDS_NODE).child(normalizedRoom);
        raidRoot.child("meta").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() == null) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("hostUid", user.getUid());
                    meta.put("hostEmail", user.getEmail());
                    meta.put("title", TextUtils.isEmpty(bossTitle) ? "Raid Boss" : bossTitle);
                    meta.put("image", "fireraidboss.jpg");
                    meta.put("bossHpTotal", 0);
                    meta.put("bossHpRemaining", 0);
                    meta.put("createdAt", System.currentTimeMillis());
                    currentData.setValue(meta);
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    callback.onResult(false, error.getMessage());
                    return;
                }
                addMemberToRaid(normalizedRoom, user);
                callback.onResult(true, null);
            }
        });
    }

    public void joinRaidRoom(String roomCode, ResultCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        String normalizedRoom = normalizeRoomCode(roomCode);
        if (user == null || TextUtils.isEmpty(normalizedRoom)) {
            callback.onResult(false, "Invalid room or user");
            return;
        }

        addMemberToRaid(normalizedRoom, user, (memberAdded, memberMessage) -> {
            if (!memberAdded) {
                callback.onResult(false, memberMessage);
                return;
            }

            DatabaseReference raidMeta = root.child(RAIDS_NODE).child(normalizedRoom).child("meta");
            raidMeta.get().addOnSuccessListener(snapshot -> {
                if (!snapshot.exists()) {
                    removeMemberFromRaid(normalizedRoom, user.getUid());
                    callback.onResult(false, "Room not found");
                    return;
                }
                callback.onResult(true, null);
            }).addOnFailureListener(e -> callback.onResult(false, safeError(e)));
        });
    }

    private void addMemberToRaid(String normalizedRoom, FirebaseUser user) {
        addMemberToRaid(normalizedRoom, user, null);
    }

    private void addMemberToRaid(String normalizedRoom, FirebaseUser user, @Nullable ResultCallback callback) {
        Map<String, Object> member = new HashMap<>();
        member.put("email", user.getEmail());
        member.put("displayName", user.getDisplayName());
        member.put("joinedAt", System.currentTimeMillis());

        String memberBase = RAIDS_NODE + "/" + normalizedRoom + "/members/" + user.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put(memberBase + "/email", member.get("email"));
        updates.put(memberBase + "/displayName", member.get("displayName"));
        updates.put(memberBase + "/joinedAt", member.get("joinedAt"));
        updates.put(USERS_NODE + "/" + user.getUid() + "/joinedRaids/" + normalizedRoom, true);

        root.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onResult(true, null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onResult(false, safeError(e));
                    }
                });
    }

    private void removeMemberFromRaid(String normalizedRoom, String uid) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(RAIDS_NODE + "/" + normalizedRoom + "/members/" + uid, null);
        updates.put(USERS_NODE + "/" + uid + "/joinedRaids/" + normalizedRoom, null);
        root.updateChildren(updates);
    }

    public void pushRaidTask(Task task, @Nullable TaskDao taskDao, ResultCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || task == null || TextUtils.isEmpty(task.roomCode)) {
            callback.onResult(false, "Invalid task/user");
            return;
        }

        String normalizedRoom = normalizeRoomCode(task.roomCode);
        task.raidTask = true;
        task.roomCode = normalizedRoom;
        task.ownerUid = user.getUid();
        task.raidBossHpContribution = task.raidBossHpContribution <= 0 ? 1 : task.raidBossHpContribution;

        if (TextUtils.isEmpty(task.remoteId)) {
            task.remoteId = root.child(RAIDS_NODE).child(normalizedRoom).child("tasks").push().getKey();
            if (taskDao != null) {
                taskDao.updateTask(task);
            }
        }

        if (TextUtils.isEmpty(task.remoteId)) {
            callback.onResult(false, "Could not create task key");
            return;
        }

        task.updatedAt = System.currentTimeMillis();
        root.child(RAIDS_NODE).child(normalizedRoom).child("tasks").child(task.remoteId)
                .setValue(toTaskMap(task))
                .addOnSuccessListener(unused -> {
                    int hp = task.raidBossHpContribution;
                    DatabaseReference meta = root.child(RAIDS_NODE).child(normalizedRoom).child("meta");
                    meta.child("bossHpTotal").runTransaction(incrementTransaction(hp));
                    meta.child("bossHpRemaining").runTransaction(incrementTransaction(hp));
                    addMemberToRaid(normalizedRoom, user);
                    callback.onResult(true, null);
                })
                .addOnFailureListener(e -> callback.onResult(false, safeError(e)));
    }

    public void markRaidTaskCompleted(Task task) {
        if (task == null || !task.raidTask || TextUtils.isEmpty(task.roomCode) || TextUtils.isEmpty(task.remoteId)) {
            return;
        }

        String normalizedRoom = normalizeRoomCode(task.roomCode);
        DatabaseReference taskRef = root.child(RAIDS_NODE).child(normalizedRoom).child("tasks").child(task.remoteId);
        taskRef.child("completed").setValue(true);
        taskRef.child("updatedAt").setValue(System.currentTimeMillis());

        int decrement = Math.max(1, task.raidBossHpContribution);
        root.child(RAIDS_NODE).child(normalizedRoom).child("meta").child("bossHpRemaining")
                .runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        Long current = currentData.getValue(Long.class);
                        long remaining = Math.max(0L, (current == null ? 0L : current) - decrement);
                        currentData.setValue(remaining);
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                        // No-op
                    }
                });
    }

    public String generateRoomCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * alphabet.length());
            builder.append(alphabet.charAt(index));
        }
        return builder.toString();
    }

    private Transaction.Handler incrementTransaction(int delta) {
        return new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long current = currentData.getValue(Long.class);
                currentData.setValue((current == null ? 0L : current) + Math.max(0, delta));
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                // No-op
            }
        };
    }

    private Map<String, Object> toTaskMap(Task task) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", task.title);
        data.put("description", task.description);
        data.put("priority", task.priority);
        data.put("completed", task.completed);
        data.put("createdAt", task.createdAt);
        data.put("dueDate", task.dueDate);
        data.put("subTasks", task.subTasks);
        data.put("bossHpTotal", task.bossHpTotal);
        data.put("bossHpRemaining", task.bossHpRemaining);
        data.put("ownerUid", task.ownerUid);
        data.put("assignedTo", task.assignedTo);
        data.put("roomCode", task.roomCode);
        data.put("raidTask", task.raidTask);
        data.put("raidBossHpContribution", task.raidBossHpContribution <= 0 ? 1 : task.raidBossHpContribution);
        data.put("updatedAt", task.updatedAt);
        return data;
    }

    private void detachListeners() {
        if (syncUid == null) {
            return;
        }
        DatabaseReference userRoot = root.child(USERS_NODE).child(syncUid);
        if (profileListener != null) {
            userRoot.child("profile").removeEventListener(profileListener);
            profileListener = null;
        }
        if (tasksListener != null) {
            userRoot.child("tasks").removeEventListener(tasksListener);
            tasksListener = null;
        }
        if (joinedRaidsListener != null) {
            userRoot.child("joinedRaids").removeEventListener(joinedRaidsListener);
            joinedRaidsListener = null;
        }
        attachedRaidRooms.clear();
    }

    private String normalizeRoomCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.US);
    }

    private String safeError(Throwable throwable) {
        if (throwable instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) throwable).getErrorCode();
            if ("CONFIGURATION_NOT_FOUND".equals(code)) {
                return "Firebase config missing. In Firebase Console: enable Email/Password and Google providers, add SHA-1/SHA-256 for this Android app, then download a new google-services.json.";
            }
            if ("INVALID_API_KEY".equals(code)) {
                return "Invalid Firebase API key. Replace app/google-services.json with the latest file from Firebase Console.";
            }
            if ("APP_NOT_AUTHORIZED".equals(code)) {
                return "This app is not authorized in Firebase. Verify package name and SHA fingerprints, then refresh google-services.json.";
            }
        }
        if (throwable == null || TextUtils.isEmpty(throwable.getMessage())) {
            return "Operation failed";
        }
        return throwable.getMessage();
    }

    private FirebaseDatabase databaseFromConfig(@Nullable Context context) {
        String databaseUrl = context == null ? "" : context.getString(com.example.todoquest.R.string.firebase_database_url);
        if (!TextUtils.isEmpty(databaseUrl)) {
            return FirebaseDatabase.getInstance(databaseUrl.trim());
        }
        return FirebaseDatabase.getInstance();
    }

    private String stringValue(DataSnapshot snapshot, String fallback) {
        String value = snapshot.getValue(String.class);
        return value == null ? fallback : value;
    }

    private int intValue(DataSnapshot snapshot, int fallback) {
        Long value = snapshot.getValue(Long.class);
        return value == null ? fallback : value.intValue();
    }

    private long longValue(DataSnapshot snapshot, long fallback) {
        Long value = snapshot.getValue(Long.class);
        return value == null ? fallback : value;
    }

    private boolean boolValue(DataSnapshot snapshot, boolean fallback) {
        Boolean value = snapshot.getValue(Boolean.class);
        return value == null ? fallback : value;
    }

    public interface ResultCallback {
        void onResult(boolean success, @Nullable String message);
    }
}

