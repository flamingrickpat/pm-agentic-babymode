package com.agenticbabymode.command;

import com.agenticbabymode.AgenticBabymodeMod;
import com.agenticbabymode.config.ConfigManager;
import com.agenticbabymode.config.ModConfig;
import com.agenticbabymode.server.BabymodeServer;
import com.agenticbabymode.state.BabymodeState;
import com.agenticbabymode.state.PlayerState;
import com.agenticbabymode.system.NutritionSystem;
import com.agenticbabymode.system.SleepinessSystem;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class BabymodeCommand {
	private BabymodeCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("babymode")
					.then(literal("version").executes(ctx -> version(ctx)))
					.then(literal("help").executes(ctx -> help(ctx)))
					.then(literal("reload").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> reload(ctx)))
					.then(literal("reset").requires(s -> s.hasPermissionLevel(2))
							.then(argument("amount", IntegerArgumentType.integer(0, 100))
									.executes(ctx -> reset(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))
					.then(literal("status")
							.executes(ctx -> status(ctx, false))
							.then(literal("json").executes(ctx -> status(ctx, true)))));
		});
	}

	private static int version(CommandContext<ServerCommandSource> ctx) {
		ctx.getSource().sendFeedback(Text.literal("Agentic Babymode v" + AgenticBabymodeMod.MOD_VERSION), false);
		return 1;
	}

	private static int help(CommandContext<ServerCommandSource> ctx) {
		ModConfig c = ConfigManager.get();
		StringBuilder sb = new StringBuilder();
		sb.append("Agentic Babymode v").append(AgenticBabymodeMod.MOD_VERSION).append(" — current rules:\n");
		sb.append(String.format("Player: move %.2fx | mine %.1fx | underwater air %.1fx | mined loot to inventory, overflow to ground | natural regen %s | PvP %s\n",
				c.player.movementSpeedMultiplier, c.player.miningSpeedMultiplier,
				c.environment.drowningTimeMultiplier,
				c.player.naturalRegeneration ? "on" : "off",
				c.player.pvp ? "on" : "off"));
		sb.append(String.format("Mobs: move %.2fx | deal %.2fx damage | attack cooldown %.1fx | take %.1fx from you\n",
				c.mobs.movementSpeedMultiplier, c.mobs.damageMultiplier,
				c.mobs.attackCooldownMultiplier, c.mobs.damageTakenMultiplier));
		sb.append(String.format("Sleepiness: 0→100 over %.1f days; only a bed resets it. Slowness/Mining Fatigue at 50+, can't sprint at 100.\n",
				c.sleepiness.fullAfterDays));
		sb.append(String.format("Nutrition (grain/protein/produce 0-100): decays to 0 over %.1f days.\n",
				c.nutrition.daysToEmpty));
		sb.append(String.format("  grain <%s/<%s → Slowness I/II | protein <%s/<%s → Weakness I/II | produce <%s/<%s → Mining Fatigue I/II\n",
				c.nutrition.deficitThresholdI, c.nutrition.deficitThresholdII,
				c.nutrition.deficitThresholdI, c.nutrition.deficitThresholdII,
				c.nutrition.deficitThresholdI, c.nutrition.deficitThresholdII));
		sb.append(String.format("  avg <%s → STARVING (-4 max HP, hunger, no sprint)%s\n",
				c.nutrition.starvingThreshold,
				c.nutrition.enableWellFedBuffs
						? String.format(" | avg ≥%s → Regen+Strength+Haste", c.nutrition.wellFedThreshold)
						: ""));
		sb.append(String.format("Food bar: drains to 0 in %.1f days idle; actions drain %.2fx.\n",
				c.environment.hungerPassiveDaysToEmpty, c.environment.hungerActivityExhaustionMultiplier));
		sb.append("Commands: /babymode version | help | status | status json | reload | reset <0-100>");
		ctx.getSource().sendFeedback(Text.literal(sb.toString()), false);
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

	private static int reset(CommandContext<ServerCommandSource> ctx, int amount) {
		if (BabymodeServer.INSTANCE == null || BabymodeServer.INSTANCE.getState() == null) {
			ctx.getSource().sendFeedback(Text.literal("Agentic Babymode: no server state yet"), false);
			return 0;
		}
		BabymodeState state = BabymodeServer.INSTANCE.getState();
		int count = 0;
		for (ServerPlayerEntity player : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
			PlayerState ps = state.getOrCreate(player.getUuid());
			ps.grain = amount;
			ps.protein = amount;
			ps.produce = amount;
			ps.sleepiness = 0.0;
			count++;
		}
		state.markDirty();
		ctx.getSource().sendFeedback(Text.literal("Set nutrition to " + amount
				+ "/100 and sleepiness to 0 for " + count + " player(s)"), true);
		return 1;
	}

	private static int status(CommandContext<ServerCommandSource> ctx, boolean json) {
		ServerPlayerEntity player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFeedback(Text.literal("Agentic Babymode: run this in game to see player state"), false);
			return 0;
		}
		BabymodeState state = BabymodeServer.INSTANCE != null ? BabymodeServer.INSTANCE.getState() : null;
		PlayerState ps = state != null ? state.get(player.getUuid()) : null;
		if (ps == null) {
			ctx.getSource().sendFeedback(Text.literal("Agentic Babymode: no state yet"), false);
			return 0;
		}

		if (json) {
			JsonObject root = new JsonObject();
			root.addProperty("version", AgenticBabymodeMod.MOD_VERSION);
			root.addProperty("sleepiness", ps.sleepiness);
			root.addProperty("fatigue", ps.sleepiness); // alias for older integrations
			JsonObject nutrition = new JsonObject();
			nutrition.addProperty("grain", ps.grain);
			nutrition.addProperty("protein", ps.protein);
			nutrition.addProperty("produce", ps.produce);
			root.add("nutrition", nutrition);
			root.addProperty("health", player.getHealth());
			root.addProperty("maxHealth", player.getMaxHealth());
			root.addProperty("hunger", player.getHungerManager().getFoodLevel());
			root.addProperty("air", player.getAir());
			JsonArray effects = new JsonArray();
			for (StatusEffectInstance inst : player.getStatusEffects()) {
				Identifier id = Registries.STATUS_EFFECT.getId(inst.getEffectType());
				JsonObject e = new JsonObject();
				e.addProperty("id", id.toString());
				e.addProperty("amplifier", inst.getAmplifier());
				e.addProperty("duration", inst.getDuration());
				effects.add(e);
			}
			root.add("effects", effects);
			ctx.getSource().sendFeedback(Text.literal(root.toString()), false);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("Agentic Babymode v").append(AgenticBabymodeMod.MOD_VERSION).append('\n');
			sb.append(String.format("Health: %.1f/%.1f  Hunger: %d/20  Air: %d/%d",
					player.getHealth(), player.getMaxHealth(),
					player.getHungerManager().getFoodLevel(),
					player.getAir(), player.getMaxAir())).append('\n');
			sb.append(String.format("Sleepiness: %.0f/100", ps.sleepiness)).append('\n');
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
