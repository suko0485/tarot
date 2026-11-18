package com.tarotmod.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Datos persistentes por jugador (se guardan en tarot_data.json dentro del mundo). */
public class PlayerData {
    public int points = 0;
    public int cosmic = 0;                       // cartas Cósmicas forjadas
    public Set<String> cards = new LinkedHashSet<>();
    public Set<String> nodes = new LinkedHashSet<>();
    /** Profecías pendientes: {tipo, ticksRestantes, poder}. */
    public List<int[]> prophecies = new ArrayList<>();

    public boolean has(String node) { return nodes.contains(node); }

    // ---- bonificaciones de colección (Paso 2) ----
    public int activeSlots() { return 1 + (cards.size() >= 5 ? 1 : 0) + (has("seer_4") ? 1 : 0); }
    public int deckCapacity() { return 8 + (cards.size() >= 10 ? 2 : 0) + (cards.size() >= 22 ? 4 : 0) + (has("seer_4") ? 3 : 0); }
    public boolean arcaneEnergy() { return cards.size() >= 15; }

    // ---- bonificaciones del árbol ----
    public double cdMult() { return Math.max(0.4, 1.0 - (has("seer_1") ? .10 : 0) - (has("seer_5") ? .15 : 0)); }
    public double dmgMult() { return 1.0 + (has("arc_1") ? .15 : 0) + (has("arc_5") ? .30 : 0); }
    public double radiusMult() { return 1.0 + (has("arc_2") ? .20 : 0); }
    public double healMult() { return 1.0 + (has("sur_2") ? .25 : 0); }
}
