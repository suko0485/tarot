package com.tarotmod.card;

import com.tarotmod.net.SkyPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * "Cielo Rojo + Grieta": presagio de algo malo (El Diablo, La Torre, La Muerte en una tirada, Peligro, pacto del Diablo).
 * Servidor: grieta quebrada de partículas sobre el jugador que se va abriendo + truenos.
 * Cliente (SkyPayload): velo rojo con viñeta y una grieta dibujada en pantalla.
 */
public final class SkyEvent {
    private SkyEvent() {}

    public static void play(ServerWorld w, ServerPlayerEntity p, int ticks) {
        for (ServerPlayerEntity o : w.getPlayers(x -> x.squaredDistanceTo(p) < 48 * 48)) ServerPlayNetworking.send(o, new SkyPayload(ticks));
        Vec3d base = Fx.pos(p);
        Fx.sound(w, base, SoundEvents.ENTITY_WITHER_SPAWN, .5f, .5f);
        Fx.sound(w, base, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, .5f);
        // trazado fijo de la grieta (dentro de los ~32 bloques en que se ven las partículas)
        var rnd = w.getRandom();
        List<Vec3d> pts = new ArrayList<>();
        double x = base.x + rnd.nextDouble() * 4 - 2, z = base.z + rnd.nextDouble() * 4 - 2;
        for (int i = 0; i <= 12; i++) {
            pts.add(new Vec3d(x, base.y + 8 + i * 1.7, z));
            x += rnd.nextDouble() * 3 - 1.5; z += rnd.nextDouble() * 3 - 1.5;
        }
        int steps = Math.max(1, ticks / 2);
        Sched.repeat(steps, 2, i -> {
            int upTo = Math.min(pts.size() - 1, 1 + (int) ((pts.size() - 1) * (i + 1.0) / (steps * .6)));
            for (int k = 0; k < upTo; k++) {
                Fx.line(w, ParticleTypes.END_ROD, pts.get(k), pts.get(k + 1), .7);
                if (i % 3 == 0) Fx.line(w, ParticleTypes.SOUL_FIRE_FLAME, pts.get(k), pts.get(k + 1), 1.4);
            }
            if (i % 10 == 5) Fx.sound(w, base, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, .6f, .6f + i * .01f);
        });
        Sched.later(ticks, () -> Fx.sound(w, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, 1f, .6f));
    }
}
