package com.agenticbabymode.mixin;

import com.agenticbabymode.server.WorldCheckpoint;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin {
	@Inject(method = "setBlockState", at = @At("HEAD"))
	private void agenticBabymode$recordOriginalBlock(BlockPos pos, BlockState state, boolean moved,
			CallbackInfoReturnable<BlockState> cir) {
		WorldChunk chunk = (WorldChunk) (Object) this;
		if (chunk.getWorld() instanceof ServerWorld world) {
			WorldCheckpoint.INSTANCE.recordBlockChange(
					world,
					pos,
					chunk.getBlockState(pos),
					state,
					chunk.getBlockEntity(pos)
			);
		}
	}
}
