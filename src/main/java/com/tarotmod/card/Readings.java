package com.tarotmod.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Textos de lectura: narrativa por posición/aspecto, combinaciones especiales y veredicto según cartas invertidas. */
public final class Readings {
    private Readings() {}

    private static final String[][] BASE = {
        // Pasado: FORTUNE, DANGER, ENCOUNTER
        {"una racha de suerte te dejó algo que aún no cobraste", "viejas batallas dejaron cicatrices que todavía guardás", "alguien cruzó tu camino y dejó huella"},
        // Presente
        {"tenés a mano una oportunidad; falta que la reconozcas", "algo te pone a prueba justo ahora", "una conexión se está formando a tu alrededor"},
        // Futuro
        {"se abre una puerta de abundancia", "se acerca un desafío que exigirá preparación", "alguien viene a tu encuentro"}
    };
    private static final String[] REV = {" — pero lo viviste a medias", " — aunque algo te nubla la vista", " — con una sombra que conviene vigilar"};
    private static final String[] POS = {"Pasado", "Presente", "Futuro"};
    private static final String[] VERDICT = {
        "§aUn camino claro: el destino y vos miran hacia el mismo lado.",
        "§eHay una sombra, pero es manejable.",
        "§6Mucha sombra: andá con cuidado.",
        "§cTirada de sombras: el destino pone a prueba tu voluntad."
    };
    private static final String[][] COMBOS = {
        {"tower", "death", "Un final y una ruptura: lo viejo cae para que algo nuevo nazca."},
        {"sun", "moon", "Luz y sombra en la misma mesa: no todo lo que brilla es claro."},
        {"lovers", "devil", "Elección y ataduras: cuidá a qué te comprometés."},
        {"star", "tower", "Después de la caída, la esperanza vuelve a encenderse."},
        {"fool", "world", "El comienzo y el cierre se tocan: un ciclo completo."},
        {"wheel", "justice", "Destino y consecuencias: lo que giró vuelve a su lugar."},
        {"hermit", "priestess", "Silencio e intuición: la respuesta está adentro."},
        {"emperor", "hierophant", "Orden y tradición: seguí las reglas o rompelas a conciencia."}
    };

    public static List<String> compose(TarotCard[] cards, boolean[] rev) {
        List<String> out = new ArrayList<>();
        int revCount = 0;
        for (int i = 0; i < 3; i++) {
            int a = cards[i].aspect().ordinal();
            out.add("§7" + POS[i] + ": §f" + BASE[i][a] + (rev[i] ? REV[i] : "") + ".");
            if (rev[i]) revCount++;
        }
        Set<TarotCard.Aspect> aspects = new java.util.HashSet<>();
        for (TarotCard c : cards) aspects.add(c.aspect());
        if (aspects.size() == 1) out.add("§dLas tres cartas hablan de lo mismo: es un mensaje muy fuerte.");
        else if (aspects.size() == 3) out.add("§dTres caminos distintos: el destino aún no se decidió.");
        Set<String> ids = new java.util.HashSet<>();
        for (TarotCard c : cards) ids.add(c.id());
        for (String[] cb : COMBOS) if (ids.contains(cb[0]) && ids.contains(cb[1])) out.add("§5✦ " + cb[2]);
        out.add(VERDICT[revCount]);
        return out;
    }

    /** Consejo de una sola carta. */
    public static String advice(TarotCard c, boolean rev) {
        String tail = switch (c.aspect()) {
            case FORTUNE -> rev ? "no dejes pasar lo que ya tenés a mano." : "aprovechá la suerte que te rodea.";
            case DANGER -> rev ? "frená antes de que sea tarde." : "enfrentá lo que viene con la guardia en alto.";
            case ENCOUNTER -> rev ? "no te cierres a los demás." : "abrite a quien se acerque.";
        };
        return (rev ? "§cInvertida: " : "§aDerecha: ") + "§f" + tail;
    }
}
