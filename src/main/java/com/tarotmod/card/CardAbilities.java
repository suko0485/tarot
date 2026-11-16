package com.tarotmod.card;

import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.entity.ExperienceOrbEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Todas las habilidades activas/pasivas. `level` = 0 Normal, 1 Astral, 2 Cósmica. */
public final class CardAbilities {
    private CardAbilities() {}

    /** Jugadores con la Rueda de la Fortuna activa -> tick (del servidor) en que expira. */
    public static final Map<UUID, Integer> WHEEL_UNTIL = new ConcurrentHashMap<>();

    private static double scale(int level) { return 1.0 + 0.5 * level; }

    private static void effect(ServerPlayerEntity p, RegistryEntry<StatusEffect> e, int ticks, int amp) {
        p.addStatusEffect(new StatusEffectInstance(e, ticks, amp, true, true));
    }

    private static void passiveEffect(ServerPlayerEntity p, RegistryEntry<StatusEffect> e, int amp) {
        // 60 ticks, ambient + sin partículas: se renueva cada segundo mientras la carta esté en la mano secundaria
        p.addStatusEffect(new StatusEffectInstance(e, 60, amp, true, false, true));
    }

    // ---------------------------------------------------------------- 0 · EL LOCO
    private static final List<RegistryEntry<StatusEffect>> RANDOM_EFFECTS = List.of(
            StatusEffects.SPEED, StatusEffects.JUMP_BOOST, StatusEffects.STRENGTH, StatusEffects.REGENERATION,
            StatusEffects.NIGHT_VISION, StatusEffects.SLOW_FALLING, StatusEffects.GLOWING, StatusEffects.LUCK,
            StatusEffects.HASTE, StatusEffects.RESISTANCE, StatusEffects.INVISIBILITY,
            StatusEffects.LEVITATION, StatusEffects.NAUSEA, StatusEffects.BLINDNESS);

