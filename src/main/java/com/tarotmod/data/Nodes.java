package com.tarotmod.data;

import java.util.List;

/** Definición del Árbol del Destino. 3 ramas x 5 nodos. Los nodos "cósmicos" exigen haber forjado una carta Cósmica. */
public final class Nodes {
    private Nodes() {}

    public record Node(String id, int branch, String name, String desc, int cost, List<String> parents, int x, int y, boolean cosmic) {}

    public static final String[] BRANCHES = {"Rama del Vidente", "Rama del Arcanista", "Rama del Sobreviviente"};
    public static final String[] BRANCH_SUB = {"Utilidad y Suerte", "Poder Elemental y Daño", "Defensa y Almas"};

    public static final List<Node> ALL = List.of(
        new Node("seer_1", 0, "Ojo Sereno", "Cooldown de las cartas -10%.", 1, List.of(), 0, 0, false),
        new Node("seer_2", 0, "Suerte del Tarotista", "25% de recibir una carta extra al abrir un sobre.", 2, List.of("seer_1"), 1, 1, false),
        new Node("seer_3", 0, "Sentido del Tesoro", "Cada 20 s un destello con partículas revela cofres cercanos.", 2, List.of("seer_1"), -1, 1, false),
        new Node("seer_4", 0, "Bolsillo Astral", "+3 espacios en el Mazo y +1 pasiva activa.", 3, List.of("seer_2", "seer_3"), 0, 2, false),
        new Node("seer_5", 0, "Vidente Cósmico", "Cooldown -15% extra. Requiere forjar una carta Cósmica.", 5, List.of("seer_4"), 0, 3, true),
        new Node("arc_1", 1, "Chispa Arcana", "Daño de cartas ofensivas +15%.", 1, List.of(), 0, 0, false),
        new Node("arc_2", 1, "Radio Expandido", "Radio de las cartas +20%.", 2, List.of("arc_1"), 1, 1, false),
        new Node("arc_3", 1, "Ahorro Estelar", "25% de que una carta no consuma Polvo Estelar.", 2, List.of("arc_1"), -1, 1, false),
        new Node("arc_4", 1, "Efecto Secundario", "Tus ataques con cartas queman y ralentizan.", 3, List.of("arc_2", "arc_3"), 0, 2, false),
        new Node("arc_5", 1, "Tormenta del Arcanista", "+30% de daño y rayos encadenados. Requiere carta Cósmica.", 5, List.of("arc_4"), 0, 3, true),
        new Node("sur_1", 2, "Piel de Piedra", "Los efectos negativos duran menos.", 1, List.of(), 0, 0, false),
        new Node("sur_2", 2, "Sanación Profunda", "La curación de las cartas +25%.", 2, List.of("sur_1"), 1, 1, false),
        new Node("sur_3", 2, "Escudo Arcano", "Un golpe fatal te deja con 3 corazones y un escudo (enfriamiento 120 s).", 3, List.of("sur_1"), -1, 1, false),
        new Node("sur_4", 2, "Cosecha de Almas", "Matar enemigos te cura medio corazón.", 2, List.of("sur_2", "sur_3"), 0, 2, false),
        new Node("sur_5", 2, "Inmortal del Destino", "El escudo recarga en 60 s y te regenera. Requiere carta Cósmica.", 5, List.of("sur_4"), 0, 3, true)
    );

    public static Node get(String id) {
        for (Node n : ALL) if (n.id().equals(id)) return n;
        return null;
    }
}
