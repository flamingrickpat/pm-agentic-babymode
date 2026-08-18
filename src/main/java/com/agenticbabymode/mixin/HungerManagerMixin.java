package com.agenticbabymode.mixin;

import com.agenticbabymode.config.ConfigManager;
import net.minecraft.entity.player.HungerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scales vanilla action exhaustion (sprinting, jumping, mining, ...) by
 * environment.hungerActivityExhaustionMultiplier so the food bar lasts longer.
 * Replicates the vanilla addExhaustion clamp (min(exhaustion + value, 40)).
 */
@Mixin(HungerManager.class)
public abstract class HungerManagerMixin {
	@Shadow
	private float exhaustion;

	@Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
	private void babymode$scaleExhaustion(float amount, CallbackInfo ci) {
		double multiplier = ConfigManager.get().environment.hungerActivityExhaustionMultiplier;
		if (multiplier <= 0.0 || Math.abs(multiplier - 1.0) < 1e-9) {
			return;
		}
		ci.cancel();
		this.exhaustion = Math.min(this.exhaustion + (float) (amount * multiplier), 40.0F);
	}
}
