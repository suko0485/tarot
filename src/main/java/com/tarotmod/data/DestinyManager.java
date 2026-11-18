package com.tarotmod.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tarotmod.TarotMod;
import com.tarotmod.card.Fx;
import com.tarotmod.item.DestinyDeckItem;
import com.tarotmod.item.ModItems;
import com.tarotmod.item.TarotCardItem;
import com.tarotmod.net.SyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Puntos de Destino, colección y árbol de habilidades. Persistencia en <mundo>/tarot_data.json + sincronización al cliente. */
public final class DestinyManager {
    private DestinyManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, PlayerData> DATA = new HashMap<>();

    public static PlayerData get(ServerPlayerEntity p) { return DATA.computeIfAbsent(p.getUuid(), k -> new PlayerData()); }

    // ------------------------------------------------------------- persistencia
    private static Path file(MinecraftServer s) { return s.getSavePath(WorldSavePath.ROOT).resolve("tarot_data.json"); }

    public static void load(MinecraftServer s) {
        DATA.clear();
        try {
            Path f = file(s);
            if (Files.exists(f)) {
                Map<String, PlayerData> m = GSON.fromJson(Files.readString(f), new TypeToken<Map<String, PlayerData>>() {}.getType());
                if (m != null) m.forEach((k, v) -> DATA.put(UUID.fromString(k), v));
            }
        } catch (Exception e) { TarotMod.LOGGER.warn("No se pudo leer tarot_data.json", e); }
    }

    public static void save(MinecraftServer s) {
        try {
            Map<String, PlayerData> m = new HashMap<>();
            DATA.forEach((k, v) -> m.put(k.toString(), v));
            Files.writeString(file(s), GSON.toJson(m));
        } catch (Exception e) { TarotMod.LOGGER.warn("No se pudo guardar tarot_data.json", e); }
    }

    // ------------------------------------------------------------- sincronización
    public static void sync(ServerPlayerEntity p) {
        PlayerData d = get(p);
        String s = d.points + ";" + String.join(",", d.cards) + ";" + String.join(",", d.nodes) + ";" + d.cosmic;
        ServerPlayNetworking.send(p, new SyncPayload(s));
    }

    // ------------------------------------------------------------- puntos y logros
    public static void addPoints(ServerPlayerEntity p, int n, String why) {
        get(p).points += n;
        p.sendMessage(Text.literal("§d✦ +" + n + " Punto" + (n > 1 ? "s" : "") + " de Destino §7(" + why + ")  §8[Z]"), true);
        sync(p);
    }

    public static void grant(ServerPlayerEntity p, String advId) {
        try {
            MinecraftServer s = ((ServerWorld) p.getEntityWorld()).getServer();
            var entry = s.getAdvancementLoader().get(TarotMod.id(advId));
            if (entry != null) p.getAdvancementTracker().grantCriterion(entry, "done");
        } catch (Exception e) { TarotMod.LOGGER.debug("Logro {} no otorgado: {}", advId, e.toString()); }
    }

