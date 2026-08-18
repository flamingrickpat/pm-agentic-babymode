package com.agenticbabymode.mixin;

import com.agenticbabymode.config.ConfigManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.WeakHashMap;
import java.util.Map;

/**
 * Slows air consumption while underwater for players, implementing the
 * environment.drowningTimeMultiplier config value (10x default = air drops
 * one point every 10 ticks instead of every tick).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAirMixin {
	@Unique
	private static final Map<LivingEntity, Integer> BABYMODE_UNDERWATER_COUNTER = new WeakHashMap<>();

	@Inject(method = "getNextAirUnderwater", at = @At("HEAD"), cancellable = true)
	private void babymode$slowDrowning(int airSupply, CallbackInfoReturnable<Integer> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof PlayerEntity)) {
			return;
		}
		double multiplier = ConfigManager.get().environment.drowningTimeMultiplier;
		if (multiplier <= 1.0) {
			return;
		}
		Integer counter = BABYMODE_UNDERWATER_COUNTER.get(self);
		int next = (counter == null ? 0 : counter) + 1;
		if (next < (int) Math.round(multiplier)) {
			BABYMODE_UNDERWATER_COUNTER.put(self, next);
			cir.setReturnValue(airSupply); // hold air this tick
		} else {
			BABYMODE_UNDERWATER_COUNTER.put(self, 0);
			cir.setReturnValue(airSupply - 1); // normal 1-point drop
		}
	}
}
