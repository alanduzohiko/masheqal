# Phone-only delivery path

The project deliberately avoids requiring Android Studio or a desktop toolchain.

The intended path is:

`source → GitHub repository → GitHub Actions → app-debug.apk`

The workflow installs its own JDK/Android SDK/Gradle in the cloud and uploads the resulting APK/AAB artifacts.

Because the GitHub integration is not connected in the current session, the workflow has been prepared but has **not** been executed here. Once the repository is connected, the remaining user action can be performed from the Android phone's GitHub web UI by opening **Actions → Masheqal Android Build → Run workflow** and retrieving the debug artifact.

Release signing remains intentionally outside the repository: a private keystore should be stored only as a secure CI secret owned by the publisher.
