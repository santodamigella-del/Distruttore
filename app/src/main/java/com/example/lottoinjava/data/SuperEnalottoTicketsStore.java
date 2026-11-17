package com.example.lottoinjava.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.lottoinjava.model.SuperEnalottoTicket;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/** Storage locale delle sestine SuperEnalotto, indipendente dalle giocate del Lotto. */
public final class SuperEnalottoTicketsStore {
    private static final String TAG = "SuperEnalottoTickets";
    private static final String PREFS = "superenalotto_tickets";
    private static final String KEY = "list";

    private SuperEnalottoTicketsStore() { }

    public static synchronized List<SuperEnalottoTicket> load(Context context) {
        List<SuperEnalottoTicket> result = new ArrayList<>();
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
        if (raw == null) return result;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                result.add(SuperEnalottoTicket.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Impossibile leggere le sestine salvate", e);
        }
        return result;
    }

    public static synchronized void save(Context context, List<SuperEnalottoTicket> tickets) {
        try {
            JSONArray array = new JSONArray();
            for (SuperEnalottoTicket ticket : tickets) array.put(ticket.toJson());
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (!prefs.edit().putString(KEY, array.toString()).commit()) {
                throw new IllegalStateException("Impossibile salvare le sestine SuperEnalotto.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Impossibile salvare le sestine SuperEnalotto", e);
            throw new IllegalStateException("Impossibile salvare le sestine SuperEnalotto.", e);
        }
    }
}
