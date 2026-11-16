package com.tarotmod.block;

import com.tarotmod.TarotMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Rarity;

public final class ModBlocks {
    private ModBlocks() {}

    public static Block TAROT_TABLE;
    public static Item TAROT_TABLE_ITEM;

    public static void init() {
        RegistryKey<Block> bKey = RegistryKey.of(RegistryKeys.BLOCK, TarotMod.id("tarot_table"));
        TAROT_TABLE = Registry.register(Registries.BLOCK, bKey, new TarotTableBlock(
                AbstractBlock.Settings.create()
                        .registryKey(bKey)
                        .mapColor(MapColor.PURPLE)
                        .strength(2.5f)
                        .sounds(BlockSoundGroup.WOOD)
                        .luminance(s -> 6)));

        RegistryKey<Item> iKey = RegistryKey.of(RegistryKeys.ITEM, TarotMod.id("tarot_table"));
        TAROT_TABLE_ITEM = Registry.register(Registries.ITEM, iKey, new BlockItem(TAROT_TABLE,
                new Item.Settings().registryKey(iKey).useBlockPrefixedTranslationKey().rarity(Rarity.RARE)));
    }
}
