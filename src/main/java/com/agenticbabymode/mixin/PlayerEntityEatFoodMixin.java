package com.agenticbabymode.mixin;

import com.agenticbabymode.config.ConfigManager;
import com.agenticbabymode.server.BabymodeServer;
import com.agenticbabymode.state.PlayerState;
import com.agenticbabymode.system.FoodCatalog;
import com.agenticbabymode.system.NutritionGain;
import com.agenticbabymode.system.NutritionSystem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grants nutrition when a player eats food that is in the catalog.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityEatFoodMixin {
	@Inject(method = "eatFood", at = @At("RETURN"))
	private void babymode$grantNutrition(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
		if (!((Object) this instanceof ServerPlayerEntity player) || player.getWorld().isClient) {
			return;
		}
		if (!ConfigManager.get().nutrition.enabled || stack.isEmpty()) {
			return;
		}
		Identifier id = Registries.ITEM.getId(stack.getItem());
		NutritionGain gain = FoodCatalog.gainFor(id.toString());
		if (gain == null || gain.isEmpty()) {
			return;
		}
		BabymodeServer server = BabymodeServer.INSTANCE;
		if (server == null || server.getState() == null) {
			return;
		}
		PlayerState ps = server.getState().getOrCreate(player.getUuid());
		NutritionSystem.add(ps, gain);
		server.getState().markDirty();
	}
}
