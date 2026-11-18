package com.tarotmod.net;

import com.tarotmod.TarotMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor -> cliente: inicia el "Cielo Rojo + Grieta" durante N ticks (velo rojo y grieta en pantalla). */
public record SkyPayload(int ticks) implements CustomPayload {
    public static final CustomPayload.Id<SkyPayload> ID = new CustomPayload.Id<>(TarotMod.id("sky"));
    public static final PacketCodec<RegistryByteBuf, SkyPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, SkyPayload::ticks, SkyPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
