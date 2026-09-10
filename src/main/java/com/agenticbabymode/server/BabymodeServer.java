package com.agenticbabymode.server;

import com.agenticbabymode.AgenticBabymodeMod;
import com.agenticbabymode.config.ConfigManager;
import com.agenticbabymode.config.ModConfig;
import com.agenticbabymode.state.BabymodeState;
import com.agenticbabymode.state.PlayerState;
import com.agenticbabymode.system.NutritionSystem;
import com.agenticbabymode.system.SleepinessSystem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wires Fabric API events into the babymode systems. This is the "logical
 * server side" glue: it only runs inside the integrated/dedicated server.
 */
public final class BabymodeServer {
	public static BabymodeServer INSTANCE;

	private static final String MSG_PREFIX = "[Babymode] ";
	private static final UUID STARVING_HP_MODIFIER_ID = UUID.fromString("a7b14f72-3c9a-4b6e-9f0d-1c2d3e4f5a6b");
	private static final String STARVING_HP_MODIFIER_NAME = "agentic_babymode_starving_hp";
	private static final double STARVING_HP_PENALTY = -4.0;

	private final Map<UUID, Double> lastAppliedSpeed = new HashMap<>();
	private final Map<UUID, PlayerReport> lastReport = new HashMap<>();
	private final Map<UUID, Double> passiveFoodFraction = new HashMap<>();
	private final Map<UUID, Double> starvingFoodFraction = new HashMap<>();

	private MinecraftServer server;
	private ServerWorld overworld;
	private BabymodeState state;

	private BabymodeServer() {
	}

	public static void init() {
		INSTANCE = new BabymodeServer();
		INSTANCE.register();
	}

	public BabymodeState getState() {
		return state;
	}

	public MinecraftServer getServer() {
		return server;
	}

