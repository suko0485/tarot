package com.tarotmod.item;

import com.tarotmod.card.CardAbilities;
import com.tarotmod.card.TarotCard;
import com.tarotmod.data.DestinyManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplay;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class TarotCardItem extends Item {
    private final TarotCard card;

    public TarotCardItem(Settings settings, TarotCard card) {
        super(settings);
        this.card = card;
    }

    public TarotCard card() { return card; }

    public static int level(ItemStack stack) {
        return Math.max(0, Math.min(TarotCard.MAX_LEVEL, stack.getOrDefault(ModComponents.CARD_LEVEL, 0)));
    }

    public static boolean reversed(ItemStack stack) { return stack.getOrDefault(ModComponents.REVERSED, false); }

    public static ItemStack asReversed(ItemStack stack, boolean rev) {
        ItemStack s = stack.copy();
        if (rev) s.set(ModComponents.REVERSED, true);
        return s;
    }

    /** Habilidad ACTIVA: clic derecho. Cuesta Polvo Estelar (o niveles de XP) y tiene un cooldown visible en el ítem. */
    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!(world instanceof ServerWorld sw) || !(player instanceof ServerPlayerEntity sp)) return ActionResult.SUCCESS;
        if (player.getItemCooldownManager().isCoolingDown(stack)) return ActionResult.FAIL;
        boolean rev = reversed(stack);
        int cost = rev ? Math.max(1, card.cost() - 1) : card.cost();
        if (!CardAbilities.canPay(sp, cost)) {
            sp.sendMessage(Text.literal("§cNecesitás " + cost + " Polvo Estelar (o " + cost * 3 + " niveles de XP)."), true);
            return ActionResult.FAIL;
        }
        int lvl = level(stack);
        if (!card.active().cast(sw, sp, lvl)) return ActionResult.FAIL;
        CardAbilities.charge(sp, cost);
        int cd = (int) (card.cooldownTicks() * (rev ? 1.25 : 1.0) * DestinyManager.get(sp).cdMult() / (1.0 + 0.2 * lvl));
        player.getItemCooldownManager().set(stack, cd);
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display,
                              Consumer<Text> out, TooltipType type) {
        int lvl = level(stack);
        boolean rev = reversed(stack);
        out.accept(Text.literal(card.numeral() + " · Carta " + TarotCard.LEVEL_NAMES[lvl] + (rev ? " · ▼ Invertida" : "")).formatted(Formatting.GOLD));
        var mn = com.tarotmod.card.Meanings.of(card.id());
        out.accept(Text.literal((rev ? "▼ " + mn.rev() : "▲ " + mn.up())).formatted(Formatting.YELLOW, Formatting.ITALIC));
        out.accept(Text.translatable("tooltip.tarot." + card.id() + ".active").formatted(Formatting.GRAY));
        out.accept(Text.translatable("tooltip.tarot." + card.id() + ".passive").formatted(Formatting.DARK_PURPLE));
        out.accept(Text.literal("Costo: " + (rev ? Math.max(1, card.cost() - 1) : card.cost()) + " Polvo Estelar · Enfriamiento: " + (int) (card.cooldownTicks() * (rev ? 1.25 : 1.0) / 20) + " s").formatted(Formatting.AQUA));
        out.accept(Text.literal("Pasiva: mano secundaria o ranuras activas del Mazo").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }

    /** Sube de nivel una copia de la carta (con brillo). */
    public static ItemStack withLevel(ItemStack base, int level) {
        ItemStack s = base.copy();
        s.setCount(1);
        s.set(ModComponents.CARD_LEVEL, level);
        if (level > 0) s.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }
}
