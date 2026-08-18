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
	void defaultsMatchHandoffSpec() {
		ModConfig cfg = ModConfig.defaults();
		assertEquals(1.5, cfg.player.movementSpeedMultiplier, 1e-9);
		assertEquals(5.0, cfg.player.miningSpeedMultiplier, 1e-9);
		assertFalse(cfg.player.naturalRegeneration);
		assertFalse(cfg.player.pvp);
		assertEquals(10.0, cfg.environment.drowningTimeMultiplier, 1e-9);
		assertEquals(0.35, cfg.mobs.movementSpeedMultiplier, 1e-9);
		assertEquals(3.0, cfg.mobs.attackCooldownMultiplier, 1e-9);
		assertEquals(0.35, cfg.mobs.damageMultiplier, 1e-9);
		assertEquals(2.0, cfg.mobs.damageTakenMultiplier, 1e-9);
		assertTrue(cfg.fatigue.enabled);
		assertTrue(cfg.nutrition.enabled);
	}

	@Test
	void missingFieldsFallBackToDefaults() {
		ModConfig cfg = GSON.fromJson("{}", ModConfig.class);
		cfg.applyDefaults();
		assertEquals(1.5, cfg.player.movementSpeedMultiplier, 1e-9);
		assertEquals(10.0, cfg.environment.drowningTimeMultiplier, 1e-9);
		assertTrue(cfg.fatigue.enabled);
	}

	@Test
	void presentFieldsAreKept() {
		ModConfig cfg = GSON.fromJson(
				"{\"player\":{\"movementSpeedMultiplier\":2.0,\"naturalRegeneration\":true},\"mobs\":{\"damageMultiplier\":0.5}}",
				ModConfig.class);
		cfg.applyDefaults();
		assertEquals(2.0, cfg.player.movementSpeedMultiplier, 1e-9);
		assertTrue(cfg.player.naturalRegeneration);
		assertEquals(0.5, cfg.mobs.damageMultiplier, 1e-9);
		// untouched sections still defaulted
		assertEquals(5.0, cfg.player.miningSpeedMultiplier, 1e-9);
		assertEquals(10.0, cfg.environment.drowningTimeMultiplier, 1e-9);
		assertEquals(3.0, cfg.mobs.attackCooldownMultiplier, 1e-9);
	}
}