	/** Re-apply everything driven by config (used by /babymode reload). */
	public void applyConfig() {
		ModConfig cfg = ConfigManager.get();
		if (server != null) {
			server.getGameRules().get(GameRules.NATURAL_REGENERATION)
					.set(cfg.player.naturalRegeneration, server);
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				applyMovementSpeed(player);
			}
		}
	}

	private void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server1) -> {
			ServerPlayerEntity player = handler.player;
			ensureState();
			state.getOrCreate(player.getUuid());
			applyMovementSpeed(player);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, sender) -> {
			UUID uuid = handler.player.getUuid();
			lastReport.remove(uuid);
			lastAppliedSpeed.remove(uuid);
			passiveFoodFraction.remove(uuid);
			starvingFoodFraction.remove(uuid);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			// Dying is a soft-reset: back to full nutrition and zero sleepiness.
			resetOnRespawn(newPlayer);
			applyMovementSpeed(newPlayer);
		});

		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

		// Bed interaction = the only way to reset sleepiness (works even at day).
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClient || !(player instanceof ServerPlayerEntity spe)) {
				return ActionResult.PASS;
			}
			if (world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof BedBlock) {
				ensureState();
				PlayerState ps = state.getOrCreate(spe.getUuid());
				if (ps.sleepiness > 0.0) {
					SleepinessSystem.reset(ps);
					state.markDirty();
					send(spe, "You press against the bed... sleepiness reset to 0.");
				}
			}
			return ActionResult.PASS;
		});

		// PvP toggle
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			Entity attacker = source.getAttacker();
			ModConfig cfg = ConfigManager.get();
			if (!cfg.player.pvp && entity instanceof PlayerEntity && attacker instanceof PlayerEntity) {
				return false;
			}
			// Failsafe: disabled mobs can never harm the player even if one slips through.
			if (entity instanceof PlayerEntity && isDisabledMob(attacker)) {
				return false;
			}
			return true;
		});

		// Mob movement speed scaling on spawn/load, complete removal of disabled mobs,
		// and loot that never despawns.
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			ModConfig cfg = ConfigManager.get();
			if (!world.isClient && disableMobOnLoad(entity, cfg)) {
				return;
			}
			// Agent comfort: dropped items never expire so loot is never lost.
			if (!world.isClient && cfg.environment.itemsNeverDespawn && entity instanceof ItemEntity item) {
				item.setNeverDespawn();
			}
			double m = cfg.mobs.movementSpeedMultiplier;
			if (m == 1.0 || world.isClient || !(entity instanceof MobEntity mob)) {
				return;
			}
			EntityAttributeInstance inst = mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
			if (inst != null) {
				inst.setBaseValue(inst.getBaseValue() * m);
			}
		});
	}

	/** True when the entity is a mob the config says to fully remove (discard). */
	private static boolean disableMobOnLoad(Entity entity, ModConfig cfg) {
		ModConfig.MobsConfig m = cfg.mobs;
		boolean disabled = (m.disablePhantoms && entity instanceof PhantomEntity)
				|| (m.disableCreepers && entity instanceof CreeperEntity)
				|| (m.disablePillagers && entity instanceof PillagerEntity)
				|| (m.disableSkeletons && entity instanceof SkeletonEntity);
		if (disabled) {
			entity.discard();
		}
		return disabled;
	}

	/** True when the entity is one of the config-disabled hostile mobs. */
	private static boolean isDisabledMob(Entity entity) {
		if (entity == null) {
			return false;
		}
		ModConfig.MobsConfig m = ConfigManager.get().mobs;
		return (m.disablePhantoms && entity instanceof PhantomEntity)
				|| (m.disableCreepers && entity instanceof CreeperEntity)
				|| (m.disablePillagers && entity instanceof PillagerEntity)
				|| (m.disableSkeletons && entity instanceof SkeletonEntity);
	}

	private void onServerStarted(MinecraftServer server) {
		this.server = server;
		this.overworld = server.getOverworld();
		ensureState();
		applyConfig();
		AgenticBabymodeMod.LOGGER.info("Agentic Babymode state loaded ({} tracked players)",
				state.getPlayers().size());
	}

	private void onServerStopping(MinecraftServer server) {
		if (state != null) {
			state.markDirty();
		}
	}

	private void ensureState() {
		if (state == null && overworld != null) {
			state = overworld.getPersistentStateManager().getOrCreate(
					BabymodeState::fromNbt, BabymodeState::new, BabymodeState.ID);
		}
	}

	private void onServerTick(MinecraftServer server) {
		if (state == null) {
			return;
		}
		ModConfig cfg = ConfigManager.get();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.isRemoved()) {
				continue;
			}
			PlayerState ps = state.getOrCreate(player.getUuid());
			updatePlayer(player, ps, cfg);
		}
	}

	private void updatePlayer(ServerPlayerEntity player, PlayerState ps, ModConfig cfg) {
		// Sleepiness accrues purely with time; only a bed resets it.
		if (cfg.sleepiness.enabled) {
			SleepinessSystem.accrue(ps, SleepinessSystem.perTick(cfg.sleepiness.fullAfterDays));
		}

		// Nutrition decays slowly (100 -> 0 over daysToEmpty days).
		if (cfg.nutrition.enabled) {
			NutritionSystem.decay(ps, NutritionSystem.decayPerDay(cfg.nutrition.daysToEmpty), 1);
		}

		tickHunger(player, cfg);
		applyMovementSpeed(player);
		applyAndReport(player, ps, cfg);

		// Sprint gates: max sleepiness, or starving.
		boolean tooSleepy = cfg.sleepiness.enabled && ps.sleepiness >= SleepinessSystem.MAX;
		boolean starving = cfg.nutrition.enabled
				&& NutritionSystem.isStarving(ps, cfg.nutrition.starvingThreshold);
		if (tooSleepy || (starving && cfg.nutrition.preventSprintWhenStarving)) {
			if (player.isSprinting()) {
				player.setSprinting(false);
			}
		}

		state.markDirty();
	}

	private void tickHunger(ServerPlayerEntity player, ModConfig cfg) {
		HungerManager hm = player.getHungerManager();
		double days = cfg.environment.hungerPassiveDaysToEmpty;
		if (days > 0) {
			UUID uuid = player.getUuid();
			double fraction = passiveFoodFraction.getOrDefault(uuid, 0.0)
					+ 1.0 / (20.0 * days * SleepinessSystem.TICKS_PER_DAY);
			if (fraction >= 1.0) {
				fraction -= 1.0;
				hm.setFoodLevel(Math.max(0, hm.getFoodLevel() - 1));
			}
			passiveFoodFraction.put(uuid, fraction);
		}
	}

	// ------------------------------------------------------------------
	// Effects + chat reporting
	// ------------------------------------------------------------------

	private void applyAndReport(ServerPlayerEntity player, PlayerState ps, ModConfig cfg) {
		UUID uuid = player.getUuid();
		Map<StatusEffect, EffectRequest> desired = new LinkedHashMap<>();
		double avg = NutritionSystem.average(ps);
		boolean starving = cfg.nutrition.enabled
				&& NutritionSystem.isStarving(ps, cfg.nutrition.starvingThreshold);
		// Well-fed buff tier is only tracked/applied while the master flag is on.
		boolean wellFedEnabled = cfg.nutrition.enabled && cfg.nutrition.enableWellFedBuffs;
		boolean wellFed = wellFedEnabled
				&& NutritionSystem.isWellFed(ps, cfg.nutrition.wellFedThreshold);

		// Sleepiness-driven effects
		if (cfg.sleepiness.enabled) {
			int band = SleepinessSystem.band(ps.sleepiness);
			if (band >= 2) {
				int amp = band >= 4 ? 1 : 0;
				String reason = String.format("sleepiness %.0f/100%s",
						ps.sleepiness, band >= 4 ? " (≥100)" : " (≥50)");
				putEffect(desired, StatusEffects.SLOWNESS, amp, reason);
			}
			if (band >= 3) {
				int amp = band >= 4 ? 1 : 0;
				putEffect(desired, StatusEffects.MINING_FATIGUE, amp,
						String.format("sleepiness %.0f/100 (≥75)", ps.sleepiness));
			}
		}

		// Nutrition per-category deficits: grain -> slowness, protein -> weakness, produce -> mining fatigue
		if (cfg.nutrition.enabled) {
			int tier = NutritionSystem.categoryTier(ps.grain,
					cfg.nutrition.deficitThresholdI, cfg.nutrition.deficitThresholdII);
			if (tier > 0) {
				putEffect(desired, StatusEffects.SLOWNESS, tier == 2 ? 1 : 0,
						String.format("grain %.0f/100 (<%s)", ps.grain,
								tier == 2 ? cfg.nutrition.deficitThresholdII : cfg.nutrition.deficitThresholdI));
			}
			tier = NutritionSystem.categoryTier(ps.protein,
					cfg.nutrition.deficitThresholdI, cfg.nutrition.deficitThresholdII);
			if (tier > 0) {
				putEffect(desired, StatusEffects.WEAKNESS, tier == 2 ? 1 : 0,
						String.format("protein %.0f/100 (<%s)", ps.protein,
								tier == 2 ? cfg.nutrition.deficitThresholdII : cfg.nutrition.deficitThresholdI));
			}
			tier = NutritionSystem.categoryTier(ps.produce,
					cfg.nutrition.deficitThresholdI, cfg.nutrition.deficitThresholdII);
			if (tier > 0) {
				putEffect(desired, StatusEffects.MINING_FATIGUE, tier == 2 ? 1 : 0,
						String.format("produce %.0f/100 (<%s)", ps.produce,
								tier == 2 ? cfg.nutrition.deficitThresholdII : cfg.nutrition.deficitThresholdI));
			}

			// Starving tier
			if (starving) {
				putEffect(desired, StatusEffects.HUNGER, 0,
						String.format("avg nutrition %.0f/100 (<%s)", avg, cfg.nutrition.starvingThreshold));
			}

			// Well-fed tier: only applied when the buffs are enabled.
			if (wellFedEnabled && wellFed) {
				String reason = String.format("avg nutrition=%.0f/100 (≥%s)", avg, cfg.nutrition.wellFedThreshold);
				putEffect(desired, StatusEffects.REGENERATION, 0, reason);
				putEffect(desired, StatusEffects.STRENGTH, 0, reason);
				putEffect(desired, StatusEffects.HASTE, 0, reason);
			}
		}

		PlayerReport old = lastReport.get(uuid);
		if (old == null) {
			// First report after join: apply silently, no chat spam.
			applyDesired(player, desired);
			handleStarvingHp(player, starving, cfg);
			lastReport.put(uuid, new PlayerReport(desired, starving, wellFed));
			return;
		}

		List<String> applied = new ArrayList<>();
		List<String> removed = new ArrayList<>();
		for (Map.Entry<StatusEffect, EffectRequest> entry : desired.entrySet()) {
			if (!old.effects.containsKey(entry.getKey())) {
				applied.add(friendlyName(entry.getKey()) + " " + roman(entry.getValue().amplifier)
						+ " (" + entry.getValue().reason + ")");
			}
		}
		for (StatusEffect effect : old.effects.keySet()) {
			if (!desired.containsKey(effect)) {
				removed.add(friendlyName(effect));
				player.removeStatusEffect(effect);
			}
		}

		if (!applied.isEmpty()) {
			send(player, "⚠ Debuffs applied: " + String.join(", ", applied));
		}
		if (!removed.isEmpty()) {
			send(player, "✓ Debuffs removed: " + String.join(", ", removed));
		}

			// Starving transitions
		if (starving && !old.starving) {
			send(player, "⚠⚠ STARVING! Avg nutrition " + Math.round(avg) + "/100 < "
					+ cfg.nutrition.starvingThreshold + ": -4 max health, hunger drain, no sprint. Find food quickly!");
			handleStarvingHp(player, true, cfg);
		} else if (!starving && old.starving) {
			send(player, "✓ No longer starving — max health restored, +4 HP healed.");
			handleStarvingHp(player, false, cfg);
		} else {
			handleStarvingHp(player, starving, cfg);
		}

		// Well-fed transitions (only reported when the buff tier is enabled).
		if (wellFedEnabled && wellFed && !old.wellFed) {
			send(player, "✓ Well-fed! Avg nutrition " + Math.round(avg) + "/100 ≥ "
					+ cfg.nutrition.wellFedThreshold + ": +Regeneration, +Strength, +Haste");
		} else if (wellFedEnabled && !wellFed && old.wellFed) {
			send(player, "Well-fed ended — buffs removed (avg nutrition " + Math.round(avg) + "/100).");
		}

		applyDesired(player, desired);
		lastReport.put(uuid, new PlayerReport(desired, starving, wellFed));
	}

	private void putEffect(Map<StatusEffect, EffectRequest> desired, StatusEffect effect, int amplifier, String reason) {
		EffectRequest existing = desired.get(effect);
		if (existing == null || amplifier > existing.amplifier) {
			desired.put(effect, new EffectRequest(amplifier, reason));
		}
	}

	private void applyDesired(ServerPlayerEntity player, Map<StatusEffect, EffectRequest> desired) {
		for (Map.Entry<StatusEffect, EffectRequest> entry : desired.entrySet()) {
			StatusEffectInstance inst = player.getStatusEffect(entry.getKey());
			// Keep the effect alive; re-apply on expiry or when the desired amplifier changes.
			if (inst == null || inst.getDuration() < 160 || inst.getAmplifier() != entry.getValue().amplifier) {
				player.addStatusEffect(new StatusEffectInstance(
						entry.getKey(), 200, entry.getValue().amplifier, true, false, true));
			}
		}
	}

	private void handleStarvingHp(ServerPlayerEntity player, boolean starving, ModConfig cfg) {
		EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		if (starving && maxHealth.getModifier(STARVING_HP_MODIFIER_ID) == null) {
			maxHealth.addTemporaryModifier(new EntityAttributeModifier(
					STARVING_HP_MODIFIER_ID, STARVING_HP_MODIFIER_NAME,
					STARVING_HP_PENALTY, EntityAttributeModifier.Operation.ADDITION));
			// Vanilla does not clamp current health when max health drops via modifier.
			if (player.getHealth() > player.getMaxHealth()) {
				player.setHealth(player.getMaxHealth());
			}
		} else if (!starving && maxHealth.getModifier(STARVING_HP_MODIFIER_ID) != null) {
			maxHealth.removeModifier(STARVING_HP_MODIFIER_ID);
			if (cfg.nutrition.enabled) {
				player.heal((float) -STARVING_HP_PENALTY);
			}
		}
	}

	/** Soft-death reset: nutrition back to 100 and sleepiness to 0 (its best value). */
	private void resetOnRespawn(ServerPlayerEntity player) {
		if (state == null) {
			ensureState();
		}
		if (state == null) {
			return;
		}
		PlayerState ps = state.getOrCreate(player.getUuid());
		ps.grain = 100.0;
		ps.protein = 100.0;
		ps.produce = 100.0;
		ps.sleepiness = 0.0;
		state.markDirty();
	}

	private void applyMovementSpeed(ServerPlayerEntity player) {
		ModConfig cfg = ConfigManager.get();
		PlayerState ps = state != null ? state.get(player.getUuid()) : null;
		if (ps == null) {
			return;
		}
		double factor = SleepinessSystem.movementFactor(ps.sleepiness, cfg.sleepiness.penaltyPercent);
		double base = 0.1 * cfg.player.movementSpeedMultiplier * factor;
		EntityAttributeInstance inst = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		if (inst == null) {
			return;
		}
		Double last = lastAppliedSpeed.get(player.getUuid());
		if (last == null || Math.abs(last - base) > 1e-4) {
			inst.setBaseValue(base);
			lastAppliedSpeed.put(player.getUuid(), base);
		}
	}

	private static void send(ServerPlayerEntity player, String message) {
		if (ConfigManager.get().chat.notificationsEnabled == Boolean.FALSE) {
			return;
		}
		player.sendMessage(Text.literal(MSG_PREFIX + message), false);
	}

	private static String friendlyName(StatusEffect effect) {
		if (effect == StatusEffects.SLOWNESS) return "Slowness";
		if (effect == StatusEffects.MINING_FATIGUE) return "Mining Fatigue";
		if (effect == StatusEffects.WEAKNESS) return "Weakness";
		if (effect == StatusEffects.HUNGER) return "Hunger";
		if (effect == StatusEffects.REGENERATION) return "Regeneration";
		if (effect == StatusEffects.STRENGTH) return "Strength";
		if (effect == StatusEffects.HASTE) return "Haste";
		return effect.getTranslationKey();
	}

	private static String roman(int n) {
		return switch (n) {
			case 0 -> "I";
			case 1 -> "II";
			case 2 -> "III";
			default -> Integer.toString(n + 1);
		};
	}

	/** Desired effect with amplifier + human-readable reason for chat messages. */
	private static final class EffectRequest {
		final int amplifier;
		final String reason;

		EffectRequest(int amplifier, String reason) {
			this.amplifier = amplifier;
			this.reason = reason;
		}
	}

	/** Snapshot of the previously reported state, for transition diffing. */
	private static final class PlayerReport {
		final Map<StatusEffect, Integer> effects;
		final boolean starving;
		final boolean wellFed;

		PlayerReport(Map<StatusEffect, EffectRequest> desired, boolean starving, boolean wellFed) {
			Map<StatusEffect, Integer> m = new HashMap<>();
			for (Map.Entry<StatusEffect, EffectRequest> e : desired.entrySet()) {
				m.put(e.getKey(), e.getValue().amplifier);
			}
			this.effects = m;
			this.starving = starving;
			this.wellFed = wellFed;
		}
	}
}
