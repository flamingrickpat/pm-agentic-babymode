package com.agenticbabymode.mixin;

import com.agenticbabymode.server.WorldCheckpoint;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
	@Inject(method = "markDirty()V", at = @At("HEAD"))
	private void agenticBabymode$recordBlockEntityChange(CallbackInfo ci) {
		BlockEntity blockEntity = (BlockEntity) (Object) this;
		World world = blockEntity.getWorld();
		if (world instanceof ServerWorld serverWorld) {
			WorldCheckpoint.INSTANCE.recordBlockEntityChange(serverWorld, blockEntity.getPos());
		}
	}
}
