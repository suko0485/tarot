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
        Action active,
        Action passive
) {
    /** Hacia qué tipo de profecía empuja esta carta en la Mesa de Lectura. */
    public enum Aspect { FORTUNE, DANGER, ENCOUNTER }

    @FunctionalInterface
    public interface Action {
        void run(ServerWorld world, ServerPlayerEntity player, int level);
    }

    public static final String[] LEVEL_NAMES = {"Normal", "Astral", "Cósmica"};
    public static final int MAX_LEVEL = 2;
}
