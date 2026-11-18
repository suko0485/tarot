package com.tarotmod.block;

import com.mojang.serialization.MapCodec;
import com.tarotmod.boss.BossManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Altar Arcano: clic derecho para intentar el ritual de invocación. */
public class AltarBlock extends Block {
    public static final MapCodec<AltarBlock> CODEC = createCodec(AltarBlock::new);
    public AltarBlock(Settings s) { super(s); }
    @Override protected MapCodec<? extends Block> getCodec() { return CODEC; }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world instanceof ServerWorld sw && player instanceof ServerPlayerEntity sp) BossManager.tryRitual(sw, sp, pos);
        return ActionResult.SUCCESS;
    }
}
