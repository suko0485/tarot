package com.tarotmod;

import com.tarotmod.block.ModBlocks;
import com.tarotmod.card.CardAbilities;
import com.tarotmod.item.ModItems;
import com.tarotmod.item.TarotCardItem;
import com.tarotmod.prophecy.ProphecyManager;
import com.tarotmod.screen.ModScreens;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TarotMod implements ModInitializer {
    public static final String MOD_ID = "tarot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) { return Identifier.of(MOD_ID, path); }

    @Override
    public void onInitialize() {
        ModBlocks.init();   // antes que ModItems (el creative tab usa el bloque)
        ModItems.init();
        ModScreens.init();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ProphecyManager.tick(server);
            // Pasivas: 1 vez por segundo, carta en la mano secundaria
            if (server.getTicks() % 20 == 0) {
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    ItemStack off = p.getStackInHand(Hand.OFF_HAND);
                    if (off.getItem() instanceof TarotCardItem c) {
                        c.card().passive().run((ServerWorld) p.getEntityWorld(), p, TarotCardItem.level(off));
                    }
                }
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity killer && entity.getEntityWorld() instanceof ServerWorld w) {
                CardAbilities.onKill(w, killer, entity);
            }
        });

        LOGGER.info("Arcanos del Tarot cargado: {} cartas.", ModItems.CARDS.size());
    }
}
