package com.tarotmod.boss;

import com.tarotmod.card.Fx;
import com.tarotmod.card.Sched;
import com.tarotmod.data.DestinyManager;
import com.tarotmod.item.ModItems;
import com.tarotmod.item.TarotCardItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Jefes del Altar Arcano (+ mini-jefe de las profecías de Peligro).
 * Cada uno: BossBar, 2 fases (a 50% de vida), ataques telegrafiados en el suelo (círculo rojo ~1.5 s antes del golpe),
 * invocación de esbirros y un drop exclusivo, indispensable para forjar cartas Cósmicas.
 */
public final class BossManager {
    private BossManager() {}

    private static final class Boss {
        final MobEntity mob; final ServerBossBar bar; final int kind; final String name;
        int phase = 1, cooldown = 60, attackCount = 0; final List<UUID> minions = new ArrayList<>();
        Boss(MobEntity m, ServerBossBar b, int k, String n) { mob = m; bar = b; kind = k; name = n; }
    }
    private static final Map<UUID, Boss> BOSSES = new HashMap<>();

    private record Def(String name, EntityType<? extends MobEntity> type, double hp, BossBar.Color color, boolean night, String[] cards, int pages) {}

    private static Def def(int id) {
        return switch (id) {
            case 0 -> new Def("El Hierofante Corrompido", EntityType.EVOKER, 260, BossBar.Color.PURPLE, false, new String[]{"tower", "magician", "sun"}, 0);
            case 1 -> new Def("La Sombra de la Luna", EntityType.ILLUSIONER, 280, BossBar.Color.BLUE, true, new String[]{"star", "fool", "chariot"}, 0);
            default -> new Def("El Juicio Final", EntityType.RAVAGER, 600, BossBar.Color.YELLOW, false, new String[]{"wheel", "star", "empress"}, 3);
        };
    }

