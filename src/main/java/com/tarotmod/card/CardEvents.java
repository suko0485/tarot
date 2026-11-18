package com.tarotmod.card;

import com.tarotmod.data.DestinyManager;
import com.tarotmod.data.PlayerData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/** Ganchos de eventos: inmunidad a caída, Balanza, Escudo Arcano, robo de vida del Diablo, marcas y bosses. */
public final class CardEvents {
    private CardEvents() {}

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity p) || !(p.getEntityWorld() instanceof ServerWorld w)) return true;
            int now = w.getServer().getTicks();

            // El Loco / Carro / Fuerza / Luna: sin daño de caída mientras dura
            Integer nf = CardAbilities.NO_FALL_UNTIL.get(p.getUuid());
            if (nf != null && source.isIn(DamageTypeTags.IS_FALL)) {
                if (now <= nf) return false;
                CardAbilities.NO_FALL_UNTIL.remove(p.getUuid());
            }
            // La Justicia: refleja 50% al atacante
            Integer rf = CardAbilities.REFLECT_UNTIL.get(p.getUuid());
            if (rf != null && now <= rf && source.getAttacker() instanceof LivingEntity att && att != p) {
                att.damage(w, w.getDamageSources().magic(), amount * 0.5f);
                w.spawnParticles(ParticleTypes.ENCHANTED_HIT, att.getX(), att.getBodyY(.5), att.getZ(), 10, .3, .3, .3, .1);
            }
            // La Rueda: golpe mortal -> ruleta que decide si vivís o morís
            if (CardAbilities.ROULETTE.contains(p.getUuid())) return false;
            if (amount >= p.getHealth() + p.getAbsorptionAmount() && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                int wl = CardAbilities.wheelLevel(p);
                Integer cdw = CardAbilities.ROULETTE_CD.get(p.getUuid());
                if (wl >= 0 && (cdw == null || now >= cdw)) { CardAbilities.startRoulette(w, p, wl); return false; }
            }
            // Escudo Arcano: un golpe fatal te deja con 3 corazones
            PlayerData d = DestinyManager.get(p);
            if (d.has("sur_3") && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && amount >= p.getHealth() + p.getAbsorptionAmount()) {
                Integer ready = CardAbilities.SHIELD_READY.get(p.getUuid());
                if (ready == null || now >= ready) {
                    CardAbilities.SHIELD_READY.put(p.getUuid(), now + (d.has("sur_5") ? 1200 : 2400));
                    p.setHealth(6f);
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 2));
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 3));
                    if (d.has("sur_5")) p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 1));
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_TOTEM_USE, 1f, 1.4f);
                    Fx.ring(w, ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 1.2, 30);
                    p.sendMessage(Text.literal("§b✦ Escudo Arcano: el destino te perdona la vida."), true);
                    return false;
                }
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (!blocked && source.getAttacker() instanceof ServerPlayerEntity p && entity != p && p.getEntityWorld() instanceof ServerWorld w)
                CardAbilities.onPactHit(w, p, taken);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.getEntityWorld() instanceof ServerWorld w)) return;
            CardAbilities.onDeathMark(w, entity);
            com.tarotmod.boss.BossManager.onDeath(w, entity, source);
            if (source.getAttacker() instanceof ServerPlayerEntity killer) CardAbilities.onKill(w, killer, entity);
        });
    }
}
