<div align="center">

<a href="https://github.com/Borelli99/AniTenshi">
    <img src="./.github/assets/icon.png" alt="Logo di AniTenshi" title="AniTenshi" width="96"/>
</a>

# AniTenshi

### Il lettore di anime e manga per Android, basato su Animetail.
Cerca, guarda e leggi anime, serie, film e manga dalle estensioni che preferisci, con un lettore video completo e la libreria sempre sincronizzata con i tuoi tracker.

[![Ultima versione](https://img.shields.io/github/v/release/Borelli99/AniTenshi?label=Ultima%20versione&labelColor=14163A&color=F2C14E)](https://github.com/Borelli99/AniTenshi/releases/latest)
[![Build](https://github.com/Borelli99/AniTenshi/actions/workflows/build_fork.yml/badge.svg)](https://github.com/Borelli99/AniTenshi/actions/workflows/build_fork.yml)
[![Licenza: Apache-2.0](https://img.shields.io/github/license/Borelli99/AniTenshi?label=Licenza&labelColor=14163A&color=818cf8)](/LICENSE)

</div>

## Scarica

Scarica l'ultima versione dalla pagina **[Releases](https://github.com/Borelli99/AniTenshi/releases/latest)**:

- **`anitenshi-arm64-v8a.apk`**: quello giusto per quasi tutti i telefoni.
- **`anitenshi-universal.apk`**: se il primo non si installa (funziona su qualsiasi dispositivo, ma pesa di più).
- **`anitenshi-armeabi-v7a.apk`**: per telefoni Android a 32 bit, più vecchi.

*Serve Android 8.0 o superiore.* Ogni nuova versione si installa sopra la precedente senza perdere i dati.

## Cosa cambia rispetto ad Animetail

AniTenshi è una fork di [Animetail](https://github.com/Animetailapp/Animetail) e ne segue gli aggiornamenti. In più:

- **Le repository anime moderne funzionano.** Animetail leggeva l'indice delle repository anime nel formato dei manga di Mihon, quindi le repository che includono estensioni torrent (come Yūzōnō) non si potevano aggiungere ("Error while decoding … NetworkExtensionStore"), e nelle altre le estensioni perdevano la lingua. AniTenshi legge il formato di Aniyomi usato dalle repository anime e resta compatibile con quello di Mihon.
- **Si installa accanto ad Animetail.** Ha un nome di pacchetto suo (`com.borelli99.anitenshi`), quindi puoi tenere entrambe le app.
- **Versioni firmate e pubblicate in automatico.** A ogni modifica GitHub compila l'app, esegue i test e pubblica l'APK nelle Releases.

## Primi passi

1. Installa l'APK (se Android lo chiede, consenti l'installazione di app dal browser).
2. **Vieni da Animetail?** In Animetail crea un backup (*Impostazioni → Dati e archiviazione → Crea backup*) e ripristinalo in AniTenshi dallo stesso menu.
3. Aggiungi una repository di estensioni: *Impostazioni → Sfoglia → Repo delle estensioni anime → Aggiungi*, e incolla l'indirizzo `index.min.json` della repository.
4. Installa le estensioni da *Sfoglia → Estensioni* e conferma di fidarti di quelle che hai scelto.

## Funzionalità

Tutto quello che offre Animetail, che a sua volta unisce il meglio di Aniyomi, Mihon e altre fork:

- Lettore video basato su mpv con sottotitoli, tracce audio, anteprime durante la ricerca e streaming torrent
- Lettore manga configurabile, con più modalità di lettura
- Home con consigli su film, serie, anime e manga, e "Continua a guardare e leggere"
- Tracker: MyAnimeList, AniList, Kitsu, MangaUpdates, Shikimori, Bangumi e Hikka
- Categorie, raggruppamenti e ricerca avanzata nella libreria
- Download per guardare e leggere offline, backup locali o sul cloud
- Cast, temi Material You, Discord Rich Presence

L'elenco completo è nel [README di Animetail](https://github.com/Animetailapp/Animetail#features).

## Segnalare problemi e contribuire

Hai trovato un bug o hai un'idea? Apri una [issue](https://github.com/Borelli99/AniTenshi/issues) su questa repository, non su quella di Animetail.
I problemi di una singola estensione (un sito che non carica, un episodio che non parte) vanno segnalati a chi mantiene quella estensione.

Prima di contribuire leggi la [guida per contribuire](./CONTRIBUTING.md) e il [codice di condotta](./CODE_OF_CONDUCT.md).

## Crediti

AniTenshi esiste grazie al lavoro di chi ha creato e mantiene:

- [Animetail](https://github.com/Animetailapp/Animetail), su cui si basa direttamente
- [Aniyomi](https://github.com/aniyomiorg/aniyomi) e [Mihon](https://github.com/mihonapp/mihon), e prima ancora Tachiyomi
- le fork da cui Animetail prende alcune funzioni: Kuukiyomi, TachiyomiSY, Animiru, TachiyomiJ2K e Komikku

## Disclaimer

Chi sviluppa questa applicazione non ha alcun legame con i fornitori dei contenuti disponibili tramite le estensioni, e l'applicazione non ospita alcun contenuto.

## Licenza

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
