package com.tarotmod.net;

import com.tarotmod.TarotMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Cliente -> servidor: pedir desbloquear un nodo del árbol. El servidor valida todo. */
public record UnlockPayload(String node) implements CustomPayload {
    public static final CustomPayload.Id<UnlockPayload> ID = new CustomPayload.Id<>(TarotMod.id("unlock"));
    public static final PacketCodec<RegistryByteBuf, UnlockPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, UnlockPayload::node, UnlockPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
