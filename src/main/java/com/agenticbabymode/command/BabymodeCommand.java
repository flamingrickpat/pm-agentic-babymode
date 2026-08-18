package com.agenticbabymode.command;

import com.agenticbabymode.AgenticBabymodeMod;
import com.agenticbabymode.config.ConfigManager;
import com.agenticbabymode.server.BabymodeServer;
import com.agenticbabymode.state.PlayerState;
import com.agenticbabymode.system.FatigueSystem;
import com.agenticbabymode.system.NutritionSystem;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public final class BabymodeCommand {
	private BabymodeCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("babymode")
					.then(literal("version").executes(ctx -> version(ctx)))
					.then(literal("reload").executes(ctx -> reload(ctx)))
					.then(literal("status")
							.executes(ctx -> status(ctx, false))
							.then(literal("json").executes(ctx -> status(ctx, true)))));
		});
	}

	private static int version(CommandContext<ServerCommandSource> ctx) {
		ctx.getSource().sendFeedback(Text.literal("Agentic Babymode v" + AgenticBabymodeMod.MOD_VERSION), false);
		return 1;
	}

	private static int reload(CommandContext<ServerCommandSource> ctx) {
		ConfigManager.reload();
		if (BabymodeServer.INSTANCE != null) {
			BabymodeServer.INSTANCE.applyConfig();
		}
		ctx.getSource().sendFeedback(Text.literal("Agentic Babymode config reloaded"), true);
		return 1;
	}

	private static int status(CommandContext<ServerCommandSource> ctx, boolean json) {
		ServerPlayerEntity player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFeedback(Text.literal("Agentic Babymode: run this in game to see player state"), false);
			return 0;
		}
		PlayerState ps = BabymodeServer.INSTANCE != null
				? BabymodeServer.INSTANCE.getState().get(player.getUuid())
				: null;
		if (ps == null) {
			ctx.getSource().sendFeedback(Text.literal("Agentic Babymode: no state yet"), false);
			return 0;
		}

		if (json) {
			JsonObject root = new JsonObject();
			root.addProperty("version", AgenticBabymodeMod.MOD_VERSION);
			root.addProperty("fatigue", ps.fatigue);
			JsonObject nutrition = new JsonObject();
			nutrition.addProperty("grain", ps.grain);
			nutrition.addProperty("protein", ps.protein);
			nutrition.addProperty("produce", ps.produce);
			root.add("nutrition", nutrition);
			root.addProperty("health", player.getHealth());
			root.addProperty("hunger", player.getHungerManager().getFoodLevel());
			root.addProperty("air", player.getAir());
			ctx.getSource().sendFeedback(Text.literal(root.toString()), false);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("Agentic Babymode v").append(AgenticBabymodeMod.MOD_VERSION).append('\n');
			sb.append(String.format("Health: %.1f/%.1f  Hunger: %d/20  Air: %d/%d",
					player.getHealth(), player.getMaxHealth(),
					player.getHungerManager().getFoodLevel(),
					player.getAir(), player.getMaxAir())).append('\n');
			sb.append(String.format("Fatigue: %.0f/100 (band %d)",
					ps.fatigue, FatigueSystem.band(ps.fatigue))).append('\n');
			sb.append("Nutrition:\n");
			sb.append(String.format("  Grain:   %.0f/100", ps.grain)).append('\n');
			sb.append(String.format("  Protein: %.0f/100", ps.protein)).append('\n');
			sb.append(String.format("  Produce: %.0f/100", ps.produce)).append('\n');
			sb.append(String.format("  Average: %.0f/100", NutritionSystem.average(ps)));
			ctx.getSource().sendFeedback(Text.literal(sb.toString()), false);
		}
		return 1;
	}
}
