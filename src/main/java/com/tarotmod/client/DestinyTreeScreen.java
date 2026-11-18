package com.tarotmod.client;

import com.tarotmod.data.ClientState;
import com.tarotmod.data.Nodes;
import com.tarotmod.net.UnlockPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

/** Árbol del Destino (tecla Z): 3 ramas de nodos conectados. Los nodos son botones; el servidor valida cada compra. */
public class DestinyTreeScreen extends Screen {
    private static final int[] BRANCH_COLOR = {0xFF6FD6FF, 0xFFFF7A4D, 0xFF8CFF8C};
    private final Map<String, int[]> pos = new HashMap<>();
    private int lastVersion = -1;

    public DestinyTreeScreen() { super(Text.literal("Árbol del Destino")); }

    @Override
    protected void init() {
        pos.clear();
        lastVersion = ClientState.version;
        int cx = this.width / 2;
        int base = this.height - 64;
        for (Nodes.Node n : Nodes.ALL) {
            int colX = cx + (n.branch() - 1) * Math.min(150, this.width / 3);
            int x = colX + n.x() * 34 - 13;
            int y = base - n.y() * 52;
            pos.put(n.id(), new int[]{x, y});
            boolean owned = ClientState.nodes.contains(n.id());
            boolean parentsOk = n.parents().stream().allMatch(ClientState.nodes::contains);
            boolean cosmicOk = !n.cosmic() || ClientState.cosmic > 0;
            boolean afford = ClientState.points >= n.cost();
            String label = owned ? "✔" : String.valueOf(n.cost());
            ButtonWidget b = ButtonWidget.builder(Text.literal(label), btn -> {
                if (!owned) ClientPlayNetworking.send(new UnlockPayload(n.id()));
            }).dimensions(x, y, 26, 26).build();
            String state = owned ? "§aDesbloqueado" : !parentsOk ? "§cBloqueado: falta el nodo anterior"
                    : !cosmicOk ? "§cRequiere una carta Cósmica" : afford ? "§eClic para desbloquear" : "§cTe faltan puntos";
            b.setTooltip(Tooltip.of(Text.literal("§f§l" + n.name() + "\n§7" + n.desc() + "\n§dCosto: " + n.cost() + " Puntos\n" + state)));
            b.active = !owned && parentsOk && cosmicOk && afford;
            this.addDrawableChild(b);
        }
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"), b -> this.close())
                .dimensions(cx - 30, this.height - 28, 60, 20).build());
    }

    @Override
    public void tick() {
        if (lastVersion != ClientState.version) this.clearAndInit();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(0, 0, this.width, this.height, 0xCC0B0618);
        // líneas entre nodos (en L: vertical y luego horizontal)
        for (Nodes.Node n : Nodes.ALL) {
            int[] to = pos.get(n.id());
            if (to == null) continue;
            for (String par : n.parents()) {
                int[] from = pos.get(par);
                if (from == null) continue;
                boolean lit = ClientState.nodes.contains(par);
                int col = lit ? BRANCH_COLOR[n.branch()] : 0xFF3A3050;
                int fx = from[0] + 13, fy = from[1] + 13, tx = to[0] + 13, ty = to[1] + 13;
                int midY = (fy + ty) / 2;
                ctx.fill(fx - 1, midY, fx + 2, fy, col);
                ctx.fill(Math.min(fx, tx) - 1, midY - 1, Math.max(fx, tx) + 2, midY + 2, col);
                ctx.fill(tx - 1, ty, tx + 2, midY, col);
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;
        String title = "✦ Árbol del Destino ✦";
        ctx.drawText(this.textRenderer, Text.literal(title), cx - this.textRenderer.getWidth(title) / 2, 10, 0xFFFFD866, true);
        String pts = "Puntos de Destino: " + ClientState.points + "   ·   Cartas: " + ClientState.cards.size() + "/22   ·   Cósmicas: " + ClientState.cosmic;
        ctx.drawText(this.textRenderer, Text.literal(pts), cx - this.textRenderer.getWidth(pts) / 2, 24, 0xFFE9D8FF, true);
        for (int b = 0; b < 3; b++) {
            int colX = cx + (b - 1) * Math.min(150, this.width / 3);
            String t = Nodes.BRANCHES[b];
            ctx.drawText(this.textRenderer, Text.literal(t), colX - this.textRenderer.getWidth(t) / 2, 44, BRANCH_COLOR[b], true);
            String s = Nodes.BRANCH_SUB[b];
            ctx.drawText(this.textRenderer, Text.literal(s), colX - this.textRenderer.getWidth(s) / 2, 55, 0xFF9A8FB5, false);
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
