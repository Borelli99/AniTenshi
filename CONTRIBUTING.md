# Contributing to AniTenshi

Thanks for your interest! First of all, please read the [code of conduct](./CODE_OF_CONDUCT.md).

## Reporting an issue or suggesting an idea

- Open an [issue](https://github.com/Borelli99/AniTenshi/issues) in this repository. Please don't report AniTenshi issues to Animetail.
- Include your AniTenshi version (*More → About*), your device model and Android version, the steps to reproduce the problem and, if you can, a screenshot.
- If the problem is about a single extension (a site that doesn't load, an episode showing "loading failed"), try the same extension in Aniyomi or Animetail first: if it happens there too, report it to whoever maintains the extension.

## Contributing code

Pull requests are welcome. For big changes, open an issue first so we can talk about it.

### What you need

- Basic [Android development](https://developer.android.com/) and [Kotlin](https://kotlinlang.org/) knowledge
- [Android Studio](https://developer.android.com/studio) and JDK 21
- An emulator or a phone with developer options enabled

### Building and testing

```bash
./gradlew assembleDebug           # debug APK (package com.borelli99.anitenshi.dev)
./gradlew testDebugUnitTest       # tests
./gradlew spotlessApply           # format the code the way the checks expect
```

The debug build has a different package name, so it installs next to the regular app.

### How releases are published

The [`Build AniTenshi APK`](./.github/workflows/build_fork.yml) workflow runs on every push to `master` (changes to `.md` files only don't start a build), or manually from the *Actions* tab. It runs the tests, builds the release APK, signs it and publishes it on the [Releases](https://github.com/Borelli99/AniTenshi/releases) page.

- Releases are tagged `r<commit count>` (for example `r9150`). The app compares that number with its own to know when an update is available, so **don't rewrite the history of `master`**, or the numbers will stop increasing.
- The APK is signed with the key stored in the repository's `KEYSTORE_BASE64` secret. It must always be the same key: with a different one, new versions won't install over the old ones.

### Staying up to date with Animetail

AniTenshi follows Animetail. To pull in its changes, tap **Sync fork** on the repository's GitHub page, or from a terminal:

```bash
git remote add upstream https://github.com/Animetailapp/Animetail.git
git fetch upstream
git merge upstream/master
```

Fixes that help everyone (like the anime repository one) are worth proposing to [Animetail](https://github.com/Animetailapp/Animetail) too, so they don't have to be maintained only here.

## Translations

The app's texts come from Animetail, Aniyomi and Mihon: to improve a translation, contribute to the project the text comes from. The only strings specific to AniTenshi are the app name and the texts in this repository.

## Forking AniTenshi

You're welcome to fork AniTenshi under the terms of the [license](./LICENSE). To avoid confusion and conflicts:

- change the app name (`app_name` in `i18n/src/commonMain/moko-resources/base/strings.xml`) and the icon;
- change the `applicationId` in [`app/build.gradle.kts`](./app/build.gradle.kts), otherwise your app can't be installed next to this one;
- point the update checker and the links to your repository (`GITHUB_REPO` and the URLs in `core/common/src/main/java/eu/kanade/tachiyomi/core/Constants.kt`);
- create your own signing key (PKCS12, alias and password `anitenshi`, or change these values in the workflow) and store it base64-encoded in your repository's `KEYSTORE_BASE64` secret.
