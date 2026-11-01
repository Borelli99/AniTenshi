# Contribuire ad AniTenshi

Grazie per l'interesse! Prima di tutto leggi il [codice di condotta](./CODE_OF_CONDUCT.md).

## Segnalare un problema o proporre un'idea

- Apri una [issue](https://github.com/Borelli99/AniTenshi/issues) su questa repository. Non segnalare ad Animetail i problemi di AniTenshi.
- Scrivi la versione di AniTenshi (*Altro → Info*), il modello del telefono e la versione di Android, i passaggi per riprodurre il problema e, se puoi, uno screenshot.
- Se il problema riguarda una singola estensione (un sito che non carica, un episodio con "loading failed"), prova prima la stessa estensione su Aniyomi o Animetail: se succede anche lì, va segnalato a chi mantiene l'estensione.

## Contribuire al codice

Le pull request sono benvenute. Per modifiche grandi apri prima una issue, così ne parliamo.

### Cosa serve

- Conoscere le basi dello [sviluppo Android](https://developer.android.com/) e di [Kotlin](https://kotlinlang.org/)
- [Android Studio](https://developer.android.com/studio) e JDK 21
- Un emulatore o un telefono con le opzioni sviluppatore attive

### Compilare e provare

```bash
./gradlew assembleDebug           # APK di debug (pacchetto com.borelli99.anitenshi.dev)
./gradlew testDebugUnitTest       # test
./gradlew spotlessApply           # formatta il codice come richiesto dai controlli
```

La build di debug ha un nome di pacchetto diverso, quindi si installa accanto alla versione normale.

### Come vengono pubblicate le versioni

Il workflow [`Build AniTenshi APK`](./.github/workflows/build_fork.yml) parte a ogni push su `master` (le modifiche ai soli file `.md` non avviano la build), oppure a mano dalla scheda *Actions*. Esegue i test, compila l'APK di release, lo firma e lo pubblica nelle [Releases](https://github.com/Borelli99/AniTenshi/releases).

La firma usa la chiave salvata nel secret `KEYSTORE_BASE64` della repository. Deve restare sempre la stessa: con una chiave diversa le nuove versioni non si installano sopra quelle vecchie.

### Restare aggiornati con Animetail

AniTenshi segue Animetail. Per prendere le novità, sulla pagina GitHub della repository tocca **Sync fork**, oppure da terminale:

```bash
git remote add upstream https://github.com/Animetailapp/Animetail.git
git fetch upstream
git merge upstream/master
```

Le correzioni utili a tutti (come quella sulle repository anime) conviene proporle anche ad [Animetail](https://github.com/Animetailapp/Animetail), così non devono essere mantenute solo qui.

## Traduzioni

I testi dell'app arrivano da Animetail, Aniyomi e Mihon: per migliorare una traduzione contribuisci al progetto da cui proviene il testo. Le uniche stringhe proprie di AniTenshi sono il nome dell'app e i testi di questa repository.

## Fork di AniTenshi

Puoi fare una fork di AniTenshi rispettando la [licenza](./LICENSE). Per evitare confusione e conflitti:

- cambia il nome dell'app (`app_name` in `i18n/src/commonMain/moko-resources/base/strings.xml`) e l'icona;
- cambia l'`applicationId` in [`app/build.gradle.kts`](./app/build.gradle.kts), altrimenti la tua app non può essere installata accanto a questa;
- crea una tua chiave di firma (formato PKCS12, alias e password `anitenshi`, oppure cambia questi valori nel workflow) e salvala in base64 nel secret `KEYSTORE_BASE64` della tua repository;
- lascia disattivato il controllo aggiornamenti (lo è già nelle build di questa repository), oppure puntalo alle tue Releases.
