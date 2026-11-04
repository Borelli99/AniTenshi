# Privacy policy

_Last updated: November 4, 2026_

AniTenshi is a free, open-source Android app maintained by [Borelli99](https://github.com/Borelli99). This page explains what happens to your data when you use it.

## Summary

- AniTenshi has **no account**, **no analytics**, **no ads** and **no crash reporting service**.
- Your library, history, settings and downloads are stored **on your device**.
- The app only talks to the services you choose to use: extension sources, trackers, sync services, Discord, and GitHub for update checks.

## Data stored on your device

Your library, categories, watch and read history, downloads, settings, tracker logins and backups are saved in the app's private storage or in the folders you pick. They are not sent to the developer. Uninstalling the app or clearing its data removes them, except backups and downloads saved in folders you chose.

## Services the app connects to

| Service | When | What is sent |
| --- | --- | --- |
| **Extension sources** (websites of anime and manga providers) | When you browse, search, watch or read | Normal web requests to that website, like any browser. Extensions are made by third parties and are not part of AniTenshi. |
| **Extension repositories** | When you add a repository or check for extension updates | A request to download the repository index and the extensions you install |
| **GitHub** | When the app checks for updates and when you download one | A request to the GitHub API for the latest AniTenshi release |
| **Trackers** (MyAnimeList, AniList, Kitsu, Shikimori, Bangumi, Simkl, Trakt and others) | Only if you log in to them | Your progress and scores for the titles you track |
| **Sync services** (Google Drive, SyncYomi) | Only if you enable sync | Your library data, to keep your devices in sync |
| **Discord Rich Presence** | Only if you enable it in Connections | What you are watching or reading, shown on your Discord profile. Your Discord token stays on your device. |
| **Cast devices** | Only when you cast | The video stream you are casting |

Each of these services has its own privacy policy, which applies to the data you share with it.

## Google Drive sync

Google Drive sync is optional and off until you sign in from **Settings › Data and storage › Sync**. When you sign in, AniTenshi asks Google only for these permissions:

- `https://www.googleapis.com/auth/drive.file`: the files the app itself creates in your Drive;
- `https://www.googleapis.com/auth/drive.appdata`: the app's private folder in your Drive.

AniTenshi uses them only to save and read its sync file (your library, progress, categories and settings), so that your devices show the same data. The data goes directly between your device and your Google account: it is never sent to the developer or to anyone else, and it is not used for ads or analytics. The sign-in tokens are stored only on your device.

You can delete the synced data from the app (**Settings › Data and storage › Sync › Clear Sync Data from Google Drive**), sign out at any time, and remove AniTenshi's access in your Google account at <https://myaccount.google.com/permissions>.

AniTenshi's use and transfer to any other app of information received from Google APIs will adhere to the [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy), including the Limited Use requirements.

## Permissions

AniTenshi asks for permissions only to do what you ask it to do, for example notifications for updates and downloads, access to the folders you choose for downloads and backups, and permission to install the app updates you download.

## Children

AniTenshi does not knowingly collect any data from anyone, including children.

## Changes

If this policy changes, the new version will be published in this file, in the [AniTenshi repository](https://github.com/Borelli99/AniTenshi).

## Contact

For questions about this policy, write to anitenshi26@gmail.com, open an [issue](https://github.com/Borelli99/AniTenshi/issues) or contact the maintainer through the [GitHub profile](https://github.com/Borelli99).
