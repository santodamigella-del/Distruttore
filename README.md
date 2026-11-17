# Distruttore

App Android (Android Studio) con **tutte le 25 tecniche** della versione Linux e **4 temi**, costruita sopra la tua app "lotto in java":
archivio estrazioni, giocate, ritardatari, quadro e ricerca spia restano come prima.

L'app si installa **accanto** alla vecchia (identificativo `com.example.distruttore`, icona con fondo rosso): non la sostituisce.

## Come compilarla (Android Studio, anche su Linux/EndeavourOS)
1. Apri Android Studio → **File > Open** → scegli questa cartella (`Distruttore`). Attendi la sincronizzazione di Gradle.
   (Il progetto usa Gradle 9.6, AGP 9.4.1 e JDK 25: Android Studio li scarica da solo; serve internet la prima volta.)
2. Collega il telefono con il **debug USB** attivo (oppure usa un emulatore) e premi **Run ▶**.
3. Per avere il file da installare a mano: **Build > Build Bundle(s) / APK(s) > Build APK(s)**. L'APK è in
   `app/build/outputs/apk/debug/app-debug.apk`; da terminale: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Cosa c'è di nuovo
- **🧰 Tecniche (25, come Linux)**: voce nel menu in alto a destra. Stesso elenco e stessi parametri della versione Linux (ricerca spia,
  previsioni, D'Artagnan, Fibonacci, Cappuccino, Multistatico, super previsione, previsione IA, spia a tutte, **verifica spia su tutte le
  ruote, spia in posizione, spia multipla, scanner di spie, collaudo previsioni**). Il calcolo gira in background; il rapporto si può
  copiare e condividere. I rapporti sono identici a quelli della versione Linux (verificato su archivio di prova).
- **🎰 SuperEnalotto**: voce nel menu. Archivio **separato** da quello del Lotto (file interno `superenalotto_archive.csv`), con
  **Importa CSV** (formato `DATA;CONCORSO;N1..N6;JOLLY;SUPERSTAR`, sostituisce l'archivio), **Online** (due fonti, unione senza doppioni),
  **Reset** (con copia di sicurezza) e archivio con le palline colorate (spia verde, candidati blu, riscontri nei colpi successivi rossi).
  Le 8 tecniche della versione Linux: analisi post-spia, previsione statistica, ciclometria, resoconto unificato, backtest walk-forward,
  confronto modelli, backtest ciclometria vs casuale, verifica sadica + holdout; in più **numeri caldi/freddi** con grafici e viste separate
  per sestina, Jolly e SuperStar.
  Include anche uno **scanner di tutte le 90 spie**, valutato walk-forward, più confronto fuori campione su un periodo successivo.
  **Le mie sestine** salva localmente le giocate e controlla le corrispondenze con le nuove estrazioni; il **generatore** applica vincoli
  su pari/dispari e somma senza attribuire loro valore predittivo. **Controlla archivio** mostra l'ultima estrazione, i duplicati nel CSV
  e gli intervalli temporali insolitamente lunghi.
  `analysis/SuperEnalottoCore` è una copia della logica di `SuperEnalottoDialog` (Linux) senza JavaFX; ho solo reso più veloci i backtest
  (non ricalcolano più le sestine candidate dentro ogni ciclo) con rapporti identici carattere per carattere.
- **🎨 Cambia tema**: Blu scuro, Chiaro, Rosso fuoco, Vampiro (le stesse palette di Linux). Il tema si ricorda tra un avvio e l'altro.
  Si può cambiare anche dalla schermata Tecniche.
- **Evidenzia nell'archivio**: dal rapporto di una tecnica, la spia diventa verde e i numeri da giocare rossi nell'archivio
  (con la spia in posizione, solo nella posizione scelta). Su Android l'evidenziazione colora i numeri ovunque compaiano, come le altre
  tecniche dell'app; la versione Linux colora solo i colpi successivi alla spia.

## Archivio
Usa lo stesso archivio dell'app: **Carica File CSV** (formato `AAAA-MM-GG;SIGLA;Città;n1;n2;n3;n4;n5` dal 1939 in poi) oppure
**Ricostruisci archivio Lotto**. Le tecniche più pesanti (scanner, collaudo) con archivi lunghi possono richiedere da qualche secondo
a qualche minuto sul telefono.

## Cose da sapere
- Nel gioco del Lotto, **Nazionale è selezionabile singolarmente**; l'opzione **Tutte** comprende le 10 ruote classiche e la esclude.
- Le statistiche, il generatore e i backtest descrivono dati passati: non aumentano le probabilità di vincita.
- `analysis/TecnicheCore` è una copia del runner della versione Linux (senza JavaFX): se cambi una tecnica su Linux, va aggiornata anche qui.
- Nessun metodo di previsione batte il caso in un gioco equo: usa il **Collaudo previsioni** per verificarlo sul tuo archivio.
