package com.agenticbabymode.mixin;

import com.agenticbabymode.config.ConfigManager;
import com.agenticbabymode.system.CombatMath;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Scales player melee damage dealt to mobs (mobs.damageTakenMultiplier).
 * Covers the main hit and the sweep hit in PlayerEntity#attack.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityAttackMixin {
	@Redirect(method = "attack",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
	private boolean babymode$scalePlayerMelee(Entity target, DamageSource source, float amount) {
		return babymode$apply(target, source, amount);
	}

	@Redirect(method = "attack",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
	private boolean babymode$scalePlayerSweep(net.minecraft.entity.LivingEntity target, DamageSource source, float amount) {
		return babymode$apply(target, source, amount);
	}

	private static boolean babymode$apply(Entity target, DamageSource source, float amount) {
		if (target instanceof MobEntity && !target.getWorld().isClient) {
			amount = CombatMath.playerDamageToMob(amount, ConfigManager.get().mobs.damageTakenMultiplier);
		}
		return target.damage(source, amount);
	}
}
