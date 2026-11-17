package com.example.lottoinjava.analysis;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SuperEnalottoScannerTest {

    @Test
    public void scannerValutaLeSpieConCicliCompletiEPreparaLeEvidenze() {
        List<SuperEnalottoCore.Draw> draws = new ArrayList<>();
        for (int i = 0; i < 180; i++) {
            int[] numbers = new int[6];
            int start = 2 + (i * 7) % 80;
            for (int j = 0; j < numbers.length; j++) {
                numbers[j] = start + j;
            }
            if (i % 4 == 0) numbers[0] = 1;
            if (i > 0 && (i - 1) % 4 == 0) numbers[1] = 90;
            draws.add(new SuperEnalottoCore.Draw(
                    LocalDate.of(2020, 1, 1).plusDays(i),
                    String.valueOf(i + 1), numbers, "", ""));
        }

        SuperEnalottoCore.RisultatoScanner result = SuperEnalottoCore.scansiona(draws, 1);
        SuperEnalottoCore.RigaScanner spiaUno = null;
        for (SuperEnalottoCore.RigaScanner riga : result.righe) {
            if (riga.spia == 1) spiaUno = riga;
        }
        assertNotNull(spiaUno);
        assertTrue(spiaUno.prove > 0);
        assertTrue(spiaUno.candidati.contains(90));
        assertEquals(90, result.spieValutate + result.spieSaltate);

        SuperEnalottoCore.Evidenza evidenza = SuperEnalottoCore.evidenzaScanner(
                draws, spiaUno.spia, 1, spiaUno.candidati);
        assertTrue(evidenza.chiaviSpia.contains(draws.get(0).key()));
        assertTrue(evidenza.chiaviRisultato.contains(draws.get(1).key()));
        assertFalse(evidenza.candidati.isEmpty());
    }

    @Test
    public void scannerIgnoraSpieSenzaCicliStoriciSufficienti() {
        List<SuperEnalottoCore.Draw> draws = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            draws.add(new SuperEnalottoCore.Draw(
                    LocalDate.of(2020, 1, 1).plusDays(i),
                    String.valueOf(i + 1), new int[]{1, 2, 3, 4, 5, 6}, "", ""));
        }

        SuperEnalottoCore.RisultatoScanner result = SuperEnalottoCore.scansiona(draws, 1);

        assertTrue(result.righe.isEmpty());
        assertEquals(90, result.spieSaltate);
    }

    @Test
    public void scannerConfrontaIPrimiRisultatiConUnPeriodoMaiUsatoPerLaScelta() {
        List<SuperEnalottoCore.Draw> draws = new ArrayList<>();
        for (int i = 0; i < 240; i++) {
            int[] numbers = new int[6];
            int start = 2 + (i * 7) % 80;
            for (int j = 0; j < numbers.length; j++) numbers[j] = start + j;
            if (i % 4 == 0) numbers[0] = 1;
            draws.add(new SuperEnalottoCore.Draw(
                    LocalDate.of(2020, 1, 1).plusDays(i), String.valueOf(i + 1), numbers, "", ""));
        }

        SuperEnalottoCore.ConfrontoScanner confronto = SuperEnalottoCore.confrontaScanner(draws, 1);

        assertTrue(confronto.report.contains("TEST FUORI CAMPIONE"));
        assertTrue(confronto.report.contains("Test mai usato per scegliere le spie"));
        assertTrue(confronto.inizioTest > 0 && confronto.inizioTest < draws.size());
    }

    @Test
    public void importCsvContaRigheDuplicateEDiagnosticaIntervalli() throws Exception {
        String csv = "DATA;CONCORSO;N1;N2;N3;N4;N5;N6;JOLLY;SUPERSTAR\n"
                + "01/01/2020;1;1;2;3;4;5;6;7;8\n"
                + "01/01/2020;1;2;3;4;5;6;7;8;9\n"
                + "20/01/2020;2;3;4;5;6;7;8;9;10\n";

        SuperEnalottoCore.ImportCsv imported = SuperEnalottoCore.parseCsvWithAudit(csv);

        assertEquals(3, imported.righeValide);
        assertEquals(1, imported.righeDuplicate);
        assertEquals(2, imported.draws.size());
        assertTrue(SuperEnalottoCore.diagnosticaArchivio(imported.draws).contains("Intervalli oltre 10 giorni: 1"));
        assertEquals(1, SuperEnalottoCore.frequenze(imported.draws, 1)[8]);
        assertEquals(1, SuperEnalottoCore.frequenze(imported.draws, 2)[9]);
    }
}
