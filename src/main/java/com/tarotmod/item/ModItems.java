package com.tarotmod.item;

import com.tarotmod.TarotMod;
import com.tarotmod.block.ModBlocks;
import com.tarotmod.card.TarotCard;
import com.tarotmod.card.TarotCards;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class ModItems {
    private ModItems() {}

    public static final Map<String, TarotCardItem> CARDS = new LinkedHashMap<>();

    public static Item DESTINY_DECK;
    public static Item BOOSTER_PACK;
    public static Item STARDUST;
    public static Item GRIMOIRE;
    public static Item EXPANSION_PAGE;
    public static Item DESTINY_HEART;
    public static Item DESTINY_SHARD;
    public static Item ASTRAL_TEAR;
    public static Item BROKEN_CROWN;
    public static ItemGroup GROUP;

    public static <T extends Item> T register(String name, Function<Item.Settings, T> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, TarotMod.id(name));
        return Registry.register(Registries.ITEM, key, factory.apply(settings.registryKey(key)));
    }

    public static void init() {
        ModComponents.init();

        for (TarotCard card : TarotCards.ALL.values()) {
            CARDS.put(card.id(), register("card_" + card.id(),
                    s -> new TarotCardItem(s, card),
                    new Item.Settings().maxCount(1).rarity(card.rarity())));
        }
        DESTINY_DECK = register("destiny_deck", DestinyDeckItem::new, new Item.Settings().maxCount(1).rarity(Rarity.RARE));
        BOOSTER_PACK = register("booster_pack", BoosterPackItem::new, new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON));
        STARDUST = register("stardust", Item::new, new Item.Settings().rarity(Rarity.UNCOMMON));
        GRIMOIRE = register("grimoire", GrimoireItem::new, new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));

        EXPANSION_PAGE = register("expansion_page", Item::new, new Item.Settings().rarity(Rarity.EPIC));

        DESTINY_HEART = register("destiny_heart", Item::new, new Item.Settings().rarity(Rarity.EPIC).maxCount(1).component(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true));

        DESTINY_SHARD = register("destiny_shard", Item::new, new Item.Settings().rarity(Rarity.RARE));
        ASTRAL_TEAR = register("astral_tear", Item::new, new Item.Settings().rarity(Rarity.EPIC).maxCount(16));
        BROKEN_CROWN = register("broken_crown", Item::new, new Item.Settings().rarity(Rarity.EPIC).maxCount(16));

        GROUP = Registry.register(Registries.ITEM_GROUP, TarotMod.id("tarot"),
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(DESTINY_DECK))
                        .displayName(Text.translatable("itemGroup.tarot"))
                        .entries((ctx, entries) -> {
                            entries.add(GRIMOIRE);
                            entries.add(DESTINY_DECK);
                            entries.add(BOOSTER_PACK);
                            entries.add(STARDUST);
                            entries.add(ModBlocks.TAROT_TABLE_ITEM);
                            entries.add(ModBlocks.ALTAR_ITEM);
                            entries.add(EXPANSION_PAGE);
                            entries.add(DESTINY_HEART);
                            entries.add(DESTINY_SHARD);
                            entries.add(ASTRAL_TEAR);
                            entries.add(BROKEN_CROWN);
                            CARDS.values().forEach(entries::add);
                        })
                        .build());
    }
}
