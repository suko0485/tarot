package com.tarotmod.net;

import com.tarotmod.TarotMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor -> cliente: "puntos;cartas,csv;nodos,csv;cósmicas". */
public record SyncPayload(String data) implements CustomPayload {
    public static final CustomPayload.Id<SyncPayload> ID = new CustomPayload.Id<>(TarotMod.id("sync"));
    public static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, SyncPayload::data, SyncPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
