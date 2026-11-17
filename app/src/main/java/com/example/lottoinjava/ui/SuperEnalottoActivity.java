package com.example.lottoinjava.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottoinjava.R;
import com.example.lottoinjava.analysis.SuperEnalottoCore;
import com.example.lottoinjava.analysis.SuperEnalottoCore.Draw;
import com.example.lottoinjava.data.SuperEnalottoTicketsStore;
import com.example.lottoinjava.model.SuperEnalottoTicket;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * SuperEnalotto di Distruttore: archivio separato da quello del Lotto, importazione CSV, aggiornamento online,
 * reset con copia di sicurezza, le tecniche di analisi e l'archivio con le palline colorate
 * (spia verde, candidati blu, riscontri nei colpi successivi rossi).
 */
public class SuperEnalottoActivity extends AppCompatActivity {

    private int cBg, cCard, cStroke, cText, cTextSec, cAccent, cLost, cWin;

    private final List<Draw> draws = new ArrayList<>();
    private File fileArchivio;
    private boolean inCorso = false;

    private EditText spiaEt, daEt, aEt, colpiEt, cercaEt;
    private TextView infoTv, statoTv, risultatoTv;
    private LinearLayout vistaTecniche, vistaArchivio, azioniBox;
    private MaterialButton tabTecniche, tabArchivio;
    private CheckBox coloraCb;
    private RecyclerView lista;
    private DrawAdapter adapter;
    private final List<MaterialButton> bottoniTecniche = new ArrayList<>();

    private SuperEnalottoCore.Evidenza evidenza = new SuperEnalottoCore.Evidenza();
    private int spiaEvidenza = 0;
    private String ultimoTesto = "";

