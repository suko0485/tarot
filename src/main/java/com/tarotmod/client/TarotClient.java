package com.tarotmod.client;

import com.tarotmod.TarotMod;
import com.tarotmod.data.ClientState;
import com.tarotmod.item.GrimoireItem;
import com.tarotmod.net.SyncPayload;
import com.tarotmod.screen.ModScreens;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TarotClient implements ClientModInitializer {
    public static KeyBinding OPEN_TREE;

    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreens.TABLE, TarotTableScreen::new);

        // Tecla Z (configurable en Controles -> "Arcanos: Tarot")
        OPEN_TREE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tarot.open_tree", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z,
                KeyBinding.Category.create(TarotMod.id("main"))));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_TREE.wasPressed()) {
                if (mc.player != null && mc.currentScreen == null) mc.setScreen(new DestinyTreeScreen());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> ClientState.apply(payload.data()));

        ClientPlayNetworking.registerGlobalReceiver(com.tarotmod.net.SkyPayload.ID, (payload, context) -> SkyOverlay.trigger(payload.ticks()));
        // Capa de HUD (API nueva de Fabric: HudElementRegistry) para el velo rojo
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementBefore(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT, TarotMod.id("sky_overlay"),
                (ctx, tickCounter) -> SkyOverlay.render(ctx));

        GrimoireItem.CLIENT_OPEN = () -> MinecraftClient.getInstance().setScreen(new GrimoireScreen());
    }
}
