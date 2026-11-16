package com.tarotmod.screen;

import com.tarotmod.block.ModBlocks;
import com.tarotmod.card.TarotCard;
import com.tarotmod.item.ModItems;
import com.tarotmod.item.TarotCardItem;
import com.tarotmod.prophecy.ProphecyManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * Slots: 0 = Pasado, 1 = Presente, 2 = Futuro (también salida de la fusión), 3 = combustible (amatista / polvo estelar).
 * Botón 0 = Tirada del Futuro · Botón 1 = Fusionar / Mejorar.
 */
public class TarotTableScreenHandler extends ScreenHandler {
    public static final int BTN_READ = 0;
    public static final int BTN_FUSE = 1;
    public static final int XP_COST_LEVELS = 5;

    private final Inventory inventory = new SimpleInventory(4) {
        @Override public void markDirty() { super.markDirty(); TarotTableScreenHandler.this.onContentChanged(this); }
    };
    private final ScreenHandlerContext context;

    /** Constructor cliente. */
    public TarotTableScreenHandler(int syncId, PlayerInventory playerInv) {
        this(syncId, playerInv, ScreenHandlerContext.EMPTY);
    }

    public TarotTableScreenHandler(int syncId, PlayerInventory playerInv, ScreenHandlerContext context) {
        super(ModScreens.TABLE, syncId);
        this.context = context;

        for (int i = 0; i < 3; i++) {
            this.addSlot(new Slot(inventory, i, 44 + i * 36, 20) {
                @Override public boolean canInsert(ItemStack s) { return s.getItem() instanceof TarotCardItem; }
                @Override public int getMaxItemCount() { return 1; }
            });
        }
        this.addSlot(new Slot(inventory, 3, 80, 46) {
            @Override public boolean canInsert(ItemStack s) { return s.isOf(Items.AMETHYST_SHARD) || s.isOf(ModItems.STARDUST); }
        });

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity sp)) return false;
        if (id == BTN_READ) { doReading(sp); return true; }
        if (id == BTN_FUSE) { doFusion(sp); return true; }
        return false;
    }

    // ------------------------------------------------------------ Tirada del Futuro
    private void doReading(ServerPlayerEntity player) {
        TarotCardItem[] cards = new TarotCardItem[3];
        int[] levels = new int[3];
        for (int i = 0; i < 3; i++) {
            ItemStack s = inventory.getStack(i);
            if (!(s.getItem() instanceof TarotCardItem c)) {
                player.sendMessage(Text.literal("§cColocá 3 cartas: Pasado, Presente y Futuro."), true);
                return;
            }
            cards[i] = c;
            levels[i] = TarotCardItem.level(s);
        }

        ItemStack fuel = inventory.getStack(3);
        if (fuel.isOf(Items.AMETHYST_SHARD)) {
            fuel.decrement(1);
        } else if (player.isCreative() || player.experienceLevel >= XP_COST_LEVELS) {
            if (!player.isCreative()) player.addExperienceLevels(-XP_COST_LEVELS);
        } else {
            player.sendMessage(Text.literal("§cNecesitás 1 amatista o " + XP_COST_LEVELS + " niveles de experiencia."), true);
            return;
        }

        double[] w = new double[TarotCard.Aspect.values().length];
        java.util.Arrays.fill(w, 0.5);
        double[] pos = {1.0, 1.5, 1.2}; // pasado, presente, futuro
        int power = 0;
        for (int i = 0; i < 3; i++) {
            w[cards[i].card().aspect().ordinal()] += pos[i] * (1 + levels[i]);
            power += levels[i];
        }
        ProphecyManager.Prophecy chosen = ProphecyManager.pick(player.getRandom(), w);
        ProphecyManager.schedule(player, chosen, power);

        for (int i = 0; i < 3; i++) inventory.setStack(i, ItemStack.EMPTY);
        inventory.markDirty();
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.6f);
    }

    // ------------------------------------------------------------ Fusión
    private void doFusion(ServerPlayerEntity player) {
        ItemStack a = inventory.getStack(0), b = inventory.getStack(1);
        if (!(a.getItem() instanceof TarotCardItem ca) || a.getItem() != b.getItem()) {
            player.sendMessage(Text.literal("§cPonés 2 cartas idénticas en Pasado y Presente."), true);
            return;
        }
        int la = TarotCardItem.level(a), lb = TarotCardItem.level(b);
        if (la != lb || la >= TarotCard.MAX_LEVEL) {
            player.sendMessage(Text.literal("§cDeben ser del mismo nivel (y no Cósmicas)."), true);
            return;
        }
        if (!inventory.getStack(2).isEmpty()) {
            player.sendMessage(Text.literal("§cVaciá la ranura Futuro: ahí sale la carta fusionada."), true);
            return;
        }
        int need = (la + 1) * 2;
        ItemStack fuel = inventory.getStack(3);
        if (!fuel.isOf(ModItems.STARDUST) || fuel.getCount() < need) {
            player.sendMessage(Text.literal("§cNecesitás " + need + " Polvos Estelares."), true);
            return;
        }
        fuel.decrement(need);
        inventory.setStack(0, ItemStack.EMPTY);
        inventory.setStack(1, ItemStack.EMPTY);
        inventory.setStack(2, TarotCardItem.withLevel(new ItemStack(ca), la + 1));
        inventory.markDirty();
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.8f);
        player.sendMessage(Text.literal("§6¡Carta " + TarotCard.LEVEL_NAMES[la + 1] + " forjada!"), true);
    }

    // ------------------------------------------------------------ Boilerplate
    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < 4) {
                if (!this.insertItem(stack, 4, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.insertItem(stack, 0, 4, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty();
        }
        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(this.context, player, ModBlocks.TAROT_TABLE);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.context.run((world, pos) -> this.dropInventory(player, this.inventory));
    }
}
