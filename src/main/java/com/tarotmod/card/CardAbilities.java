package com.tarotmod.card;

import com.tarotmod.data.DestinyManager;
import com.tarotmod.data.PlayerData;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Los 22 Arcanos Mayores. Cada activa es una mecánica propia (telegrafiado, áreas, marcas, auras, cúpulas...),
 * no una poción genérica. level: 0 Normal, 1 Astral, 2 Cósmica (escala suave: x1.0 / x1.35 / x1.7).
 */
public final class CardAbilities {
    private CardAbilities() {}

    // ------------------------------------------------------------ estado temporal (se limpia solo al expirar)
    public static final Map<UUID, Integer> NO_FALL_UNTIL = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> REFLECT_UNTIL = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> PACT_UNTIL = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> WHEEL_UNTIL = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> SHIELD_READY = new ConcurrentHashMap<>();
    /** Marcas de Segador: víctima -> {tick de expiración} y dueño. */
    private record Mark(UUID owner, int until, int level) {}
    private static final Map<UUID, Mark> MARKS = new ConcurrentHashMap<>();

    // ------------------------------------------------------------ helpers
    static int now(ServerWorld w) { return w.getServer().getTicks(); }
    static double sc(int level) { return 1.0 + 0.35 * level; }
    static double radius(ServerPlayerEntity p, double base, int l) { return base * sc(l) * DestinyManager.get(p).radiusMult(); }
    static float dmg(ServerPlayerEntity p, double base, int l) { return (float) (base * sc(l) * DestinyManager.get(p).dmgMult()); }
    static float heal(ServerPlayerEntity p, double base, int l) { return (float) (base * sc(l) * DestinyManager.get(p).healMult()); }

    static void effect(LivingEntity e, RegistryEntry<StatusEffect> ef, int ticks, int amp) {
        e.addStatusEffect(new StatusEffectInstance(ef, ticks, amp, true, true));
    }
    static void passiveEffect(ServerPlayerEntity p, RegistryEntry<StatusEffect> e, int amp) {
        p.addStatusEffect(new StatusEffectInstance(e, 60, amp, true, false, true)); // se renueva cada segundo
    }

