package com.example.lottoinjava.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;

import com.example.lottoinjava.R;

/**
 * Temi di Distruttore (stessi della versione Linux): Blu scuro, Chiaro, Rosso fuoco, Vampiro.
 * Il tema scelto si ricorda tra un avvio e l'altro; le palline dell'archivio restano uguali in tutti i temi.
 */
public final class ThemeHelper {

    private ThemeHelper() { }

    public static final String[] NOMI = {"Blu scuro", "Chiaro", "Rosso fuoco", "Vampiro"};
    public static final String[] ICONE = {"\uD83C\uDF19", "\u2600\uFE0F", "\uD83D\uDD25", "\uD83E\uDDDB"};
    private static final String PREFS = "distruttore_prefs";
    private static final String KEY_TEMA = "tema";

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Indice del tema corrente (0 = Blu scuro). */
    public static int indice(Context c) {
        int i = prefs(c).getInt(KEY_TEMA, 0);
        return (i < 0 || i >= NOMI.length) ? 0 : i;
    }

    public static void imposta(Context c, int indice) {
        prefs(c).edit().putInt(KEY_TEMA, ((indice % NOMI.length) + NOMI.length) % NOMI.length).apply();
    }

    /** Passa al tema successivo, lo salva e restituisce il nuovo indice. */
    public static int prossimo(Context c) {
        int i = (indice(c) + 1) % NOMI.length;
        imposta(c, i);
        return i;
    }

    public static int styleRes(Context c) {
        switch (indice(c)) {
            case 1: return R.style.Theme_Distruttore_Chiaro;
            case 2: return R.style.Theme_Distruttore_Fuoco;
            case 3: return R.style.Theme_Distruttore_Vampiro;
            default: return R.style.Theme_Distruttore_Blu;
        }
    }

    /** Da chiamare in onCreate prima di setContentView. */
    public static void apply(Activity a) {
        a.setTheme(styleRes(a));
    }

    /** Colore di un attributo del tema (es. R.attr.dText). */
    public static int color(Context c, int attr) {
        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    public static String descrizione(Context c) {
        int i = indice(c);
        return ICONE[i] + " " + NOMI[i];
    }
}
