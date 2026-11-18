package com.tarotmod.item;

import com.tarotmod.data.DestinyManager;
import com.tarotmod.data.PlayerData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Mazo del Destino.
 *  - Clic derecho: roba una carta al azar.
 *  - Shift + clic derecho con una carta en la otra mano: la guarda al final del mazo.
 *  - Shift + clic derecho con la otra mano vacía: muestra qué cartas son tus pasivas activas.
 * Las primeras N cartas del mazo (N = ranuras activas, crece con la colección y el árbol) dan su pasiva sin sostenerlas.
 */
public class DestinyDeckItem extends Item {
    private static final int DRAW_COOLDOWN = 40;

    public DestinyDeckItem(Settings settings) { super(settings); }

    public static List<ItemStack> cards(ItemStack deck) {
        ContainerComponent c = deck.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        List<ItemStack> list = new ArrayList<>();
        c.streamNonEmpty().forEach(s -> list.add(s.copy()));
        return list;
    }

    private static void write(ItemStack deck, List<ItemStack> cards) {
        deck.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(cards));
    }

    public static int count(ItemStack deck) { return cards(deck).size(); }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack deck = player.getStackInHand(hand);
        if (!(world instanceof ServerWorld sw) || !(player instanceof ServerPlayerEntity sp)) return ActionResult.SUCCESS;
        if (player.getItemCooldownManager().isCoolingDown(deck)) return ActionResult.FAIL;

        PlayerData data = DestinyManager.get(sp);
        List<ItemStack> cards = cards(deck);
        Hand other = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack inOther = player.getStackInHand(other);

        if (player.isSneaking()) {
            if (inOther.getItem() instanceof TarotCardItem) {
                if (cards.size() >= data.deckCapacity()) {
                    sp.sendMessage(Text.literal("§cEl mazo está lleno (" + data.deckCapacity() + "). Ampliá con tu colección y el Árbol (Z)."), true);
                    return ActionResult.FAIL;
                }
                cards.add(inOther.copyWithCount(1));
                inOther.decrement(1);
                write(deck, cards);
                sp.sendMessage(Text.literal("§dCarta guardada (" + cards.size() + "/" + data.deckCapacity() + "). Pasivas activas: las primeras " + data.activeSlots() + "."), true);
                player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            } else {
                StringBuilder sb = new StringBuilder("§dPasivas activas: ");
                for (int i = 0; i < Math.min(data.activeSlots(), cards.size()); i++) sb.append(i > 0 ? ", " : "").append(cards.get(i).getName().getString());
                if (cards.isEmpty()) sb.append("(mazo vacío)");
                sp.sendMessage(Text.literal(sb.toString()), true);
                player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1f, 0.8f);
            }
            return ActionResult.SUCCESS;
        }

        if (cards.isEmpty()) { sp.sendMessage(Text.literal("§7El mazo está vacío."), true); return ActionResult.FAIL; }
        ItemStack drawn = cards.remove(sw.getRandom().nextInt(cards.size()));
        write(deck, cards);
        player.getInventory().offerOrDrop(drawn);
        player.getItemCooldownManager().set(deck, DRAW_COOLDOWN);
        player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1f, 1.4f);
        sp.sendMessage(Text.literal("§6Robás: ").append(drawn.getName()), true);
        return ActionResult.SUCCESS;
    }
}
