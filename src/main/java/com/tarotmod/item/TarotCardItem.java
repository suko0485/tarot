package com.tarotmod.item;

import com.tarotmod.card.TarotCard;
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

    /** Habilidad ACTIVA: clic derecho con la carta en la mano. */
    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!(world instanceof ServerWorld sw) || !(player instanceof ServerPlayerEntity sp)) {
            return ActionResult.SUCCESS;
        }
        if (player.getItemCooldownManager().isCoolingDown(stack)) return ActionResult.FAIL;
        int lvl = level(stack);
        card.active().run(sw, sp, lvl);
        player.getItemCooldownManager().set(stack, (int) (card.cooldownTicks() / (1.0 + 0.25 * lvl)));
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display,
                              Consumer<Text> out, TooltipType type) {
        int lvl = level(stack);
        out.accept(Text.literal(card.numeral() + " · Carta " + TarotCard.LEVEL_NAMES[lvl]).formatted(Formatting.GOLD));
        out.accept(Text.translatable("tooltip.tarot." + card.id() + ".active").formatted(Formatting.GRAY));
        out.accept(Text.translatable("tooltip.tarot." + card.id() + ".passive").formatted(Formatting.DARK_PURPLE));
        out.accept(Text.literal("Pasiva: ponela en la mano secundaria").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
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