    // ------------------------------------------------------------ ritual
    private static boolean take(ServerPlayerEntity p, Item item) {
        for (int i = 0; i < p.getInventory().size(); i++) { ItemStack s = p.getInventory().getStack(i); if (s.isOf(item)) { s.decrement(1); return true; } }
        return false;
    }
    private static int count(ServerPlayerEntity p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().size(); i++) { ItemStack s = p.getInventory().getStack(i); if (s.isOf(item)) n += s.getCount(); }
        return n;
    }

    public static void tryRitual(ServerWorld w, ServerPlayerEntity p, BlockPos altar) {
        for (int id = 2; id >= 0; id--) {
            Def d = def(id);
            boolean ok = true;
            for (String c : d.cards()) ok &= count(p, ModItems.CARDS.get(c)) > 0;
            if (id == 2) ok &= count(p, ModItems.EXPANSION_PAGE) >= d.pages();
            if (!ok) continue;
            if (d.night() && w.isDay()) { p.sendMessage(Text.literal("§9Esta invocación solo funciona de noche."), true); return; }
            for (String c : d.cards()) take(p, ModItems.CARDS.get(c));
            for (int i = 0; i < d.pages(); i++) take(p, ModItems.EXPANSION_PAGE);
            spawn(w, p, altar, id, d);
            return;
        }
        p.sendMessage(Text.literal("§5El altar permanece mudo. §7Reuní las tres cartas-llave de un jefe (ver Grimorio, capítulo 4)."), true);
        Fx.sound(w, altar.getX() + .5, altar.getY() + 1, altar.getZ() + .5, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, .8f, .5f);
    }

    private static void announce(ServerPlayerEntity p, String big, String sub) {
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 20));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(sub)));
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(big)));
    }

    private static MobEntity makeBoss(ServerWorld w, EntityType<? extends MobEntity> type, Vec3d at, String name, double hp) {
        MobEntity boss = type.create(w, SpawnReason.EVENT);
        if (boss == null) return null;
        boss.setPosition(at);
        boss.setCustomName(Text.literal(name));
        boss.setCustomNameVisible(true);
        boss.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(hp);
        boss.setHealth((float) hp);
        boss.setPersistent();
        w.spawnEntity(boss);
        return boss;
    }

    private static void spawn(ServerWorld w, ServerPlayerEntity p, BlockPos altar, int id, Def d) {
        Vec3d at = new Vec3d(altar.getX() + .5, altar.getY() + 1, altar.getZ() + .5);
        MobEntity boss = makeBoss(w, d.type(), at, d.name(), d.hp());
        if (boss == null) return;
        boss.setTarget(p);
        ServerBossBar bar = new ServerBossBar(Text.literal("§5" + d.name() + " §7· Fase 1"), d.color(), BossBar.Style.NOTCHED_10);
        bar.addPlayer(p);
        BOSSES.put(boss.getUuid(), new Boss(boss, bar, id, d.name()));
        Fx.sound(w, at, SoundEvents.ENTITY_WITHER_SPAWN, 1f, .7f);
        Fx.sound(w, at, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, .5f);
        for (int i = 0; i < 4; i++) Fx.ring(w, ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + i * .6, at.z, 1.5 + i * .5, 20);
        announce(p, "§4" + d.name(), "§cHa despertado...");
    }

    /** Mini-jefe del Augurio de Peligro. */
    public static void spawnMini(ServerWorld w, ServerPlayerEntity p, Vec3d at, int power) {
        String name = "Guardián del Presagio";
        MobEntity m = makeBoss(w, EntityType.WITHER_SKELETON, at, "§8" + name, 90 + power * 15);
        if (m == null) return;
        m.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        m.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, Integer.MAX_VALUE, 0, true, false));
        m.setTarget(p);
        ServerBossBar bar = new ServerBossBar(Text.literal("§8" + name), BossBar.Color.RED, BossBar.Style.PROGRESS);
        bar.addPlayer(p);
        BOSSES.put(m.getUuid(), new Boss(m, bar, 10, name));
    }

    // ------------------------------------------------------------ ataques telegrafiados
    /** Círculo rojo en el suelo durante `ticks`; al terminar daña a los jugadores que sigan dentro. */
    private static void telegraph(ServerWorld w, Boss b, Vec3d c, double r, int ticks, float dmg) {
        Fx.sound(w, c, SoundEvents.BLOCK_BEACON_POWER_SELECT, .8f, .6f);
        int steps = ticks / 2;
        Sched.repeat(steps, 2, i -> {
            Fx.ring(w, ParticleTypes.CRIMSON_SPORE, c.x, c.y + .12, c.z, r, (int) (r * 8));
            Fx.ring(w, ParticleTypes.FLAME, c.x, c.y + .12, c.z, r * (1.0 - (double) i / steps), (int) (r * 5));
        });
        Sched.later(ticks, () -> {
            Fx.sound(w, c, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, .9f, 1.2f);
            w.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y + .4, c.z, 2, r * .3, .1, r * .3, 0);
            Fx.ring(w, ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + .3, c.z, r, (int) (r * 10));
            for (ServerPlayerEntity pl : w.getPlayers(x -> x.squaredDistanceTo(c) <= r * r)) {
                pl.damage(w, w.getDamageSources().mobAttack(b.mob), dmg);
                Vec3d dir = new Vec3d(pl.getX() - c.x, 0, pl.getZ() - c.z); dir = dir.lengthSquared() < .01 ? Vec3d.ZERO : dir.normalize();
                pl.takeKnockback(.8, -dir.x, -dir.z);
            }
        });
    }

    private static List<ServerPlayerEntity> nearPlayers(ServerWorld w, MobEntity m, double r) {
        return w.getPlayers(x -> x.isAlive() && !x.isSpectator() && x.squaredDistanceTo(m) < r * r);
    }

    private static void minions(ServerWorld w, Boss b, int n) {
        b.minions.removeIf(u -> !(w.getEntity(u) instanceof LivingEntity le) || !le.isAlive());
        List<ServerPlayerEntity> pls = nearPlayers(w, b.mob, 40);
        for (int i = 0; i < n && b.minions.size() < 6; i++) {
            EntityType<? extends MobEntity> t = b.kind == 0 ? EntityType.VEX : (i % 2 == 0 ? EntityType.ZOMBIE : EntityType.SKELETON);
            MobEntity m = t.create(w, SpawnReason.EVENT);
            if (m == null) continue;
            double a = w.getRandom().nextDouble() * Math.PI * 2;
            m.setPosition(b.mob.getX() + Math.cos(a) * 3, b.mob.getY(), b.mob.getZ() + Math.sin(a) * 3);
            m.setCustomName(Text.literal("§8Esbirro del Destino"));
            m.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            if (!pls.isEmpty()) m.setTarget(pls.get(w.getRandom().nextInt(pls.size())));
            w.spawnEntity(m);
            b.minions.add(m.getUuid());
            w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, m.getX(), m.getY() + 1, m.getZ(), 20, .3, .6, .3, .05);
        }
        Fx.sound(w, Fx.pos(b.mob), SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.2f, .6f);
    }

    private static void attack(ServerWorld w, Boss b) {
        MobEntity m = b.mob;
        Vec3d c = Fx.pos(m);
        List<ServerPlayerEntity> pls = nearPlayers(w, m, 32);
        if (pls.isEmpty()) return;
        boolean p2 = b.phase == 2;
        float scale = b.kind == 10 ? .6f : 1f;
        int pick = b.attackCount++ % (p2 ? 5 : 3);
        switch (pick) {
            case 0 -> telegraph(w, b, c, b.kind == 2 ? 8 : 6.5, 30, 9f * scale);                       // GOLPE en torno al jefe
            case 1 -> { for (ServerPlayerEntity pl : pls) telegraph(w, b, Fx.pos(pl), 3, 30, 7f * scale); } // BOMBARDEO sobre cada jugador (se puede esquivar caminando)
            case 2 -> minions(w, b, b.kind == 10 ? 2 : 3);                                               // ESBIRROS
            case 3 -> { // ESPECIAL de fase 2, distinto por jefe
                switch (b.kind) {
                    case 0 -> { for (int i = 0; i < 6; i++) { double a = i * Math.PI / 3; telegraph(w, b, c.add(Math.cos(a) * 6, 0, Math.sin(a) * 6), 2.5, 34, 8f); } }  // pilares de energía
                    case 1 -> { telegraph(w, b, c, 11, 40, 6f); for (ServerPlayerEntity pl : pls) if (pl.squaredDistanceTo(m) < 121) { pl.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0)); } } // oscuridad en anillo
                    case 2 -> { for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; telegraph(w, b, c.add(Math.cos(a) * 9, 0, Math.sin(a) * 9), 3, 36, 9f); } }  // corona de fuego
                    default -> telegraph(w, b, c, 5, 24, 6f);
                }
            }
            default -> { for (ServerPlayerEntity pl : pls) { // BOMBARDEO anticipado: en la posición futura del jugador
                Vec3d v = pl.getVelocity(); telegraph(w, b, Fx.pos(pl).add(v.x * 14, 0, v.z * 14), 3, 28, 8f * scale); } }
        }
    }

    // ------------------------------------------------------------ tick
    public static void tick(MinecraftServer server) {
        if (BOSSES.isEmpty()) return;
        int t = server.getTicks();
        Iterator<Boss> it = BOSSES.values().iterator();
        while (it.hasNext()) {
            Boss b = it.next();
            if (b.mob.isRemoved()) { b.bar.clearPlayers(); it.remove(); continue; }
            if (!(b.mob.getEntityWorld() instanceof ServerWorld w)) continue;
            double frac = b.mob.getHealth() / b.mob.getMaxHealth();
            b.bar.setPercent((float) frac);
            if (t % 20 == 0) {
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    boolean near = p.getEntityWorld() == w && p.squaredDistanceTo(b.mob) < 64 * 64;
                    if (near) b.bar.addPlayer(p); else b.bar.removePlayer(p);
                }
                List<ServerPlayerEntity> pls = nearPlayers(w, b.mob, 40);
                if (!pls.isEmpty() && (b.mob.getTarget() == null || !b.mob.getTarget().isAlive())) b.mob.setTarget(pls.get(0));
                if (b.kind == 1) for (ServerPlayerEntity pl : pls) if (pl.squaredDistanceTo(b.mob) < 400) w.spawnParticles(ParticleTypes.SMOKE, pl.getX(), pl.getY() + 2, pl.getZ(), 3, .4, .2, .4, 0);
            }
            // transición a fase 2
            if (b.phase == 1 && frac < .5 && b.kind != 10) {
                b.phase = 2; b.cooldown = 30;
                b.bar.setName(Text.literal("§4" + b.name + " §c· Fase 2"));
                b.mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 0, true, false));
                b.mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 0, true, false));
                Fx.sound(w, Fx.pos(b.mob), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.2f, .6f);
                for (int i = 0; i < 5; i++) Fx.ring(w, ParticleTypes.SOUL_FIRE_FLAME, b.mob.getX(), b.mob.getY() + i * .5, b.mob.getZ(), 1 + i * .8, 24);
                for (ServerPlayerEntity pl : nearPlayers(w, b.mob, 48)) announce(pl, "§c¡FASE 2!", "§4" + b.name + " se enfurece");
                minions(w, b, 3);
            }
            if (t % 10 == 0) {
                b.cooldown -= 10;
                if (b.cooldown <= 0) { attack(w, b); b.cooldown = b.phase == 2 ? 50 : 80; }
            }
        }
    }

    // ------------------------------------------------------------ muerte y botín
    private static ItemStack rareCard(ServerWorld w) {
        List<TarotCardItem> pool = new ArrayList<>();
        for (TarotCardItem c : ModItems.CARDS.values()) if (c.card().rarity() == Rarity.RARE || c.card().rarity() == Rarity.EPIC) pool.add(c);
        return new ItemStack(pool.get(w.getRandom().nextInt(pool.size())));
    }
    private static void drop(ServerWorld w, LivingEntity e, ItemStack s) {
        w.spawnEntity(new ItemEntity(w, e.getX(), e.getY() + 1, e.getZ(), s));
    }

    public static void onDeath(ServerWorld w, LivingEntity e, DamageSource source) {
        Boss b = BOSSES.remove(e.getUuid());
        if (b == null) return;
        b.bar.clearPlayers();
        for (UUID u : b.minions) if (w.getEntity(u) instanceof LivingEntity le) le.discard();
        Fx.sound(w, Fx.pos(e), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.4f, .8f);
        Fx.sound(w, Fx.pos(e), SoundEvents.BLOCK_BEACON_ACTIVATE, 1.2f, 1.6f);
        for (int i = 0; i < 6; i++) Fx.ring(w, ParticleTypes.END_ROD, e.getX(), e.getY() + i * .5, e.getZ(), 1 + i * .5, 20);
        if (b.kind == 10) { // mini-jefe: cartas difíciles + puntos
            drop(w, e, rareCard(w)); drop(w, e, rareCard(w));
            drop(w, e, new ItemStack(ModItems.DESTINY_SHARD, 1 + w.getRandom().nextInt(2)));
            for (ServerPlayerEntity p : nearPlayers(w, b.mob, 48)) { DestinyManager.addPoints(p, 3, "sobrevivir al Peligro"); p.sendMessage(Text.literal("§6¡Venciste al Guardián del Presagio!"), false); }
            return;
        }
        // jefes del Altar: material exclusivo para forjar Cósmicas
        switch (b.kind) {
            case 0 -> { drop(w, e, new ItemStack(ModItems.BROKEN_CROWN, 2)); drop(w, e, new ItemStack(ModItems.EXPANSION_PAGE, 3)); }
            case 1 -> { drop(w, e, new ItemStack(ModItems.ASTRAL_TEAR, 2)); drop(w, e, new ItemStack(ModItems.EXPANSION_PAGE, 3)); }
            default -> { drop(w, e, new ItemStack(ModItems.DESTINY_HEART)); drop(w, e, new ItemStack(ModItems.ASTRAL_TEAR)); drop(w, e, new ItemStack(ModItems.BROKEN_CROWN)); }
        }
        drop(w, e, new ItemStack(ModItems.BOOSTER_PACK, 3));
        drop(w, e, new ItemStack(ModItems.DESTINY_SHARD, 2));
        drop(w, e, rareCard(w));
        for (ServerPlayerEntity p : nearPlayers(w, b.mob, 64)) {
            DestinyManager.addPoints(p, 5, "derrotar a " + b.name);
            DestinyManager.grant(p, "arcana_hunter");
            p.sendMessage(Text.literal("§6§l¡" + b.name + " ha caído!"), false);
        }
    }
}
