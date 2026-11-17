package com.example.lottoinjava.analysis;

/**
 * Catalogo delle tecniche (stesso elenco e stessi parametri della versione Linux): nomi, note e righe del modulo visibili.
 * Nessuna dipendenza da Android o JavaFX.
 */
public final class TecnicheCatalogo {

    private TecnicheCatalogo() { }

    public static final String[] NOMI = {
            "🔎 Ricerca spia & resoconto esiti",
            "⭐ Previsione post-spia (TOP 5, ambi, terni)",
            "🧭 Ciclometria spia",
            "✨ Terni magici",
            "🧩 Resoconto unificato (post-spia + ciclometria)",
            "⚡ Convergenze tecniche",
            "✅ Verifica combinazioni dopo la spia",
            "🛒 Apriori / Market basket",
            "➕ Costruttore Somma Ruote",
            "➕ Costruttore Somma + Fisso",
            "📈 Previsione a 5 numeri (frequenza/ritardo)",
            "🗡️ Metodo D'Artagnan",
            "📐 Isotopia estrazionale",
            "🌀 Serie di Fibonacci",
            "☕ Metodo Cappuccino",
            "📊 Costruttore Multistatico",
            "🚀 Super previsione statistica light",
            "🧠 Previsione con algoritmi IA",
            "🕵️ Spia a tutte (TOP 5 multi-ruota)",
            "🎯 Spia a tutte — risultati sulla ruota origine",
            "🌐 Verifica spia su tutte le ruote",
            "📍 Spia in posizione (verifica su tutte)",
            "🔗 Spia multipla, fino a 5 spie (verifica su tutte)",
            "🔎 Scanner di spie (classifica di tutte le spie)",
            "🧪 Collaudo previsioni (test onesto sul tuo archivio)"
    };

    public static final String NOTA_MAIN = "✔ Calcolo allineato al main.py (confrontato su archivio reale: risultati identici).";
    public static final String NOTA_MAIN_FIX = "✔ Calcolo allineato al main.py; le co-presenze usano la chiave ordinata "
            + "(il main.py le cerca con chiave non ordinata e ne perde circa la metà).";
    public static final String NOTA_JAVA = "ℹ Metodo presente solo nell'app Java: nel main.py non esiste, quindi non è confrontabile. "
            + "Usa tutto l'archivio (non il periodo).";

    public static final String NOTA_SPIA_TUTTE = "ℹ Metodo presente solo nell'app Java: i numeri da giocare sono quelli che escono sulla ruota di origine dopo la spia; "
            + "vengono verificati su tutte le 11 ruote (ambo, terno, quaterna, cinquina, percentuali), con test walk-forward, test su due periodi e bilancio di gioco.";

    public static final String[] NOTE = {
            NOTA_MAIN, NOTA_MAIN, NOTA_MAIN, NOTA_MAIN_FIX, NOTA_MAIN_FIX, NOTA_MAIN_FIX,
            NOTA_MAIN, NOTA_MAIN, NOTA_MAIN, NOTA_MAIN,
            NOTA_JAVA, NOTA_JAVA, NOTA_JAVA, NOTA_JAVA, NOTA_JAVA, NOTA_JAVA,
            NOTA_MAIN, NOTA_MAIN, NOTA_JAVA, NOTA_JAVA,
            NOTA_SPIA_TUTTE, NOTA_SPIA_TUTTE, NOTA_SPIA_TUTTE,
            "ℹ Metodo presente solo nell'app Java: prova tutte le spie da 1 a 90 sulla ruota scelta e le ordina per quanto aggiungono rispetto "
            + "a giocare gli stessi numeri senza spia (walk-forward, due periodi, bilancio). Con archivi lunghi può richiedere qualche secondo.",
            "ℹ Metodo presente solo nell'app Java: mette alla prova i metodi di previsione dell'app contro il caso, usando a ogni passo solo le "
            + "estrazioni precedenti (anni = storia usata e ultimo anno valutato; test dal = primo anno valutato, 0 = ultimi 10). "
            + "Richiede da una decina di secondi a qualche minuto."
    };

    // righe del modulo visibili per ogni tecnica
    public static final int R_RUOTA = 1, R_SPIA = 2, R_ANNI = 4, R_COLPI = 8, R_MULTI = 16, R_DIM = 32,
            R_RUOTESEL = 64, R_SOMMA = 128, R_FISSO = 256, R_TARGET = 512, R_WHEELB = 1024,
            R_POSIZ = 2048, R_NGIOCO = 4096, R_MODMULTI = 8192,
            R_ANNOTEST = 16384, R_TIPOGIOCO = 32768;

    public static final int SPIA_STD = R_RUOTA | R_SPIA | R_ANNI | R_COLPI;
    public static final int[] FLAGS = {
            SPIA_STD, SPIA_STD, SPIA_STD, SPIA_STD, SPIA_STD, SPIA_STD,
            R_SPIA | R_COLPI | R_DIM | R_RUOTESEL,
            SPIA_STD,
            R_RUOTA | R_SOMMA | R_FISSO | R_TARGET | R_ANNI | R_COLPI,
            R_RUOTA | R_ANNI,
            R_RUOTA,
            R_RUOTA | R_SPIA | R_COLPI,
            R_RUOTA | R_WHEELB | R_COLPI,
            R_RUOTA,
            R_RUOTA,
            R_RUOTA | R_MULTI | R_COLPI,
            SPIA_STD, SPIA_STD | R_RUOTESEL, SPIA_STD | R_RUOTESEL, SPIA_STD | R_RUOTESEL,
            SPIA_STD | R_NGIOCO | R_ANNOTEST | R_TIPOGIOCO,
            SPIA_STD | R_POSIZ | R_NGIOCO | R_ANNOTEST | R_TIPOGIOCO,
            R_RUOTA | R_MULTI | R_MODMULTI | R_ANNI | R_COLPI | R_NGIOCO | R_ANNOTEST | R_TIPOGIOCO,
            R_RUOTA | R_ANNI | R_COLPI | R_NGIOCO | R_ANNOTEST | R_TIPOGIOCO,
            R_ANNI | R_COLPI | R_NGIOCO | R_ANNOTEST | R_TIPOGIOCO
    };

    public static boolean ha(int tecnica, int riga) {
        return tecnica >= 0 && tecnica < FLAGS.length && (FLAGS[tecnica] & riga) != 0;
    }
}
