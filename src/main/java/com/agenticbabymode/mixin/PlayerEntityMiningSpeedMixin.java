package com.agenticbabymode.mixin;

import com.agenticbabymode.config.ConfigManager;
import com.agenticbabymode.config.ModConfig;
import com.agenticbabymode.server.BabymodeServer;
import com.agenticbabymode.state.PlayerState;
import com.agenticbabymode.system.FatigueSystem;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Multiplies block breaking speed by player.miningSpeedMultiplier and the
 * fatigue penalty.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMiningSpeedMixin {
	@Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
	private void babymode$scaleMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		ModConfig cfg = ConfigManager.get();
		double fatigueFactor = 1.0;
		BabymodeServer server = BabymodeServer.INSTANCE;
		if (server != null) {
			PlayerState ps = server.getState() != null ? server.getState().get(self.getUuid()) : null;
			if (ps != null) {
				fatigueFactor = FatigueSystem.movementFactor(ps.fatigue, cfg.fatigue.movementPenaltyPercent);
			}
		}
		float scaled = (float) (cir.getReturnValue() * cfg.player.miningSpeedMultiplier * fatigueFactor);
		cir.setReturnValue(scaled);
	}
}
