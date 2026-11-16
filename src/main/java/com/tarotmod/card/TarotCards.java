package com.tarotmod.card;

import com.tarotmod.card.TarotCard.Action;
import com.tarotmod.card.TarotCard.Aspect;
import net.minecraft.util.Rarity;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registro de definiciones. Para añadir una carta: agregá una línea acá + lang + textura (el script la genera). */
public final class TarotCards {
    private TarotCards() {}

    public static final Map<String, TarotCard> ALL = new LinkedHashMap<>();

    private static final Action NONE = (w, p, l) -> {};

    private static void add(String id, String numeral, Rarity r, Aspect a, int cdSeconds, Action active, Action passive) {
        ALL.put(id, new TarotCard(id, numeral, r, a, cdSeconds * 20, active, passive));
    }

    static {
        add("fool",      "0",     Rarity.COMMON,   Aspect.ENCOUNTER, 20, CardAbilities::foolActive,     CardAbilities::foolPassive);
        add("magician",  "I",     Rarity.UNCOMMON, Aspect.FORTUNE,    2, CardAbilities::magicianActive,  CardAbilities::magicianPassive);
        add("empress",   "III",   Rarity.UNCOMMON, Aspect.FORTUNE,   45, CardAbilities::empressActive,   CardAbilities::empressPassive);
        add("chariot",   "VII",   Rarity.RARE,     Aspect.DANGER,    15, CardAbilities::chariotActive,   CardAbilities::chariotPassive);
        add("wheel",     "X",     Rarity.RARE,     Aspect.FORTUNE,   90, CardAbilities::wheelActive,     CardAbilities::wheelPassive);
        add("tower",     "XVI",   Rarity.EPIC,     Aspect.DANGER,    40, CardAbilities::towerActive,     CardAbilities::towerPassive);
        add("star",      "XVII",  Rarity.RARE,     Aspect.ENCOUNTER, 60, CardAbilities::starActive,      CardAbilities::starPassive);
        add("sun",       "XIX",   Rarity.EPIC,     Aspect.ENCOUNTER, 60, CardAbilities::sunActive,       CardAbilities::sunPassive);
    }
}
