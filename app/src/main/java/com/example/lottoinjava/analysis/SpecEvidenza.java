package com.example.lottoinjava.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Descrizione di cosa colorare nell'archivio dopo una tecnica: spia (verde) e numeri da giocare (rosso).
 * Nessuna dipendenza da Android o JavaFX.
 */
public final class SpecEvidenza {
    public final String titolo;
    public final List<Integer> spie;          // vuota se la tecnica non ha una spia
    public final List<String> ruoteSpia;      // ruote in cui si cerca la spia
    public final List<Integer> numeri;        // numeri da colorare in rosso
    public final List<String> ruoteEsito;     // ruote dell'esito (la prima è quella principale)
    public final int colpi, anno0, anno1;
    public int posizioneSpia = 0;             // 0 = qualsiasi posizione, 1-5 = solo quella
    public boolean spieTutteInsieme = false;  // con più spie: tutte nella stessa estrazione

    public SpecEvidenza(String titolo, List<Integer> spie, List<String> ruoteSpia, List<Integer> numeri,
                        List<String> ruoteEsito, int colpi, int anno0, int anno1) {
        this.titolo = titolo;
        this.spie = spie != null ? new ArrayList<>(spie) : new ArrayList<Integer>();
        this.ruoteSpia = ruoteSpia != null ? new ArrayList<>(ruoteSpia) : new ArrayList<String>();
        this.numeri = numeri != null ? new ArrayList<>(numeri) : new ArrayList<Integer>();
        this.ruoteEsito = ruoteEsito != null ? new ArrayList<>(ruoteEsito) : new ArrayList<String>();
        this.colpi = colpi;
        this.anno0 = anno0;
        this.anno1 = anno1;
    }

    public SpecEvidenza withPosizioneSpia(int posizione) {
        this.posizioneSpia = Math.max(0, Math.min(5, posizione));
        return this;
    }

    public SpecEvidenza withSpieTutteInsieme(boolean tutte) {
        this.spieTutteInsieme = tutte;
        return this;
    }

    /** Ruota principale dell'evidenziazione: quella dell'esito se è una sola, altrimenti quella della spia, altrimenti "TUTTE". */
    public String ruotaPrincipale() {
        if (ruoteEsito.size() == 1) return ruoteEsito.get(0);
        if (ruoteEsito.isEmpty() && ruoteSpia.size() == 1) return ruoteSpia.get(0);
        return "TUTTE";
    }
}