    public static void foolActive(ServerWorld w, ServerPlayerEntity p, int level) {
        var rnd = w.getRandom();
        if (rnd.nextBoolean() && !w.getDimension().hasCeiling()) {
            for (int i = 0; i < 12; i++) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                double dist = 8 + rnd.nextDouble() * (16 + 8 * level);
                int x = (int) Math.floor(p.getX() + Math.cos(ang) * dist);
                int z = (int) Math.floor(p.getZ() + Math.sin(ang) * dist);
                int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
                BlockPos pos = new BlockPos(x, y, z);
                if (w.getWorldBorder().contains(pos)
                        && !w.getBlockState(pos.down()).isAir()
                        && w.getFluidState(pos.down()).isEmpty()
                        && w.getFluidState(pos).isEmpty()) {
                    w.spawnParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, .4, .8, .4, .2);
                    p.requestTeleport(x + 0.5, y, z + 0.5);
                    w.spawnParticles(ParticleTypes.PORTAL, x + .5, y + 1, z + .5, 40, .4, .8, .4, .2);
                    p.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    p.sendMessage(Text.literal("§dEl Loco da un paso al vacío..."), true);
                    return;
                }
            }
        }
        RegistryEntry<StatusEffect> e = RANDOM_EFFECTS.get(rnd.nextInt(RANDOM_EFFECTS.size()));
        effect(p, e, (int) (400 * scale(level)), level);
        p.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.7f);
        p.sendMessage(Text.literal("§dEl azar te concede: ").append(e.value().getName()), true);
    }

    public static void foolPassive(ServerWorld w, ServerPlayerEntity p, int level) {
        // "Salto de fe": caída lenta mientras caes
        if (p.fallDistance > 3) passiveEffect(p, StatusEffects.SLOW_FALLING, 0);
    }

    // ---------------------------------------------------------------- I · EL MAGO
    public static void magicianActive(ServerWorld w, ServerPlayerEntity p, int level) {
        Vec3d start = p.getEyePos();
        Vec3d dir = p.getRotationVec(1.0f);
        float dmg = (float) (6 * scale(level));
        p.playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1f, 1.3f);
        for (double d = 1; d <= 24; d += 0.5) {
            Vec3d pt = start.add(dir.multiply(d));
            BlockPos bp = BlockPos.ofFloored(pt);
            w.spawnParticles(ParticleTypes.WITCH, pt.x, pt.y, pt.z, 2, .05, .05, .05, 0);
            w.spawnParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            if (!w.getBlockState(bp).getCollisionShape(w, bp).isEmpty()) return;
            Box box = new Box(pt, pt).expand(0.6);
            List<LivingEntity> hit = w.getEntitiesByClass(LivingEntity.class, box, e -> e != p && e.isAlive());
            if (!hit.isEmpty()) {
                LivingEntity target = hit.get(0);
                target.damage(w, w.getDamageSources().playerAttack(p), dmg);
                w.spawnParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getBodyY(0.5), target.getZ(), 20, .3, .3, .3, .1);
                return;
            }
        }
    }

    public static void magicianPassive(ServerWorld w, ServerPlayerEntity p, int level) {
        // TODO (necesita mixin): descuento real de XP al encantar. Por ahora: manos rápidas arcanas.
        passiveEffect(p, StatusEffects.HASTE, level);
    }

    // ---------------------------------------------------------------- III · LA EMPERATRIZ
    private static int growCrops(ServerWorld w, BlockPos center, int radius, int maxGrown) {
        int grown = 0;
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -1, -radius), center.add(radius, 1, radius))) {
            if (grown >= maxGrown) break;
            BlockState st = w.getBlockState(pos);
            if (st.getBlock() instanceof Fertilizable f && f.isFertilizable(w, pos, st) && f.canGrow(w, w.getRandom(), pos, st)) {
                f.grow(w, w.getRandom(), pos.toImmutable(), st);
                w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 4, .3, .3, .3, 0);
                grown++;
            }
        }
        return grown;
    }

    public static void empressActive(ServerWorld w, ServerPlayerEntity p, int level) {
        p.heal((float) (8 * scale(level)));
        growCrops(w, p.getBlockPos(), (int) (5 * scale(level)), 64);
        for (AnimalEntity a : w.getEntitiesByClass(AnimalEntity.class, p.getBoundingBox().expand(10), AnimalEntity::isAlive)) {
            a.heal(6);
            a.setTarget(null);
            a.lovePlayer(p);
        }
        p.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
    }

    public static void empressPassive(ServerWorld w, ServerPlayerEntity p, int level) {
        passiveEffect(p, StatusEffects.REGENERATION, level > 1 ? 1 : 0);
        growCrops(w, p.getBlockPos(), 3, 1 + level);
        for (AnimalEntity a : w.getEntitiesByClass(AnimalEntity.class, p.getBoundingBox().expand(8), AnimalEntity::isAlive)) {
            if (a.getTarget() == p) a.setTarget(null);
        }
    }

    // ---------------------------------------------------------------- VII · EL CARRO
    public static void chariotActive(ServerWorld w, ServerPlayerEntity p, int level) {
        Vec3d dir = p.getRotationVec(1.0f);
        Vec3d flat = new Vec3d(dir.x, 0, dir.z).normalize();
        double power = 2.2 * scale(level);
        p.setVelocity(flat.x * power, 0.25, flat.z * power);
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
        effect(p, StatusEffects.RESISTANCE, 40, 2);
        p.playSound(SoundEvents.ENTITY_HORSE_GALLOP, 1f, 0.8f);
        Box path = p.getBoundingBox().stretch(flat.multiply(6)).expand(1.2);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, path, e -> e != p && e.isAlive())) {
            e.damage(w, w.getDamageSources().playerAttack(p), (float) (8 * scale(level)));
            e.takeKnockback(1.5, -flat.x, -flat.z);
        }
        w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + .2, p.getZ(), 25, .5, .1, .5, .05);
    }

    public static void chariotPassive(ServerWorld w, ServerPlayerEntity p, int level) {
        passiveEffect(p, StatusEffects.SPEED, level);
        passiveEffect(p, StatusEffects.JUMP_BOOST, 0);
    }

    // ---------------------------------------------------------------- X · LA RUEDA DE LA FORTUNA
    public static void wheelActive(ServerWorld w, ServerPlayerEntity p, int level) {
        int dur = (int) (600 * scale(level)); // 30 s base
        boolean unlucky = w.getRandom().nextFloat() < 0.20f;
        if (unlucky) {
            effect(p, StatusEffects.UNLUCK, dur, 2);
            effect(p, StatusEffects.WEAKNESS, dur, 0);
            effect(p, StatusEffects.MINING_FATIGUE, dur / 2, 0);
            p.sendMessage(Text.literal("§cLa Rueda gira en tu contra..."), true);
            p.playSound(SoundEvents.ENTITY_WITHER_HURT, 0.6f, 0.6f);
        } else {
            effect(p, StatusEffects.LUCK, dur, 3);
            effect(p, StatusEffects.STRENGTH, dur, 1 + level);
            WHEEL_UNTIL.put(p.getUuid(), w.getServer().getTicks() + dur);
            p.sendMessage(Text.literal("§6¡La Rueda gira a tu favor! XP doble y golpes potenciados."), true);
            p.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    public static void wheelPassive(ServerWorld w, ServerPlayerEntity p, int level) {
        passiveEffect(p, StatusEffects.LUCK, level);
    }

    /** Llamado desde AFTER_DEATH: bonus de XP mientras la Rueda está activa. */
    public static void onKill(ServerWorld w, ServerPlayerEntity killer, LivingEntity victim) {
        Integer until = WHEEL_UNTIL.get(killer.getUuid());
        if (until == null) return;
        if (w.getServer().getTicks() > until) { WHEEL_UNTIL.remove(killer.getUuid()); return; }
        ExperienceOrbEntity.spawn(w, new Vec3d(victim.getX(), victim.getY(), victim.getZ()), 10);
    }

    // ---------------------------------------------------------------- XVI · LA TORRE
    public static void towerActive(ServerWorld w, ServerPlayerEntity p, int level) {
        HitResult hit = p.raycast(40, 1.0f, false);
        Vec3d t = hit.getPos();
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w, SpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setPosition(t);
            bolt.setCosmetic(true); // sin fuego ni daño a ítems: el daño lo controlamos nosotros
            w.spawnEntity(bolt);
        }
        w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, t.x, t.y + 0.5, t.z, 2, .5, .2, .5, 0);
        double r = 5 * scale(level);
        Box area = new Box(t, t).expand(r);
        // Solo LivingEntity: los ítems del suelo NO se tocan
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, area, e -> e != p && e.isAlive())) {
            e.damage(w, w.getDamageSources().playerAttack(p), (float) (14 * scale(level)));
        }
    }

    public static void towerPassive(ServerWorld w, ServerPlayerEntity p, int level) {
        if (w.isThundering()) passiveEffect(p, StatusEffects.RESISTANCE, 1);
    }

    // ---------------------------------------------------------------- XVII · LA ESTRELLA
    public static void starActive(ServerWorld w, ServerPlayerEntity p, int level) {
        effect(p, StatusEffects.NIGHT_VISION, (int) (1200 * scale(level)), 0);
        effect(p, StatusEffects.REGENERATION, (int) (400 * scale(level)), 1);
        for (StatusEffectInstance inst : new ArrayList<>(p.getStatusEffects())) {
            if (inst.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
                p.removeStatusEffect(inst.getEffectType());
            }
        }
        w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 40, .5, 1, .5, .05);
        p.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
    }

    public static void starPassive(ServerWorld w, ServerPlayerEntity p, int level) {
        passiveEffect(p, StatusEffects.NIGHT_VISION, 0);
    }

    // ---------------------------------------------------------------- XIX · EL SOL
    public static void sunActive(ServerWorld w, ServerPlayerEntity p, int level) {
        double r = 12 * scale(level);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(r),
                e -> e != p && e.isAlive() && e.getType().isIn(EntityTypeTags.UNDEAD))) {
            e.setOnFireForTicks(200);
            e.damage(w, w.getDamageSources().playerAttack(p), (float) (6 * scale(level)));
            w.spawnParticles(ParticleTypes.FLAME, e.getX(), e.getBodyY(0.5), e.getZ(), 12, .3, .5, .3, .02);
        }
        effect(p, StatusEffects.FIRE_RESISTANCE, (int) (1200 * scale(level)), 0);
        w.spawnParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 60, 1.5, .5, 1.5, .03);
        p.playSound(SoundEvents.ITEM_FIRECHARGE_USE, 1f, 0.8f);
    }

    public static void sunPassive(ServerWorld w, ServerPlayerEntity p, int level) {
        passiveEffect(p, StatusEffects.FIRE_RESISTANCE, 0);
    }
}
