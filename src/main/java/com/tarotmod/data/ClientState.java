package com.tarotmod.data;

import java.util.LinkedHashSet;
import java.util.Set;

/** Copia en el cliente de lo que el servidor sincroniza (puntos, colección, nodos). Sin clases de cliente: es seguro en común. */
public final class ClientState {
    private ClientState() {}
    public static volatile int points, cosmic, version;
    public static volatile Set<String> cards = new LinkedHashSet<>();
    public static volatile Set<String> nodes = new LinkedHashSet<>();

    public static void apply(String data) {
        String[] f = data.split(";", -1);
        try {
            points = Integer.parseInt(f[0]);
            cosmic = Integer.parseInt(f[3]);
        } catch (Exception ignored) {}
        cards = split(f.length > 1 ? f[1] : "");
        nodes = split(f.length > 2 ? f[2] : "");
        version++;
    }
    private static Set<String> split(String s) {
        Set<String> out = new LinkedHashSet<>();
        for (String p : s.split(",")) if (!p.isBlank()) out.add(p);
        return out;
    }
}
