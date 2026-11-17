package com.example.lottoinjava.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Sestina salvata, verificata sulle estrazioni successive alla data di creazione. */
public final class SuperEnalottoTicket {
    public final long id;
    public final List<Integer> numbers;
    public final int fromDateKey;

    public SuperEnalottoTicket(long id, List<Integer> numbers, int fromDateKey) {
        this.id = id;
        this.numbers = new ArrayList<>(numbers);
        this.fromDateKey = fromDateKey;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("from", fromDateKey);
        JSONArray values = new JSONArray();
        for (int n : numbers) values.put(n);
        o.put("numbers", values);
        return o;
    }

    public static SuperEnalottoTicket fromJson(JSONObject o) throws JSONException {
        JSONArray values = o.getJSONArray("numbers");
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < values.length(); i++) numbers.add(values.getInt(i));
        return new SuperEnalottoTicket(o.getLong("id"), numbers, o.optInt("from", 0));
    }
}
