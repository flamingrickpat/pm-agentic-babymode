package com.agenticbabymode.server;

import com.agenticbabymode.AgenticBabymodeMod;
import com.agenticbabymode.config.ConfigManager;
import com.agenticbabymode.config.ModConfig;
import com.agenticbabymode.state.BabymodeState;
import com.agenticbabymode.state.PlayerState;
import com.agenticbabymode.system.FatigueSystem;
import com.agenticbabymode.system.NutritionGain;
import com.agenticbabymode.system.NutritionSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Wires Fabric API events into the babymode systems. This is the "logical
 * server side" glue: it only runs inside the integrated/dedicated server.
 */
public final class BabymodeServer {
	public static BabymodeServer INSTANCE;

	private final Map<UUID, Double> lastAppliedSpeed = new WeakHashMap<>();
	private final Map<ServerPlayerEntity, Boolean> lastSprintState = new WeakHashMap<>();

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
			if (state == null) {
				state = overworld.getPersistentStateManager().getOrCreate(
						BabymodeState::fromNbt, BabymodeState::new, BabymodeState.ID);
			}
			state.getOrCreate(player.getUuid());
			applyMovementSpeed(player);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			applyMovementSpeed(newPlayer);
		});

		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state1, blockEntity) -> {
			if (world.isClient) {
				return;
			}
			if (player instanceof ServerPlayerEntity spe && fatigueEnabled()) {
				PlayerState ps = stateFor(spe);
				FatigueSystem.accrue(ps, ConfigManager.get().fatigue.mineCostPerBlock);
				state.markDirty();
			}
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			ModConfig cfg = ConfigManager.get();
			Entity attacker = source.getAttacker();
			// PvP toggle
			if (!cfg.player.pvp && entity instanceof PlayerEntity && attacker instanceof PlayerEntity) {
				return false;
			}
			// Fatigue from taking damage
			if (entity instanceof ServerPlayerEntity spe && fatigueEnabled() && amount > 0.0F) {
				PlayerState ps = stateFor(spe);
				FatigueSystem.accrue(ps, amount * cfg.fatigue.damageCostPerPoint);
				state.markDirty();
			}
			return true;
		});

		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			ModConfig cfg = ConfigManager.get();
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

	private void onServerStarted(MinecraftServer server) {
		this.server = server;
		this.overworld = server.getOverworld();
		this.state = overworld.getPersistentStateManager().getOrCreate(
				BabymodeState::fromNbt, BabymodeState::new, BabymodeState.ID);
		applyConfig();
		AgenticBabymodeMod.LOGGER.info("Agentic Babymode state loaded ({} tracked players)",
				state.getPlayers().size());
	}

	private void onServerStopping(MinecraftServer server) {
		if (state != null) {
			state.markDirty();
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
			PlayerState ps = stateFor(player);
			tickFatigue(player, ps, cfg);
			if (cfg.nutrition.enabled) {
				NutritionSystem.decay(ps, cfg.nutrition.decayPerDay, 1);
			}
			applyMovementSpeed(player);
			applyEffects(player, ps, cfg);
		}
	}

	private void tickFatigue(ServerPlayerEntity player, PlayerState ps, ModConfig cfg) {
		if (!fatigueEnabled()) {
			return;
		}
		double perSecond = 0.0;
		if (player.isSleeping()) {
			perSecond = -cfg.fatigue.sleepRecoveryPerSecond;
		} else if (player.isSprinting()) {
			perSecond = cfg.fatigue.sprintCostPerSecond;
		} else {
			perSecond = -cfg.fatigue.idleRecoveryPerSecond;
		}
		double perTick = perSecond / 20.0;
		if (perTick > 0) {
			FatigueSystem.accrue(ps, perTick);
		} else if (perTick < 0) {
			FatigueSystem.recover(ps, -perTick);
		}
		// Hard cap: cannot sprint at max fatigue.
		if (ps.fatigue >= FatigueSystem.MAX && player.isSprinting()) {
			player.setSprinting(false);
		}
		state.markDirty();
	}

	private void applyMovementSpeed(ServerPlayerEntity player) {
		ModConfig cfg = ConfigManager.get();
		PlayerState ps = stateFor(player);
		double fatigueFactor = FatigueSystem.movementFactor(ps.fatigue, cfg.fatigue.movementPenaltyPercent);
		double base = 0.1 * cfg.player.movementSpeedMultiplier * fatigueFactor;
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

	private void applyEffects(ServerPlayerEntity player, PlayerState ps, ModConfig cfg) {
		int fatigueBand = fatigueEnabled() ? FatigueSystem.band(ps.fatigue) : 0;
		if (fatigueBand != ps.fatigueBand) {
			ps.fatigueBand = fatigueBand;
			clearEffects(player, StatusEffects.SLOWNESS);
			clearEffects(player, StatusEffects.MINING_FATIGUE);
			if (fatigueBand >= 4) {
				apply(player, StatusEffects.SLOWNESS, 1, true);
				apply(player, StatusEffects.MINING_FATIGUE, 1, true);
			} else if (fatigueBand == 3) {
				apply(player, StatusEffects.SLOWNESS, 0, true);
				apply(player, StatusEffects.MINING_FATIGUE, 0, true);
			} else if (fatigueBand == 2) {
				apply(player, StatusEffects.SLOWNESS, 0, true);
			}
		}

		int nutritionBand = -1;
		if (cfg.nutrition.enabled) {
			double avg = NutritionSystem.average(ps);
			if (avg >= cfg.nutrition.wellFedRegenThreshold) {
				nutritionBand = 1;
			} else if (avg <= cfg.nutrition.malnourishedThreshold) {
				nutritionBand = 2;
			}
		}
		if (nutritionBand != ps.nutritionBand) {
			ps.nutritionBand = nutritionBand;
			clearEffects(player, StatusEffects.REGENERATION);
			clearEffects(player, StatusEffects.WEAKNESS);
			if (nutritionBand == 1) {
				apply(player, StatusEffects.REGENERATION, 0, true);
			} else if (nutritionBand == 2) {
				apply(player, StatusEffects.WEAKNESS, 0, true);
			}
		}
	}

	private static void apply(ServerPlayerEntity player, net.minecraft.entity.effect.StatusEffect effect, int amplifier, boolean ambient) {
		player.addStatusEffect(new StatusEffectInstance(effect, 120, amplifier, ambient, false, true));
	}

	private static void clearEffects(ServerPlayerEntity player, net.minecraft.entity.effect.StatusEffect effect) {
		player.removeStatusEffect(effect);
	}

	private PlayerState stateFor(ServerPlayerEntity player) {
		if (state == null && overworld != null) {
			state = overworld.getPersistentStateManager().getOrCreate(
					BabymodeState::fromNbt, BabymodeState::new, BabymodeState.ID);
		}
		return state.getOrCreate(player.getUuid());
	}

	private boolean fatigueEnabled() {
		return ConfigManager.get().fatigue.enabled;
	}
}
