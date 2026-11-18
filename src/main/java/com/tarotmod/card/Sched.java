package com.tarotmod.card;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Planificador de tareas diferidas en ticks del servidor (para telegrafiados, auras y marcas). */
public final class Sched {
    private Sched() {}
    private record Task(int[] left, Runnable run) {}
    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> ADD = new ArrayList<>();

    public static void later(int ticks, Runnable r) { synchronized (ADD) { ADD.add(new Task(new int[]{Math.max(1, ticks)}, r)); } }

    /** Ejecuta step(i) para i = 0..times-1, una vez cada `interval` ticks (primera ejecución inmediata en el siguiente tick). */
    public static void repeat(int times, int interval, IntConsumer step) {
        for (int i = 0; i < times; i++) { final int k = i; later(1 + i * interval, () -> step.accept(k)); }
    }

    public static void tick() {
        synchronized (ADD) { TASKS.addAll(ADD); ADD.clear(); }
        List<Runnable> due = new ArrayList<>();
        TASKS.removeIf(t -> { if (--t.left()[0] <= 0) { due.add(t.run()); return true; } return false; });
        for (Runnable r : due) {
            try { r.run(); } catch (Exception e) { com.tarotmod.TarotMod.LOGGER.warn("Tarea diferida falló", e); }
        }
    }
}
