package com.example.lottoinjava.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Extraction {

    public static final List<String> ALL_RUOTE = Arrays.asList(
            "Bari", "Cagliari", "Firenze", "Genova", "Milano",
            "Napoli", "Palermo", "Roma", "Torino", "Venezia", "Nazionale"
    );

    private final int concorsoNumber;
    private final String date; // e.g. "08/03/2025"
    private final int year;
    private final Map<String, List<Integer>> wheels; // Ruota Name -> List of 5 extracted numbers

    public Extraction(int concorsoNumber, String date, int year, Map<String, List<Integer>> wheels) {
        this.concorsoNumber = concorsoNumber;
        this.date = date;
        this.year = year;
        this.wheels = wheels;
    }

    public int getConcorsoNumber() {
        return concorsoNumber;
    }

    public String getDate() {
        return date;
    }

    /** Anno reale: ricavato dalla data dell'estrazione; se non è ricavabile usa il valore salvato. */
    public int getYear() {
        int key = getDateKey();
        return key >= 18000000 ? key / 10000 : year;
    }

    public static String normalizeRuotaName(String text) {
        if (text == null) return "";
        String s = text.trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "ba": case "bari": return "Bari";
            case "ca": case "cagliari": return "Cagliari";
            case "fi": case "firenze": return "Firenze";
            case "ge": case "genova": return "Genova";
            case "mi": case "milano": return "Milano";
            case "na": case "napoli": return "Napoli";
            case "pa": case "palermo": return "Palermo";
            case "rm": case "ro": case "roma": return "Roma";
            case "to": case "torino": return "Torino";
            case "ve": case "venezia": return "Venezia";
            case "nz": case "rn": case "rnz": case "naz": case "nazionale": return "Nazionale";
            default:
                for (String ruota : ALL_RUOTE) {
                    if (ruota.equalsIgnoreCase(s)) return ruota;
                }
                if (text.length() > 0) {
                    return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1).toLowerCase(Locale.ROOT);
                }
                return text;
        }
    }

    public Map<String, List<Integer>> getWheels() {
        return wheels;
    }

    public List<Integer> getNumbersForRuota(String ruotaName) {
        if (wheels == null || ruotaName == null) return new ArrayList<>();
        if (wheels.containsKey(ruotaName)) {
            return wheels.get(ruotaName);
        }
        String normalizedTarget = normalizeRuotaName(ruotaName);
        for (Map.Entry<String, List<Integer>> entry : wheels.entrySet()) {
            if (entry.getKey() != null && normalizeRuotaName(entry.getKey()).equalsIgnoreCase(normalizedTarget)) {
                return entry.getValue();
            }
        }
        return new ArrayList<>();
    }

    /** Data reale dell'estrazione: prima da "date" (anche "Sabato 08/03/2025"), altrimenti dal numero concorso aaaammgg. Null se non ricavabile. */
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
    private transient int dateKeyCache = -1;

    private Date parseDate() {
        if (date != null) {
            Matcher m = DATE_PATTERN.matcher(date);
            if (m.find()) {
                try {
                    SimpleDateFormat f = new SimpleDateFormat("d/M/yyyy", Locale.ITALY);
                    f.setLenient(false);
                    return f.parse(m.group(1) + "/" + m.group(2) + "/" + m.group(3));
                } catch (Exception ignored) { }
            }
        }
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd", Locale.ITALY);
            f.setLenient(false);
            return f.parse(String.valueOf(concorsoNumber));
        } catch (Exception ignored) { }
        return null;
    }

    /** Chiave di ordinamento cronologico aaaammgg (se la data non è ricavabile usa il numero concorso). */
    public int getDateKey() {
        if (dateKeyCache >= 0) return dateKeyCache;
        Date d = parseDate();
        if (d == null) {
            dateKeyCache = concorsoNumber;
        } else {
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            dateKeyCache = c.get(Calendar.YEAR) * 10000
                    + (c.get(Calendar.MONTH) + 1) * 100
                    + c.get(Calendar.DAY_OF_MONTH);
        }
        return dateKeyCache;
    }

    /** Es. "Giovedì 15 ottobre 2026" */
    public String getLongDate() {
        Date d = parseDate();
        if (d == null) return date != null && !date.isEmpty() ? date : String.valueOf(concorsoNumber);
        String out = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALY).format(d);
        return Character.toUpperCase(out.charAt(0)) + out.substring(1);
    }

    /** Es. "Gio 15/10/2026" */
    public String getShortDate() {
        Date d = parseDate();
        if (d == null) return date != null && !date.isEmpty() ? date : String.valueOf(concorsoNumber);
        String out = new SimpleDateFormat("EEE dd/MM/yyyy", Locale.ITALY).format(d).replace(".", "");
        return Character.toUpperCase(out.charAt(0)) + out.substring(1);
    }

    public boolean matchesSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String q = query.trim().toLowerCase();

        // Search by Concorso number
        if (String.valueOf(concorsoNumber).contains(q) || ("concorso " + concorsoNumber).toLowerCase().contains(q) || ("n " + concorsoNumber).toLowerCase().contains(q)) {
            return true;
        }

        // Search by Date
        if (date != null && date.toLowerCase().contains(q)) {
            return true;
        }

        // Search by formatted date (es. "15/10/2026", "ottobre")
        if (getShortDate().toLowerCase(Locale.ITALY).contains(q) || getLongDate().toLowerCase(Locale.ITALY).contains(q)) {
            return true;
        }

        // Search by Ruota name or numbers
        if (wheels != null) {
            for (Map.Entry<String, List<Integer>> entry : wheels.entrySet()) {
                String ruota = entry.getKey().toLowerCase();
                if (ruota.contains(q)) {
                    return true;
                }
                for (Integer num : entry.getValue()) {
                    if (String.valueOf(num).equals(q)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("concorso", concorsoNumber);
        json.put("date", date);
        json.put("year", getYear());

        JSONObject wheelsJson = new JSONObject();
        if (wheels != null) {
            for (Map.Entry<String, List<Integer>> entry : wheels.entrySet()) {
                JSONArray arr = new JSONArray();
                for (Integer n : entry.getValue()) {
                    arr.put(n);
                }
                wheelsJson.put(entry.getKey(), arr);
            }
        }
        json.put("wheels", wheelsJson);
        return json;
    }

    public static Extraction fromJsonObject(JSONObject json) throws JSONException {
        int concorso = json.optInt("concorso", 0);
        String date = json.optString("date", "");
        int year = json.optInt("year", 2025);

        Map<String, List<Integer>> wheelsMap = new LinkedHashMap<>();
        JSONObject wheelsJson = json.optJSONObject("wheels");

        if (wheelsJson != null) {
            Iterator<String> keys = wheelsJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String normRuota = normalizeRuotaName(key);
                JSONArray arr = wheelsJson.getJSONArray(key);
                List<Integer> numbers = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    numbers.add(arr.getInt(i));
                }
                wheelsMap.put(normRuota, numbers);
            }
        }

        return new Extraction(concorso, date, year, wheelsMap);
    }
}
