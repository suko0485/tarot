package com.tarotmod.world;

import com.tarotmod.block.ModBlocks;
import com.tarotmod.item.ModItems;
import com.tarotmod.item.TarotCardItem;
import com.tarotmod.prophecy.TraderOffers;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;

/**
 * Santuarios del Tarot. Posición determinista por hash de chunk (no guarda nada):
 * ~1 de cada 150 chunks, solo Overworld. Se construye cuando un jugador se acerca.
 * Ruinas místicas de 11x11: pilares de piedra cincelada con faroles de almas, alfombra violeta, velas, amatistas,
 * una Mesa del Tarot (funcional o dañada), un cofre oculto bajo una baldosa agrietada con cartas iniciales,
 * 2 guardianes y El Vidente (comerciante).
 */
public final class Shrine {
    private Shrine() {}

    private static boolean isShrineChunk(ServerWorld w, int cx, int cz) {
        long h = cx * 341873128712L + cz * 132897987541L + w.getSeed() * 31L;
        h ^= (h >>> 29); h *= 0xBF58476D1CE4E5B9L; h ^= (h >>> 32);
        return Math.floorMod(h, 150L) == 0;
    }

    private static final int R = 5;

    public static void tick(ServerWorld w, ServerPlayerEntity p) {
        if (!w.getRegistryKey().equals(World.OVERWORLD)) return;
        int pcx = p.getBlockPos().getX() >> 4, pcz = p.getBlockPos().getZ() >> 4;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            int cx = pcx + dx, cz = pcz + dz;
            if (!isShrineChunk(w, cx, cz) || !w.getChunkManager().isChunkLoaded(cx, cz)) continue;
            int x = (cx << 4) + 8, z = (cz << 4) + 8;
            int y = w.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
            BlockPos c = new BlockPos(x, y, z);
            if (!w.getFluidState(c.down()).isEmpty()) continue;
            if (w.getBlockState(c.down()).isOf(Blocks.AMETHYST_BLOCK)) {            // ya construido: pista sutil del cofre oculto
                if (p.squaredDistanceTo(c.getX(), c.getY(), c.getZ()) < 14 * 14)
                    w.spawnParticles(ParticleTypes.ENCHANT, c.getX() + 3.5, c.getY() + .3, c.getZ() + 2.5, 12, .3, .3, .3, .3);
                continue;
            }
            build(w, c);
        }
    }

    private static void put(ServerWorld w, BlockPos pos, BlockState st) { w.setBlockState(pos, st); }

    private static void build(ServerWorld w, BlockPos c) {
        var r = w.getRandom();
        // 1) terreno: relleno bajo la plataforma y despeje del aire superior
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            for (int dy = -2; dy >= -5; dy--) {
                BlockPos q = c.add(dx, dy, dz);
                if (w.getBlockState(q).isAir() || !w.getFluidState(q).isEmpty() || w.getBlockState(q).isReplaceable()) put(w, q, Blocks.STONE_BRICKS.getDefaultState());
            }
            for (int h = 0; h < 8; h++) put(w, c.add(dx, h, dz), Blocks.AIR.getDefaultState());
        }
        // 2) suelo ruinoso: borde de ladrillos, centro de deepslate pulido agrietado
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            boolean edge = Math.abs(dx) == R || Math.abs(dz) == R;
            float f = r.nextFloat();
            BlockState floor = edge ? (f < .35f ? Blocks.MOSSY_STONE_BRICKS.getDefaultState() : f < .5f ? Blocks.CRACKED_STONE_BRICKS.getDefaultState() : Blocks.CHISELED_STONE_BRICKS.getDefaultState())
                    : (f < .15f ? Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState() : f < .25f ? Blocks.MOSSY_COBBLESTONE.getDefaultState() : Blocks.POLISHED_DEEPSLATE.getDefaultState());
            put(w, c.add(dx, -1, dz), floor);
        }
        // 3) alfombra violeta en cruz (camino de entrada por el sur)
        for (int i = -R + 1; i <= R - 1; i++) {
            if (r.nextFloat() > .1f) put(w, c.add(0, 0, i), Blocks.PURPLE_CARPET.getDefaultState());
            if (Math.abs(i) <= 2 && r.nextFloat() > .1f) put(w, c.add(i, 0, 0), Blocks.PURPLE_CARPET.getDefaultState());
        }
        // 4) pilares de piedra cincelada, algunos rotos, con faroles de almas
        for (int sx : new int[]{-4, 4}) for (int sz : new int[]{-4, 4, 0}) {
            if (sz == 0 && r.nextBoolean()) continue;
            int hgt = 3 + r.nextInt(3);
            put(w, c.add(sx, 0, sz), Blocks.CHISELED_STONE_BRICKS.getDefaultState());
            for (int h = 1; h < hgt; h++) put(w, c.add(sx, h, sz), (h % 2 == 0 ? Blocks.STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS).getDefaultState());
            if (hgt == 5) { put(w, c.add(sx, 5, sz), Blocks.CHISELED_STONE_BRICKS.getDefaultState()); put(w, c.add(sx, 6, sz), Blocks.SOUL_LANTERN.getDefaultState()); }
            else put(w, c.add(sx, hgt, sz), Blocks.SOUL_LANTERN.getDefaultState());
        }
        // 5) velas y amatistas
        for (int[] cp : new int[][]{{-2, -1}, {2, -1}, {-2, 1}, {2, 1}}) put(w, c.add(cp[0], 0, cp[1]), Blocks.CANDLE.getDefaultState().with(AbstractCandleBlock.LIT, true));
        for (int i = 0; i < 6; i++) {
            int ax = r.nextInt(2 * R - 1) - R + 1, az = r.nextInt(2 * R - 1) - R + 1;
            BlockPos ap = c.add(ax, 0, az);
            if (w.getBlockState(ap).isAir()) put(w, ap, Blocks.AMETHYST_CLUSTER.getDefaultState());
        }
        // 6) la Mesa: marca de construcción = bloque de amatista bajo el centro
        put(w, c.down(), Blocks.AMETHYST_BLOCK.getDefaultState());
        if (r.nextFloat() < .6f) {
            put(w, c, ModBlocks.TAROT_TABLE.getDefaultState());                       // funcional
        } else {
            put(w, c, Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState());              // dañada: solo ruina
            put(w, c.up(), Blocks.CANDLE.getDefaultState().with(AbstractCandleBlock.LIT, true));
            put(w, c.add(1, 0, 0), Blocks.COBWEB.getDefaultState());
        }
        // 7) cofre OCULTO bajo una baldosa agrietada con cartas iniciales
        put(w, c.add(3, -1, 2), Blocks.CRACKED_STONE_BRICKS.getDefaultState());
        BlockPos chestPos = c.add(3, -2, 2);
        put(w, chestPos, Blocks.CHEST.getDefaultState());
        if (w.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            var starters = new ArrayList<TarotCardItem>();
            for (TarotCardItem it : ModItems.CARDS.values()) if (it.card().rarity() == net.minecraft.util.Rarity.COMMON || it.card().rarity() == net.minecraft.util.Rarity.UNCOMMON) starters.add(it);
            chest.setStack(0, new ItemStack(ModItems.BOOSTER_PACK, 2 + r.nextInt(2)));
            chest.setStack(1, new ItemStack(ModItems.STARDUST, 3 + r.nextInt(4)));
            chest.setStack(2, new ItemStack(Items.AMETHYST_SHARD, 4 + r.nextInt(6)));
            chest.setStack(3, new ItemStack(starters.get(r.nextInt(starters.size()))));
            chest.setStack(4, new ItemStack(starters.get(r.nextInt(starters.size()))));
            chest.setStack(5, new ItemStack(starters.get(r.nextInt(starters.size()))));
        }
        // 8) guardianes y El Vidente
        for (int i = 0; i < 2; i++) {
            MobEntity g = EntityType.ZOMBIE.create(w, SpawnReason.STRUCTURE);
            if (g == null) continue;
            g.setPosition(c.getX() + (i == 0 ? 3 : -3) + .5, c.getY(), c.getZ() - 3.5);
            g.setCustomName(Text.literal("Guardián Arcano"));
            g.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            g.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            g.setPersistent();
            w.spawnEntity(g);
        }
        WanderingTraderEntity seer = EntityType.WANDERING_TRADER.create(w, SpawnReason.STRUCTURE);
        if (seer != null) {
            seer.setPosition(c.getX() + .5, c.getY(), c.getZ() + 3.5);
            seer.setCustomName(Text.literal("§dEl Vidente"));
            seer.setCustomNameVisible(true);
            seer.setPersistent();
            TraderOffers.addSeerOffers(seer);
            w.spawnEntity(seer);
        }
    }
}
