package com.tarotmod.prophecy;

import com.tarotmod.boss.BossManager;
import com.tarotmod.card.Fx;
import com.tarotmod.card.Sched;
import com.tarotmod.data.DestinyManager;
import com.tarotmod.data.PlayerData;
import com.tarotmod.item.ModItems;
import com.tarotmod.item.TarotCardItem;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Profecías. Se programan al hacer una Tirada y se cumplen a los 2-5 minutos. El estado vive en PlayerData
 * (persistente: sobrevive a reinicios) y el temporizador solo corre mientras el jugador está conectado.
 */
public final class ProphecyManager {
    private ProphecyManager() {}

    public enum Prophecy { FORTUNE, DANGER, ENCOUNTER }

    public static Prophecy pick(Random rnd, double[] weights) {
        double total = 0;
        for (double w : weights) total += w;
        double roll = rnd.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) { roll -= weights[i]; if (roll < 0) return Prophecy.values()[i]; }
        return Prophecy.FORTUNE;
    }

    private static void title(ServerPlayerEntity p, String big, String sub) {
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(15, 70, 25));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(sub)));
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(big)));
    }

    public static void schedule(ServerPlayerEntity p, Prophecy prophecy, int power, int futureIdx, boolean reversed) {
        ServerWorld w = (ServerWorld) p.getEntityWorld();
        int delay = 2400 + p.getRandom().nextInt(3600 + 1); // 2 a 5 minutos
        DestinyManager.get(p).prophecies.add(new int[]{prophecy.ordinal(), delay, power, futureIdx, reversed ? 1 : 0});
        String[] hint = switch (prophecy) {
            case FORTUNE -> new String[]{"§6✦ Augurio de Fortuna", "§eUn resplandor dorado se agita bajo la tierra..."};
            case DANGER -> new String[]{"§4☠ Augurio de Peligro", "§cLas sombras se agolpan. El cielo se oscurece..."};
            case ENCOUNTER -> new String[]{"§d☾ Augurio de Encuentro", "§5Una figura recorre un camino lejano hacia vos..."};
        };
        title(p, hint[0], hint[1]);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, .5f);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, 1f, .8f);
        Fx.ring(w, ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 1.5, 30);
        p.sendMessage(Text.literal("§7El destino se cumplirá en unos minutos... mantenete con vida."), false);
    }

    /** 1 vez por segundo por jugador: cuenta regresiva, tensión ambiental y cumplimiento. */
    public static void secondTick(ServerWorld w, ServerPlayerEntity p, PlayerData d) {
        if (d.prophecies.isEmpty()) return;
        List<int[]> due = new ArrayList<>();
        for (int[] pr : d.prophecies) { pr[1] -= 20; if (pr[1] <= 0) due.add(pr); }
        d.prophecies.removeAll(due);
        int soon = Integer.MAX_VALUE;
        for (int[] pr : d.prophecies) soon = Math.min(soon, pr[1]);
        if (soon != Integer.MAX_VALUE) {
            int sec = soon / 20;
            if (sec % 5 == 0 || sec <= 10) p.sendMessage(Text.literal("§8⌛ §7El destino se acerca... §8" + sec / 60 + ":" + String.format("%02d", sec % 60)), true);
            if (sec <= 10) { Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, .5f, .5f + (10 - sec) * .08f); w.spawnParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.5, p.getZ(), 10, .6, .6, .6, .6); }
        }
        for (int[] pr : due) {
            int idx = pr.length > 3 ? pr[3] : -1;
            boolean prepared = idx >= 0 && carries(p, idx);
            boolean rev = pr.length > 4 && pr[4] == 1;
            if (prepared) p.sendMessage(Text.literal("§d✦ Llevabas la carta del Futuro: el destino te encuentra preparado."), false);
            else if (idx >= 0) p.sendMessage(Text.literal("§7No llevabas la carta del Futuro contigo..."), false);
            fulfil(w, p, Prophecy.values()[pr[0]], pr[2], prepared, rev);
        }
    }

    private static BlockPos surfaceAround(ServerWorld w, ServerPlayerEntity p, int min, int max) {
        Random r = w.getRandom();
        for (int i = 0; i < 24; i++) {
            double ang = r.nextDouble() * Math.PI * 2, dist = min + r.nextDouble() * (max - min);
            int x = (int) Math.floor(p.getX() + Math.cos(ang) * dist), z = (int) Math.floor(p.getZ() + Math.sin(ang) * dist);
            int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (w.getFluidState(pos.down()).isEmpty() && !w.getBlockState(pos.down()).isAir() && w.getWorldBorder().contains(pos)) return pos;
        }
        return p.getBlockPos();
    }

    private static String where(ServerPlayerEntity p, BlockPos t) {
        int dx = t.getX() - p.getBlockPos().getX(), dz = t.getZ() - p.getBlockPos().getZ();
        String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "Este" : "Oeste") : (dz > 0 ? "Sur" : "Norte");
        return "~" + (int) Math.sqrt(dx * dx + dz * dz) + " bloques al " + dir;
    }

    /** ¿Lleva encima (inventario o dentro del Mazo) la carta del Futuro de la lectura? */
    private static boolean carries(ServerPlayerEntity p, int idx) {
        var list = new ArrayList<>(ModItems.CARDS.values());
        if (idx < 0 || idx >= list.size()) return false;
        TarotCardItem want = list.get(idx);
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack st = p.getInventory().getStack(i);
            if (st.getItem() == want) return true;
            if (st.getItem() instanceof com.tarotmod.item.DestinyDeckItem)
                for (ItemStack in : com.tarotmod.item.DestinyDeckItem.cards(st)) if (in.getItem() == want) return true;
        }
        return false;
    }

    private static void fulfil(ServerWorld w, ServerPlayerEntity p, Prophecy pr, int power, boolean prepared, boolean reversed) {
        DestinyManager.grant(p, "defy_fate");
        switch (pr) {
            case FORTUNE -> fortune(w, p, power, prepared);
            case DANGER -> danger(w, p, power, prepared, reversed);
            case ENCOUNTER -> encounter(w, p, power + (prepared ? 3 : 0));
        }
    }

    private static ItemStack rareCard(Random r) {
        List<TarotCardItem> pool = new ArrayList<>();
        for (TarotCardItem c : ModItems.CARDS.values()) if (c.card().rarity() == Rarity.RARE || c.card().rarity() == Rarity.EPIC) pool.add(c);
        return new ItemStack(pool.get(r.nextInt(pool.size())));
    }

    // ---------------------------------------------------------------- FORTUNA: meteorito de partículas + cofre
    private static void fortune(ServerWorld w, ServerPlayerEntity p, int power, boolean prepared) {
        title(p, "§6✦ ¡La fortuna llega!", "§eUn meteorito cruza el cielo...");
        BlockPos land = surfaceAround(w, p, 12, 22);
        Vec3d t = new Vec3d(land.getX() + .5, land.getY(), land.getZ() + .5);
        Fx.sound(w, t, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, .5f);
        Sched.repeat(40, 1, i -> {
            double f = 1.0 - i / 39.0, y = t.y + 45 * f, off = 18 * f;
            w.spawnParticles(ParticleTypes.FLAME, t.x - off, y, t.z - off * .5, 6, .3, .3, .3, .02);
            w.spawnParticles(ParticleTypes.END_ROD, t.x - off, y, t.z - off * .5, 3, .2, .2, .2, .05);
            w.spawnParticles(ParticleTypes.LAVA, t.x - off, y, t.z - off * .5, 1, .2, .2, .2, 0);
            if (i % 8 == 0) Fx.sound(w, t.x - off, y, t.z - off * .5, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.2f, .5f);
        });
        Sched.later(41, () -> {
            Fx.sound(w, t, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, .6f);
            w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, t.x, t.y + .5, t.z, 2, .5, .2, .5, 0);
            Fx.ring(w, ParticleTypes.FLAME, t.x, t.y + .3, t.z, 3, 30);
            w.setBlockState(land, Blocks.CHEST.getDefaultState());
            if (w.getBlockEntity(land) instanceof ChestBlockEntity chest) {
                Random r = w.getRandom();
                chest.setStack(0, new ItemStack(ModItems.DESTINY_SHARD, 1 + r.nextInt(2) + power / 3));
                chest.setStack(1, new ItemStack(Items.AMETHYST_SHARD, 6 + r.nextInt(8)));
                chest.setStack(2, rareCard(r));
                chest.setStack(3, new ItemStack(ModItems.STARDUST, 3 + r.nextInt(4)));
                chest.setStack(4, new ItemStack(ModItems.BOOSTER_PACK, 1 + (power >= 3 ? 1 : 0)));
                if ((power >= 4 && r.nextBoolean()) || prepared) chest.setStack(5, rareCard(r));
                if (prepared) chest.setStack(6, new ItemStack(ModItems.DESTINY_SHARD, 1));
            }
            Sched.repeat(60, 10, i -> Fx.column(w, ParticleTypes.END_ROD, t.x, t.y + 1, t.z, 8, 20));
            p.sendMessage(Text.literal("§6¡Un meteorito cayó a " + where(p, land) + "! §e(seguí la columna de luz)"), false);
            DestinyManager.addPoints(p, 1, "profecía de fortuna");
        });
    }

    // ---------------------------------------------------------------- PELIGRO: oscuridad + emboscada + mini-jefe
    private static void danger(ServerWorld w, ServerPlayerEntity p, int power, boolean prepared, boolean reversed) {
        com.tarotmod.card.SkyEvent.play(w, p, 120);
        title(p, "§4☠ ¡El cielo se oscurece!", "§cLos guardianes del presagio te rodean...");
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 160, 0, true, false));
        w.setWeather(0, 20 * 75, true, true);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WITHER_SPAWN, .6f, .5f);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, .6f);
        Sched.later(60, () -> {
            Random r = w.getRandom();
            int n = Math.max(1, 3 + power + r.nextInt(2) + (reversed && !prepared ? 2 : 0) - (prepared ? 2 : 0));
            for (int i = 0; i < n; i++) {
                BlockPos s = surfaceAround(w, p, 6, 11);
                EntityType<? extends MobEntity> type = (i % 2 == 0) ? EntityType.SKELETON : EntityType.ZOMBIE;
                MobEntity mob = type.create(w, SpawnReason.EVENT);
                if (mob == null) continue;
                mob.setPosition(s.getX() + .5, s.getY(), s.getZ() + .5);
                mob.setCustomName(Text.literal("§8Espectro Guardián"));
                mob.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 60, 0));
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 60, 0));
                mob.setTarget(p);
                w.spawnEntity(mob);
                w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, s.getX() + .5, s.getY() + 1, s.getZ() + .5, 25, .3, .8, .3, .05);
            }
            BlockPos s = surfaceAround(w, p, 8, 12);
            BossManager.spawnMini(w, p, new Vec3d(s.getX() + .5, s.getY(), s.getZ() + .5), prepared ? power - 3 : power);
            if (prepared) DestinyManager.addPoints(p, 1, "estabas preparado");
            p.sendMessage(Text.literal("§4¡Emboscada! §cVencé al §lGuardián del Presagio§r§c para reclamar su botín y Puntos de Destino."), false);
        });
    }

    // ---------------------------------------------------------------- ENCUENTRO: mercader místico temporal
    private static void encounter(ServerWorld w, ServerPlayerEntity p, int power) {
        title(p, "§d☾ Un viajero te encuentra", "§5Mercader místico: sus ofertas no durarán...");
        BlockPos s = surfaceAround(w, p, 6, 12);
        WanderingTraderEntity trader = EntityType.WANDERING_TRADER.create(w, SpawnReason.EVENT);
        if (trader == null) return;
        trader.setPosition(s.getX() + .5, s.getY(), s.getZ() + .5);
        trader.setCustomName(Text.literal("§dEl Vidente Errante"));
        trader.setCustomNameVisible(true);
        trader.setDespawnDelay(20 * 60 * 4); // 4 minutos
        trader.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 60 * 4, 0));
        TraderOffers.addExclusiveOffers(trader, power);
        w.spawnEntity(trader);
        Fx.sound(w, s.getX(), s.getY(), s.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, .8f);
        Sched.repeat(48, 5, i -> { if (trader.isAlive()) { Fx.ring(w, ParticleTypes.WITCH, trader.getX(), trader.getY() + .1, trader.getZ(), 1.2, 14); w.spawnParticles(ParticleTypes.PORTAL, trader.getX(), trader.getY() + 1, trader.getZ(), 8, .4, .8, .4, .3); } });
        p.sendMessage(Text.literal("§d¡Un encuentro! §5Un mercader místico está a " + where(p, s) + " (4 minutos)."), false);
        DestinyManager.addPoints(p, 1, "profecía de encuentro");
    }
}