    private final ActivityResultLauncher<String[]> importaLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importa(uri);
            });

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
        fileArchivio = new File(getFilesDir(), "superenalotto_archive.csv");

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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(larghezzaDp), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(10);
        e.setLayoutParams(lp);
        return e;
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

    private LinearLayout.LayoutParams pieno(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        return lp;
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

        // intestazione
        LinearLayout testata = new LinearLayout(this);
        testata.setOrientation(LinearLayout.HORIZONTAL);
        testata.setGravity(Gravity.CENTER_VERTICAL);
        testata.setPadding(dp(16), dp(12), dp(16), dp(8));
        LinearLayout titoli = new LinearLayout(this);
        titoli.setOrientation(LinearLayout.VERTICAL);
        TextView titolo = new TextView(this);
        titolo.setText("\uD83C\uDFB0 Distruttore \u00B7 SuperEnalotto");
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

        // schede
        LinearLayout schede = new LinearLayout(this);
        schede.setOrientation(LinearLayout.HORIZONTAL);
        schede.setPadding(dp(16), 0, dp(16), dp(8));
        tabTecniche = bottone("\uD83E\uDDEA Tecniche", true);
        tabArchivio = bottone("\uD83D\uDDC2 Archivio", false);
        LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpA.rightMargin = dp(8);
        tabTecniche.setLayoutParams(lpA);
        tabArchivio.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tabTecniche.setOnClickListener(v -> mostra(false));
        tabArchivio.setOnClickListener(v -> mostra(true));
        schede.addView(tabTecniche);
        schede.addView(tabArchivio);
        radice.addView(schede);

        costruisciTecniche();
        costruisciArchivio();
        radice.addView(vistaTecniche);
        radice.addView(vistaArchivio);
        mostra(false);
        return radice;
    }

    private void costruisciTecniche() {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(4), dp(16), dp(24));
        scroll.addView(col);

        spiaEt = campoNum(7, 70);
        daEt = campoNum(2000, 90);
        aEt = campoNum(2025, 90);
        colpiEt = campoNum(9, 70);
        LinearLayout riga1 = new LinearLayout(this);
        riga1.setOrientation(LinearLayout.VERTICAL);
        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout cSpia = new LinearLayout(this);
        cSpia.setOrientation(LinearLayout.VERTICAL);
        cSpia.addView(etichetta("Spia (1-90)"));
        cSpia.addView(spiaEt);
        LinearLayout cColpi = new LinearLayout(this);
        cColpi.setOrientation(LinearLayout.VERTICAL);
        cColpi.addView(etichetta("Colpi (1-50)"));
        cColpi.addView(colpiEt);
        r1.addView(cSpia);
        r1.addView(cColpi);
        LinearLayout r2 = new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout cDa = new LinearLayout(this);
        cDa.setOrientation(LinearLayout.VERTICAL);
        cDa.addView(etichetta("Anno da"));
        cDa.addView(daEt);
        LinearLayout cA = new LinearLayout(this);
        cA.setOrientation(LinearLayout.VERTICAL);
        cA.addView(etichetta("Anno a"));
        cA.addView(aEt);
        r2.addView(cDa);
        r2.addView(cA);
        riga1.addView(r1);
        riga1.addView(r2);
        col.addView(riga1);

        for (String[] t : SuperEnalottoCore.TECNICHE) {
            final String modo = t[0];
            MaterialButton b = bottone(t[1], "unified".equals(modo));
            b.setLayoutParams(pieno(8));
            b.setOnClickListener(v -> esegui(modo));
            bottoniTecniche.add(b);
            col.addView(b);
        }
        MaterialButton scanner = bottone("\uD83D\uDD0E Scanner di spie (1-90)", false);
        scanner.setLayoutParams(pieno(8));
        scanner.setOnClickListener(v -> eseguiScanner());
        bottoniTecniche.add(scanner);
        col.addView(scanner);
        MaterialButton stat = bottone("\uD83D\uDCCA Numeri caldi/freddi", false);
        stat.setLayoutParams(pieno(8));
        stat.setOnClickListener(v -> mostraStatistiche());
        col.addView(stat);
        MaterialButton giocate = bottone("\uD83D\uDCCB Le mie sestine", false);
        giocate.setLayoutParams(pieno(8));
        giocate.setOnClickListener(v -> mostraGiocate(null));
        col.addView(giocate);
        MaterialButton generatore = bottone("\uD83C\uDFB2 Generatore di sestine", false);
        generatore.setLayoutParams(pieno(8));
        generatore.setOnClickListener(v -> mostraGeneratore());
        col.addView(generatore);
        MaterialButton diagnostica = bottone("\uD83E\uDDEA Controlla archivio", false);
        diagnostica.setLayoutParams(pieno(8));
        diagnostica.setOnClickListener(v -> mostraTesto(SuperEnalottoCore.diagnosticaArchivio(draws)));
        col.addView(diagnostica);
        MaterialButton fuoriCampione = bottone("\uD83D\uDCCA Scanner · test fuori campione", false);
        fuoriCampione.setLayoutParams(pieno(8));
        fuoriCampione.setOnClickListener(v -> eseguiConfrontoScanner());
        bottoniTecniche.add(fuoriCampione);
        col.addView(fuoriCampione);

        statoTv = new TextView(this);
        statoTv.setTextColor(cTextSec);
        statoTv.setTextSize(13);
        statoTv.setPadding(0, dp(10), 0, dp(4));
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

        azioniBox = new LinearLayout(this);
        azioniBox.setOrientation(LinearLayout.VERTICAL);
        MaterialButton copia = bottone("\uD83D\uDCCB  Copia il rapporto", false);
        MaterialButton condividi = bottone("\uD83D\uDCE4  Condividi", false);
        MaterialButton vediArchivio = bottone("\uD83D\uDDC2  Vedi l'archivio colorato", true);
        copia.setLayoutParams(pieno(8));
        condividi.setLayoutParams(pieno(8));
        vediArchivio.setLayoutParams(pieno(8));
        azioniBox.addView(vediArchivio);
        azioniBox.addView(copia);
        azioniBox.addView(condividi);
        azioniBox.setVisibility(View.GONE);
        col.addView(azioniBox);
        copia.setOnClickListener(v -> copiaRapporto());
        condividi.setOnClickListener(v -> condividiRapporto());
        vediArchivio.setOnClickListener(v -> mostra(true));

        vistaTecniche = new LinearLayout(this);
        vistaTecniche.setOrientation(LinearLayout.VERTICAL);
        vistaTecniche.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        vistaTecniche.addView(scroll);
    }

    private void costruisciArchivio() {
        vistaArchivio = new LinearLayout(this);
        vistaArchivio.setOrientation(LinearLayout.VERTICAL);
        vistaArchivio.setPadding(dp(16), dp(4), dp(16), 0);
        vistaArchivio.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout azioni = new LinearLayout(this);
        azioni.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton importa = bottone("\uD83D\uDCC2 Importa CSV", false);
        MaterialButton online = bottone("\uD83C\uDF10 Online", true);
        MaterialButton reset = bottone("\uD83D\uDDD1 Reset", false);
        for (MaterialButton b : new MaterialButton[]{importa, online, reset}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.rightMargin = dp(6);
            b.setLayoutParams(lp);
            b.setTextSize(12);
            azioni.addView(b);
        }
        importa.setOnClickListener(v -> importaLauncher.launch(new String[]{"*/*"}));
        online.setOnClickListener(v -> aggiornaOnline());
        reset.setOnClickListener(v -> chiediReset());
        vistaArchivio.addView(azioni);

        cercaEt = new EditText(this);
        cercaEt.setInputType(InputType.TYPE_CLASS_TEXT);
        cercaEt.setHint("Cerca data, concorso o numero\u2026");
        cercaEt.setHintTextColor(cTextSec);
        cercaEt.setTextColor(cText);
        cercaEt.setSingleLine(true);
        cercaEt.setBackground(riquadro(cCard, cStroke));
        cercaEt.setPadding(dp(10), dp(8), dp(10), dp(8));
        cercaEt.setLayoutParams(pieno(8));
        cercaEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                aggiornaLista();
            }
        });
        vistaArchivio.addView(cercaEt);

        coloraCb = new CheckBox(this);
        coloraCb.setText("Colora: spia verde, candidati blu, riscontri nei colpi successivi rossi");
        coloraCb.setTextColor(cText);
        coloraCb.setTextSize(12);
        coloraCb.setChecked(true);
        coloraCb.setOnClickListener(v -> adapter.notifyDataSetChanged());
        vistaArchivio.addView(coloraCb);

        lista = new RecyclerView(this);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DrawAdapter();
        lista.setAdapter(adapter);
        lista.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        vistaArchivio.addView(lista);
    }

    private void mostra(boolean archivio) {
        vistaTecniche.setVisibility(archivio ? View.GONE : View.VISIBLE);
        vistaArchivio.setVisibility(archivio ? View.VISIBLE : View.GONE);
        tabTecniche.setBackgroundTintList(ColorStateList.valueOf(archivio ? cCard : cAccent));
        tabTecniche.setTextColor(archivio ? cText : 0xFF000000);
        tabArchivio.setBackgroundTintList(ColorStateList.valueOf(archivio ? cAccent : cCard));
        tabArchivio.setTextColor(archivio ? 0xFF000000 : cText);
    }

    private void stato(String testo, int colore) {
        statoTv.setTextColor(colore);
        statoTv.setText(testo);
    }

    // ------------------------------------------------------------------ archivio (separato da quello del Lotto)

    private static byte[] leggiTutto(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private void caricaArchivio() {
        new Thread(() -> {
            List<Draw> lette = new ArrayList<>();
            try {
                if (fileArchivio.exists()) {
                    try (FileInputStream in = new FileInputStream(fileArchivio)) {
                        lette = SuperEnalottoCore.parseCsv(new String(leggiTutto(in), StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception ignorata) {
                lette = new ArrayList<>();
            }
            final List<Draw> fl = lette;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                impostaArchivio(fl);
                if (fl.isEmpty()) {
                    mostraTesto("Archivio SuperEnalotto vuoto. Apri la scheda Archivio e importa un CSV storico oppure premi Online.\n\n"
                            + "L'archivio SuperEnalotto \u00E8 separato dall'archivio Lotto.");
                } else {
                    mostraTesto(SuperEnalottoCore.riepilogo(fl));
                }
            });
        }, "carica-superenalotto").start();
    }

    private void impostaArchivio(List<Draw> nuove) {
        draws.clear();
        draws.addAll(nuove);
        evidenza = new SuperEnalottoCore.Evidenza();
        aggiornaInfoArchivio();
        aggiornaLista();
    }

    private void aggiornaInfoArchivio() {
        if (draws.isEmpty()) {
            infoTv.setText("Archivio vuoto");
        } else {
            int mn = draws.get(0).date.getYear(), mx = draws.get(draws.size() - 1).date.getYear();
            infoTv.setText(draws.size() + " estrazioni \u00B7 " + draws.get(0).date + " \u2013 " + draws.get(draws.size() - 1).date);
            daEt.setText(String.valueOf(mn));
            aEt.setText(String.valueOf(mx));
        }
        infoTv.append("\n" + riepilogoGiocate());
    }

    private String riepilogoGiocate() {
        List<SuperEnalottoTicket> tickets = SuperEnalottoTicketsStore.load(this);
        if (tickets.isEmpty() || draws.isEmpty()) return "Sestine salvate: " + tickets.size();
        int verificate = 0, massimo = 0;
        Draw latest = draws.get(draws.size() - 1);
        for (SuperEnalottoTicket ticket : tickets) {
            if (dateKey(latest) <= ticket.fromDateKey) continue;
            verificate++;
            massimo = Math.max(massimo, corrispondenze(ticket, latest));
        }
        return "Sestine salvate: " + tickets.size() + " · ultimo concorso: "
                + verificate + " verificate, max " + massimo + "/6";
    }

    private static int dateKey(Draw draw) {
        return draw.date.getYear() * 10000 + draw.date.getMonthValue() * 100 + draw.date.getDayOfMonth();
    }

    private static int corrispondenze(SuperEnalottoTicket ticket, Draw draw) {
        int hits = 0;
        for (int n : ticket.numbers) if (draw.has(n)) hits++;
        return hits;
    }

    private void aggiornaLista() {
        if (adapter == null) return;
        String q = cercaEt == null || cercaEt.getText() == null ? "" : cercaEt.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Draw> filtrate = new ArrayList<>();
        for (Draw d : draws) {
            StringBuilder s = new StringBuilder(d.date.toString()).append(' ').append(d.contest);
            for (int n : d.numbers) s.append(' ').append(n);
            if (q.isEmpty() || s.toString().toLowerCase(Locale.ROOT).contains(q)) filtrate.add(d);
        }
        Collections.reverse(filtrate);
        adapter.items = filtrate;
        adapter.notifyDataSetChanged();
    }

    private void backup() throws IOException {
        if (!fileArchivio.exists() || fileArchivio.length() == 0) return;
        File dir = new File(getFilesDir(), "backups");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Impossibile creare la cartella dei backup.");
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.ROOT).format(new Date());
        File copia = new File(dir, "superenalotto_backup_" + stamp + ".csv");
        try (FileInputStream in = new FileInputStream(fileArchivio); FileOutputStream out = new FileOutputStream(copia)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private void scrivi(List<Draw> data) throws IOException {
        File tmp = new File(getFilesDir(), "superenalotto_archive.csv.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(SuperEnalottoCore.aCsv(data).getBytes(StandardCharsets.UTF_8));
        }
        if (!tmp.renameTo(fileArchivio)) {
            if (fileArchivio.exists() && !fileArchivio.delete()) throw new IOException("Impossibile sostituire l'archivio.");
            if (!tmp.renameTo(fileArchivio)) throw new IOException("Impossibile salvare l'archivio.");
        }
    }

    private void errore(String titolo, Throwable t) {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle(titolo)
                .setMessage(t == null || t.getMessage() == null ? "Errore sconosciuto" : t.getMessage())
                .setPositiveButton("OK", null)
                .show();
    }

    private void importa(final Uri uri) {
        stato("Importazione in corso\u2026", cTextSec);
        new Thread(() -> {
            List<Draw> lette = null;
            SuperEnalottoCore.ImportCsv audit = null;
            Throwable err = null;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new IOException("Impossibile aprire il file selezionato.");
                audit = SuperEnalottoCore.parseCsvWithAudit(new String(leggiTutto(in), StandardCharsets.UTF_8));
                lette = audit.draws;
                if (lette.isEmpty()) throw new IllegalArgumentException("Nessuna estrazione valida trovata nel CSV.");
                backup();
                scrivi(lette);
            } catch (Throwable t) {
                err = t;
            }
            final List<Draw> fl = lette;
            final SuperEnalottoCore.ImportCsv fa = audit;
            final Throwable fe = err;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (fe != null) {
                    stato("Importazione non riuscita", cLost);
                    errore("Importazione non riuscita", fe);
                    return;
                }
                impostaArchivio(fl);
                stato("Importazione completata.", cWin);
                mostraTesto("IMPORTAZIONE COMPLETATA\n\nIl CSV selezionato ha SOSTITUITO l'archivio SuperEnalotto attivo.\n"
                        + "Non \u00E8 stato accodato al vecchio archivio.\n\n" + fa.report()
                        + "\n\n" + SuperEnalottoCore.riepilogo(fl)
                        + "\n\n" + SuperEnalottoCore.diagnosticaArchivio(fl));
            });
        }, "superenalotto-import").start();
    }

    private void aggiornaOnline() {
        if (inCorso) return;
        inCorso = true;
        stato("Download archivio SuperEnalotto in corso\u2026", cTextSec);
        final List<Draw> attuali = new ArrayList<>(draws);
        new Thread(() -> {
            Throwable ultimoErrore = null;
            List<Draw> scaricate = null;
            String diagnosticaFonte = "";
            for (String indirizzo : SuperEnalottoCore.URLS) {
                HttpURLConnection c = null;
                try {
                    c = (HttpURLConnection) new URL(indirizzo).openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(30000);
                    c.setRequestProperty("User-Agent", "Distruttore/1.0");
                    int codice = c.getResponseCode();
                    if (codice < 200 || codice >= 300) throw new IOException("HTTP " + codice);
                    try (InputStream in = c.getInputStream()) {
                        SuperEnalottoCore.ImportCsv parsed = SuperEnalottoCore.parseCsvWithAudit(
                                new String(leggiTutto(in), StandardCharsets.UTF_8));
                        scaricate = parsed.draws;
                        diagnosticaFonte = parsed.report();
                    }
                    if (scaricate.isEmpty()) throw new IOException("CSV online senza estrazioni riconosciute");
                    break;
                } catch (Throwable t) {
                    ultimoErrore = t;
                    scaricate = null;
                    diagnosticaFonte = "";
                } finally {
                    if (c != null) c.disconnect();
                }
            }
            List<Draw> unite = null;
            Throwable erroreSalvataggio = null;
            if (scaricate != null) {
                try {
                    unite = SuperEnalottoCore.unisci(attuali, scaricate);
                    backup();
                    scrivi(unite);
                } catch (Throwable t) {
                    erroreSalvataggio = t;
                }
            }
            final List<Draw> fScaricate = scaricate, fUnite = unite;
            final String fDiagnosticaFonte = diagnosticaFonte;
            final Throwable fe = scaricate == null ? (ultimoErrore != null ? ultimoErrore : new Exception("Nessuna fonte valida")) : erroreSalvataggio;
            final int prima = attuali.size();
            runOnUiThread(() -> {
                inCorso = false;
                if (isFinishing() || isDestroyed()) return;
                if (fe != null) {
                    stato(fScaricate == null ? "Aggiornamento online non riuscito" : "Salvataggio non riuscito", cLost);
                    errore(fScaricate == null ? "Aggiornamento non riuscito" : "Salvataggio non riuscito", fe);
                    return;
                }
                impostaArchivio(fUnite);
                stato("Aggiornamento completato.", cWin);
                mostraTesto("AGGIORNAMENTO ONLINE COMPLETATO\nEstrazioni scaricate e validate: " + fScaricate.size()
                        + "\nNuove estrazioni: " + (fUnite.size() - prima) + "\nTotale archivio: " + fUnite.size()
                        + "\n\nControllo CSV scaricato:\n" + fDiagnosticaFonte
                        + "\n\n" + SuperEnalottoCore.riepilogo(fUnite)
                        + "\n\n" + SuperEnalottoCore.diagnosticaArchivio(fUnite));
            });
        }, "superenalotto-online").start();
    }

    private void chiediReset() {
        new AlertDialog.Builder(this)
                .setTitle("Conferma reset archivio")
                .setMessage("Vuoi svuotare l'archivio SuperEnalotto attivo? Prima verr\u00E0 salvata una copia di sicurezza. "
                        + "L'archivio Lotto non verr\u00E0 toccato.")
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Svuota", (d, w) -> {
                    try {
                        backup();
                        if (fileArchivio.exists() && !fileArchivio.delete()) throw new IOException("Impossibile eliminare l'archivio.");
                        impostaArchivio(new ArrayList<Draw>());
                        mostraTesto("Archivio SuperEnalotto azzerato. Le copie di sicurezza restano nella memoria dell'app (cartella backups).");
                        stato("Archivio azzerato.", cWin);
                    } catch (Throwable t) {
                        errore("Reset non riuscito", t);
                    }
                })
                .show();
    }

    // ------------------------------------------------------------------ tecniche

    private static int leggi(EditText e, String etichetta) {
        String t = e.getText() == null ? "" : e.getText().toString().trim();
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valore non valido per \"" + etichetta + "\".");
        }
    }

    private void mostraTesto(String testo) {
        ultimoTesto = testo;
        risultatoTv.setText(testo);
        risultatoTv.setVisibility(View.VISIBLE);
        azioniBox.setVisibility(View.VISIBLE);
    }

    private void mostraStatistiche() {
        new AlertDialog.Builder(this)
                .setTitle("Statistiche per tipo di numero")
                .setItems(new String[]{"I 6 estratti", "Jolly", "SuperStar"}, (dialog, which) -> {
                    mostraGraficoStatistiche(which);
                    stato("", cTextSec);
                })
                .setNegativeButton("Chiudi", null)
                .show();
    }

    private void mostraGraficoStatistiche(int modalita) {
        int[] frequenze = SuperEnalottoCore.frequenze(draws, modalita);
        String titolo = new String[]{"I 6 estratti", "Jolly", "SuperStar"}[modalita];
        String rapporto = modalita == 0
                ? SuperEnalottoCore.statisticheNumeri(draws)
                : SuperEnalottoCore.statisticheExtra(draws, modalita == 2);
        LinearLayout contenuto = new LinearLayout(this);
        contenuto.setOrientation(LinearLayout.VERTICAL);
        contenuto.setPadding(dp(16), dp(8), dp(16), dp(16));
        FrequencyChart chart = new FrequencyChart(frequenze);
        chart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(260)));
        contenuto.addView(chart);
        if (modalita == 0) {
            spiaEvidenza = 0;
            evidenza = new SuperEnalottoCore.Evidenza();
            for (int i = 0; i < Math.min(10, chart.top.size()); i++) {
                evidenza.candidatiBlu.add(chart.top.get(i));
            }
            aggiornaLista();
        }
        TextView testo = new TextView(this);
        testo.setText(rapporto);
        testo.setTextColor(cText);
        testo.setTypeface(Typeface.MONOSPACE);
        testo.setTextSize(12);
        testo.setPadding(0, dp(8), 0, 0);
        contenuto.addView(testo);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(contenuto);
        new AlertDialog.Builder(this)
                .setTitle("Frequenze · " + titolo)
                .setView(scroll)
                .setPositiveButton("Chiudi", null)
                .show();
    }

    private final class FrequencyChart extends View {
        private final int[] frequenze;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<Integer> top = new ArrayList<>();

        FrequencyChart(int[] frequenze) {
            super(SuperEnalottoActivity.this);
            this.frequenze = frequenze;
            for (int n = 1; n <= 90; n++) top.add(n);
            top.sort((a, b) -> frequenze[b] != frequenze[a]
                    ? Integer.compare(frequenze[b], frequenze[a]) : Integer.compare(a, b));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int max = 0;
            for (int i = 0; i < Math.min(10, top.size()); i++) max = Math.max(max, frequenze[top.get(i)]);
            float row = dp(24), left = dp(42), right = getWidth() - dp(42);
            paint.setTextSize(dp(12));
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setColor(cText);
            for (int i = 0; i < Math.min(10, top.size()); i++) {
                int n = top.get(i), value = frequenze[n];
                float y = dp(18) + i * row;
                canvas.drawText(String.format(Locale.ROOT, "%02d", n), 0, y + dp(11), paint);
                float width = max == 0 ? 0 : (right - left) * value / max;
                paint.setColor(cAccent);
                canvas.drawRoundRect(left, y, left + width, y + dp(16), dp(6), dp(6), paint);
                paint.setColor(cText);
                canvas.drawText(String.valueOf(value), Math.min(left + width + dp(6), right), y + dp(12), paint);
            }
        }
    }

    private void mostraGiocate(List<Integer> precompilati) {
        List<SuperEnalottoTicket> tickets = SuperEnalottoTicketsStore.load(this);
        List<String> righe = new ArrayList<>();
        for (SuperEnalottoTicket ticket : tickets) {
            StringBuilder item = new StringBuilder(numeriFormattati(ticket.numbers));
            int max = 0, ultime = -1;
            for (Draw draw : draws) {
                if (dateKey(draw) <= ticket.fromDateKey) continue;
                int hits = corrispondenze(ticket, draw);
                max = Math.max(max, hits);
                ultime = hits;
            }
            item.append(" · max ").append(max).append("/6");
            if (ultime >= 0) item.append(" · ultima ").append(ultime).append("/6");
            else item.append(" · in attesa di estrazioni successive");
            righe.add(item.toString());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Le mie sestine · verifica automatica");
        if (righe.isEmpty()) builder.setMessage("Nessuna sestina salvata.");
        else builder.setItems(righe.toArray(new String[0]), (dialog, which) -> confermaEliminazione(ticketId(tickets, which)));
        builder.setPositiveButton("Aggiungi sestina", (dialog, which) -> chiediNuovaGiocata(precompilati));
        builder.setNegativeButton("Chiudi", null);
        builder.show();
    }

    private long ticketId(List<SuperEnalottoTicket> tickets, int index) {
        return tickets.get(index).id;
    }

    private void confermaEliminazione(long id) {
        new AlertDialog.Builder(this)
                .setTitle("Sestina salvata")
                .setMessage("Vuoi eliminare questa sestina?")
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Elimina", (dialog, which) -> {
                    List<SuperEnalottoTicket> tickets = SuperEnalottoTicketsStore.load(this);
                    tickets.removeIf(ticket -> ticket.id == id);
                    try {
                        SuperEnalottoTicketsStore.save(this, tickets);
                        aggiornaInfoArchivio();
                        mostraGiocate(null);
                    } catch (IllegalStateException ex) {
                        errore("Salvataggio sestine non riuscito", ex);
                    }
                })
                .show();
    }

    private void chiediNuovaGiocata(List<Integer> precompilati) {
        if (draws.isEmpty()) {
            stato("Carica prima l'archivio per associare la sestina alle estrazioni successive.", cLost);
            return;
        }
        EditText numeri = new EditText(this);
        numeri.setSingleLine(true);
        numeri.setInputType(InputType.TYPE_CLASS_TEXT);
        numeri.setHint("Es. 3 14 27 48 62 89");
        numeri.setTextColor(cText);
        numeri.setHintTextColor(cTextSec);
        numeri.setBackground(riquadro(cCard, cStroke));
        numeri.setPadding(dp(12), dp(10), dp(12), dp(10));
        if (precompilati != null) numeri.setText(numeriFormattati(precompilati));
        new AlertDialog.Builder(this)
                .setTitle("Salva sestina")
                .setMessage("Inserisci sei numeri distinti tra 1 e 90. La verifica parte dal prossimo concorso in archivio.")
                .setView(numeri)
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Salva", (dialog, which) -> salvaTicket(numeri.getText().toString()))
                .show();
    }

    private void salvaTicket(String input) {
        List<Integer> numbers = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        try {
            for (String token : input.trim().split("[\\s,;]+")) {
                if (token.isEmpty()) continue;
                int n = Integer.parseInt(token);
                if (n < 1 || n > 90 || !seen.add(n)) throw new IllegalArgumentException();
                numbers.add(n);
            }
            if (numbers.size() != 6) throw new IllegalArgumentException();
        } catch (RuntimeException ex) {
            errore("Sestina non valida", new IllegalArgumentException("Inserisci esattamente sei numeri distinti da 1 a 90."));
            return;
        }
        Collections.sort(numbers);
        int from = dateKey(draws.get(draws.size() - 1));
        List<SuperEnalottoTicket> tickets = SuperEnalottoTicketsStore.load(this);
        tickets.add(0, new SuperEnalottoTicket(System.currentTimeMillis(), numbers, from));
        try {
            SuperEnalottoTicketsStore.save(this, tickets);
            aggiornaInfoArchivio();
            mostraGiocate(null);
        } catch (IllegalStateException ex) {
            errore("Salvataggio sestina non riuscito", ex);
        }
    }

    private void mostraGeneratore() {
        LinearLayout contenuto = new LinearLayout(this);
        contenuto.setOrientation(LinearLayout.VERTICAL);
        contenuto.setPadding(dp(20), dp(8), dp(20), dp(4));
        contenuto.setBackgroundColor(cBg);
        EditText pariMin = campoNum(2, 72), pariMax = campoNum(4, 72);
        EditText sommaMin = campoNum(120, 84), sommaMax = campoNum(330, 84);
        LinearLayout rigaPari = new LinearLayout(this);
        rigaPari.addView(gruppoEtichetta("Pari min.", pariMin));
        rigaPari.addView(gruppoEtichetta("Pari max.", pariMax));
        LinearLayout rigaSomma = new LinearLayout(this);
        rigaSomma.addView(gruppoEtichetta("Somma min.", sommaMin));
        rigaSomma.addView(gruppoEtichetta("Somma max.", sommaMax));
        contenuto.addView(rigaPari);
        contenuto.addView(rigaSomma);
        TextView nota = etichetta("I filtri organizzano la sestina, ma non ne aumentano le probabilità.");
        contenuto.addView(nota);
        TextView output = new TextView(this);
        output.setTextColor(cText);
        output.setTextSize(16);
        output.setPadding(0, dp(12), 0, dp(8));
        contenuto.addView(output);
        final List<List<Integer>> generata = new ArrayList<>();
        generata.add(new ArrayList<Integer>());
        Runnable genera = () -> {
            try {
                int minEven = leggi(pariMin, "Pari min.");
                int maxEven = leggi(pariMax, "Pari max.");
                int minSum = leggi(sommaMin, "Somma min.");
                int maxSum = leggi(sommaMax, "Somma max.");
                generata.set(0, generaSestina(minEven, maxEven, minSum, maxSum));
                output.setText(numeriFormattati(generata.get(0)) + " · somma "
                        + generata.get(0).stream().mapToInt(Integer::intValue).sum());
            } catch (IllegalArgumentException ex) {
                output.setText(ex.getMessage());
                generata.set(0, new ArrayList<>());
            }
        };
        MaterialButton rigenera = bottone("Genera un'altra sestina", false);
        rigenera.setLayoutParams(pieno(6));
        rigenera.setOnClickListener(v -> genera.run());
        contenuto.addView(rigenera);
        genera.run();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Generatore di sestine")
                .setView(contenuto)
                .setPositiveButton("Salva sestina", null)
                .setNegativeButton("Chiudi", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (generata.get(0).size() != 6) {
                stato("Impossibile salvare: genera prima una sestina valida.", cLost);
                return;
            }
            dialog.dismiss();
            chiediNuovaGiocata(generata.get(0));
        }));
        dialog.show();
    }

    private LinearLayout gruppoEtichetta(String label, EditText field) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.addView(etichetta(label));
        group.addView(field);
        return group;
    }

    private List<Integer> generaSestina(int minEven, int maxEven, int minSum, int maxSum) {
        if (minEven < 0 || maxEven > 6 || minEven > maxEven) {
            throw new IllegalArgumentException("Il numero di pari deve essere compreso tra 0 e 6.");
        }
        if (minSum < 21 || maxSum > 534 || minSum > maxSum) {
            throw new IllegalArgumentException("La somma deve essere compresa tra 21 e 534.");
        }
        Random random = new Random();
        for (int attempt = 0; attempt < 20000; attempt++) {
            Set<Integer> chosen = new HashSet<>();
            while (chosen.size() < 6) chosen.add(random.nextInt(90) + 1);
            List<Integer> numbers = new ArrayList<>(chosen);
            Collections.sort(numbers);
            int even = 0, sum = 0;
            for (int n : numbers) {
                if (n % 2 == 0) even++;
                sum += n;
            }
            if (even >= minEven && even <= maxEven && sum >= minSum && sum <= maxSum) return numbers;
        }
        throw new IllegalArgumentException("Nessuna sestina trovata con questi vincoli. Allarga gli intervalli.");
    }

    private static String numeriFormattati(List<Integer> numbers) {
        List<String> formatted = new ArrayList<>();
        for (int n : numbers) formatted.add(String.format(Locale.ROOT, "%02d", n));
        return android.text.TextUtils.join(" - ", formatted);
    }

    private void eseguiConfrontoScanner() {
        if (inCorso || draws.isEmpty()) {
            if (draws.isEmpty()) stato("Archivio vuoto: importa un CSV o aggiorna online dalla scheda Archivio.", cLost);
            return;
        }
        final int da, a, colpi;
        try {
            da = leggi(daEt, "Anno da");
            a = leggi(aEt, "Anno a");
            colpi = leggi(colpiEt, "Colpi");
            if (colpi < 1 || colpi > 50) throw new IllegalArgumentException("I colpi devono essere tra 1 e 50.");
            if (da > a) throw new IllegalArgumentException("Intervallo anni non valido.");
        } catch (IllegalArgumentException ex) {
            stato(ex.getMessage(), cLost);
            return;
        }
        inCorso = true;
        for (MaterialButton b : bottoniTecniche) b.setEnabled(false);
        stato("Confronto dello scanner sul test fuori campione in corso\u2026", cTextSec);
        final List<Draw> snapshot = new ArrayList<>(draws);
        new Thread(() -> {
            SuperEnalottoCore.ConfrontoScanner confronto = null;
            List<Draw> periodo = null;
            String err = null;
            try {
                periodo = SuperEnalottoCore.filtra(snapshot, da, a);
                confronto = SuperEnalottoCore.confrontaScanner(periodo, colpi);
            } catch (IllegalArgumentException ex) {
                err = ex.getMessage();
            } catch (Throwable t) {
                err = "Errore nel confronto: " + t;
            }
            final SuperEnalottoCore.ConfrontoScanner fc = confronto;
            final List<Draw> fp = periodo;
            final String fe = err;
            runOnUiThread(() -> {
                inCorso = false;
                if (isFinishing() || isDestroyed()) return;
                for (MaterialButton b : bottoniTecniche) b.setEnabled(true);
                if (fc == null) {
                    stato("Confronto non completato: " + (fe == null ? "nessun risultato" : fe), cLost);
                    return;
                }
                SuperEnalottoCore.RigaScanner best = fc.addestramento.migliore();
                if (best != null) {
                    List<Draw> train = new ArrayList<>(fp.subList(0, fc.inizioTest));
                    evidenza = SuperEnalottoCore.evidenzaScanner(train, best.spia, colpi, best.candidati);
                    spiaEvidenza = best.spia;
                }
                mostraTesto(fc.report);
                stato("Test fuori campione completato. I candidati sono stati scelti solo nel periodo di addestramento.", cWin);
                aggiornaLista();
            });
        }, "superenalotto-scanner-holdout").start();
    }

    private void esegui(final String modo) {
        if (inCorso) return;
        if ("scanner".equals(modo)) {
            eseguiScanner();
            return;
        }
        if (draws.isEmpty()) {
            stato("Archivio vuoto: importa un CSV o aggiorna online dalla scheda Archivio.", cLost);
            return;
        }
        final int spia, da, a, colpi;
        try {
            spia = leggi(spiaEt, "Spia");
            da = leggi(daEt, "Anno da");
            a = leggi(aEt, "Anno a");
            colpi = leggi(colpiEt, "Colpi");
            if (spia < 1 || spia > 90) throw new IllegalArgumentException("La spia deve essere tra 1 e 90.");
            if (colpi < 1 || colpi > 50) throw new IllegalArgumentException("I colpi devono essere tra 1 e 50.");
            if (da > a) throw new IllegalArgumentException("Intervallo anni non valido.");
        } catch (IllegalArgumentException ex) {
            stato(ex.getMessage(), cLost);
            return;
        }
        inCorso = true;
        for (MaterialButton b : bottoniTecniche) b.setEnabled(false);
        stato("Calcolo in corso\u2026 (i backtest possono richiedere qualche secondo)", cTextSec);
        final List<Draw> copia = new ArrayList<>(draws);
        new Thread(() -> {
            String rapporto = null, err = null;
            SuperEnalottoCore.Evidenza ev = null;
            try {
                List<Draw> periodo = SuperEnalottoCore.filtra(copia, da, a);
                rapporto = SuperEnalottoCore.rapporto(modo, periodo, spia, colpi);
                ev = SuperEnalottoCore.evidenza(periodo, spia, colpi, modo);
            } catch (IllegalArgumentException ex) {
                err = ex.getMessage();
            } catch (Throwable t) {
                err = "Errore nel calcolo: " + t;
            }
            final String fr = rapporto, fe = err;
            final SuperEnalottoCore.Evidenza fev = ev;
            runOnUiThread(() -> {
                inCorso = false;
                if (isFinishing() || isDestroyed()) return;
                for (MaterialButton b : bottoniTecniche) b.setEnabled(true);
                if (fe != null || fr == null) {
                    stato("Analisi non completata: " + (fe != null ? fe : "nessun risultato"), cLost);
                    return;
                }
                evidenza = fev;
                spiaEvidenza = spia;
                mostraTesto(fr);
                stato("Fatto. Nell'archivio: spia verde, candidati blu e riscontri rossi.", cWin);
                aggiornaLista();
            });
        }, "superenalotto-tecnica").start();
    }

    private void eseguiScanner() {
        if (inCorso) return;
        if (draws.isEmpty()) {
            stato("Archivio vuoto: importa un CSV o aggiorna online dalla scheda Archivio.", cLost);
            return;
        }
        final int da, a, colpi;
        try {
            da = leggi(daEt, "Anno da");
            a = leggi(aEt, "Anno a");
            colpi = leggi(colpiEt, "Colpi");
            if (colpi < 1 || colpi > 50) throw new IllegalArgumentException("I colpi devono essere tra 1 e 50.");
            if (da > a) throw new IllegalArgumentException("Intervallo anni non valido.");
        } catch (IllegalArgumentException ex) {
            stato(ex.getMessage(), cLost);
            return;
        }

        inCorso = true;
        for (MaterialButton b : bottoniTecniche) b.setEnabled(false);
        stato("Scansione delle 90 spie in corso\u2026", cTextSec);
        final List<Draw> copia = new ArrayList<>(draws);
        new Thread(() -> {
            String rapporto = null, err = null;
            SuperEnalottoCore.RisultatoScanner risultato = null;
            List<Draw> periodo = null;
            try {
                periodo = SuperEnalottoCore.filtra(copia, da, a);
                risultato = SuperEnalottoCore.scansiona(periodo, colpi);
                rapporto = risultato.report(colpi);
            } catch (IllegalArgumentException ex) {
                err = ex.getMessage();
            } catch (Throwable t) {
                err = "Errore nello scanner: " + t;
            }
            final String fr = rapporto, fe = err;
            final SuperEnalottoCore.RisultatoScanner fres = risultato;
            final List<Draw> fperiodo = periodo;
            runOnUiThread(() -> {
                inCorso = false;
                if (isFinishing() || isDestroyed()) return;
                for (MaterialButton b : bottoniTecniche) b.setEnabled(true);
                if (fe != null || fr == null || fres == null) {
                    stato("Scansione non completata: " + (fe != null ? fe : "nessun risultato"), cLost);
                    return;
                }
                SuperEnalottoCore.RigaScanner migliore = fres.migliore();
                if (migliore == null) {
                    evidenza = new SuperEnalottoCore.Evidenza();
                    spiaEvidenza = 0;
                    stato("Nessuna spia ha abbastanza cicli completi nel periodo.", cLost);
                } else {
                    evidenza = SuperEnalottoCore.evidenzaScanner(
                            fperiodo, migliore.spia, colpi, migliore.candidati);
                    spiaEvidenza = migliore.spia;
                    stato("Scanner completato: spia in testa " + String.format(Locale.ROOT, "%02d", migliore.spia)
                            + ". In archivio: spia verde, candidati blu e riscontri rossi.", cWin);
                }
                mostraTesto(fr);
                aggiornaLista();
            });
        }, "superenalotto-scanner").start();
    }

    private void copiaRapporto() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Distruttore SuperEnalotto", ultimoTesto));
            Toast.makeText(this, "Rapporto copiato.", Toast.LENGTH_SHORT).show();
        }
    }

    private void condividiRapporto() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, ultimoTesto);
        startActivity(Intent.createChooser(send, "Condividi il rapporto"));
    }

    // ------------------------------------------------------------------ elenco dell'archivio

    private final class DrawAdapter extends RecyclerView.Adapter<DrawAdapter.VH> {
        List<Draw> items = new ArrayList<>();
        private final DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        final class VH extends RecyclerView.ViewHolder {
            final TextView data, extra;
            final TextView[] palle;

            VH(LinearLayout card, TextView data, TextView[] palle, TextView extra) {
                super(card);
                this.data = data;
                this.palle = palle;
                this.extra = extra;
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setBackground(riquadro(cCard, cStroke));
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(4);
            lp.bottomMargin = dp(4);
            card.setLayoutParams(lp);

            TextView data = new TextView(parent.getContext());
            data.setTextColor(cAccent);
            data.setTextSize(13);
            data.setTypeface(Typeface.DEFAULT_BOLD);
            card.addView(data);

            LinearLayout riga = new LinearLayout(parent.getContext());
            riga.setOrientation(LinearLayout.HORIZONTAL);
            riga.setPadding(0, dp(6), 0, dp(2));
            TextView[] palle = new TextView[6];
            for (int i = 0; i < 6; i++) {
                TextView p = new TextView(parent.getContext());
                p.setGravity(Gravity.CENTER);
                p.setTextSize(14);
                p.setTypeface(Typeface.DEFAULT_BOLD);
                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(40), dp(40));
                plp.rightMargin = dp(6);
                p.setLayoutParams(plp);
                palle[i] = p;
                riga.addView(p);
            }
            card.addView(riga);

            TextView extra = new TextView(parent.getContext());
            extra.setTextColor(cTextSec);
            extra.setTextSize(12);
            card.addView(extra);
            return new VH(card, data, palle, extra);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            Draw d = items.get(position);
            String etichetta = d.date.format(formato);
            if (!d.contest.trim().isEmpty()) etichetta += "  \u00B7  CONCORSO " + d.contest;
            h.data.setText(etichetta);
            boolean colora = coloraCb == null || coloraCb.isChecked();
            String chiave = d.key();
            for (int i = 0; i < 6; i++) {
                int n = d.numbers[i];
                TextView p = h.palle[i];
                p.setText(String.format(Locale.ROOT, "%02d", n));
                if (colora && spiaEvidenza > 0 && n == spiaEvidenza && evidenza.chiaviSpia.contains(chiave)) {
                    p.setBackgroundResource(R.drawable.bg_lotto_ball_spy);
                    p.setTextColor(0xFFFFFFFF);
                } else if (colora && evidenza.chiaviRisultato.contains(chiave) && (evidenza.candidati.contains(n) || evidenza.candidatiBlu.contains(n))) {
                    p.setBackgroundResource(R.drawable.bg_lotto_ball_hit);
                    p.setTextColor(0xFFFFFFFF);
                } else if (colora && (evidenza.candidatiBlu.contains(n) || evidenza.candidati.contains(n))) {
                    p.setBackgroundResource(R.drawable.bg_lotto_ball_blue);
                    p.setTextColor(0xFFFFFFFF);
                } else {
                    p.setBackgroundResource(R.drawable.bg_lotto_ball);
                    p.setTextColor(0xFF0F172A);
                }
            }
            boolean haJolly = !d.jolly.trim().isEmpty(), haStar = !d.superstar.trim().isEmpty();
            if (haJolly || haStar) {
                h.extra.setVisibility(View.VISIBLE);
                h.extra.setText("Jolly: " + (haJolly ? d.jolly : "\u2014") + "   \u00B7   SuperStar: " + (haStar ? d.superstar : "\u2014"));
            } else {
                h.extra.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
