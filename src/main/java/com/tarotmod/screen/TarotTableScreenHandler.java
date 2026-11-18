package com.tarotmod.screen;

import com.tarotmod.block.ModBlocks;
import com.tarotmod.card.TarotCard;
import com.tarotmod.data.DestinyManager;
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
 * Slots: 0 = Pasado, 1 = Presente, 2 = Futuro (también salida de la fusión), 3 = combustible (amatista / polvo estelar), 4 = material de ascensión.
 * Botón 0 = Tirada del Futuro · Botón 1 = Fusionar / Mejorar.
 */
public class TarotTableScreenHandler extends ScreenHandler {
    public static final int BTN_READ = 0;
    public static final int BTN_FUSE = 1;
    public static final int BTN_ADVICE = 2;
    public static final int XP_COST_LEVELS = 5;

    private final Inventory inventory = new SimpleInventory(5) {
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
        this.addSlot(new Slot(inventory, 3, 66, 46) {
            @Override public boolean canInsert(ItemStack s) { return s.isOf(Items.AMETHYST_SHARD) || s.isOf(ModItems.STARDUST); }
        });
        // Ranura de material de ascensión: Esquirla del Destino (Astral) o material de jefe (Cósmica)
        this.addSlot(new Slot(inventory, 4, 94, 46) {
            @Override public boolean canInsert(ItemStack s) {
                return s.isOf(ModItems.DESTINY_SHARD) || s.isOf(ModItems.ASTRAL_TEAR) || s.isOf(ModItems.BROKEN_CROWN) || s.isOf(ModItems.DESTINY_HEART);
            }
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
        if (id == BTN_ADVICE) { doAdvice(sp); return true; }
        return false;
    }

    // ------------------------------------------------------------ Tirada del Futuro
    private void doReading(ServerPlayerEntity player) {
        TarotCardItem[] cards = new TarotCardItem[3];
        int[] levels = new int[3];
        boolean[] rev = new boolean[3];
        for (int i = 0; i < 3; i++) {
            ItemStack s = inventory.getStack(i);
            if (!(s.getItem() instanceof TarotCardItem c)) {
                player.sendMessage(Text.literal("§cColocá 3 cartas: Pasado, Presente y Futuro."), true);
                return;
            }
            cards[i] = c;
            levels[i] = TarotCardItem.level(s);
            rev[i] = TarotCardItem.reversed(s);
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
            if (rev[i]) w[TarotCard.Aspect.DANGER.ordinal()] += pos[i] * .8; // lo invertido empuja hacia el Peligro
            power += levels[i];
        }
        ProphecyManager.Prophecy chosen = ProphecyManager.pick(player.getRandom(), w);
        int futureIdx = new java.util.ArrayList<>(ModItems.CARDS.values()).indexOf(cards[2]);
        ProphecyManager.schedule(player, chosen, power, futureIdx, rev[2]);

        for (TarotCardItem tc : cards) {
            String id = tc.card().id();
            if (id.equals("devil") || id.equals("tower") || id.equals("death")) {
                com.tarotmod.card.SkyEvent.play((net.minecraft.server.world.ServerWorld) player.getEntityWorld(), player, 100); break;
            }
        }
        // Lectura interpretada: cada carta según su posición y orientación
        String[] pos2 = {"Pasado · heredás", "Presente · enfrentás", "Futuro · se acerca"};
        player.sendMessage(Text.literal("§5§l✦ Lectura de la Mesa ✦"), false);
        for (int i = 0; i < 3; i++) {
            var mn = com.tarotmod.card.Meanings.of(cards[i].card().id());
            player.sendMessage(Text.literal("§7" + pos2[i] + ": §e").append(Text.translatable("item.tarot.card_" + cards[i].card().id()))
                    .append(Text.literal((rev[i] ? " §c▼ " + mn.rev() : " §a▲ " + mn.up()))), false);
        }
        TarotCard[] defs = {cards[0].card(), cards[1].card(), cards[2].card()};
        for (String line : com.tarotmod.card.Readings.compose(defs, rev)) player.sendMessage(Text.literal("  " + line), false);
        player.sendMessage(Text.literal(rev[2] ? "§cConsejo: la carta del Futuro está invertida. Llevala contigo: te evitará lo peor."
                : "§dConsejo: la carta del Futuro vuelve a tus manos. Llevala contigo cuando el destino se cumpla y te hallará preparado."), false);

        // Pasado y Presente se consumen; la carta del Futuro vuelve (es la que podés llevar para estar preparado)
        ItemStack fut = inventory.getStack(2).copy();
        for (int i = 0; i < 3; i++) inventory.setStack(i, ItemStack.EMPTY);
        player.getInventory().offerOrDrop(fut);
        inventory.markDirty();
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.6f);
    }

    // ------------------------------------------------------------ Consejo (1 carta, en la ranura Futuro)
    private void doAdvice(ServerPlayerEntity player) {
        ItemStack s = inventory.getStack(2);
        if (!(s.getItem() instanceof TarotCardItem c)) {
            player.sendMessage(Text.literal("§cPoné una carta en la ranura Futuro para pedir un consejo."), true);
            return;
        }
        ItemStack fuel = inventory.getStack(3);
        if (fuel.isOf(Items.AMETHYST_SHARD)) fuel.decrement(1);
        else if (player.isCreative() || player.experienceLevel >= 2) { if (!player.isCreative()) player.addExperienceLevels(-2); }
        else { player.sendMessage(Text.literal("§cNecesitás 1 amatista o 2 niveles de experiencia."), true); return; }
        boolean rev = TarotCardItem.reversed(s);
        var mn = com.tarotmod.card.Meanings.of(c.card().id());
        player.sendMessage(Text.literal("§5✦ Consejo del tarot: §e").append(Text.translatable("item.tarot.card_" + c.card().id()))
                .append(Text.literal(rev ? " §c▼ " + mn.rev() : " §a▲ " + mn.up())), false);
        player.sendMessage(Text.literal("  " + com.tarotmod.card.Readings.advice(c.card(), rev)), false);
        // Bendición breve (3 min) según el aspecto; si está invertida, la "visión de la sombra"
        var eff = rev ? net.minecraft.entity.effect.StatusEffects.NIGHT_VISION : switch (c.card().aspect()) {
            case FORTUNE -> net.minecraft.entity.effect.StatusEffects.LUCK;
            case DANGER -> net.minecraft.entity.effect.StatusEffects.RESISTANCE;
            case ENCOUNTER -> net.minecraft.entity.effect.StatusEffects.SPEED;
        };
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(eff, 3600, 0, true, true));
        player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1f, 1.2f);
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, .8f, 1.0f);
        inventory.markDirty();
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
            player.sendMessage(Text.literal("§cNecesitás " + need + " Polvos Estelares en la ranura de combustible."), true);
            return;
        }
        // Material de ascensión: Normal->Astral pide Esquirla del Destino; Astral->Cósmica pide el drop de jefe según el aspecto
        net.minecraft.item.Item mat = la == 0 ? ModItems.DESTINY_SHARD : switch (ca.card().aspect()) {
            case FORTUNE -> ModItems.ASTRAL_TEAR;
            case DANGER -> ModItems.BROKEN_CROWN;
            case ENCOUNTER -> ModItems.DESTINY_HEART;
        };
        ItemStack material = inventory.getStack(4);
        if (!material.isOf(mat)) {
            String nm = la == 0 ? "una Esquirla del Destino" : switch (ca.card().aspect()) {
                case FORTUNE -> "una Lágrima Astral (Fortuna)";
                case DANGER -> "una Corona Rota (Peligro)";
                case ENCOUNTER -> "un Corazón del Destino (Encuentro)";
            };
            player.sendMessage(Text.literal("§cEsta fusión exige " + nm + " en la ranura de material."), true);
            return;
        }
        fuel.decrement(need);
        material.decrement(1);
        inventory.setStack(0, ItemStack.EMPTY);
        inventory.setStack(1, ItemStack.EMPTY);
        inventory.setStack(2, TarotCardItem.withLevel(new ItemStack(ca), la + 1));
        inventory.markDirty();
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.8f);
        player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
        player.sendMessage(Text.literal("§6¡Carta " + TarotCard.LEVEL_NAMES[la + 1] + " forjada!"), true);
        if (la + 1 == TarotCard.MAX_LEVEL) {
            DestinyManager.get(player).cosmic++;
            DestinyManager.grant(player, "cosmos_lord");
            DestinyManager.addPoints(player, 2, "carta Cósmica");
        }
    }

    // ------------------------------------------------------------ Boilerplate
    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < 5) {
                if (!this.insertItem(stack, 5, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.insertItem(stack, 0, 5, false)) {
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