    /** ¿Es un enemigo legítimo? (hostil, o algo que te está atacando). Nunca jugadores, mascotas ni aldeanos. */
    static boolean isFoe(LivingEntity e, ServerPlayerEntity p) {
        if (e == p || !e.isAlive() || e instanceof PlayerEntity) return false;
        if (e instanceof TameableEntity t && t.isTamed()) return false;
        return e instanceof Monster || (e instanceof MobEntity m && m.getTarget() == p);
    }
    static List<LivingEntity> foes(ServerWorld w, Vec3d c, double r, ServerPlayerEntity p) {
        return w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(r), e -> isFoe(e, p) && e.squaredDistanceTo(c) <= r * r);
    }
    static List<LivingEntity> foes(ServerWorld w, ServerPlayerEntity p, double r) { return foes(w, Fx.pos(p), r, p); }

    static void push(Entity e, Vec3d v) {
        e.setVelocity(v);
        e.velocityModified = true;
        if (e instanceof ServerPlayerEntity sp) sp.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(sp));
    }

    /** Daño de carta: aplica los bonos del árbol (daño, quemar y ralentizar con Efecto Secundario). */
    static void hurt(ServerWorld w, ServerPlayerEntity p, LivingEntity t, float amount) {
        t.damage(w, w.getDamageSources().playerAttack(p), amount);
        if (DestinyManager.get(p).has("arc_4")) { t.setOnFireForTicks(60); effect(t, StatusEffects.SLOWNESS, 60, 1); }
    }

    static LivingEntity lookTarget(ServerWorld w, ServerPlayerEntity p, double range) {
        Vec3d s = p.getEyePos(), d = p.getRotationVec(1f);
        for (double t = 1; t <= range; t += 0.5) {
            Vec3d pt = s.add(d.multiply(t));
            BlockPos bp = BlockPos.ofFloored(pt);
            if (!w.getBlockState(bp).getCollisionShape(w, bp).isEmpty()) return null;
            List<LivingEntity> l = w.getEntitiesByClass(LivingEntity.class, new Box(pt, pt).expand(0.7), e -> e != p && e.isAlive() && !(e instanceof PlayerEntity));
            if (!l.isEmpty()) return l.get(0);
        }
        return null;
    }

    static void msg(ServerPlayerEntity p, String s) { p.sendMessage(Text.literal(s), true); }

    // ------------------------------------------------------------ costo
    public static int stardustIn(ServerPlayerEntity p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().size(); i++) { ItemStack s = p.getInventory().getStack(i); if (s.isOf(com.tarotmod.item.ModItems.STARDUST)) n += s.getCount(); }
        return n;
    }
    public static boolean canPay(ServerPlayerEntity p, int cost) {
        return p.isCreative() || stardustIn(p) >= cost || p.experienceLevel >= cost * 3;
    }
    /** Cobra en Polvo Estelar; si falta, en niveles de XP (3 por Polvo). Ahorro Estelar: 25% gratis. */
    public static void charge(ServerPlayerEntity p, int cost) {
        if (p.isCreative()) return;
        if (DestinyManager.get(p).has("arc_3") && p.getRandom().nextFloat() < .25f) { msg(p, "§b✧ Ahorro Estelar: esta carta no consumió Polvo."); return; }
        if (stardustIn(p) >= cost) {
            int left = cost;
            for (int i = 0; i < p.getInventory().size() && left > 0; i++) {
                ItemStack s = p.getInventory().getStack(i);
                if (s.isOf(com.tarotmod.item.ModItems.STARDUST)) { int t = Math.min(left, s.getCount()); s.decrement(t); left -= t; }
            }
        } else p.addExperienceLevels(-cost * 3);
    }

    // ================================================================ 0 · EL LOCO — dash caótico
    public static boolean foolActive(ServerWorld w, ServerPlayerEntity p, int l) {
        Vec3d look = p.getRotationVec(1f);
        push(p, new Vec3d(look.x * 1.7 * sc(l), Math.max(0.35, look.y * 1.2), look.z * 1.7 * sc(l)));
        NO_FALL_UNTIL.put(p.getUuid(), now(w) + 80);
        for (LivingEntity e : foes(w, p, radius(p, 5, l))) {
            effect(e, StatusEffects.NAUSEA, 100, 0); effect(e, StatusEffects.SLOWNESS, 50, 1);
            if (e instanceof MobEntity m) m.setTarget(null);
        }
        Fx.sound(w, Fx.pos(p), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.5f);
        Fx.sound(w, Fx.pos(p), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, .8f, 0.6f);
        Fx.ring(w, ParticleTypes.CLOUD, p.getX(), p.getY() + .2, p.getZ(), 1.6, 20);
        Sched.repeat(10, 2, i -> { Vec3d c = Fx.pos(p); w.spawnParticles(ParticleTypes.END_ROD, c.x, c.y + 1, c.z, 6, .3, .3, .3, .02); w.spawnParticles(ParticleTypes.CLOUD, c.x, c.y + .5, c.z, 3, .2, .2, .2, .01); });
        msg(p, "§d¡Caos! §7Sin daño de caída por 4 s.");
        return true;
    }
    public static void foolPassive(ServerWorld w, ServerPlayerEntity p, int l) {
        if (!p.isOnGround() && p.isSneaking() && p.fallDistance > 2) passiveEffect(p, StatusEffects.SLOW_FALLING, 0);
    }

    // ================================================================ I · EL MAGO — rayo prismático con rebotes
    public static boolean magicianActive(ServerWorld w, ServerPlayerEntity p, int l) {
        Vec3d s = p.getEyePos(), d = p.getRotationVec(1f);
        Fx.sound(w, s, SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1f, 1.4f);
        LivingEntity first = null; Vec3d end = s.add(d.multiply(28));
        for (double t = 1; t <= 28; t += 0.5) {
            Vec3d pt = s.add(d.multiply(t)); BlockPos bp = BlockPos.ofFloored(pt);
            w.spawnParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            if (t % 1.5 == 0) w.spawnParticles(ParticleTypes.WITCH, pt.x, pt.y, pt.z, 2, .08, .08, .08, 0);
            if (!w.getBlockState(bp).getCollisionShape(w, bp).isEmpty()) { end = pt; break; }
            List<LivingEntity> hit = w.getEntitiesByClass(LivingEntity.class, new Box(pt, pt).expand(0.6), e -> e != p && e.isAlive() && !(e instanceof PlayerEntity));
            if (!hit.isEmpty()) { first = hit.get(0); end = pt; break; }
        }
        if (first == null) { w.spawnParticles(ParticleTypes.ENCHANTED_HIT, end.x, end.y, end.z, 8, .2, .2, .2, .05); return true; }
        float base = dmg(p, 7, l);
        hurt(w, p, first, base);
        Set<UUID> done = new HashSet<>(); done.add(first.getUuid());
        LivingEntity cur = first;
        for (int hop = 0; hop < 2; hop++) {
            final LivingEntity from = cur;
            LivingEntity nxt = foes(w, Fx.pos(from), 7, p).stream().filter(e -> !done.contains(e.getUuid()))
                    .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(from))).orElse(null);
            if (nxt == null) break;
            Fx.line(w, ParticleTypes.WITCH, Fx.pos(from).add(0, 1, 0), Fx.pos(nxt).add(0, 1, 0), .4);
            hurt(w, p, nxt, base * (hop == 0 ? .75f : .5f));
            w.spawnParticles(ParticleTypes.ENCHANTED_HIT, nxt.getX(), nxt.getBodyY(.5), nxt.getZ(), 12, .3, .3, .3, .1);
            done.add(nxt.getUuid()); cur = nxt;
        }
        w.spawnParticles(ParticleTypes.ENCHANTED_HIT, first.getX(), first.getBodyY(.5), first.getZ(), 20, .3, .3, .3, .1);
        return true;
    }
    public static void magicianPassive(ServerWorld w, ServerPlayerEntity p, int l) { passiveEffect(p, StatusEffects.HASTE, 0); }

    // ================================================================ II · LA SUMA SACERDOTISA — pulso de revelación
    public static boolean priestessActive(ServerWorld w, ServerPlayerEntity p, int l) {
        Vec3d c = Fx.pos(p); double r = radius(p, 22, l);
        Fx.sound(w, c, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1f, 1.8f);
        Sched.repeat(6, 2, i -> { double rr = (i + 1) * r / 6; Fx.ring(w, ParticleTypes.ENCHANT, c.x, c.y + .3, c.z, rr, (int) (10 + rr * 3)); Fx.ring(w, ParticleTypes.END_ROD, c.x, c.y + 1, c.z, rr, 8 + i * 2); });
        int seen = 0;
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(r), e -> e != p && e.isAlive())) { effect(e, StatusEffects.GLOWING, 200, 0); seen++; }
        DestinyManager.revealChests(w, p, (int) Math.min(30, r));
        msg(p, "§9La Sacerdotisa revela " + seen + " presencias.");
        return true;
    }
    public static void priestessPassive(ServerWorld w, ServerPlayerEntity p, int l) {
        if (now(w) % 300 == 0) DestinyManager.revealChests(w, p, 12);
    }

    // ================================================================ III · LA EMPERATRIZ — jardín de 8 s
    static int growCrops(ServerWorld w, BlockPos center, int radius, int max) {
        int grown = 0;
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -1, -radius), center.add(radius, 1, radius))) {
            if (grown >= max) break;
            BlockState st = w.getBlockState(pos);
            if (st.getBlock() instanceof Fertilizable f && f.isFertilizable(w, pos, st) && f.canGrow(w, w.getRandom(), pos, st)) {
                f.grow(w, w.getRandom(), pos.toImmutable(), st);
                w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 4, .3, .3, .3, 0);
                grown++;
            }
        }
        return grown;
    }
    public static boolean empressActive(ServerWorld w, ServerPlayerEntity p, int l) {
        Vec3d c = Fx.pos(p); double r = radius(p, 5, l);
        Fx.sound(w, c, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.3f);
        Sched.repeat(8, 20, i -> {
            Fx.ring(w, ParticleTypes.HAPPY_VILLAGER, c.x, c.y + .2, c.z, r, 30);
            Fx.ring(w, ParticleTypes.HEART, c.x, c.y + 1, c.z, r * .5, 6);
            growCrops(w, BlockPos.ofFloored(c), (int) r, 6);
            for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(r), e -> e.isAlive() && e.squaredDistanceTo(c) <= r * r)) {
                boolean ally = e instanceof PlayerEntity || (e instanceof TameableEntity t && t.isTamed()) || e instanceof net.minecraft.entity.passive.AnimalEntity;
                if (ally) e.heal(e == p ? heal(p, 2, l) : (float) (2 * sc(l)));
            }
        });
        return true;
    }
    public static void empressPassive(ServerWorld w, ServerPlayerEntity p, int l) {
        passiveEffect(p, StatusEffects.REGENERATION, 0);
        growCrops(w, p.getBlockPos(), 3, 1 + l);
    }

    // ================================================================ IV · EL EMPERADOR — decreto
    public static boolean emperorActive(ServerWorld w, ServerPlayerEntity p, int l) {
        double r = radius(p, 7, l);
        List<LivingEntity> list = foes(w, p, r);
        for (LivingEntity e : list) {
            Vec3d dir = Fx.pos(e).subtract(Fx.pos(p)).multiply(1, 0, 1).normalize();
            push(e, new Vec3d(dir.x * 1.6, 0.45, dir.z * 1.6));
            effect(e, StatusEffects.WEAKNESS, 100, 0);
            if (e instanceof MobEntity m) m.setTarget(null);
            hurt(w, p, e, dmg(p, 3, l));
        }
        int amp = Math.min(3, Math.max(0, list.size() - 1));
        effect(p, StatusEffects.ABSORPTION, 400, amp);
        Fx.sound(w, Fx.pos(p), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, .7f, 1.6f);
        Fx.ring(w, ParticleTypes.CRIT, p.getX(), p.getY() + .3, p.getZ(), r, 40);
        Fx.ring(w, ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), r * .6, 20);
        msg(p, "§cDecreto: " + list.size() + " enemigo(s) repelidos.");
        return true;
    }
    public static void emperorPassive(ServerWorld w, ServerPlayerEntity p, int l) { if (p.getHealth() < 8) passiveEffect(p, StatusEffects.RESISTANCE, 0); }

    // ================================================================ V · EL HIEROFANTE — consagración
    public static boolean hierophantActive(ServerWorld w, ServerPlayerEntity p, int l) {
        int repaired = 0;
        List<ItemStack> gear = new ArrayList<>(List.of(p.getMainHandStack(), p.getOffHandStack()));
        for (net.minecraft.entity.EquipmentSlot sl : new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST, net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET})
            gear.add(p.getEquippedStack(sl));
        for (ItemStack s : gear) if (!s.isEmpty() && s.isDamaged()) { s.setDamage(Math.max(0, s.getDamage() - (int) (120 * sc(l)))); repaired++; }
        p.addExperience((int) (25 * sc(l)));
        Fx.sound(w, Fx.pos(p), SoundEvents.BLOCK_BEACON_ACTIVATE, .8f, 1.7f);
        Fx.ring(w, ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 1.2, 30);
        w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 30, .3, 1, .3, .05);
        msg(p, "§dConsagración: " + repaired + " pieza(s) reparadas.");
        return true;
    }
    public static void hierophantPassive(ServerWorld w, ServerPlayerEntity p, int l) { passiveEffect(p, StatusEffects.SATURATION, 0); }

    // ================================================================ VI · LOS ENAMORADOS — vínculo
    public static boolean loversActive(ServerWorld w, ServerPlayerEntity p, int l) {
        double r = radius(p, 10, l); int n = 0;
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(r),
                e -> e.isAlive() && (e instanceof PlayerEntity || (e instanceof TameableEntity t && t.isTamed())))) {
            e.heal(e == p ? heal(p, 7, l) : (float) (6 * sc(l)));
            effect(e, StatusEffects.ABSORPTION, 200, 0);
            if (e != p) { Fx.line(w, ParticleTypes.HEART, Fx.pos(p).add(0, 1.2, 0), Fx.pos(e).add(0, 1.2, 0), 1.0); n++; }
            w.spawnParticles(ParticleTypes.HEART, e.getX(), e.getY() + 1.8, e.getZ(), 6, .3, .3, .3, 0);
        }
        Fx.sound(w, Fx.pos(p), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.1f);
        msg(p, "§dVínculo: " + n + " aliado(s) sanados.");
        return true;
    }
    public static void loversPassive(ServerWorld w, ServerPlayerEntity p, int l) {
        for (TameableEntity t : w.getEntitiesByClass(TameableEntity.class, p.getBoundingBox().expand(8), t -> t.isTamed() && t.isAlive()))
            effect(t, StatusEffects.REGENERATION, 60, 0);
    }

    // ================================================================ VII · EL CARRO — embestida
    public static boolean chariotActive(ServerWorld w, ServerPlayerEntity p, int l) {
        Vec3d dir = p.getRotationVec(1f); Vec3d flat = new Vec3d(dir.x, 0, dir.z).normalize();
        push(p, new Vec3d(flat.x * 2.0 * sc(l), 0.2, flat.z * 2.0 * sc(l)));
        NO_FALL_UNTIL.put(p.getUuid(), now(w) + 40);
        effect(p, StatusEffects.RESISTANCE, 30, 1);
        Fx.sound(w, Fx.pos(p), SoundEvents.ENTITY_HORSE_GALLOP, 1f, .8f);
        Set<UUID> hit = new HashSet<>();
        Sched.repeat(8, 2, i -> {
            Vec3d c = Fx.pos(p);
            w.spawnParticles(ParticleTypes.CLOUD, c.x, c.y + .2, c.z, 6, .4, .1, .4, .03);
            w.spawnParticles(ParticleTypes.CRIT, c.x, c.y + 1, c.z, 6, .4, .4, .4, .1);
            for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(1.3), e -> isFoe(e, p) && !hit.contains(e.getUuid()))) {
                hit.add(e.getUuid()); hurt(w, p, e, dmg(p, 7, l)); e.takeKnockback(1.6, -flat.x, -flat.z);
            }
        });
        return true;
    }
    public static void chariotPassive(ServerWorld w, ServerPlayerEntity p, int l) { if (p.isSprinting()) passiveEffect(p, StatusEffects.SPEED, 0); }

    // ================================================================ VIII · LA FUERZA — salto e impacto
    public static boolean strengthActive(ServerWorld w, ServerPlayerEntity p, int l) {
        Vec3d look = p.getRotationVec(1f);
        push(p, new Vec3d(look.x * .5, 1.0, look.z * .5));
        NO_FALL_UNTIL.put(p.getUuid(), now(w) + 120);
        Fx.sound(w, Fx.pos(p), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1f, .6f);
        boolean[] landed = {false};
        Sched.later(9, () -> push(p, new Vec3d(0, -2.4, 0)));
        Sched.repeat(30, 2, i -> {
            if (landed[0] || i < 5 || !p.isOnGround()) return;
            landed[0] = true;
            Vec3d c = Fx.pos(p); double r = radius(p, 5, l);
            for (LivingEntity e : foes(w, c, r, p)) {
                hurt(w, p, e, dmg(p, 11, l));
                Vec3d dir = Fx.pos(e).subtract(c).multiply(1, 0, 1).normalize();
                push(e, new Vec3d(dir.x * 1.2, 0.5, dir.z * 1.2));
            }
            Fx.sound(w, c, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, .8f, .7f);
            Fx.sound(w, c, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, .6f);
            Sched.repeat(4, 2, k -> Fx.ring(w, ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + .1, c.z, (k + 1) * r / 4, 18 + k * 8));
            w.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y + .3, c.z, 3, 1, .1, 1, 0);
        });
        return true;
    }
    public static void strengthPassive(ServerWorld w, ServerPlayerEntity p, int l) { if (p.getHealth() < p.getMaxHealth() * .5f) passiveEffect(p, StatusEffects.STRENGTH, 0); }

    // ================================================================ IX · EL ERMITAÑO — manto de sombras
    public static boolean hermitActive(ServerWorld w, ServerPlayerEntity p, int l) {
        int dur = (int) (160 * sc(l));
        effect(p, StatusEffects.INVISIBILITY, dur, 0);
        Fx.sound(w, Fx.pos(p), SoundEvents.ENTITY_ENDERMAN_TELEPORT, .6f, .5f);
        w.spawnParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 1, p.getZ(), 40, .4, .8, .4, .02);
        Sched.repeat(dur / 8, 8, i -> {
            Vec3d c = Fx.pos(p);
            w.spawnParticles(ParticleTypes.SMOKE, c.x, c.y + 1, c.z, 4, .3, .6, .3, 0);
            for (MobEntity m : w.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(22), m -> m.getTarget() == p)) m.setTarget(null);
        });
        msg(p, "§8Las sombras te cubren.");
        return true;
    }
    public static void hermitPassive(ServerWorld w, ServerPlayerEntity p, int l) {
        if (p.isSneaking() && p.getVelocity().horizontalLengthSquared() < 0.003) {
            passiveEffect(p, StatusEffects.INVISIBILITY, 0);
            for (MobEntity m : w.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(10), m -> m.getTarget() == p && m.squaredDistanceTo(p) > 16)) m.setTarget(null);
        }
    }

    // ================================================================ X · LA RUEDA DE LA FORTUNA — ruleta
    public static boolean wheelActive(ServerWorld w, ServerPlayerEntity p, int l) {
        int roll = w.getRandom().nextInt(100);
        Vec3d c = Fx.pos(p);
        Fx.sound(w, c, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, .6f + w.getRandom().nextFloat());
        Sched.repeat(8, 2, i -> Fx.ring(w, i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT, c.x, c.y + 1 + i * .1, c.z, 1.5, 16));
        if (roll < 25) {
            p.getInventory().offerOrDrop(new ItemStack(com.tarotmod.item.ModItems.STARDUST, 2 + w.getRandom().nextInt(3)));
            ExperienceOrbEntity.spawn(w, c.add(0, 1, 0), (int) (40 * sc(l)));
            msg(p, "§6★ ¡JACKPOT! §eRecibís Polvo Estelar y experiencia.");
        } else if (roll < 50) {
            effect(p, StatusEffects.STRENGTH, (int) (600 * sc(l)), 0); effect(p, StatusEffects.SPEED, 600, 0);
            msg(p, "§c⚔ Furia del destino: Fuerza y Velocidad por 30 s.");
        } else if (roll < 70) {
            effect(p, StatusEffects.HASTE, 800, 1); effect(p, StatusEffects.LUCK, 800, 1);
            WHEEL_UNTIL.put(p.getUuid(), now(w) + 800);
            msg(p, "§e⛏ Prosperidad: Prisa, Suerte y +XP al matar por 40 s.");
        } else if (roll < 90) {
            effect(p, StatusEffects.ABSORPTION, 600, 2); effect(p, StatusEffects.REGENERATION, 200, 0);
            msg(p, "§b🛡 Amparo: escudo y regeneración.");
        } else {
            effect(p, StatusEffects.WEAKNESS, 300, 0); effect(p, StatusEffects.UNLUCK, 600, 1); effect(p, StatusEffects.MINING_FATIGUE, 200, 0);
            p.getInventory().offerOrDrop(new ItemStack(com.tarotmod.item.ModItems.STARDUST));
            Fx.sound(w, c, SoundEvents.ENTITY_WITHER_HURT, .5f, .6f);
            msg(p, "§8☠ La Rueda gira en tu contra... (te deja 1 Polvo de consuelo)");
        }
        return true;
    }
    public static void wheelPassive(ServerWorld w, ServerPlayerEntity p, int l) { passiveEffect(p, StatusEffects.LUCK, 0); } // + ruleta del destino (ver startRoulette)

    // ================================================================ Ruleta del destino (pasiva de La Rueda)
    public static final Set<UUID> ROULETTE = ConcurrentHashMap.newKeySet();
    public static final Map<UUID, Integer> ROULETTE_CD = new ConcurrentHashMap<>();

    /** Nivel de La Rueda que lleva encima (inventario o Mazo), o -1 si no la tiene. */
    public static int wheelLevel(ServerPlayerEntity p) {
        net.minecraft.item.Item want = com.tarotmod.item.ModItems.CARDS.get("wheel");
        int best = -1;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack st = p.getInventory().getStack(i);
            if (st.getItem() == want) best = Math.max(best, com.tarotmod.item.TarotCardItem.level(st));
            else if (st.getItem() instanceof com.tarotmod.item.DestinyDeckItem)
                for (ItemStack in : com.tarotmod.item.DestinyDeckItem.cards(st)) if (in.getItem() == want) best = Math.max(best, com.tarotmod.item.TarotCardItem.level(in));
        }
        return best;
    }

    /** Golpe mortal con La Rueda: el jugador queda "congelado" a 1 de vida y una ruleta decide (40/50/60% de vivir según nivel). */
    public static void startRoulette(ServerWorld w, ServerPlayerEntity p, int lvl) {
        ROULETTE.add(p.getUuid());
        p.setHealth(1f);
        effect(p, StatusEffects.RESISTANCE, 160, 4);
        effect(p, StatusEffects.SLOWNESS, 100, 9);
        boolean saved = w.getRandom().nextFloat() < 0.40f + 0.10f * lvl;
        int[] delays = {0, 3, 6, 9, 13, 17, 22, 28, 35, 43, 52, 62};
        int last = delays.length - 1;
        int offset = ((saved ? 0 : 1) - last % 2 + 2) % 2; // la última casilla coincide con el resultado (par = VIDA)
        Fx.sound(w, Fx.pos(p), SoundEvents.ENTITY_WITHER_SPAWN, .4f, 1.8f);
        for (int i = 0; i <= last; i++) {
            final int k = i;
            Sched.later(1 + delays[i], () -> {
                if (!p.isAlive()) { ROULETTE.remove(p.getUuid()); return; }
                int cur = (k + offset) % 6;
                StringBuilder sb = new StringBuilder("§6✦ RUEDA DEL DESTINO ✦  ");
                for (int j = 0; j < 6; j++) {
                    String lab = j % 2 == 0 ? "§a VIDA " : "§c MUERTE ";
                    sb.append(j == cur ? "§e[" + lab + "§e] " : "§8" + (j % 2 == 0 ? " vida " : " muerte ") + " ");
                }
                msg(p, sb.toString());
                Fx.sound(w, Fx.pos(p), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, .9f, .6f + k * .08f);
                Fx.ring(w, ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 1.2, 10);
                if (k == last) resolveRoulette(w, p, saved);
            });
        }
    }

    private static void resolveRoulette(ServerWorld w, ServerPlayerEntity p, boolean saved) {
        ROULETTE.remove(p.getUuid());
        if (!p.isAlive()) return;
        if (saved) {
            ROULETTE_CD.put(p.getUuid(), now(w) + 3600);
            p.removeStatusEffect(StatusEffects.SLOWNESS);
            p.setHealth(8f);
            effect(p, StatusEffects.RESISTANCE, 80, 3);
            for (LivingEntity e : foes(w, p, 6)) { Vec3d d = Fx.pos(e).subtract(Fx.pos(p)).multiply(1, 0, 1); if (d.lengthSquared() > .01) { d = d.normalize(); push(e, new Vec3d(d.x * 1.3, .5, d.z * 1.3)); } }
            Fx.sound(w, Fx.pos(p), SoundEvents.ITEM_TOTEM_USE, 1f, 1.2f);
            w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 60, .5, 1, .5, .4);
            p.sendMessage(Text.literal("§a§l¡La Rueda te perdona la vida!"), false);
        } else {
            p.removeStatusEffect(StatusEffects.RESISTANCE);
            p.removeStatusEffect(StatusEffects.SLOWNESS);
            Fx.sound(w, Fx.pos(p), SoundEvents.ENTITY_WITHER_HURT, 1f, .5f);
            p.sendMessage(Text.literal("§c§l☠ La Rueda ha decidido."), false);
            p.damage(w, w.getDamageSources().genericKill(), Float.MAX_VALUE);
        }
    }

    // ================================================================ XI · LA JUSTICIA — balanza
    public static boolean justiceActive(ServerWorld w, ServerPlayerEntity p, int l) {
        int dur = (int) (120 * sc(l));
        REFLECT_UNTIL.put(p.getUuid(), now(w) + dur);
        for (LivingEntity e : foes(w, p, radius(p, 10, l))) effect(e, StatusEffects.WEAKNESS, 100, 0);
        Fx.sound(w, Fx.pos(p), SoundEvents.BLOCK_BEACON_ACTIVATE, 1f, 1.3f);
        Sched.repeat(dur / 10, 10, i -> {
            Vec3d c = Fx.pos(p);
            Fx.ring(w, ParticleTypes.END_ROD, c.x, c.y + .2, c.z, 1.4, 16);
            Fx.ring(w, ParticleTypes.ENCHANTED_HIT, c.x, c.y + 2.1, c.z, .8, 8);
        });
        msg(p, "§eBalanza: reflejás 50% del daño por " + dur / 20 + " s.");
        return true;
    }
    public static void justicePassive(ServerWorld w, ServerPlayerEntity p, int l) { if (p.getHealth() < 8) passiveEffect(p, StatusEffects.RESISTANCE, 0); }

    // ================================================================ XII · EL COLGADO — inversión
    public static boolean hangedActive(ServerWorld w, ServerPlayerEntity p, int l) {
        List<LivingEntity> list = foes(w, p, radius(p, 8, l));
        if (list.isEmpty()) { msg(p, "§7No hay enemigos que suspender."); return false; }
        for (LivingEntity e : list) {
            effect(e, StatusEffects.LEVITATION, 55, 1); effect(e, StatusEffects.SLOWNESS, 55, 3); effect(e, StatusEffects.GLOWING, 80, 0);
            if (e instanceof MobEntity m) m.setTarget(null);
        }
        Fx.sound(w, Fx.pos(p), SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1f, .7f);
        Sched.repeat(5, 11, i -> { for (LivingEntity e : list) if (e.isAlive()) { w.spawnParticles(ParticleTypes.END_ROD, e.getX(), e.getY(), e.getZ(), 5, .3, .1, .3, .02); Fx.ring(w, ParticleTypes.ENCHANT, e.getX(), e.getY() + .1, e.getZ(), .9, 8); } });
        Sched.later(56, () -> { for (LivingEntity e : list) if (e.isAlive()) { e.removeStatusEffect(StatusEffects.LEVITATION); push(e, new Vec3d(0, -1.8, 0)); } });
        return true;
    }
    public static void hangedPassive(ServerWorld w, ServerPlayerEntity p, int l) { if (p.fallDistance > 4) passiveEffect(p, StatusEffects.SLOW_FALLING, 0); }

    // ================================================================ XIII · LA MUERTE — Marca de Segador
    public static boolean deathActive(ServerWorld w, ServerPlayerEntity p, int l) {
        LivingEntity t = lookTarget(w, p, 30);
        if (t == null) { msg(p, "§7Mirá a una criatura para marcarla."); return false; }
        MARKS.put(t.getUuid(), new Mark(p.getUuid(), now(w) + 160, l));
        effect(t, StatusEffects.GLOWING, 160, 0);
        Fx.sound(w, Fx.pos(t), SoundEvents.ENTITY_WITHER_SPAWN, .35f, 1.9f);
        Fx.sound(w, Fx.pos(p), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, .5f);
        Sched.repeat(32, 5, i -> {
            Mark m = MARKS.get(t.getUuid());
            if (m == null || !t.isAlive()) return;
            Fx.ring(w, ParticleTypes.SOUL_FIRE_FLAME, t.getX(), t.getY() + .1 + (i % 4) * .5, t.getZ(), .7, 10);
            w.spawnParticles(ParticleTypes.SOUL, t.getX(), t.getY() + 2.4, t.getZ(), 2, .1, .1, .1, .01);
        });
        Sched.later(161, () -> MARKS.remove(t.getUuid()));
        msg(p, "§5Marca de Segador sobre §f" + t.getName().getString() + "§5 (8 s).");
        return true;
    }
    /** Llamado desde AFTER_DEATH: si la víctima estaba marcada, explota en almas. */
    public static void onDeathMark(ServerWorld w, LivingEntity victim) {
        Mark m = MARKS.remove(victim.getUuid());
        if (m == null || now(w) > m.until()) return;
        ServerPlayerEntity owner = w.getServer().getPlayerManager().getPlayer(m.owner());
        if (owner == null) return;
        Vec3d c = Fx.pos(victim);
        Fx.sound(w, c, SoundEvents.ENTITY_WITHER_SPAWN, .6f, 1.6f);
        Fx.sound(w, c, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, .5f);
        for (int i = 0; i < 4; i++) Fx.ring(w, ParticleTypes.SOUL, c.x, c.y + .3 + i * .5, c.z, 1 + i * .8, 14 + i * 4);
        w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1, c.z, 40, .5, .8, .5, .15);
        for (LivingEntity e : foes(w, c, radius(owner, 5, m.level()), owner)) {
            if (e == victim) continue;
            hurt(w, owner, e, dmg(owner, 8, m.level()));
            Fx.line(w, ParticleTypes.SOUL, c.add(0, 1, 0), Fx.pos(e).add(0, 1, 0), .5);
        }
        owner.heal(heal(owner, 6, m.level()));
        Fx.line(w, ParticleTypes.SOUL, c.add(0, 1, 0), Fx.pos(owner).add(0, 1, 0), .4);
        msg(owner, "§5✦ Las almas te devuelven 3 corazones.");
    }
    public static void deathPassive(ServerWorld w, ServerPlayerEntity p, int l) { if (w.getLightLevel(p.getBlockPos()) < 7) passiveEffect(p, StatusEffects.SPEED, 0); }

    // ================================================================ XIV · LA TEMPLANZA — alquimia
    public static boolean temperanceActive(ServerWorld w, ServerPlayerEntity p, int l) {
        int n = 0;
        for (StatusEffectInstance i : new ArrayList<>(p.getStatusEffects()))
            if (i.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) { p.removeStatusEffect(i.getEffectType()); n++; }
        p.heal(heal(p, 4, l));
        int dur = (int) (200 * sc(l));
        effect(p, StatusEffects.REGENERATION, 100, 0);
        if (n >= 1) effect(p, StatusEffects.RESISTANCE, dur, 0);
        if (n >= 2) effect(p, StatusEffects.SPEED, dur, 0);
        if (n >= 3) effect(p, StatusEffects.ABSORPTION, dur * 2, 1);
        Fx.sound(w, Fx.pos(p), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.0f + n * .2f);
        Fx.ring(w, ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 1, 20);
        w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 20, .4, .8, .4, .03);
        msg(p, n > 0 ? "§bAlquimia: " + n + " mal(es) transmutados en bendiciones." : "§bAlquimia: equilibrio.");
        return true;
    }
    public static void temperancePassive(ServerWorld w, ServerPlayerEntity p, int l) { passiveEffect(p, StatusEffects.SATURATION, 0); }

    // ================================================================ XV · EL DIABLO — pacto
    public static boolean devilActive(ServerWorld w, ServerPlayerEntity p, int l) {
        if (p.getHealth() <= 8) { msg(p, "§4El Diablo exige sangre: necesitás más de 4 corazones."); return false; }
        p.damage(w, w.getDamageSources().magic(), 6f);
        int dur = 240;
        PACT_UNTIL.put(p.getUuid(), now(w) + dur);
        SkyEvent.play(w, p, 80);
        effect(p, StatusEffects.STRENGTH, dur, 0);
        Fx.sound(w, Fx.pos(p), SoundEvents.ENTITY_WITHER_SPAWN, .5f, .5f);
        Fx.sound(w, Fx.pos(p), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, .6f, .6f);
        Sched.repeat(dur / 10, 10, i -> { Vec3d c = Fx.pos(p); Fx.ring(w, ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + .2, c.z, 1.2, 14); w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1, c.z, 4, .3, .6, .3, .02); });
        Sched.later(dur, () -> { if (p.isAlive()) { effect(p, StatusEffects.WEAKNESS, 100, 0); effect(p, StatusEffects.HUNGER, 100, 1); msg(p, "§8El pacto se cobra su precio..."); } });
        msg(p, "§4Pacto sellado: robo de vida por 12 s.");
        return true;
    }
    /** Robo de vida del Pacto (llamado desde AFTER_DAMAGE). */
    public static void onPactHit(ServerWorld w, ServerPlayerEntity p, float dealt) {
        Integer until = PACT_UNTIL.get(p.getUuid());
        if (until == null || now(w) > until) return;
        p.heal(Math.min(4f, dealt * .35f));
        w.spawnParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1, p.getZ(), 3, .3, .3, .3, .02);
    }
    public static void devilPassive(ServerWorld w, ServerPlayerEntity p, int l) { passiveEffect(p, StatusEffects.STRENGTH, 0); passiveEffect(p, StatusEffects.HUNGER, 0); }

    // ================================================================ XVI · LA TORRE — rayo telegrafiado
    public static boolean towerActive(ServerWorld w, ServerPlayerEntity p, int l) {
        Vec3d t = p.raycast(40, 1.0f, false).getPos();
        double r = radius(p, 3.5, l);
        Fx.sound(w, t, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.2f, .6f);
        // aviso visual: círculo rojo que se cierra durante 1.5 s (30 ticks)
        Sched.repeat(15, 2, i -> {
            double rr = r * (1.0 - i * .03);
            Fx.ring(w, ParticleTypes.CRIMSON_SPORE, t.x, t.y + .15, t.z, r, 28);
            Fx.ring(w, ParticleTypes.FLAME, t.x, t.y + .15, t.z, rr, 20);
            if (i % 5 == 0) Fx.sound(w, t, SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, .8f, .5f + i * .05f);
        });
        Sched.later(30, () -> {
            LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w, SpawnReason.TRIGGERED);
            if (bolt != null) { bolt.setPosition(t); bolt.setCosmetic(true); w.spawnEntity(bolt); } // cosmético: sin fuego ni daño a ítems
            Fx.sound(w, t, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.4f, .8f);
            w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, t.x, t.y + .5, t.z, 1, 0, 0, 0, 0);
            Fx.ring(w, ParticleTypes.ELECTRIC_SPARK, t.x, t.y + .3, t.z, r, 36);
            List<LivingEntity> hit = foes(w, t, r, p);
            for (LivingEntity e : hit) { // solo criaturas: los ítems del suelo no se tocan
                hurt(w, p, e, dmg(p, 16, l));
                Vec3d dir = Fx.pos(e).subtract(t).multiply(1, 0, 1); dir = dir.lengthSquared() < .01 ? new Vec3d(0, 0, 0) : dir.normalize();
                push(e, new Vec3d(dir.x * 1.1, 0.55, dir.z * 1.1));
            }
            if (DestinyManager.get(p).has("arc_5")) { // Tormenta del Arcanista: rayos encadenados
                for (LivingEntity e : foes(w, t, r + 6, p)) if (!hit.contains(e)) { hurt(w, p, e, dmg(p, 6, l)); Fx.line(w, ParticleTypes.ELECTRIC_SPARK, t.add(0, 1, 0), Fx.pos(e).add(0, 1, 0), .5); }
            }
        });
        return true;
    }
    public static void towerPassive(ServerWorld w, ServerPlayerEntity p, int l) { if (w.isRaining() || w.isThundering()) passiveEffect(p, StatusEffects.RESISTANCE, 0); }

    // ================================================================ XVII · LA ESTRELLA — constelación
    public static boolean starActive(ServerWorld w, ServerPlayerEntity p, int l) {
        Vec3d c = Fx.pos(p); double r = radius(p, 6, l);
        Fx.sound(w, c, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.2f, 1.5f);
        Sched.repeat(8, 20, i -> {
            Fx.ring(w, ParticleTypes.END_ROD, c.x, c.y + .2, c.z, r, 32);
            for (int k = 0; k < 8; k++) { double a = k * Math.PI / 4 + i * .4; Fx.column(w, ParticleTypes.GLOW, c.x + Math.cos(a) * r * .7, c.y, c.z + Math.sin(a) * r * .7, 3, 6); }
            Fx.sound(w, c, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, .6f, 1.4f + i * .06f);
            for (ServerPlayerEntity o : w.getPlayers(x -> x.squaredDistanceTo(c) <= r * r)) {
                o.heal(o == p ? heal(p, 2.5, l) : (float) (2.5 * sc(l)));
                for (StatusEffectInstance ef : new ArrayList<>(o.getStatusEffects()))
                    if (ef.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) { o.removeStatusEffect(ef.getEffectType()); break; }
            }
        });
        return true;
    }
    public static void starPassive(ServerWorld w, ServerPlayerEntity p, int l) { if (w.getLightLevel(p.getBlockPos()) < 7) passiveEffect(p, StatusEffects.NIGHT_VISION, 0); }

    // ================================================================ XVIII · LA LUNA — marea lunar
    public static boolean moonActive(ServerWorld w, ServerPlayerEntity p, int l) {
        int dur = (int) (160 * sc(l));
        effect(p, StatusEffects.JUMP_BOOST, dur, 3); effect(p, StatusEffects.SLOW_FALLING, dur + 40, 0); effect(p, StatusEffects.NIGHT_VISION, 600, 0);
        NO_FALL_UNTIL.put(p.getUuid(), now(w) + dur + 40);
        int n = 0;
        for (LivingEntity e : foes(w, p, radius(p, 26, l))) { effect(e, StatusEffects.GLOWING, 200, 0); n++; }
        Fx.sound(w, Fx.pos(p), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, .7f);
        Sched.repeat(8, 5, i -> { Vec3d c = Fx.pos(p); Fx.ring(w, ParticleTypes.END_ROD, c.x, c.y + .1 + i * .4, c.z, 1.6 - i * .12, 16); });
        msg(p, "§9Marea Lunar: gravedad baja y " + n + " enemigos revelados.");
        return true;
    }
    public static void moonPassive(ServerWorld w, ServerPlayerEntity p, int l) {
        if (!w.isDay()) passiveEffect(p, StatusEffects.NIGHT_VISION, 0);
        p.removeStatusEffect(StatusEffects.BLINDNESS);
    }

    // ================================================================ XIX · EL SOL — aura
    public static boolean sunActive(ServerWorld w, ServerPlayerEntity p, int l) {
        double r = radius(p, 6, l);
        Fx.sound(w, Fx.pos(p), SoundEvents.ITEM_FIRECHARGE_USE, 1f, .7f);
        Fx.sound(w, Fx.pos(p), SoundEvents.BLOCK_BEACON_ACTIVATE, 1f, 1.9f);
        boolean[] burned = {false};
        Sched.repeat(20, 10, i -> {
            if (!p.isAlive()) return;
            Vec3d c = Fx.pos(p);
            Fx.ring(w, ParticleTypes.FLAME, c.x, c.y + .2, c.z, r, 30);
            Fx.ring(w, ParticleTypes.END_ROD, c.x, c.y + 1.2, c.z, r * .5, 12);
            w.spawnParticles(ParticleTypes.FLAME, c.x, c.y + 1, c.z, 6, r * .4, .6, r * .4, .01);
            for (LivingEntity e : foes(w, c, r, p)) {
                if (!e.getType().isIn(EntityTypeTags.UNDEAD)) continue;
                burned[0] = true; e.setOnFireForTicks(60); hurt(w, p, e, dmg(p, 2.5, l)); // quema progresiva cada 0,5 s
            }
            for (ServerPlayerEntity o : w.getPlayers(x -> x.squaredDistanceTo(c) <= r * r)) { // purifica
                o.removeStatusEffect(StatusEffects.POISON); o.removeStatusEffect(StatusEffects.WITHER);
            }
            if (i == 19) { p.getInventory().offerOrDrop(new ItemStack(com.tarotmod.item.ModItems.STARDUST)); msg(p, "§6☀ El Sol te devuelve 1 Polvo Estelar."); }
        });
        return true;
    }
    public static void sunPassive(ServerWorld w, ServerPlayerEntity p, int l) {
        if (w.isDay() && !w.isRaining() && w.isSkyVisible(p.getBlockPos())) passiveEffect(p, StatusEffects.REGENERATION, 0);
    }

    // ================================================================ XX · EL JUICIO — columnas de luz
    public static boolean judgementActive(ServerWorld w, ServerPlayerEntity p, int l) {
        double r = radius(p, 14, l);
        List<LivingEntity> list = foes(w, p, r); list.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
        if (list.isEmpty()) { msg(p, "§7No hay nadie a quien juzgar."); return false; }
        List<LivingEntity> targets = list.subList(0, Math.min(5, list.size()));
        Fx.sound(w, Fx.pos(p), SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.2f, 1.4f);
        Sched.repeat(12, 2, i -> { for (LivingEntity e : targets) if (e.isAlive()) { Fx.ring(w, ParticleTypes.END_ROD, e.getX(), e.getY() + .1, e.getZ(), 1.6 - i * .1, 14); Fx.column(w, ParticleTypes.ENCHANT, e.getX(), e.getY(), e.getZ(), 2, 6); } });
        Sched.later(24, () -> {
            for (LivingEntity e : targets) {
                if (!e.isAlive()) continue;
                hurt(w, p, e, dmg(p, 20, l));
                Fx.column(w, ParticleTypes.END_ROD, e.getX(), e.getY(), e.getZ(), 12, 40);
                w.spawnParticles(ParticleTypes.FIREWORK, e.getX(), e.getBodyY(.5), e.getZ(), 20, .3, .5, .3, .15);
                Fx.sound(w, Fx.pos(e), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, .7f, 1.6f);
                if (!e.isAlive()) p.addExperience(8);
            }
        });
        msg(p, "§eJuicio sobre " + targets.size() + " enemigo(s).");
        return true;
    }
    public static void judgementPassive(ServerWorld w, ServerPlayerEntity p, int l) { if (foes(w, p, 10).size() >= 3) passiveEffect(p, StatusEffects.RESISTANCE, 0); }

    // ================================================================ XXI · EL MUNDO — cúpula del tiempo
    public static boolean worldActive(ServerWorld w, ServerPlayerEntity p, int l) {
        Vec3d c = Fx.pos(p); double r = radius(p, 6, l);
        Fx.sound(w, c, SoundEvents.ITEM_TOTEM_USE, 1f, .5f);
        Fx.sound(w, c, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, .5f, .5f);
        Set<UUID> slowedProjectiles = new HashSet<>();
        Sched.repeat(16, 5, i -> {
            for (int k = 0; k <= 6; k++) { double ang = (k / 6.0) * Math.PI; Fx.ring(w, ParticleTypes.END_ROD, c.x, c.y + r * Math.cos(ang), c.z, r * Math.sin(ang), 6 + (int) (r * 3 * Math.sin(ang))); }
            for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(r), e -> e.isAlive() && !(e instanceof PlayerEntity) && e.squaredDistanceTo(c) <= r * r))
                effect(e, StatusEffects.SLOWNESS, 12, 5); // Lentitud VI = -90% de velocidad
            for (ProjectileEntity pr : w.getEntitiesByClass(ProjectileEntity.class, new Box(c, c).expand(r), pr -> pr.getOwner() != p && pr.squaredDistanceTo(c) <= r * r)) {
                if (slowedProjectiles.add(pr.getUuid())) pr.setVelocity(pr.getVelocity().multiply(.1));
                pr.setNoGravity(true); pr.setVelocity(pr.getVelocity().multiply(.9));
            }
        });
        msg(p, "§bEl Mundo se detiene a tu alrededor.");
        return true;
    }
    public static void worldPassive(ServerWorld w, ServerPlayerEntity p, int l) { passiveEffect(p, StatusEffects.SPEED, 0); }

    // ================================================================ XP de la Rueda al matar
    public static void onKill(ServerWorld w, ServerPlayerEntity killer, LivingEntity victim) {
        Integer until = WHEEL_UNTIL.get(killer.getUuid());
        if (until != null) {
            if (now(w) > until) WHEEL_UNTIL.remove(killer.getUuid());
            else ExperienceOrbEntity.spawn(w, Fx.pos(victim), 10);
        }
        if (DestinyManager.get(killer).has("sur_4")) killer.heal(1f); // Cosecha de Almas: medio corazón
    }
}
