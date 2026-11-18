package com.tarotmod;

import com.tarotmod.block.ModBlocks;
import com.tarotmod.boss.BossManager;
import com.tarotmod.card.CardEvents;
import com.tarotmod.card.Sched;
import com.tarotmod.data.DestinyManager;
import com.tarotmod.data.PlayerData;
import com.tarotmod.item.DestinyDeckItem;
import com.tarotmod.item.ModItems;
import com.tarotmod.item.TarotCardItem;
import com.tarotmod.net.TarotNet;
import com.tarotmod.prophecy.ProphecyManager;
import com.tarotmod.screen.ModScreens;
import com.tarotmod.world.Shrine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TarotMod implements ModInitializer {
    public static final String MOD_ID = "tarot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) { return Identifier.of(MOD_ID, path); }

    @Override
    public void onInitialize() {
        ModBlocks.init();   // antes que ModItems (el creative tab usa el bloque)
        ModItems.init();
        ModScreens.init();
        TarotNet.init();
        CardEvents.init();

        ServerLifecycleEvents.SERVER_STARTED.register(DestinyManager::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(DestinyManager::save);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> DestinyManager.sync(handler.getPlayer()));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Sched.tick();
            BossManager.tick(server);
            int t = server.getTicks();
            if (t % 6000 == 0) DestinyManager.save(server);
            if (t % 20 != 0) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                ServerWorld w = (ServerWorld) p.getEntityWorld();
                PlayerData d = DestinyManager.get(p);
                DestinyManager.secondTick(w, p, t);
                ProphecyManager.secondTick(w, p, d);
                if (t % 100 == 0) Shrine.tick(w, p);
                // Pasivas: carta en la mano secundaria + primeras N cartas del Mazo del Destino
                ItemStack off = p.getStackInHand(Hand.OFF_HAND);
                if (off.getItem() instanceof TarotCardItem c) c.card().passive().run(w, p, TarotCardItem.level(off));
                int slots = d.activeSlots();
                for (int i = 0; i < p.getInventory().size(); i++) {
                    ItemStack s = p.getInventory().getStack(i);
                    if (s.getItem() instanceof DestinyDeckItem) {
                        List<ItemStack> in = DestinyDeckItem.cards(s);
                        for (int k = 0; k < Math.min(slots, in.size()); k++)
                            if (in.get(k).getItem() instanceof TarotCardItem c) c.card().passive().run(w, p, TarotCardItem.level(in.get(k)));
                        break; // un solo mazo cuenta
                    }
                }
            }
        });

        LOGGER.info("Arcanos del Tarot cargado: {} cartas.", ModItems.CARDS.size());
    }
}
