package com.tarotmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Grimorio Arcano: abre una GUI con capítulos (la GUI vive en el cliente; se engancha desde TarotClient). */
public class GrimoireItem extends Item {
    /** Lo asigna el cliente; en servidor dedicado queda en null. */
    public static Runnable CLIENT_OPEN = null;

    public GrimoireItem(Settings settings) { super(settings); }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        if (world.isClient() && CLIENT_OPEN != null) CLIENT_OPEN.run();
        return ActionResult.SUCCESS;
    }
}
