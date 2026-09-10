package com.agenticbabymode.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Block.class)
public abstract class BlockAutoCollectMixin {
    @Redirect(method = "afterBreak", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/block/Block;dropStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V"))
    private void babymode$collect(BlockState state, World world, BlockPos pos,
                                 BlockEntity blockEntity, Entity entity, ItemStack tool) {
        ServerWorld serverWorld = (ServerWorld) world;
        PlayerEntity player = (PlayerEntity) entity;
        // afterBreak already enforces survival harvesting and the correct tool.
        if (world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) {
            for (ItemStack stack : Block.getDroppedStacks(state, serverWorld, pos, blockEntity, player, tool)) {
                player.getInventory().insertStack(stack);
                if (!stack.isEmpty()) Block.dropStack(world, pos, stack);
            }
        }
        state.onStacksDropped(serverWorld, pos, tool, true);
    }
}
