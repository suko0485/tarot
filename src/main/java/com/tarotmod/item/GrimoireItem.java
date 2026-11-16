package com.tarotmod.item;

import com.tarotmod.card.TarotCard;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Grimorio Arcano (versión ligera en chat; se puede migrar a Patchouli más adelante). */
public class GrimoireItem extends Item {
    public GrimoireItem(Settings settings) { super(settings); }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        if (!(world instanceof ServerWorld)) return ActionResult.SUCCESS;
        player.sendMessage(Text.literal("§5§l=== Grimorio Arcano ==="), false);
        player.sendMessage(Text.literal("§7• §fClic derecho con una carta: habilidad activa."), false);
        player.sendMessage(Text.literal("§7• §fCarta en la §6mano secundaria§f: habilidad pasiva."), false);
        player.sendMessage(Text.literal("§7• §fMazo del Destino: clic = robar · shift+clic = guardar la carta de la otra mano."), false);
        player.sendMessage(Text.literal("§7• §fMesa de Lectura: 3 cartas (Pasado/Presente/Futuro) + 1 amatista o 5 niveles de XP → profecía en ≤10 min."), false);
        player.sendMessage(Text.literal("§7• §fFusión: 2 cartas idénticas del mismo nivel + Polvo Estelar (2 / 4) → nivel superior."), false);
        player.sendMessage(Text.literal("§7• §fPolvo Estelar: amatista + polvo de piedra luminosa (x2)."), false);
        player.sendMessage(Text.literal("§6Cartas conocidas:"), false);
        com.tarotmod.card.TarotCards.ALL.values().forEach((TarotCard c) ->
                player.sendMessage(Text.literal("  §e" + c.numeral() + " §f" + c.id() + " §8(" + c.aspect().name().toLowerCase() + ")"), false));
        return ActionResult.SUCCESS;
    }
}
