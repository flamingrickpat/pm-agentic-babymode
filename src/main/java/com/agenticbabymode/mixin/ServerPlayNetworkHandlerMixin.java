package com.agenticbabymode.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables vanilla's "disconnect.spam" kick.
 *
 * Vanilla calls checkForSpam() after every chat message AND every executed
 * command: it adds 20 to a per-player message cooldown (which decays by 1 per
 * tick) and kicks the player once it exceeds 200. That threshold is far too
 * aggressive for agents that legitimately issue commands several times per
 * second. Cancelling checkForSpam() keeps the cooldown at 0 forever, so the
 * kick can never trigger; nothing else reads that counter.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
	@Inject(method = "checkForSpam", at = @At("HEAD"), cancellable = true)
	private void babymode$disableSpamKick(CallbackInfo ci) {
		ci.cancel();
	}
}
