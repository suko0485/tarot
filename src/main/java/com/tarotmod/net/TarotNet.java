package com.tarotmod.net;

import com.tarotmod.data.DestinyManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class TarotNet {
    private TarotNet() {}

    public static void init() {
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SkyPayload.ID, SkyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UnlockPayload.ID, UnlockPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UnlockPayload.ID, (payload, context) ->
                context.server().execute(() -> DestinyManager.tryUnlock(context.player(), payload.node())));
    }
}
