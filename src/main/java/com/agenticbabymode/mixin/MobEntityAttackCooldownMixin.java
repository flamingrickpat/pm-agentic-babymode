package com.agenticbabymode.mixin;

import com.agenticbabymode.config.ConfigManager;
import com.agenticbabymode.system.CombatMath;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Mob-side combat tuning, applied in {@link MobEntity}:
 *  - attackCooldownMultiplier: slows how often a mob can land melee hits.
 *  - damageMultiplier: scales mob melee damage dealt to players.
 */
@Mixin(MobEntity.class)
public abstract class MobEntityAttackCooldownMixin {
	@Unique
	private static final Map<MobEntity, Long> BABYMODE_LAST_ATTACK = new WeakHashMap<>();

	@Inject(method = "tryAttack", at = @At("HEAD"), cancellable = true)
	private void babymode$slowAttacks(Entity target, CallbackInfoReturnable<Boolean> cir) {
		MobEntity self = (MobEntity) (Object) this;
		double multiplier = ConfigManager.get().mobs.attackCooldownMultiplier;
		if (multiplier <= 1.0 || self.getWorld().isClient) {
			return;
		}
		long now = self.getWorld().getTime();
		Long last = BABYMODE_LAST_ATTACK.get(self);
		if (last != null && now - last < (long) (20.0 * multiplier)) {
			cir.setReturnValue(false);
		} else {
			BABYMODE_LAST_ATTACK.put(self, now);
		}
	}

	@Redirect(method = "tryAttack",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
	private boolean babymode$scaleMobMelee(Entity target, DamageSource source, float amount) {
		if (target instanceof PlayerEntity && !target.getWorld().isClient) {
			amount = CombatMath.mobDamageToPlayer(amount, ConfigManager.get().mobs.damageMultiplier);
		}
		return target.damage(source, amount);
	}
}
