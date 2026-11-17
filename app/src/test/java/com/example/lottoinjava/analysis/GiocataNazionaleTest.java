package com.example.lottoinjava.analysis;

import com.example.lottoinjava.model.Extraction;
import com.example.lottoinjava.model.Giocata;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GiocataNazionaleTest {

    @Test
    public void laNazionaleESelezionabileSingolarmenteMaEsclusaDaTutte() {
        Map<String, List<Integer>> ruote = new LinkedHashMap<>();
        ruote.put("Nazionale", Arrays.asList(12, 24, 36, 48, 60));
        Extraction estrazione = new Extraction(20250302, "02/03/2025", 2025, ruote);
        List<Extraction> archivio = Collections.singletonList(estrazione);

        Giocata nazionale = new Giocata(1, "Nazionale", Collections.singletonList(12), 1, 1, 20250301, false);
        Giocata tutte = new Giocata(2, Giocata.TUTTE, Collections.singletonList(12), 1, 1, 20250301, false);

        assertEquals(GiocataChecker.Status.VINTA, GiocataChecker.check(nazionale, archivio).status);
        assertEquals(GiocataChecker.Status.SCADUTA, GiocataChecker.check(tutte, archivio).status);
    }

    @Test
    public void getNumbersForRuotaRiconosceNazionaleEAlias() {
        Map<String, List<Integer>> ruote = new LinkedHashMap<>();
        ruote.put("nazionale", Arrays.asList(1, 2, 3, 4, 5));
        Extraction e = new Extraction(1, "01/01/2025", 2025, ruote);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), e.getNumbersForRuota("Nazionale"));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), e.getNumbersForRuota("nazionale"));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), e.getNumbersForRuota("NZ"));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), e.getNumbersForRuota("RN"));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), e.getNumbersForRuota("NAZ"));
    }
}