    /** Registra una carta en la colección. Devuelve true si era nueva. */
    public static boolean discover(ServerPlayerEntity p, String cardId) {
        PlayerData d = get(p);
        if (!d.cards.add(cardId)) return false;
        int n = d.cards.size();
        ServerWorld w = (ServerWorld) p.getEntityWorld();
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.5f);
        w.spawnParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 40, .6, .8, .6, .4);
        p.sendMessage(Text.literal("§6✦ Carta registrada en tu colección: §e" + n + "/22"), false);
        addPoints(p, 1, "carta nueva");
        grant(p, "first_card");
        if (n >= 5) grant(p, "apprentice");
        String bonus = switch (n) {
            case 5 -> "§a+1 ranura de pasiva activa en el Mazo del Destino";
            case 10 -> "§a+2 espacios de almacenamiento en el Mazo";
            case 15 -> "§a¡Energía arcana! Regenerás Polvo Estelar con una carta en mano";
            case 22 -> "§6¡Colección completa! +4 espacios en el Mazo y +5 Puntos de Destino";
            default -> null;
        };
        if (bonus != null) {
            p.sendMessage(Text.literal("§5Recompensa de colección (" + n + " cartas): " + bonus), false);
            if (n == 22) addPoints(p, 5, "colección completa");
        }
        sync(p);
        return true;
    }

    // ------------------------------------------------------------- árbol
    public static void tryUnlock(ServerPlayerEntity p, String nodeId) {
        PlayerData d = get(p);
        Nodes.Node n = Nodes.get(nodeId);
        if (n == null || d.has(nodeId)) return;
        if (d.points < n.cost()) { p.sendMessage(Text.literal("§cNecesitás " + n.cost() + " Puntos de Destino."), true); return; }
        for (String par : n.parents()) if (!d.has(par)) { p.sendMessage(Text.literal("§cDesbloqueá primero el nodo anterior."), true); return; }
        if (n.cosmic() && d.cosmic < 1) { p.sendMessage(Text.literal("§cRequiere haber forjado una carta Cósmica."), true); return; }
        d.points -= n.cost();
        d.nodes.add(nodeId);
        ServerWorld w = (ServerWorld) p.getEntityWorld();
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.9f);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, .6f, 1.4f);
        Fx.ring(w, ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 1.2, 24);
        p.sendMessage(Text.literal("§d✦ Nodo desbloqueado: §f" + n.name()), false);
        sync(p);
    }

    // ------------------------------------------------------------- tick (1 vez por segundo por jugador)
    public static void secondTick(ServerWorld w, ServerPlayerEntity p, int serverTick) {
        PlayerData d = get(p);
        // 1) descubrir cartas que lleva encima (inventario o dentro de un mazo)
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.getItem() instanceof TarotCardItem c) discover(p, c.card().id());
            else if (s.getItem() instanceof DestinyDeckItem) for (ItemStack in : DestinyDeckItem.cards(s))
                if (in.getItem() instanceof TarotCardItem c) discover(p, c.card().id());
        }
        // 2) Piel de Piedra: los efectos negativos expiran más rápido
        if (d.has("sur_1")) {
            for (StatusEffectInstance e : new ArrayList<>(p.getStatusEffects())) {
                if (e.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL && e.getDuration() > 40 && !e.isInfinite()) {
                    p.removeStatusEffect(e.getEffectType());
                    p.addStatusEffect(new StatusEffectInstance(e.getEffectType(), e.getDuration() - 20, e.getAmplifier(), e.isAmbient(), e.shouldShowParticles(), e.shouldShowIcon()));
                }
            }
        }
        // 3) Sentido del Tesoro: destello cada 20 s
        if (d.has("seer_3") && serverTick % 400 == 0) revealChests(w, p, 20);
        // 4) Energía arcana (15 cartas): 1 Polvo Estelar cada 2 minutos sosteniendo una carta
        if (d.arcaneEnergy() && serverTick % 2400 == 0
                && (p.getMainHandStack().getItem() instanceof TarotCardItem || p.getOffHandStack().getItem() instanceof TarotCardItem)) {
            p.getInventory().offerOrDrop(new ItemStack(ModItems.STARDUST));
            p.sendMessage(Text.literal("§b✧ Tu energía arcana cristaliza Polvo Estelar."), true);
        }
        // 5) Escudo Arcano recarga mensaje se maneja en el evento de daño (CardEvents)
        if (serverTick % 200 == 0) sync(p);
    }

    public static void revealChests(ServerWorld w, ServerPlayerEntity p, int r) {
        List<BlockPos> chests = Fx.findChests(w, p.getBlockPos(), r);
        for (BlockPos c : chests) {
            Vec3d v = Vec3d.ofCenter(c);
            Fx.column(w, ParticleTypes.END_ROD, v.x, v.y + .6, v.z, 2.5, 14);
            w.spawnParticles(ParticleTypes.ENCHANT, v.x, v.y + 1, v.z, 12, .3, .4, .3, .5);
        }
        if (!chests.isEmpty()) {
            Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, 1f, 1.7f);
            p.sendMessage(Text.literal("§e✧ Un destello revela " + chests.size() + " cofre(s) cercano(s)."), true);
        }
    }
}
