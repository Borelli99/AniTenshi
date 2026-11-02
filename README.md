<div align="center">

<a href="https://github.com/Borelli99/AniTenshi">
    <img src="./.github/assets/icon.png" alt="AniTenshi logo" title="AniTenshi" width="96"/>
</a>

# AniTenshi

### An anime and manga app for Android, based on Animetail.
Find, watch and read anime, series, movies and manga from the extensions you choose, with a full-featured video player and a library that stays in sync with your trackers.

[![Latest release](https://img.shields.io/github/v/release/Borelli99/AniTenshi?label=Latest%20release&labelColor=14163A&color=F2C14E)](https://github.com/Borelli99/AniTenshi/releases/latest)
[![Build](https://github.com/Borelli99/AniTenshi/actions/workflows/build_fork.yml/badge.svg)](https://github.com/Borelli99/AniTenshi/actions/workflows/build_fork.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/Borelli99/AniTenshi?label=License&labelColor=14163A&color=818cf8)](/LICENSE)

</div>

## Download

Get the latest version from the **[Releases](https://github.com/Borelli99/AniTenshi/releases/latest)** page:

- **`anitenshi-arm64-v8a.apk`**: the right one for almost every phone.
- **`anitenshi-universal.apk`**: if the first one doesn't install (works on any device, but it's bigger).
- **`anitenshi-armeabi-v7a.apk`**: for older 32-bit Android phones.

*Requires Android 8.0 or higher.*

### Updates

AniTenshi checks for new versions by itself every time it starts, and you can check manually in *More → About → Check for updates*. Updates install over the previous version and keep all your data.

## What's different from Animetail

AniTenshi is a fork of [Animetail](https://github.com/Animetailapp/Animetail) and follows its updates. On top of that:

- **Modern anime extension repositories work.** Animetail decoded anime repository indexes with Mihon's manga format, so repositories listing torrent extensions (such as Yūzōnō) failed to add with "Error while decoding … NetworkExtensionStore", and in the others extensions lost their language. AniTenshi reads the Aniyomi format used by anime repositories and stays compatible with Mihon's.
- **Installs next to Animetail.** It has its own package name (`com.borelli99.anitenshi`), so you can keep both apps.
- **Signed releases, published automatically.** Every change is built, tested and published to the Releases page, and the app updates itself from there.

## Getting started

1. Install the APK (if Android asks, allow your browser to install apps).
2. **Coming from Animetail?** In Animetail, create a backup (*More → Settings → Data and storage → Create backup*) and restore it in AniTenshi from the same screen.
3. Add an extension repository: *More → Settings → Browse → Anime extension repos → Add*, and paste the repository's `index.min.json` URL.
4. Install extensions from *Browse → Extensions* and trust the ones you picked.

## Features

Everything Animetail offers, which in turn brings together the best of Aniyomi, Mihon and other forks:

- mpv-based video player with subtitles, audio tracks, seek previews and torrent streaming
- Configurable manga reader with multiple reading modes
- Home feed with movie, series, anime and manga recommendations, and "Continue watching & reading"
- Trackers: MyAnimeList, AniList, Kitsu, MangaUpdates, Shikimori, Bangumi, Simkl, Trakt and more
- Library categories, grouping and advanced search
- Downloads for offline watching and reading, local or cloud backups
- Cast, Material You themes, Discord Rich Presence

See the [Animetail README](https://github.com/Animetailapp/Animetail#features) for the full list.

## Reporting issues and contributing

Found a bug or have an idea? Open an [issue](https://github.com/Borelli99/AniTenshi/issues) in this repository, not in Animetail's.
Problems with a single extension (a site that doesn't load, an episode that won't play) should be reported to whoever maintains that extension.

Before contributing, read the [contributing guide](./CONTRIBUTING.md) and the [code of conduct](./CODE_OF_CONDUCT.md).

## Privacy

AniTenshi has no accounts, analytics, ads or crash reporting. See the [privacy policy](./PRIVACY.md).

## Credits

AniTenshi exists thanks to the people who created and maintain:

- [Animetail](https://github.com/Animetailapp/Animetail), which it is directly based on
- [Aniyomi](https://github.com/aniyomiorg/aniyomi) and [Mihon](https://github.com/mihonapp/mihon), and before them Tachiyomi
- the forks Animetail takes features from: Kuukiyomi, TachiyomiSY, Animiru, TachiyomiJ2K and Komikku

## Disclaimer

The developer of this application has no affiliation with the content providers available through extensions, and this application hosts no content.

## License

<pre>
Copyright © 2015 Javier Tomás
Copyright © 2024 Mihon Open Source Project
Copyright © 2024 Aniyomi Open Source Project
Copyright © 2024 The Animetail Open Source Project
Copyright © 2026 AniTenshi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>
