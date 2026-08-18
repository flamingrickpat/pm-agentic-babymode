package com.agenticbabymode.config;

/**
 * Mutable config POJO loaded from config/agentic-babymode.json via Gson.
 * All numeric fields are boxed so "missing in json" == null, which then gets
 * replaced by defaults in {@link #applyDefaults()}.
 */
public class ModConfig {
	public static class PlayerConfig {
		public Double movementSpeedMultiplier = 1.5;
		public Double miningSpeedMultiplier = 5.0;
		public Boolean naturalRegeneration = false;
		public Boolean pvp = false;
	}

	public static class EnvironmentConfig {
		public Double drowningTimeMultiplier = 10.0;
	}

	public static class MobsConfig {
		public Double movementSpeedMultiplier = 0.35;
		public Double attackCooldownMultiplier = 3.0;
		public Double damageMultiplier = 0.35;
		public Double damageTakenMultiplier = 2.0;
	}

	public static class FatigueConfig {
		public Boolean enabled = true;
		/** Fatigue gained per second of sprinting. */
		public Double sprintCostPerSecond = 1.5;
		/** Fatigue gained per block broken. */
		public Double mineCostPerBlock = 0.75;
		/** Fatigue gained per point of damage taken. */
		public Double damageCostPerPoint = 0.25;
		/** Fatigue recovered per second while idle (not sprinting). */
		public Double idleRecoveryPerSecond = 1.0;
		/** Fatigue recovered per second while sleeping. */
		public Double sleepRecoveryPerSecond = 3.0;
		/** Max % movement speed penalty at 100 fatigue. */
		public Double movementPenaltyPercent = 50.0;
	}

	public static class NutritionConfig {
		public Boolean enabled = true;
		/** Points lost from each category per in-game day (24000 ticks). */
		public Double decayPerDay = 10.0;
		/** Average nutrition at/above which the well-fed regeneration effect applies. */
		public Double wellFedRegenThreshold = 75.0;
		/** Average nutrition at/below which the malnourished weakness effect applies. */
		public Double malnourishedThreshold = 20.0;
	}

	public PlayerConfig player = new PlayerConfig();
	public EnvironmentConfig environment = new EnvironmentConfig();
	public MobsConfig mobs = new MobsConfig();
	public FatigueConfig fatigue = new FatigueConfig();
	public NutritionConfig nutrition = new NutritionConfig();

	public static ModConfig defaults() {
		return new ModConfig();
	}

	/** Fill any null sub-objects/fields with defaults. */
	public void applyDefaults() {
		if (player == null) player = new PlayerConfig();
		if (player.movementSpeedMultiplier == null) player.movementSpeedMultiplier = 1.5;
		if (player.miningSpeedMultiplier == null) player.miningSpeedMultiplier = 5.0;
		if (player.naturalRegeneration == null) player.naturalRegeneration = false;
		if (player.pvp == null) player.pvp = false;

		if (environment == null) environment = new EnvironmentConfig();
		if (environment.drowningTimeMultiplier == null) environment.drowningTimeMultiplier = 10.0;

		if (mobs == null) mobs = new MobsConfig();
		if (mobs.movementSpeedMultiplier == null) mobs.movementSpeedMultiplier = 0.35;
		if (mobs.attackCooldownMultiplier == null) mobs.attackCooldownMultiplier = 3.0;
		if (mobs.damageMultiplier == null) mobs.damageMultiplier = 0.35;
		if (mobs.damageTakenMultiplier == null) mobs.damageTakenMultiplier = 2.0;

		if (fatigue == null) fatigue = new FatigueConfig();
		if (fatigue.enabled == null) fatigue.enabled = true;
		if (fatigue.sprintCostPerSecond == null) fatigue.sprintCostPerSecond = 1.5;
		if (fatigue.mineCostPerBlock == null) fatigue.mineCostPerBlock = 0.75;
		if (fatigue.damageCostPerPoint == null) fatigue.damageCostPerPoint = 0.25;
		if (fatigue.idleRecoveryPerSecond == null) fatigue.idleRecoveryPerSecond = 1.0;
		if (fatigue.sleepRecoveryPerSecond == null) fatigue.sleepRecoveryPerSecond = 3.0;
		if (fatigue.movementPenaltyPercent == null) fatigue.movementPenaltyPercent = 50.0;

		if (nutrition == null) nutrition = new NutritionConfig();
		if (nutrition.enabled == null) nutrition.enabled = true;
		if (nutrition.decayPerDay == null) nutrition.decayPerDay = 10.0;
		if (nutrition.wellFedRegenThreshold == null) nutrition.wellFedRegenThreshold = 75.0;
		if (nutrition.malnourishedThreshold == null) nutrition.malnourishedThreshold = 20.0;
	}
}
