package com.tarotmod.prophecy;

import com.tarotmod.item.ModItems;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;

/** Ofertas del mercader de la profecía y de El Vidente de los Santuarios. */
public final class TraderOffers {
    private TraderOffers() {}

    /** Mercader místico del Augurio de Encuentro: sobres especiales y páginas de expansión (tiempo limitado). */
    static void addExclusiveOffers(WanderingTraderEntity trader, int power) {
        var o = trader.getOffers();
        o.add(new TradeOffer(new TradedItem(Items.AMETHYST_SHARD, 6), new ItemStack(ModItems.BOOSTER_PACK, 2), 4, 10, 0.05f));
        o.add(new TradeOffer(new TradedItem(Items.EMERALD, 10), new ItemStack(ModItems.BOOSTER_PACK, 3), 2, 12, 0.05f));
        o.add(new TradeOffer(new TradedItem(Items.DIAMOND, 3), new ItemStack(ModItems.EXPANSION_PAGE), 3, 20, 0.05f));
        o.add(new TradeOffer(new TradedItem(Items.GOLD_INGOT, 8), new ItemStack(ModItems.STARDUST, 4), 6, 5, 0.05f));
        o.add(new TradeOffer(new TradedItem(Items.EMERALD, 20), new ItemStack(ModItems.DESTINY_SHARD), 1, 25, 0.05f));
        if (power >= 3) o.add(new TradeOffer(new TradedItem(Items.DIAMOND, 8), new ItemStack(ModItems.EXPANSION_PAGE, 2), 1, 40, 0.05f));
    }

    /** El Vidente (Santuario): esmeraldas y amatistas por sobres. Es la vía de entrada al mod. */
    public static void addSeerOffers(WanderingTraderEntity seer) {
        var o = seer.getOffers();
        o.add(new TradeOffer(new TradedItem(Items.EMERALD, 4), new ItemStack(ModItems.BOOSTER_PACK), 12, 8, 0.05f));
        o.add(new TradeOffer(new TradedItem(Items.AMETHYST_SHARD, 6), new ItemStack(ModItems.BOOSTER_PACK), 12, 8, 0.05f));
        o.add(new TradeOffer(new TradedItem(Items.AMETHYST_SHARD, 3), new ItemStack(ModItems.STARDUST, 4), 16, 4, 0.05f));
        o.add(new TradeOffer(new TradedItem(Items.EMERALD, 24), new ItemStack(ModItems.CARDS.get("fool")), 3, 15, 0.05f));
    }
}
