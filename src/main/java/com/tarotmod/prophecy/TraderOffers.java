package com.tarotmod.prophecy;

import com.tarotmod.item.ModItems;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;

/**
 * Ofertas exclusivas del mercader de la profecía.
 * Aislado en su propia clase: si Mojang/Fabric cambian la API de trades en 1.21.11 solo hay que tocar este archivo.
 */
final class TraderOffers {
    private TraderOffers() {}

    static void addExclusiveOffers(WanderingTraderEntity trader, int power) {
        var offers = trader.getOffers();
        offers.add(new TradeOffer(new TradedItem(Items.AMETHYST_SHARD, 6), new ItemStack(ModItems.BOOSTER_PACK), 4, 10, 0.05f));
        offers.add(new TradeOffer(new TradedItem(Items.EMERALD, 8), new ItemStack(ModItems.BOOSTER_PACK, 2), 3, 12, 0.05f));
        offers.add(new TradeOffer(new TradedItem(Items.AMETHYST_SHARD, 2), new ItemStack(ModItems.STARDUST, 3), 8, 5, 0.05f));
        offers.add(new TradeOffer(new TradedItem(Items.DIAMOND, 4), new ItemStack(ModItems.DESTINY_DECK), 1, 20, 0.05f));
        if (power >= 3) {
            offers.add(new TradeOffer(new TradedItem(Items.NETHER_STAR, 1), new ItemStack(ModItems.CARDS.get("sun")), 1, 40, 0.05f));
        }
    }
}
