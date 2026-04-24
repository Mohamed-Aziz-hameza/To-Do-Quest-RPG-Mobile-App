package com.example.todoquest.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.todoquest.R;
import com.example.todoquest.data.repository.TaskRepository;
import com.example.todoquest.databinding.ActivityAuthBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.security.MessageDigest;
import java.util.Locale;

public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;
    private TaskRepository repository;
    private GoogleSignInClient googleSignInClient;
    private String resolvedWebClientId;
    private boolean isGoogleConfigured;

    private final ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (data == null) {
                    showError(getString(R.string.auth_google_cancelled));
                    return;
                }
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleGoogleResult(task);
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new TaskRepository(getApplicationContext());

        if (repository.isUserSignedIn()) {
            goToCharacterCreation();
            return;
        }

        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupGoogle();
        setupActions();
    }

    private void setupGoogle() {
        resolvedWebClientId = resolveGoogleWebClientId();
        isGoogleConfigured = !TextUtils.isEmpty(resolvedWebClientId);

        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();
        if (isGoogleConfigured) {
            builder.requestIdToken(resolvedWebClientId);
        }
        GoogleSignInOptions gso = builder.build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        updateGoogleUiState();
    }

    private void setupActions() {
        binding.btnEmailLogin.setOnClickListener(v -> login(false));
        binding.btnEmailRegister.setOnClickListener(v -> login(true));
        binding.btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
    }

    private void login(boolean register) {
        String email = binding.etEmail.getText() == null ? "" : binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText() == null ? "" : binding.etPassword.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError(getString(R.string.auth_fill_required));
            return;
        }

        binding.progressAuth.setVisibility(android.view.View.VISIBLE);
        if (register) {
            repository.registerWithEmail(email, password, (success, message) -> runOnUiThread(() -> onAuthResult(success, message)));
        } else {
            repository.loginWithEmail(email, password, (success, message) -> runOnUiThread(() -> onAuthResult(success, message)));
        }
    }

    private void startGoogleSignIn() {
        if (!isGoogleConfigured || googleSignInClient == null) {
            showError(getString(R.string.auth_google_client_id_missing));
            return;
        }
        googleSignInClient.signOut().addOnCompleteListener(task -> googleLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private void updateGoogleUiState() {
        binding.btnGoogleSignIn.setEnabled(isGoogleConfigured);
        binding.btnGoogleSignIn.setAlpha(isGoogleConfigured ? 1f : 0.45f);
        binding.tvGoogleSetupHint.setVisibility(isGoogleConfigured ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    private String resolveGoogleWebClientId() {
        int generatedId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        if (generatedId != 0) {
            String generated = getString(generatedId);
            if (!TextUtils.isEmpty(generated)) {
                return generated;
            }
        }

        String manual = getString(R.string.google_web_client_id);
        if (TextUtils.isEmpty(manual) || manual.startsWith("YOUR_") || manual.startsWith("UNSET_")) {
            return "";
        }
        return manual;
    }

    private void handleGoogleResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account == null ? null : account.getIdToken();
            if (TextUtils.isEmpty(idToken)) {
                showError(getString(R.string.auth_google_missing_token));
                return;
            }
            binding.progressAuth.setVisibility(android.view.View.VISIBLE);
            repository.loginWithGoogleToken(idToken, (success, message) -> runOnUiThread(() -> onAuthResult(success, message)));
        } catch (ApiException e) {
            showError(mapGoogleError(e));
        }
    }

    private String mapGoogleError(ApiException e) {
        int code = e.getStatusCode();
        if (code == CommonStatusCodes.NETWORK_ERROR) {
            return getString(R.string.auth_google_network_error);
        }
        if (code == CommonStatusCodes.DEVELOPER_ERROR) {
            return getString(
                    R.string.auth_google_developer_error,
                    getPackageName(),
                    getSigningSha1Fingerprint()
            );
        }
        if (code == 12500) {
            return getString(R.string.auth_google_sign_in_failed_config);
        }
        if (code == 12501) {
            return getString(R.string.auth_google_cancelled);
        }
        return getString(R.string.auth_google_failed_with_code, code);
    }

    private String getSigningSha1Fingerprint() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo packageInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo = pm.getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                if (packageInfo.signingInfo == null) {
                    return "unknown";
                }
                Signature[] signatures = packageInfo.signingInfo.hasMultipleSigners()
                        ? packageInfo.signingInfo.getApkContentsSigners()
                        : packageInfo.signingInfo.getSigningCertificateHistory();
                if (signatures == null || signatures.length == 0) {
                    return "unknown";
                }
                return sha1Hex(signatures[0].toByteArray());
            }

            packageInfo = pm.getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
                return "unknown";
            }
            return sha1Hex(packageInfo.signatures[0].toByteArray());
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String sha1Hex(byte[] cert) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        byte[] hash = digest.digest(cert);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            if (i > 0) {
                out.append(':');
            }
            out.append(String.format(Locale.US, "%02X", hash[i]));
        }
        return out.toString();
    }

    private void onAuthResult(boolean success, String message) {
        binding.progressAuth.setVisibility(android.view.View.GONE);
        if (!success) {
            showError(TextUtils.isEmpty(message) ? getString(R.string.auth_failed) : message);
            return;
        }
        goToCharacterCreation();
    }

    private void goToCharacterCreation() {
        startActivity(new Intent(this, CharacterCreationActivity.class));
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}

