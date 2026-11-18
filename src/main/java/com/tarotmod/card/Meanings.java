package com.tarotmod.card;

import java.util.Map;

/**
 * Significados de lectura (derecha / invertida) de cada Arcano Mayor. Textos cortos para caber en el Grimorio.
 * PENDIENTE: revisión con una tarotista; todos los textos se pueden editar acá sin tocar nada más.
 */
public final class Meanings {
    private Meanings() {}

    public record M(String up, String rev) {}

    public static final Map<String, M> ALL = Map.ofEntries(
        Map.entry("fool", new M("Nuevo comienzo, fe, libertad.", "Imprudencia, ingenuidad, riesgo.")),
        Map.entry("magician", new M("Voluntad, habilidad, acción.", "Manipulación, talento sin usar.")),
        Map.entry("priestess", new M("Intuición, secretos, calma.", "Secretos ocultos, ignorar la voz interior.")),
        Map.entry("empress", new M("Abundancia, cuidado, creación.", "Dependencia, estancamiento.")),
        Map.entry("emperor", new M("Orden, autoridad, estructura.", "Rigidez, abuso de poder.")),
        Map.entry("hierophant", new M("Tradición, enseñanza, guía.", "Rebeldía, dogma vacío.")),
        Map.entry("lovers", new M("Unión, elección, armonía.", "Desequilibrio, mala decisión.")),
        Map.entry("chariot", new M("Victoria, determinación, rumbo.", "Descontrol, agresión, sin rumbo.")),
        Map.entry("strength", new M("Coraje, paciencia, dominio sereno.", "Duda, fuerza mal usada.")),
        Map.entry("hermit", new M("Introspección, sabiduría, soledad.", "Aislamiento, rechazo de consejo.")),
        Map.entry("wheel", new M("Ciclos, suerte, destino.", "Mala racha, resistencia al cambio.")),
        Map.entry("justice", new M("Verdad, equilibrio, consecuencias.", "Injusticia, evasión de culpa.")),
        Map.entry("hanged", new M("Pausa, nueva mirada, entrega.", "Estancamiento, sacrificio inútil.")),
        Map.entry("death", new M("Final necesario, transformación.", "Miedo al cambio, aferrarse.")),
        Map.entry("temperance", new M("Equilibrio, paciencia, mezcla.", "Exceso, desequilibrio.")),
        Map.entry("devil", new M("Ataduras, tentación, instinto.", "Liberación, romper cadenas.")),
        Map.entry("tower", new M("Ruptura brusca, revelación.", "Evitar el desastre, cambio demorado.")),
        Map.entry("star", new M("Esperanza, calma, renovación.", "Desánimo, falta de fe.")),
        Map.entry("moon", new M("Ilusión, miedo, lo oculto.", "Claridad que llega, miedo que cede.")),
        Map.entry("sun", new M("Alegría, éxito, vitalidad.", "Alegría apagada, optimismo excesivo.")),
        Map.entry("judgement", new M("Llamado, renacer, balance.", "Autocrítica, dudar del llamado.")),
        Map.entry("world", new M("Cierre, logro, plenitud.", "Algo sin terminar, atajos."))
    );

    public static M of(String id) { return ALL.getOrDefault(id, new M("?", "?")); }
}
