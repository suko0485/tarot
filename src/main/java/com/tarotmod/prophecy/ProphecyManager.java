package com.tarotmod.prophecy;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Profecías: se programan al hacer una Tirada y se cumplen entre 1 y 10 minutos después.
 * Nota: están en memoria (se pierden si se reinicia el servidor). Para persistirlas usar Fabric Data Attachments.
 */
public final class ProphecyManager {
    private ProphecyManager() {}

    public enum Prophecy { FORTUNE, DANGER, ENCOUNTER }

    private record Pending(Prophecy prophecy, int dueTick, int power) {}

    private static final Map<UUID, List<Pending>> PENDING = new ConcurrentHashMap<>();

    public static Prophecy pick(Random rnd, double[] weights) {
        double total = 0;
        for (double w : weights) total += w;
        double roll = rnd.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) return Prophecy.values()[i];
        }
        return Prophecy.FORTUNE;
    }

    public static void schedule(ServerPlayerEntity player, Prophecy prophecy, int power) {
        MinecraftServer server = ((ServerWorld) player.getEntityWorld()).getServer();
        int delay = 1200 + player.getRandom().nextInt(10 * 1200 - 1200); // 1 a 10 min
        PENDING.computeIfAbsent(player.getUuid(), k -> new ArrayList<>())
                .add(new Pending(prophecy, server.getTicks() + delay, power));
        String hint = switch (prophecy) {
            case FORTUNE -> "Ves un resplandor dorado entre la tierra...";
            case DANGER -> "Ves sombras que se agolpan y un cielo que se oscurece...";
            case ENCOUNTER -> "Ves un camino lejano y una figura que lo recorre...";
        };
        player.sendMessage(Text.literal("§5La mesa susurra: §d" + hint), false);
        player.sendMessage(Text.literal("§7El destino se cumplirá dentro de los próximos 10 minutos."), false);
    }

    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        int now = server.getTicks();
        Iterator<Map.Entry<UUID, List<Pending>>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(entry.getKey());
            if (p == null) continue; // se pospone hasta que vuelva a conectarse
            entry.getValue().removeIf(pen -> {
                if (now < pen.dueTick()) return false;
                fulfil((ServerWorld) p.getEntityWorld(), p, pen);
                return true;
            });
            if (entry.getValue().isEmpty()) it.remove();
        }
    }

    // ------------------------------------------------------------------ cumplimiento
    private static void fulfil(ServerWorld w, ServerPlayerEntity p, Pending pen) {
        switch (pen.prophecy()) {
            case FORTUNE -> fortune(w, p, pen.power());
            case DANGER -> danger(w, p, pen.power());
            case ENCOUNTER -> encounter(w, p, pen.power());
        }
    }

    private static BlockPos surfaceAround(ServerWorld w, ServerPlayerEntity p, int min, int max) {
        Random r = w.getRandom();
        for (int i = 0; i < 16; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double d = min + r.nextDouble() * (max - min);
            int x = (int) Math.floor(p.getX() + Math.cos(ang) * d);
            int z = (int) Math.floor(p.getZ() + Math.sin(ang) * d);
            int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (w.getFluidState(pos.down()).isEmpty() && !w.getBlockState(pos.down()).isAir() && w.getWorldBorder().contains(pos)) {
                return pos;
            }
        }
        return p.getBlockPos();
    }

    private static void fortune(ServerWorld w, ServerPlayerEntity p, int power) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 20 * 60 * 5, 2 + power / 3));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20 * 60 * 3, 1));
        if (w.getRandom().nextFloat() < 0.65f) {
            BlockPos surface = surfaceAround(w, p, 8, 18);
            BlockPos chestPos = surface.down(3);
            w.setBlockState(chestPos, Blocks.CHEST.getDefaultState());
            if (w.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                Random r = w.getRandom();
                chest.setStack(0, new ItemStack(Items.DIAMOND, 1 + r.nextInt(2 + power)));
                chest.setStack(1, new ItemStack(Items.GOLD_INGOT, 4 + r.nextInt(8)));
                chest.setStack(2, new ItemStack(Items.EMERALD, 3 + r.nextInt(6)));
                chest.setStack(3, new ItemStack(Items.AMETHYST_SHARD, 2 + r.nextInt(6)));
                chest.setStack(4, new ItemStack(com.tarotmod.item.ModItems.BOOSTER_PACK, 1 + (power >= 3 ? 1 : 0)));
                chest.setStack(5, new ItemStack(com.tarotmod.item.ModItems.STARDUST, 2 + r.nextInt(4)));
            }
            w.spawnParticles(ParticleTypes.END_ROD, surface.getX() + .5, surface.getY() + 1, surface.getZ() + .5, 80, .3, 2.5, .3, .02);
            int dx = chestPos.getX() - p.getBlockPos().getX(), dz = chestPos.getZ() - p.getBlockPos().getZ();
            String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "Este" : "Oeste") : (dz > 0 ? "Sur" : "Norte");
            p.sendMessage(Text.literal("§6¡La fortuna llega! §eUn tesoro enterrado a ~" + (int) Math.sqrt(dx * dx + dz * dz)
                    + " bloques al " + dir + " (buscá la columna de luz)."), false);
        } else {
            p.sendMessage(Text.literal("§6¡La fortuna llega! §eLa suerte te acompañará durante varios minutos al minar."), false);
        }
    }

    private static void danger(ServerWorld w, ServerPlayerEntity p, int power) {
        Random r = w.getRandom();
        if (r.nextBoolean()) {
            p.sendMessage(Text.literal("§4¡El cielo se rompe! §cUna tormenta eléctrica cae sobre vos."), false);
            for (int i = 0; i < 3 + power; i++) {
                BlockPos s = surfaceAround(w, p, 4, 10);
                LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w, SpawnReason.TRIGGERED);
                if (bolt != null) { bolt.setPosition(s.getX() + .5, s.getY(), s.getZ() + .5); w.spawnEntity(bolt); }
            }
            w.setWeather(0, 20 * 60 * 2, true, true);
        } else {
            p.sendMessage(Text.literal("§4¡Emboscada! §cLas sombras del destino te rodean."), false);
            int n = 3 + power + r.nextInt(2);
            for (int i = 0; i < n; i++) {
                BlockPos s = surfaceAround(w, p, 5, 9);
                EntityType<? extends MobEntity> type = (i % 2 == 0) ? EntityType.ZOMBIE : EntityType.SKELETON;
                MobEntity mob = type.create(w, SpawnReason.EVENT);
                if (mob == null) continue;
                mob.setPosition(s.getX() + .5, s.getY(), s.getZ() + .5);
                mob.setCustomName(Text.literal("Sombra del Destino"));
                mob.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET)); // no se queman al sol
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 60, 0));
                mob.setTarget(p);
                w.spawnEntity(mob);
            }
        }
    }

    private static void encounter(ServerWorld w, ServerPlayerEntity p, int power) {
        BlockPos s = surfaceAround(w, p, 6, 12);
        WanderingTraderEntity trader = EntityType.WANDERING_TRADER.create(w, SpawnReason.EVENT);
        if (trader == null) return;
        trader.setPosition(s.getX() + .5, s.getY(), s.getZ() + .5);
        trader.setCustomName(Text.literal("§dEl Vidente Errante"));
        trader.setCustomNameVisible(true);
        trader.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 60 * 3, 0));
        TraderOffers.addExclusiveOffers(trader, power);
        w.spawnEntity(trader);
        w.spawnParticles(ParticleTypes.END_ROD, s.getX() + .5, s.getY() + 1, s.getZ() + .5, 60, .3, 1.5, .3, .02);
        p.sendMessage(Text.literal("§d¡Un encuentro! §5Un mercader místico apareció cerca con intercambios exclusivos."), false);
    }
}
