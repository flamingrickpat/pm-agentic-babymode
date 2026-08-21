package com.agenticbabymode.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigTest {

	private static final Gson GSON = new GsonBuilder().create();

	@Test
	void defaultsMatchSpec() {
		ModConfig cfg = ModConfig.defaults();
		assertEquals(1.5, cfg.player.movementSpeedMultiplier, 1e-9);
		assertEquals(5.0, cfg.player.miningSpeedMultiplier, 1e-9);
		assertFalse(cfg.player.naturalRegeneration);
		assertFalse(cfg.player.pvp);
		assertEquals(6.0, cfg.player.pickupRange, 1e-9);
		assertEquals(10.0, cfg.environment.drowningTimeMultiplier, 1e-9);
		assertEquals(30.0, cfg.environment.hungerPassiveDaysToEmpty, 1e-9);
		assertEquals(1.0, cfg.environment.hungerActivityExhaustionMultiplier, 1e-9);
		assertEquals(1.0 / 15.0, cfg.mobs.movementSpeedMultiplier, 1e-9);
		assertEquals(3.0, cfg.mobs.attackCooldownMultiplier, 1e-9);
		assertEquals(1.0 / 15.0, cfg.mobs.damageMultiplier, 1e-9);
		assertEquals(2.0, cfg.mobs.damageTakenMultiplier, 1e-9);
		assertTrue(cfg.mobs.disablePhantoms);
		assertTrue(cfg.mobs.disableCreepers);
		assertTrue(cfg.mobs.disablePillagers);
		assertTrue(cfg.sleepiness.enabled);
		assertEquals(30.0, cfg.sleepiness.fullAfterDays, 1e-9);
		assertEquals(50.0, cfg.sleepiness.penaltyPercent, 1e-9);
		assertTrue(cfg.nutrition.enabled);
		assertEquals(90.0, cfg.nutrition.daysToEmpty, 1e-9);
		assertFalse(cfg.nutrition.enableWellFedBuffs);
		assertEquals(40.0, cfg.nutrition.deficitThresholdI, 1e-9);
		assertEquals(15.0, cfg.nutrition.deficitThresholdII, 1e-9);
		assertEquals(10.0, cfg.nutrition.starvingThreshold, 1e-9);
		assertEquals(75.0, cfg.nutrition.wellFedThreshold, 1e-9);
		assertTrue(cfg.chat.notificationsEnabled);
	}

	@Test
	void missingFieldsFallBackToDefaults() {
		ModConfig cfg = GSON.fromJson("{}", ModConfig.class);
		cfg.applyDefaults();
		assertEquals(1.5, cfg.player.movementSpeedMultiplier, 1e-9);
		assertEquals(10.0, cfg.environment.drowningTimeMultiplier, 1e-9);
		assertEquals(90.0, cfg.nutrition.daysToEmpty, 1e-9);
		assertTrue(cfg.sleepiness.enabled);
		assertFalse(cfg.nutrition.enableWellFedBuffs);
		assertTrue(cfg.chat.notificationsEnabled);
	}

	@Test
	void presentFieldsAreKept() {
		ModConfig cfg = GSON.fromJson(
				"{\"player\":{\"movementSpeedMultiplier\":2.0,\"naturalRegeneration\":true},"
						+ "\"nutrition\":{\"starvingThreshold\":20.0,\"daysToEmpty\":5.0}}",
				ModConfig.class);
		cfg.applyDefaults();
		assertEquals(2.0, cfg.player.movementSpeedMultiplier, 1e-9);
		assertTrue(cfg.player.naturalRegeneration);
		assertEquals(20.0, cfg.nutrition.starvingThreshold, 1e-9);
		assertEquals(5.0, cfg.nutrition.daysToEmpty, 1e-9);
		// untouched sections still defaulted
		assertEquals(5.0, cfg.player.miningSpeedMultiplier, 1e-9);
		assertEquals(40.0, cfg.nutrition.deficitThresholdI, 1e-9);
		assertEquals(30.0, cfg.sleepiness.fullAfterDays, 1e-9);
	}

	@Test
	void notificationsAndDisabledMobsAreConfigurable() {
		ModConfig cfg = GSON.fromJson(
				"{\"chat\":{\"notificationsEnabled\":false},"
						+ "\"mobs\":{\"disablePhantoms\":false,\"disablePillagers\":false}}",
				ModConfig.class);
		cfg.applyDefaults();
		assertFalse(cfg.chat.notificationsEnabled);
		assertFalse(cfg.mobs.disablePhantoms);
		assertFalse(cfg.mobs.disablePillagers);
		// untouched fields default from defaults
		assertTrue(cfg.mobs.disableCreepers);
		assertEquals(1.0 / 15.0, cfg.mobs.damageMultiplier, 1e-9);
	}
}
