package com.example.lottoinjava.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottoinjava.R;
import com.example.lottoinjava.analysis.SpecEvidenza;
import com.example.lottoinjava.analysis.SpiaTutteEngine;
import com.example.lottoinjava.analysis.TecnicheCatalogo;
import com.example.lottoinjava.analysis.TecnicheCore;
import com.example.lottoinjava.data.LottoLocalCache;
import com.example.lottoinjava.model.Extraction;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tecniche di Distruttore: lo stesso elenco di 25 tecniche della versione Linux, con gli stessi parametri,
 * il rapporto completo e il pulsante che colora l'archivio (spia verde, risultato rosso).
 */
public class TecnicheActivity extends AppCompatActivity {

    public static final String EXTRA_TITOLO = "titolo";
    public static final String EXTRA_SPIE = "spie";
    public static final String EXTRA_NUMERI = "numeri";
    public static final String EXTRA_RUOTA = "ruota";
    public static final String EXTRA_COLPI = "colpi";
    public static final String EXTRA_POSIZIONE = "posizione";

    private int cBg, cCard, cStroke, cText, cTextSec, cAccent, cLost, cWin;

    private List<Extraction> archivio = new ArrayList<>();
    private int minYear = 2001, maxYear = 2025;
    private TecnicheCore core;
    private SpecEvidenza ultimaSpec;
    private String ultimoTesto = "";
    private boolean inCorso = false;

    private Spinner tecSpinner, ruotaSp, ruota2Sp, wheelBSp, targetSp, dimSp, modSp, multiModSp, tipoGiocoSp;
    private EditText spiaEt, y0Et, y1Et, colpiEt, posSpiaEt, nGiocoEt, annoTestEt, pos1Et, pos2Et, fissoEt, multiEt;
    private final List<CheckBox> ruoteChecks = new ArrayList<>();
    private final Map<Integer, View> righe = new LinkedHashMap<>();
    private TextView notaTv, statoTv, risultatoTv, infoTv;
    private MaterialButton calcolaBtn, evidenziaBtn, copiaBtn, condividiBtn;
    private LinearLayout azioniBox;

    // ------------------------------------------------------------------ ciclo di vita

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        cBg = ThemeHelper.color(this, R.attr.dBg);
        cCard = ThemeHelper.color(this, R.attr.dCard);
        cStroke = ThemeHelper.color(this, R.attr.dStroke);
        cText = ThemeHelper.color(this, R.attr.dText);
        cTextSec = ThemeHelper.color(this, R.attr.dTextSec);
        cAccent = ThemeHelper.color(this, R.attr.dAccent);
        cLost = ThemeHelper.color(this, R.attr.dLost);
        cWin = ThemeHelper.color(this, R.attr.dWin);

