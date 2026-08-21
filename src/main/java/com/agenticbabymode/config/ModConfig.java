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
		/** Items within this many blocks of a player are pulled into their inventory. 0 = vanilla only. */
		public Double pickupRange = 6.0;
	}

	public static class EnvironmentConfig {
		public Double drowningTimeMultiplier = 10.0;
		/** Vanilla food bar drains to empty in this many in-game days while standing still. */
		public Double hungerPassiveDaysToEmpty = 30.0;
		/** Scales all vanilla action exhaustion (sprinting/jumping/mining). 1.0 = vanilla. */
		public Double hungerActivityExhaustionMultiplier = 1.0;
		/** Direct food-bar drain per second while starving (on top of the Hunger effect). */
		public Double starvingFoodDrainPerSecond = 0.02;
	}

	public static class MobsConfig {
		/** 1/15 of vanilla + the player speed, so mobs are nearly stationary. */
		public Double movementSpeedMultiplier = 1.0 / 15.0;
		public Double attackCooldownMultiplier = 3.0;
		/** 1/15 of vanilla mob melee damage dealt to players. */
		public Double damageMultiplier = 1.0 / 15.0;
		public Double damageTakenMultiplier = 2.0;
		/** Completely remove these hostile mobs from the world (spawn + chunk load). */
		public Boolean disablePhantoms = true;
		public Boolean disableCreepers = true;
		public Boolean disablePillagers = true;
	}

	/**
	 * Sleepiness: builds up purely over in-game time, reaches 100% after
	 * fullAfterDays, and is only reset by right-clicking a bed.
	 */
	public static class SleepinessConfig {
		public Boolean enabled = true;
		/** In-game days (24000 ticks) of continuous play to reach 100%. */
		public Double fullAfterDays = 30.0;
		/** Max % movement/mining speed penalty at 100 sleepiness. */
		public Double penaltyPercent = 50.0;
	}

	public static class NutritionConfig {
		public Boolean enabled = true;
		/** Each category decays from 100 to 0 over this many in-game days. */
		public Double daysToEmpty = 90.0;
		/** Master switch for the "well-fed" buff tier (Regen + Strength + Haste). Off by default so agents aren't confused by buffs. */
		public Boolean enableWellFedBuffs = false;
		/** Category below this value applies its debuff at level I. */
		public Double deficitThresholdI = 40.0;
		/** Category below this value applies its debuff at level II. */
		public Double deficitThresholdII = 15.0;
		/** Average below this value = starving: -4 max HP, hunger, no sprint. */
		public Double starvingThreshold = 10.0;
		/** Average at/above this value = well-fed: Regeneration + Strength + Haste. */
		public Double wellFedThreshold = 75.0;
		public Boolean preventSprintWhenStarving = true;
	}

	/**
	 * Chat notification output. When notificationsEnabled is false, babymode
	 * suppresses the automatic status-effect update spam (debuffs applied/removed,
	 * starving / well-fed transitions, bed confirmations). Reply messages from
	 * /babymode commands are always shown.
	 */
	public static class ChatConfig {
		public Boolean notificationsEnabled = true;
	}

	public PlayerConfig player = new PlayerConfig();
	public EnvironmentConfig environment = new EnvironmentConfig();
	public MobsConfig mobs = new MobsConfig();
	public SleepinessConfig sleepiness = new SleepinessConfig();
	public NutritionConfig nutrition = new NutritionConfig();
	public ChatConfig chat = new ChatConfig();

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
		if (player.pickupRange == null) player.pickupRange = 6.0;

		if (environment == null) environment = new EnvironmentConfig();
		if (environment.drowningTimeMultiplier == null) environment.drowningTimeMultiplier = 10.0;
		if (environment.hungerPassiveDaysToEmpty == null) environment.hungerPassiveDaysToEmpty = 30.0;
		if (environment.hungerActivityExhaustionMultiplier == null) environment.hungerActivityExhaustionMultiplier = 1.0;
		if (environment.starvingFoodDrainPerSecond == null) environment.starvingFoodDrainPerSecond = 0.02;

		if (mobs == null) mobs = new MobsConfig();
		if (mobs.movementSpeedMultiplier == null) mobs.movementSpeedMultiplier = 1.0 / 15.0;
		if (mobs.attackCooldownMultiplier == null) mobs.attackCooldownMultiplier = 3.0;
		if (mobs.damageMultiplier == null) mobs.damageMultiplier = 1.0 / 15.0;
		if (mobs.damageTakenMultiplier == null) mobs.damageTakenMultiplier = 2.0;
		if (mobs.disablePhantoms == null) mobs.disablePhantoms = true;
		if (mobs.disableCreepers == null) mobs.disableCreepers = true;
		if (mobs.disablePillagers == null) mobs.disablePillagers = true;

		if (sleepiness == null) sleepiness = new SleepinessConfig();
		if (sleepiness.enabled == null) sleepiness.enabled = true;
		if (sleepiness.fullAfterDays == null) sleepiness.fullAfterDays = 30.0;
		if (sleepiness.penaltyPercent == null) sleepiness.penaltyPercent = 50.0;

		if (nutrition == null) nutrition = new NutritionConfig();
		if (nutrition.enabled == null) nutrition.enabled = true;
		if (nutrition.daysToEmpty == null) nutrition.daysToEmpty = 90.0;
		if (nutrition.enableWellFedBuffs == null) nutrition.enableWellFedBuffs = false;
		if (nutrition.deficitThresholdI == null) nutrition.deficitThresholdI = 40.0;
		if (nutrition.deficitThresholdII == null) nutrition.deficitThresholdII = 15.0;
		if (nutrition.starvingThreshold == null) nutrition.starvingThreshold = 10.0;
		if (nutrition.wellFedThreshold == null) nutrition.wellFedThreshold = 75.0;
		if (nutrition.preventSprintWhenStarving == null) nutrition.preventSprintWhenStarving = true;

		if (chat == null) chat = new ChatConfig();
		if (chat.notificationsEnabled == null) chat.notificationsEnabled = true;
	}
}