package com.tarotmod.client;

import com.tarotmod.screen.TarotTableScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/** GUI dibujada con rectángulos (sin textura propia) para que funcione sin assets extra. */
public class TarotTableScreen extends HandledScreen<TarotTableScreenHandler> {
    private static final String[] LABELS = {"Pasado", "Presente", "Futuro"};

    public TarotTableScreen(TarotTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Leer futuro"), b ->
                this.client.interactionManager.clickButton(this.handler.syncId, TarotTableScreenHandler.BTN_READ))
                .dimensions(this.x + 10, this.y + 46, 62, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Fusionar"), b ->
                this.client.interactionManager.clickButton(this.handler.syncId, TarotTableScreenHandler.BTN_FUSE))
                .dimensions(this.x + 104, this.y + 46, 62, 20).build());
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = this.x, y = this.y;
        ctx.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF2A1B3D);          // fondo violeta
        ctx.fill(x + 3, y + 3, x + backgroundWidth - 3, y + 78, 0xFF3B2757);            // panel de la mesa
        ctx.fill(x + 3, y + 80, x + backgroundWidth - 3, y + backgroundHeight - 3, 0xFFC6C6C6); // panel inventario
        for (Slot s : this.handler.slots) {
            int sx = x + s.x - 1, sy = y + s.y - 1;
            boolean table = s.id < 4;
            ctx.fill(sx, sy, sx + 18, sy + 18, table ? 0xFFD4AF37 : 0xFF8B8B8B);          // borde
            ctx.fill(sx + 1, sy + 1, sx + 17, sy + 17, table ? 0xFF1A0F2A : 0xFF373737);  // interior
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, this.title, 8, 6, 0xFFE9D8FF, false);
        for (int i = 0; i < 3; i++) {
            ctx.drawText(this.textRenderer, Text.literal(LABELS[i]), 44 + i * 36 + 9 - this.textRenderer.getWidth(LABELS[i]) / 2, 10 + 0, 0xFFD4AF37, false);
        }
        ctx.drawText(this.textRenderer, Text.literal("Amatista / Polvo"), 101, 50, 0xFFB9A3D6, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
