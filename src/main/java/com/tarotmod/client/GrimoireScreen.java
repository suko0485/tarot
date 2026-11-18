package com.tarotmod.client;

import com.tarotmod.card.TarotCard;
import com.tarotmod.card.TarotCards;
import com.tarotmod.data.ClientState;
import com.tarotmod.item.ModItems;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Grimorio interactivo: 4 capítulos (Inicio, Cartas y Colección, Profecías, Rituales y Jefes). */
public class GrimoireScreen extends Screen {
    private static final String[] CHAPTERS = {"Inicio", "Cartas y Colección", "Profecías", "Rituales y Jefes", "Cómo leer"};
    private static final int PER_PAGE = 4;
    private int chapter = 0, page = 0;

    public GrimoireScreen() { super(Text.literal("Grimorio Arcano")); }

    private int px() { return this.width / 2 - 170; }
    private int py() { return this.height / 2 - 105; }

    @Override
    protected void init() {
        for (int i = 0; i < CHAPTERS.length; i++) {
            final int c = i;
            ButtonWidget b = ButtonWidget.builder(Text.literal((chapter == i ? "§6" : "") + (i + 1) + ". " + CHAPTERS[i]), x -> { chapter = c; page = 0; this.clearAndInit(); })
                    .dimensions(px() + 6, py() + 28 + i * 24, 96, 20).build();
            this.addDrawableChild(b);
        }
        if (chapter == 1) {
            int pages = (TarotCards.ALL.size() + PER_PAGE - 1) / PER_PAGE;
            this.addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> { page = (page + pages - 1) % pages; this.clearAndInit(); })
                    .dimensions(px() + 116, py() + 196, 24, 16).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> { page = (page + 1) % pages; this.clearAndInit(); })
                    .dimensions(px() + 316, py() + 196, 24, 16).build());
        }
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"), b -> this.close())
                .dimensions(px() + 6, py() + 188, 96, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        super.renderBackground(ctx, mx, my, d);
        int x = px(), y = py();
        ctx.fill(x - 2, y - 2, x + 342, y + 214, 0xFFD4AF37);
        ctx.fill(x, y, x + 340, y + 212, 0xFF1E1230);
        ctx.fill(x + 106, y + 24, x + 107, y + 208, 0xFF5A3F86);
    }

    private int line(DrawContext ctx, String s, int x, int y, int color, int wrap) {
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(s), wrap);
        for (OrderedText l : lines) { ctx.drawText(this.textRenderer, l, x, y, color, false); y += 10; }
        return y + 2;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        int x = px(), y = py(), cx = x + 114, W = 218;
        ctx.drawText(this.textRenderer, Text.literal("✦ Grimorio Arcano ✦"), x + 8, y + 8, 0xFFFFD866, true);
        String stat = "Cartas " + ClientState.cards.size() + "/22 · ✦ " + ClientState.points + " pts";
        ctx.drawText(this.textRenderer, Text.literal(stat), x + 150, y + 8, 0xFFB9A3D6, false);
        int ty = y + 30;
        switch (chapter) {
            case 0 -> {
                ty = line(ctx, "§6PASO 1 · Exploración", cx, ty, 0xFFFFFFFF, W);
                ty = line(ctx, "Hallá un Santuario del Tarot (ruinas de piedra con faroles de almas) o comerciá con El Vidente: esmeraldas y amatistas por Sobres de Cartas. El cofre oculto del santuario tiene tus primeras cartas.", cx, ty, 0xFFCCCCCC, W);
                ty = line(ctx, "§6Mesa del Tarot §7(receta): amatistas, oro, obsidiana y un libro. Mazo del Destino: papel y Polvo Estelar (amatista + polvo de piedra luminosa).", cx, ty, 0xFFCCCCCC, W);
                ty = line(ctx, "§6PASO 2 · Colección §7Cada carta nueva da §dPuntos de Destino§7. Con 5, 10, 15 y 22 cartas ganás ranuras y energía arcana. Pulsá §fZ§7 para gastar puntos en el Árbol.", cx, ty, 0xFFCCCCCC, W);
                line(ctx, "§7Clic derecho: habilidad activa (cuesta Polvo Estelar o niveles). Carta en la mano secundaria o en las primeras ranuras del Mazo: pasiva.", cx, ty, 0xFFAAAAAA, W);
            }
            case 1 -> {
                List<TarotCard> list = new ArrayList<>(TarotCards.ALL.values());
                for (int i = page * PER_PAGE; i < Math.min(list.size(), (page + 1) * PER_PAGE); i++) {
                    TarotCard c = list.get(i);
                    boolean have = ClientState.cards.contains(c.id());
                    if (have) {
                        ctx.drawItem(new ItemStack(ModItems.CARDS.get(c.id())), cx, ty);
                        ctx.drawText(this.textRenderer, Text.literal(c.numeral() + " · ").append(Text.translatable("item.tarot.card_" + c.id())), cx + 20, ty, 0xFFFFD866, false);
                        ctx.drawText(this.textRenderer, Text.translatable("tooltip.tarot." + c.id() + ".active"), cx + 20, ty + 10, 0xFFCCCCCC, false);
                        ctx.drawText(this.textRenderer, Text.translatable("tooltip.tarot." + c.id() + ".passive"), cx + 20, ty + 20, 0xFFB78CFF, false);
                        var mn = com.tarotmod.card.Meanings.of(c.id());
                        ctx.drawText(this.textRenderer, Text.literal("§a▲ " + mn.up() + " §c▼ " + mn.rev()), cx + 20, ty + 30, 0xFFEEDD99, false);
                    } else {
                        ctx.drawText(this.textRenderer, Text.literal("§8?"), cx + 6, ty + 4, 0xFF777777, false);
                        ctx.drawText(this.textRenderer, Text.literal(c.numeral() + " · §8??? §7(" + switch (c.rarity()) { case COMMON -> "común"; case UNCOMMON -> "poco común"; case RARE -> "rara"; case EPIC -> "épica"; } + ")"), cx + 20, ty, 0xFF888888, false);
                        ctx.drawText(this.textRenderer, Text.literal("§8Aún no la descubriste: sobres, santuarios, profecías."), cx + 20, ty + 10, 0xFF666666, false);
                    }
                    ty += 40;
                }
                ctx.drawText(this.textRenderer, Text.literal("Pág. " + (page + 1) + "/" + ((TarotCards.ALL.size() + PER_PAGE - 1) / PER_PAGE)), cx + 108, y + 200, 0xFF9A8FB5, false);
                ctx.drawText(this.textRenderer, Text.literal("§65: +1 pasiva · 10: +2 espacios · 15: energía · 22: +4 + 5 pts"), cx, y + 16, 0xFFFFFFFF, false);
            }
            case 2 -> {
                ty = line(ctx, "§6Tirada en la Mesa del Tarot", cx, ty, 0xFFFFFFFF, W);
                ty = line(ctx, "Colocá 3 cartas (Pasado, Presente, Futuro) + 1 amatista o 5 niveles. Ver capítulo 5: cómo leer la tirada. Se cumple a los 2-5 minutos; un reloj sutil te acompaña.", cx, ty, 0xFFCCCCCC, W);
                ty = line(ctx, "§e✦ Augurio de Fortuna §7Un meteorito cae cerca y deja un cofre con §bEsquirlas del Destino§7, amatistas y cartas raras.", cx, ty, 0xFFCCCCCC, W);
                ty = line(ctx, "§c☠ Augurio de Peligro §7El cielo se oscurece: emboscada de espectros y un mini-jefe. Vencerlo da cartas difíciles y §dPuntos de Destino§7.", cx, ty, 0xFFCCCCCC, W);
                line(ctx, "§d☾ Augurio de Encuentro §7Un mercader místico vende sobres especiales y Páginas de Expansión por tiempo limitado.", cx, ty, 0xFFCCCCCC, W);
            }
            case 4 -> {
                ty = line(ctx, "§6El arte de la lectura", cx, ty, 0xFFFFFFFF, W);
                ty = line(ctx, "En la Mesa, la carta de cada posición habla de algo: §fPasado§7 lo que heredás, §fPresente§7 lo que enfrentás, §fFuturo§7 lo que se acerca.", cx, ty, 0xFFCCCCCC, W);
                ty = line(ctx, "§a▲ Derecha§7: la carta muestra su luz. §c▼ Invertida§7: su sombra (cuesta 1 Polvo menos pero enfría 25% más lento, y empuja la profecía hacia el Peligro). ~25% de las cartas de los sobres salen invertidas.", cx, ty, 0xFFCCCCCC, W);
                ty = line(ctx, "§dLa carta del Futuro vuelve a vos. Llevala encima (inventario o Mazo) cuando se cumpla el augurio y te hallará §fpreparado§d: Fortuna da más botín, Peligro trae menos enemigos y un mini-jefe más débil, Encuentro trae mejores ofertas.", cx, ty, 0xFFCCCCCC, W);
                line(ctx, "§7Botón §fConsejo§7 de la Mesa: 1 carta en la ranura Futuro (1 amatista o 2 niveles) = consejo breve y bendición de 3 min. La lectura de 3 cartas añade combinaciones especiales (Torre+Muerte, Sol+Luna...) y un veredicto según cuántas salieron invertidas. Significados: capítulo 2.", cx, ty, 0xFFAAAAAA, W);
            }
            default -> {
                ty = line(ctx, "§6Altar Arcano §7(obsidiana, diamante, Polvo Estelar y una Mesa). Clic derecho con las 3 cartas-llave en el inventario (se consumen).", cx, ty, 0xFFCCCCCC, W);
                ty = line(ctx, "§5El Hierofante Corrompido §7→ La Torre + El Mago + El Sol. Suelta §eCorona Rota§7.", cx, ty, 0xFFDDDDDD, W);
                ty = line(ctx, "§9La Sombra de la Luna §7(de noche) → La Estrella + El Loco + El Carro. Suelta §bLágrima Astral§7.", cx, ty, 0xFFDDDDDD, W);
                ty = line(ctx, "§eEl Juicio Final §7→ La Rueda + La Estrella + La Emperatriz + 3 Páginas de Expansión. Suelta §dCorazón del Destino§7.", cx, ty, 0xFFDDDDDD, W);
                line(ctx, "§7Ascensión: en la Mesa, 2 cartas iguales + Polvo + §bEsquirla§7 = Astral. 2 Astrales + Polvo + material de jefe = §6Cósmica§7 (Corona: Peligro · Lágrima: Fortuna · Corazón: Encuentro). Las Cósmicas abren los nodos finales del Árbol (Z).", cx, ty, 0xFFAAAAAA, W);
            }
        }
    }
}
