package com.tarotmod.screen;

import com.tarotmod.TarotMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreens {
    private ModScreens() {}

    public static ScreenHandlerType<TarotTableScreenHandler> TABLE;

    public static void init() {
        TABLE = Registry.register(Registries.SCREEN_HANDLER, TarotMod.id("tarot_table"),
                new ScreenHandlerType<>(TarotTableScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    }
}
