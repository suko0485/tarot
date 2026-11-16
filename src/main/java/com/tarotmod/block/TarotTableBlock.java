package com.tarotmod.block;

import com.mojang.serialization.MapCodec;
import com.tarotmod.screen.TarotTableScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Mesa de Lectura del Tarot. Sin BlockEntity: el inventario vive en el ScreenHandler y se devuelve al cerrar (como la mesa de crafteo). */
public class TarotTableBlock extends Block {
    public static final MapCodec<TarotTableBlock> CODEC = createCodec(TarotTableBlock::new);

    public TarotTableBlock(Settings settings) { super(settings); }

    @Override
    protected MapCodec<? extends Block> getCodec() { return CODEC; }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.SUCCESS;
        player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
        return ActionResult.SUCCESS;
    }

    @Override
    protected NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory(
                (syncId, inv, player) -> new TarotTableScreenHandler(syncId, inv, ScreenHandlerContext.create(world, pos)),
                Text.translatable("container.tarot.tarot_table"));
    }
}
