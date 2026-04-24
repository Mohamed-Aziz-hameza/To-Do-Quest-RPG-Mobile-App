# Firebase setup for TodoQuest

## 1) Enable providers
- Firebase Console -> Authentication -> Sign-in method
- Enable **Email/Password**
- Enable **Google**

## 2) Google Sign-In web client id
- Do not hardcode it in XML.
- Keep `google_web_client_id` empty and use the generated `default_web_client_id` from `google-services.json`.
- If Google login fails, regenerate and redownload `app/google-services.json` after SHA setup (next section).

## 3) Required SHA fingerprints (important)
- In Firebase Console -> Project settings -> Your Android app (`com.example.todoquest`) add **SHA-1** and **SHA-256**.
- For debug key on Windows:

```powershell
keytool -list -v -alias androiddebugkey -keystore "$env:USERPROFILE\.android\debug.keystore" -storepass android -keypass android
```

- After adding fingerprints, download a fresh `app/google-services.json`.
- Open the file and verify `oauth_client` is not empty. If it is empty, Google Sign-In will stay disabled in the app.

## 4) Realtime Database
- Create a Realtime Database (production mode recommended).
- Import rules from `firebase/database.rules.json`.
- Confirm URL matches `firebase_database_url` in `app/src/main/res/values/strings.xml`.

## 5) Quick validation after rebuild
- Run a clean build and install the debug APK.
- Check generated file `app/build/generated/res/processDebugGoogleServices/values/values.xml` contains `default_web_client_id`.
- If `default_web_client_id` is missing, your Firebase OAuth setup is still incomplete.

## 6) Data paths created by app
- `users/{uid}/profile`
- `users/{uid}/tasks`
- `users/{uid}/joinedRaids/{roomCode}`
- `raids/{roomCode}/meta`
- `raids/{roomCode}/members/{uid}`
- `raids/{roomCode}/tasks/{taskId}`

## 7) Important security note
Never ship Admin SDK private keys inside the app package. Remove any `*-firebase-adminsdk-*.json` file from the Android module.

