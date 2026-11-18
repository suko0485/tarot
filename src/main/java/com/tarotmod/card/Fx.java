package com.tarotmod.card;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Efectos sensoriales: anillos de partículas y sonidos con pitch. */
public final class Fx {
    private Fx() {}

    public static Vec3d pos(net.minecraft.entity.Entity e) { return new Vec3d(e.getX(), e.getY(), e.getZ()); }

    public static void sound(ServerWorld w, double x, double y, double z, SoundEvent s, float vol, float pitch) {
        w.playSound(null, x, y, z, s, SoundCategory.PLAYERS, vol, pitch);
    }
    public static void sound(ServerWorld w, Vec3d p, SoundEvent s, float vol, float pitch) { sound(w, p.x, p.y, p.z, s, vol, pitch); }

    public static void ring(ServerWorld w, ParticleEffect t, double x, double y, double z, double r, int n) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            w.spawnParticles(t, x + Math.cos(a) * r, y, z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }
    public static void ring(ServerWorld w, ParticleEffect t, Vec3d c, double r, int n) { ring(w, t, c.x, c.y, c.z, r, n); }

    /** Disco relleno (varios anillos). */
    public static void disc(ServerWorld w, ParticleEffect t, Vec3d c, double r, int per) {
        for (double rr = r; rr > 0.4; rr -= 1.2) ring(w, t, c.x, c.y, c.z, rr, Math.max(6, (int) (per * rr / r)));
    }

    public static void column(ServerWorld w, ParticleEffect t, double x, double y, double z, double h, int n) {
        for (int i = 0; i < n; i++) w.spawnParticles(t, x, y + h * i / n, z, 1, .05, 0, .05, 0);
    }

    public static void line(ServerWorld w, ParticleEffect t, Vec3d a, Vec3d b, double step) {
        Vec3d d = b.subtract(a); double len = d.length(); if (len < 0.01) return;
        Vec3d u = d.multiply(1 / len);
        for (double s = 0; s <= len; s += step) w.spawnParticles(t, a.x + u.x * s, a.y + u.y * s, a.z + u.z * s, 1, 0, 0, 0, 0);
    }

    /** Cofres/barriles/baúles de Ender en un radio, con su posición. */
    public static java.util.List<BlockPos> findChests(ServerWorld w, BlockPos c, int r) {
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(c.add(-r, -r / 2, -r), c.add(r, r / 2, r))) {
            Block b = w.getBlockState(pos).getBlock();
            if (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.BARREL || b == Blocks.ENDER_CHEST) out.add(pos.toImmutable());
        }
        return out;
    }
}