        setContentView(costruisci());
        caricaArchivio();
    }

    // ------------------------------------------------------------------ interfaccia

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable riquadro(int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(10));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private TextView etichetta(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextColor(cTextSec);
        v.setTextSize(13);
        v.setPadding(0, dp(6), 0, dp(2));
        return v;
    }

    private EditText campoNum(int def, int larghezzaDp) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(String.valueOf(def));
        e.setTextColor(cText);
        e.setSingleLine(true);
        e.setBackground(riquadro(cCard, cStroke));
        e.setPadding(dp(10), dp(8), dp(10), dp(8));
        e.setLayoutParams(new LinearLayout.LayoutParams(dp(larghezzaDp), ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private Spinner spinner(List<String> voci, int selezionata) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, voci) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView t = (TextView) super.getView(position, convertView, parent);
                t.setTextColor(cText);
                return t;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView t = (TextView) super.getDropDownView(position, convertView, parent);
                t.setTextColor(cText);
                t.setBackgroundColor(cCard);
                t.setPadding(dp(14), dp(12), dp(14), dp(12));
                return t;
            }
        };
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        sp.setSelection(Math.max(0, Math.min(selezionata, voci.size() - 1)));
        sp.setBackground(riquadro(cCard, cStroke));
        sp.setPadding(dp(6), dp(4), dp(6), dp(4));
        return sp;
    }

    private LinearLayout gruppo(View... figli) {
        LinearLayout g = new LinearLayout(this);
        g.setOrientation(LinearLayout.VERTICAL);
        for (View v : figli) g.addView(v);
        return g;
    }

    private LinearLayout fila(View... figli) {
        LinearLayout g = new LinearLayout(this);
        g.setOrientation(LinearLayout.HORIZONTAL);
        g.setGravity(Gravity.CENTER_VERTICAL);
        for (View v : figli) {
            g.addView(v);
            if (v instanceof EditText) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
                lp.rightMargin = dp(12);
            }
        }
        return g;
    }

    private MaterialButton bottone(String testo, boolean primario) {
        MaterialButton b = new MaterialButton(this);
        b.setText(testo);
        b.setAllCaps(false);
        b.setBackgroundTintList(ColorStateList.valueOf(primario ? cAccent : cCard));
        b.setTextColor(primario ? 0xFF000000 : cText);
        b.setStrokeColor(ColorStateList.valueOf(cStroke));
        b.setStrokeWidth(primario ? 0 : dp(1));
        return b;
    }

    private View costruisci() {
        final LinearLayout radice = new LinearLayout(this);
        radice.setOrientation(LinearLayout.VERTICAL);
        radice.setBackgroundColor(cBg);
        ViewCompat.setOnApplyWindowInsetsListener(radice, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, b.top, 0, b.bottom);
            return insets;
        });

        // intestazione con il cambio tema
        LinearLayout testata = new LinearLayout(this);
        testata.setOrientation(LinearLayout.HORIZONTAL);
        testata.setGravity(Gravity.CENTER_VERTICAL);
        testata.setPadding(dp(16), dp(12), dp(16), dp(8));
        LinearLayout titoli = new LinearLayout(this);
        titoli.setOrientation(LinearLayout.VERTICAL);
        TextView titolo = new TextView(this);
        titolo.setText("\uD83D\uDD2E Distruttore \u00B7 Tecniche");
        titolo.setTextColor(cAccent);
        titolo.setTextSize(20);
        titolo.setTypeface(Typeface.DEFAULT_BOLD);
        infoTv = new TextView(this);
        infoTv.setTextColor(cTextSec);
        infoTv.setTextSize(12);
        infoTv.setText("Carico l'archivio\u2026");
        titoli.addView(titolo);
        titoli.addView(infoTv);
        titoli.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        MaterialButton temaBtn = bottone(ThemeHelper.descrizione(this), false);
        temaBtn.setOnClickListener(v -> {
            ThemeHelper.prossimo(this);
            recreate();
        });
        testata.addView(titoli);
        testata.addView(temaBtn);
        radice.addView(testata);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(4), dp(16), dp(24));
        scroll.addView(col);
        radice.addView(scroll);

        // scelta della tecnica
        tecSpinner = spinner(Arrays.asList(TecnicheCatalogo.NOMI), 0);
        notaTv = new TextView(this);
        notaTv.setTextColor(cTextSec);
        notaTv.setTextSize(12);
        notaTv.setPadding(0, dp(6), 0, dp(6));
        col.addView(etichetta("Tecnica"));
        col.addView(tecSpinner);
        col.addView(notaTv);

        // campi
        List<String> ruote = new ArrayList<>(Extraction.ALL_RUOTE);
        List<String> targets = new ArrayList<>();
        targets.add("TUTTE");
        targets.addAll(ruote);
        List<String> sorti = Arrays.asList("Ambo", "Terno", "Quaterna", "Cinquina");
        ruotaSp = spinner(ruote, ruote.indexOf("Bari"));
        ruota2Sp = spinner(ruote, ruote.indexOf("Milano"));
        wheelBSp = spinner(ruote, ruote.indexOf("Milano"));
        targetSp = spinner(targets, 0);
        dimSp = spinner(sorti, 0);
        modSp = spinner(Arrays.asList("STESSA RUOTA", "RUOTE SELEZIONATE"), 0);
        multiModSp = spinner(Arrays.asList(SpiaTutteEngine.TUTTE_INSIEME, SpiaTutteEngine.ALMENO_UNA), 0);
        tipoGiocoSp = spinner(sorti, 0);
        spiaEt = campoNum(11, 80);
        y0Et = campoNum(minYear, 90);
        y1Et = campoNum(maxYear, 90);
        colpiEt = campoNum(9, 80);
        posSpiaEt = campoNum(1, 70);
        nGiocoEt = campoNum(5, 70);
        annoTestEt = campoNum(0, 90);
        pos1Et = campoNum(1, 70);
        pos2Et = campoNum(2, 70);
        fissoEt = campoNum(1, 80);
        multiEt = new EditText(this);
        multiEt.setInputType(InputType.TYPE_CLASS_TEXT);
        multiEt.setText("11 45");
        multiEt.setHint("fino a 5 spie, es. 11 45");
        multiEt.setTextColor(cText);
        multiEt.setHintTextColor(cTextSec);
        multiEt.setBackground(riquadro(cCard, cStroke));
        multiEt.setPadding(dp(10), dp(8), dp(10), dp(8));

        LinearLayout selRuote = new LinearLayout(this);
        selRuote.setOrientation(LinearLayout.VERTICAL);
        LinearLayout rigaCorrente = null;
        for (int i = 0; i < ruote.size(); i++) {
            if (i % 3 == 0) {
                rigaCorrente = new LinearLayout(this);
                rigaCorrente.setOrientation(LinearLayout.HORIZONTAL);
                selRuote.addView(rigaCorrente);
            }
            CheckBox cb = new CheckBox(this);
            cb.setText(ruote.get(i));
            cb.setTextColor(cText);
            cb.setChecked("Bari".equals(ruote.get(i)));
            ruoteChecks.add(cb);
            rigaCorrente.addView(cb);
        }

        righe.put(TecnicheCatalogo.R_RUOTA, gruppo(etichetta("Ruota"), ruotaSp));
        righe.put(TecnicheCatalogo.R_SPIA, gruppo(etichetta("Spia (1-90)"), fila(spiaEt)));
        righe.put(TecnicheCatalogo.R_ANNI, gruppo(etichetta("Anno inizio / Anno fine"), fila(y0Et, y1Et)));
        righe.put(TecnicheCatalogo.R_COLPI, gruppo(etichetta("Colpi (1-50)"), fila(colpiEt)));
        righe.put(TecnicheCatalogo.R_MULTI, gruppo(etichetta("Spie (fino a 5, separate da spazio)"), multiEt));
        righe.put(TecnicheCatalogo.R_DIM, gruppo(etichetta("Combinazione"), dimSp, etichetta("Modalit\u00E0"), modSp));
        righe.put(TecnicheCatalogo.R_RUOTESEL, gruppo(etichetta("Ruote selezionate"), selRuote));
        righe.put(TecnicheCatalogo.R_SOMMA, gruppo(etichetta("Posizione 1\u00AA ruota (1-5)"), fila(pos1Et),
                etichetta("2\u00AA ruota"), ruota2Sp, etichetta("Posizione 2\u00AA ruota (1-5)"), fila(pos2Et)));
        righe.put(TecnicheCatalogo.R_FISSO, gruppo(etichetta("Fisso (1-90)"), fila(fissoEt)));
        righe.put(TecnicheCatalogo.R_TARGET, gruppo(etichetta("Ruota esito"), targetSp));
        righe.put(TecnicheCatalogo.R_WHEELB, gruppo(etichetta("Seconda ruota"), wheelBSp));
        righe.put(TecnicheCatalogo.R_POSIZ, gruppo(etichetta("Posizione della spia (1-5)"), fila(posSpiaEt)));
        righe.put(TecnicheCatalogo.R_MODMULTI, gruppo(etichetta("Spie multiple"), multiModSp));
        righe.put(TecnicheCatalogo.R_NGIOCO, gruppo(etichetta("Numeri da giocare (2-10)"), fila(nGiocoEt)));
        righe.put(TecnicheCatalogo.R_ANNOTEST, gruppo(etichetta("Test: secondo periodo dall'anno (0 = auto)"), fila(annoTestEt)));
        righe.put(TecnicheCatalogo.R_TIPOGIOCO, gruppo(etichetta("Bilancio di gioco su"), tipoGiocoSp));
        for (View v : righe.values()) col.addView(v);

        calcolaBtn = bottone("\u25B6  Calcola", true);
        LinearLayout.LayoutParams lpCalc = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCalc.topMargin = dp(16);
        calcolaBtn.setLayoutParams(lpCalc);
        calcolaBtn.setOnClickListener(v -> avvia());
        col.addView(calcolaBtn);

        statoTv = new TextView(this);
        statoTv.setTextColor(cTextSec);
        statoTv.setTextSize(13);
        statoTv.setPadding(0, dp(8), 0, dp(4));
        col.addView(statoTv);

        risultatoTv = new TextView(this);
        risultatoTv.setTypeface(Typeface.MONOSPACE);
        risultatoTv.setTextSize(11);
        risultatoTv.setTextColor(cText);
        risultatoTv.setTextIsSelectable(true);
        risultatoTv.setPadding(dp(12), dp(12), dp(12), dp(12));
        risultatoTv.setBackground(riquadro(cCard, cStroke));
        risultatoTv.setVisibility(View.GONE);
        HorizontalScrollView orizz = new HorizontalScrollView(this);
        orizz.addView(risultatoTv);
        col.addView(orizz);

        LinearLayout azioni = new LinearLayout(this);
        azioniBox = azioni;
        azioni.setOrientation(LinearLayout.VERTICAL);
        evidenziaBtn = bottone("\uD83C\uDFA8  Evidenzia nell'archivio", true);
        copiaBtn = bottone("\uD83D\uDCCB  Copia il rapporto", false);
        condividiBtn = bottone("\uD83D\uDCE4  Condividi", false);
        for (MaterialButton b : new MaterialButton[]{evidenziaBtn, copiaBtn, condividiBtn}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            b.setLayoutParams(lp);
            azioni.addView(b);
        }
        azioni.setVisibility(View.GONE);
        col.addView(azioni);
        evidenziaBtn.setOnClickListener(v -> evidenzia());
        copiaBtn.setOnClickListener(v -> copia());
        condividiBtn.setOnClickListener(v -> condividi());

        tecSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                aggiornaForm(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        aggiornaForm(0);
        return radice;
    }

    private void aggiornaForm(int idx) {
        notaTv.setText(TecnicheCatalogo.NOTE[idx]);
        for (Map.Entry<Integer, View> e : righe.entrySet()) {
            e.getValue().setVisibility(TecnicheCatalogo.ha(idx, e.getKey()) ? View.VISIBLE : View.GONE);
        }
    }

    // ------------------------------------------------------------------ archivio

    private void caricaArchivio() {
        calcolaBtn.setEnabled(false);
        new Thread(() -> {
            List<Extraction> lista = new ArrayList<>(LottoLocalCache.loadExtractions(getApplicationContext()));
            // i motori vogliono l'archivio dal più recente al più vecchio
            Collections.sort(lista, new Comparator<Extraction>() {
                @Override
                public int compare(Extraction a, Extraction b) {
                    return Integer.compare(b.getDateKey(), a.getDateKey());
                }
            });
            int mn = Integer.MAX_VALUE, mx = Integer.MIN_VALUE;
            for (Extraction e : lista) {
                int y = e.getYear();
                if (y < mn) mn = y;
                if (y > mx) mx = y;
            }
            final int fmn = mn, fmx = mx;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                archivio = lista;
                if (lista.isEmpty()) {
                    infoTv.setText("Archivio vuoto");
                    statoTv.setTextColor(cLost);
                    statoTv.setText("Archivio vuoto: carica un CSV o aggiorna da Internet dalla schermata principale.");
                    return;
                }
                minYear = fmn;
                maxYear = fmx;
                core = new TecnicheCore(minYear, maxYear);
                infoTv.setText(lista.size() + " estrazioni \u00B7 " + minYear + "\u2013" + maxYear);
                y0Et.setText(String.valueOf(Math.max(minYear, maxYear - 5)));
                y1Et.setText(String.valueOf(maxYear));
                calcolaBtn.setEnabled(true);
            });
        }, "carica-archivio").start();
    }

    // ------------------------------------------------------------------ calcolo

    private static int leggi(EditText e, String etichetta) {
        String t = e.getText() == null ? "" : e.getText().toString().trim();
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valore non valido per \"" + etichetta + "\".");
        }
    }

    private String valore(Spinner s) {
        Object o = s.getSelectedItem();
        return o == null ? "" : o.toString();
    }

    private TecnicheCore.In leggiParametri(int idx) {
        TecnicheCore.In in = new TecnicheCore.In();
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_RUOTA)) in.ruota = valore(ruotaSp);
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_SPIA)) in.spia = leggi(spiaEt, "Spia");
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_ANNI)) {
            in.y0 = leggi(y0Et, "Anno inizio");
            in.y1 = leggi(y1Et, "Anno fine");
        } else {
            in.y0 = minYear;
            in.y1 = maxYear;
        }
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_COLPI)) in.colpi = leggi(colpiEt, "Colpi");
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_DIM)) {
            in.dim = dimSp.getSelectedItemPosition() + 2;
            in.modalita = valore(modSp);
        }
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_RUOTESEL)) {
            for (CheckBox cb : ruoteChecks) if (cb.isChecked()) in.ruoteSel.add(cb.getText().toString());
        }
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_SOMMA)) {
            in.ruota2 = valore(ruota2Sp);
            in.pos1 = leggi(pos1Et, "Posizione 1\u00AA ruota");
            in.pos2 = leggi(pos2Et, "Posizione 2\u00AA ruota");
        }
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_FISSO)) in.fisso = leggi(fissoEt, "Fisso");
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_TARGET)) in.target = valore(targetSp);
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_WHEELB)) in.wheelB = valore(wheelBSp);
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_POSIZ)) in.posSpia = leggi(posSpiaEt, "Posizione della spia");
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_MODMULTI)) in.modalita = valore(multiModSp);
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_NGIOCO)) in.nGioco = leggi(nGiocoEt, "Numeri da giocare");
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_ANNOTEST)) in.annoTest = leggi(annoTestEt, "Anno del test");
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_TIPOGIOCO)) in.tipoGioco = tipoGiocoSp.getSelectedItemPosition() + 2;
        if (TecnicheCatalogo.ha(idx, TecnicheCatalogo.R_MULTI)) {
            String testo = multiEt.getText() == null ? "" : multiEt.getText().toString().trim();
            for (String parte : testo.split("[^0-9]+")) {
                if (parte.isEmpty()) continue;
                int n;
                try {
                    n = Integer.parseInt(parte);
                } catch (NumberFormatException ex) {
                    n = -1;
                }
                if (n < 1 || n > 90) throw new IllegalArgumentException("Ogni spia deve essere tra 1 e 90.");
                if (!in.spie.contains(n)) in.spie.add(n);
            }
            if (in.spie.isEmpty()) throw new IllegalArgumentException("Inserisci almeno una spia.");
        }
        return in;
    }

    private void avvia() {
        if (inCorso || core == null) return;
        final int idx = tecSpinner.getSelectedItemPosition();
        final TecnicheCore.In in;
        try {
            in = leggiParametri(idx);
        } catch (IllegalArgumentException ex) {
            statoTv.setTextColor(cLost);
            statoTv.setText(ex.getMessage());
            return;
        }
        inCorso = true;
        calcolaBtn.setEnabled(false);
        statoTv.setTextColor(cTextSec);
        statoTv.setText("Calcolo in corso\u2026 (le tecniche pi\u00F9 pesanti richiedono qualche secondo)");
        risultatoTv.setVisibility(View.GONE);
        azioniBox.setVisibility(View.GONE);
        final TecnicheCore nucleo = core;
        final List<Extraction> dati = archivio;
        new Thread(() -> {
            TecnicheCore.Result r = null;
            String errore = null;
            try {
                r = nucleo.calcola(idx, in, dati);
            } catch (IllegalArgumentException ex) {
                errore = ex.getMessage();
            } catch (Throwable t) {
                errore = "Errore nel calcolo: " + t;
            }
            final TecnicheCore.Result fr = r;
            final String fe = errore;
            runOnUiThread(() -> {
                inCorso = false;
                if (isFinishing() || isDestroyed()) return;
                calcolaBtn.setEnabled(true);
                if (fe != null || fr == null) {
                    statoTv.setTextColor(cLost);
                    statoTv.setText(fe != null ? fe : "Nessun risultato.");
                    return;
                }
                ultimoTesto = fr.text;
                ultimaSpec = fr.spec;
                statoTv.setTextColor(cWin);
                statoTv.setText("Fatto.");
                risultatoTv.setText(fr.text);
                risultatoTv.setVisibility(View.VISIBLE);
                azioniBox.setVisibility(View.VISIBLE);
                evidenziaBtn.setVisibility(fr.spec != null ? View.VISIBLE : View.GONE);
            });
        }, "calcolo-tecnica").start();
    }

    // ------------------------------------------------------------------ azioni sul rapporto

    private static int[] aArray(List<Integer> l) {
        int[] a = new int[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }

    private void evidenzia() {
        if (ultimaSpec == null) {
            Toast.makeText(this, "Questa tecnica non ha un'evidenziazione.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_TITOLO, ultimaSpec.titolo);
        data.putExtra(EXTRA_SPIE, aArray(ultimaSpec.spie));
        data.putExtra(EXTRA_NUMERI, aArray(ultimaSpec.numeri));
        data.putExtra(EXTRA_RUOTA, ultimaSpec.ruotaPrincipale());
        data.putExtra(EXTRA_COLPI, ultimaSpec.colpi);
        data.putExtra(EXTRA_POSIZIONE, ultimaSpec.posizioneSpia);
        setResult(RESULT_OK, data);
        finish();
    }

    private void copia() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Distruttore", ultimoTesto));
            Toast.makeText(this, "Rapporto copiato.", Toast.LENGTH_SHORT).show();
        }
    }

    private void condividi() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, ultimoTesto);
        startActivity(Intent.createChooser(send, "Condividi il rapporto"));
    }
}
