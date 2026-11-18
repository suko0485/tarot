package com.tarotmod.card;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Rarity;

/** Definición de una carta (datos + habilidades). El ítem real es TarotCardItem. */
public record TarotCard(
        String id,
        String numeral,
        Rarity rarity,
        Aspect aspect,
        int cooldownTicks,
        int cost,          // Polvo Estelar por uso activo (alternativa: 3 niveles de XP por cada Polvo)
        Cast active,
        Action passive
) {
    /** Hacia qué tipo de profecía empuja esta carta en la Mesa del Tarot. */
    public enum Aspect { FORTUNE, DANGER, ENCOUNTER }

    @FunctionalInterface
    public interface Action { void run(ServerWorld world, ServerPlayerEntity player, int level); }

    /** Habilidad activa: devuelve false si no pudo lanzarse (no cobra costo ni inicia el cooldown). */
    @FunctionalInterface
    public interface Cast { boolean cast(ServerWorld world, ServerPlayerEntity player, int level); }

    public static final String[] LEVEL_NAMES = {"Normal", "Astral", "Cósmica"};
    public static final int MAX_LEVEL = 2;
}
