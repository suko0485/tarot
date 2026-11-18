package com.tarotmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Velo rojo con viñeta + grieta dibujada en pantalla mientras dura el evento "Cielo Rojo". */
public final class SkyOverlay {
    private SkyOverlay() {}
    private static long start = 0, durationMs = 0;

    public static void trigger(int ticks) { start = System.currentTimeMillis(); durationMs = ticks * 50L; }

    private static int argb(int a, int rgb) { return (Math.max(0, Math.min(255, a)) << 24) | rgb; }

    public static void render(DrawContext ctx) {
        if (durationMs <= 0) return;
        long el = System.currentTimeMillis() - start;
        if (el > durationMs) { durationMs = 0; return; }
        double t = (double) el / durationMs;                       // 0..1
        double k = t < .25 ? t / .25 : t > .75 ? (1 - t) / .25 : 1; // sube, sostiene, baja
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        ctx.fill(0, 0, w, h, argb((int) (95 * k), 0xB00A0A));      // velo rojo
        for (int i = 0; i < 6; i++) {                                // viñeta: bordes más intensos
            int bw = (int) (w * .06) * (6 - i), bh = (int) (h * .06) * (6 - i);
            int a = (int) (28 * k);
            ctx.fill(0, 0, bw, h, argb(a, 0x300000)); ctx.fill(w - bw, 0, w, h, argb(a, 0x300000));
            ctx.fill(0, 0, w, bh, argb(a, 0x300000)); ctx.fill(0, h - bh, w, h, argb(a, 0x300000));
        }
        // grieta: zigzag desde arriba al centro que avanza con t
        double reach = Math.min(1, t / .6) * h * .62;
        int x = w / 2, y = 0, dir = 1;
        int[] zig = {9, -14, 11, -8, 15, -12, 7, -10, 13, -9, 10, -11, 8};
        int idx = 0;
        while (y < reach) {
            int seg = 14 + (idx % 3) * 6;
            for (int s = 0; s < seg && y < reach; s++) {
                ctx.fill(x - 1, y, x + 2, y + 1, argb((int) (255 * k), 0xFFF2B0));
                ctx.fill(x - 2, y, x - 1, y + 1, argb((int) (120 * k), 0xFF3030));
                ctx.fill(x + 2, y, x + 3, y + 1, argb((int) (120 * k), 0xFF3030));
                y++; x += Integer.signum(zig[idx % zig.length]) * ((s % 3 == 0) ? 1 : 0);
            }
            idx++;
        }
    }
}
