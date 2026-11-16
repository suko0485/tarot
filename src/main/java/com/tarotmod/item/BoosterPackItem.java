package com.tarotmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Sobre cerrado: al abrirlo da 3 cartas al azar ponderadas por rareza. */
public class BoosterPackItem extends Item {
    public BoosterPackItem(Settings settings) { super(settings); }

    private static int weight(Rarity r) {
        return switch (r) {
            case COMMON -> 50;
            case UNCOMMON -> 30;
            case RARE -> 15;
            case EPIC -> 5;
        };
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        if (!(world instanceof ServerWorld sw)) return ActionResult.SUCCESS;
        ItemStack pack = player.getStackInHand(hand);
        List<TarotCardItem> pool = new ArrayList<>(ModItems.CARDS.values());
        int total = pool.stream().mapToInt(c -> weight(c.card().rarity())).sum();
        for (int i = 0; i < 3; i++) {
            int roll = sw.getRandom().nextInt(total);
            for (TarotCardItem c : pool) {
                roll -= weight(c.card().rarity());
                if (roll < 0) {
                    player.getInventory().offerOrDrop(new ItemStack(c));
                    break;
                }
            }
        }
        pack.decrement(1);
        player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1f, 1.6f);
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        return ActionResult.SUCCESS;
    }
}
