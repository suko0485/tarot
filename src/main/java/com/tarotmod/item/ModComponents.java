package com.tarotmod.item;

import com.mojang.serialization.Codec;
import com.tarotmod.TarotMod;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModComponents {
    private ModComponents() {}

    /** 0 = Normal, 1 = Astral, 2 = Cósmica */
    public static final ComponentType<Integer> CARD_LEVEL = Registry.register(
            Registries.DATA_COMPONENT_TYPE, TarotMod.id("card_level"),
            ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());

    /** true = carta invertida (lectura sombría: cuesta 1 Polvo menos pero enfría 25% más lento). */
    public static final ComponentType<Boolean> REVERSED = Registry.register(
            Registries.DATA_COMPONENT_TYPE, TarotMod.id("reversed"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).packetCodec(PacketCodecs.BOOLEAN).build());

    public static void init() {}
}
