package com.tarotmod.card;

import com.tarotmod.card.TarotCard.Aspect;
import com.tarotmod.card.TarotCard.Action;
import com.tarotmod.card.TarotCard.Cast;
import net.minecraft.util.Rarity;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registro de los 22 Arcanos Mayores. Cooldowns en segundos; el costo es en Polvo Estelar. */
public final class TarotCards {
    private TarotCards() {}

    public static final Map<String, TarotCard> ALL = new LinkedHashMap<>();

    private static void add(String id, String numeral, Rarity r, Aspect a, int cdSeconds, int cost, Cast active, Action passive) {
        ALL.put(id, new TarotCard(id, numeral, r, a, cdSeconds * 20, cost, active, passive));
    }

    static {
        add("fool", "0", Rarity.COMMON, Aspect.ENCOUNTER, 12, 1, CardAbilities::foolActive, CardAbilities::foolPassive);
        add("magician", "I", Rarity.UNCOMMON, Aspect.FORTUNE, 6, 1, CardAbilities::magicianActive, CardAbilities::magicianPassive);
        add("priestess", "II", Rarity.UNCOMMON, Aspect.ENCOUNTER, 30, 1, CardAbilities::priestessActive, CardAbilities::priestessPassive);
        add("empress", "III", Rarity.UNCOMMON, Aspect.FORTUNE, 40, 2, CardAbilities::empressActive, CardAbilities::empressPassive);
        add("emperor", "IV", Rarity.RARE, Aspect.DANGER, 35, 2, CardAbilities::emperorActive, CardAbilities::emperorPassive);
        add("hierophant", "V", Rarity.UNCOMMON, Aspect.FORTUNE, 40, 1, CardAbilities::hierophantActive, CardAbilities::hierophantPassive);
        add("lovers", "VI", Rarity.UNCOMMON, Aspect.ENCOUNTER, 30, 1, CardAbilities::loversActive, CardAbilities::loversPassive);
        add("chariot", "VII", Rarity.RARE, Aspect.DANGER, 14, 1, CardAbilities::chariotActive, CardAbilities::chariotPassive);
        add("strength", "VIII", Rarity.RARE, Aspect.DANGER, 20, 1, CardAbilities::strengthActive, CardAbilities::strengthPassive);
        add("hermit", "IX", Rarity.UNCOMMON, Aspect.ENCOUNTER, 30, 1, CardAbilities::hermitActive, CardAbilities::hermitPassive);
        add("wheel", "X", Rarity.RARE, Aspect.FORTUNE, 60, 2, CardAbilities::wheelActive, CardAbilities::wheelPassive);
        add("justice", "XI", Rarity.RARE, Aspect.DANGER, 35, 2, CardAbilities::justiceActive, CardAbilities::justicePassive);
        add("hanged", "XII", Rarity.UNCOMMON, Aspect.DANGER, 25, 1, CardAbilities::hangedActive, CardAbilities::hangedPassive);
        add("death", "XIII", Rarity.EPIC, Aspect.DANGER, 20, 2, CardAbilities::deathActive, CardAbilities::deathPassive);
        add("temperance", "XIV", Rarity.RARE, Aspect.FORTUNE, 30, 1, CardAbilities::temperanceActive, CardAbilities::temperancePassive);
        add("devil", "XV", Rarity.EPIC, Aspect.DANGER, 40, 2, CardAbilities::devilActive, CardAbilities::devilPassive);
        add("tower", "XVI", Rarity.EPIC, Aspect.DANGER, 25, 3, CardAbilities::towerActive, CardAbilities::towerPassive);
        add("star", "XVII", Rarity.RARE, Aspect.ENCOUNTER, 45, 2, CardAbilities::starActive, CardAbilities::starPassive);
        add("moon", "XVIII", Rarity.RARE, Aspect.ENCOUNTER, 40, 1, CardAbilities::moonActive, CardAbilities::moonPassive);
        add("sun", "XIX", Rarity.EPIC, Aspect.ENCOUNTER, 30, 2, CardAbilities::sunActive, CardAbilities::sunPassive);
        add("judgement", "XX", Rarity.EPIC, Aspect.FORTUNE, 50, 3, CardAbilities::judgementActive, CardAbilities::judgementPassive);
        add("world", "XXI", Rarity.EPIC, Aspect.ENCOUNTER, 60, 4, CardAbilities::worldActive, CardAbilities::worldPassive);
    }
}
