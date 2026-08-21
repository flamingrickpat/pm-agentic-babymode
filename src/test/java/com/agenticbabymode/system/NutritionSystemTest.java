package com.agenticbabymode.system;

import com.agenticbabymode.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NutritionSystemTest {

	@Test
	void eatBreadIncreasesOnlyGrain() {
		PlayerState ps = PlayerState.empty();
		NutritionSystem.add(ps, new NutritionGain(6.0, 0.0, 0.0));
		assertEquals(6.0, ps.grain, 1e-9);
		assertEquals(0.0, ps.protein, 1e-9);
		assertEquals(0.0, ps.produce, 1e-9);
	}

	@Test
	void eatSteakIncreasesProtein() {
		PlayerState ps = PlayerState.empty();
		NutritionSystem.add(ps, new NutritionGain(1.0, 6.0, 0.0));
		assertEquals(1.0, ps.grain, 1e-9);
		assertEquals(6.0, ps.protein, 1e-9);
		assertEquals(0.0, ps.produce, 1e-9);
	}

	@Test
	void freshPlayerStartsWithZeroSleepinessAndFullNutrition() {
		PlayerState ps = PlayerState.defaults();
		assertEquals(0.0, ps.sleepiness, 1e-9);
		assertEquals(100.0, ps.grain, 1e-9);
		assertEquals(100.0, ps.protein, 1e-9);
		assertEquals(100.0, ps.produce, 1e-9);
	}

	@Test
	void advanceOneDayDecreasesNutritionByDecayPerDay() {
		PlayerState ps = PlayerState.defaults();
		ps.grain = 50.0;
		ps.protein = 50.0;
		ps.produce = 50.0;
		NutritionSystem.decay(ps, NutritionSystem.decayPerDay(7.0), 24000);
		// 100/7 per day
		assertEquals(50.0 - 100.0 / 7.0, ps.grain, 1e-6);
		assertEquals(50.0 - 100.0 / 7.0, ps.protein, 1e-6);
		assertEquals(50.0 - 100.0 / 7.0, ps.produce, 1e-6);
	}

	@Test
	void reachesZeroAfterSevenDays() {
		PlayerState ps = PlayerState.defaults();
		NutritionSystem.decay(ps, NutritionSystem.decayPerDay(7.0), 24000 * 7);
		assertEquals(0.0, ps.grain, 1e-6);
		assertEquals(0.0, ps.protein, 1e-6);
		assertEquals(0.0, ps.produce, 1e-6);
	}

	@Test
	void nutritionCannotExceed100() {
		PlayerState ps = PlayerState.defaults();
		NutritionSystem.add(ps, new NutritionGain(99.0, 0.0, 0.0));
		NutritionSystem.add(ps, new NutritionGain(99.0, 0.0, 0.0));
		assertEquals(100.0, ps.grain, 1e-9);
	}

	@Test
	void averageIsMeanOfCategories() {
		PlayerState ps = PlayerState.defaults();
		ps.grain = 10.0;
		ps.protein = 20.0;
		ps.produce = 30.0;
		assertEquals(20.0, NutritionSystem.average(ps), 1e-9);
	}

	@Test
	void categoryTierThresholds() {
		assertEquals(0, NutritionSystem.categoryTier(45.0, 40.0, 15.0));
		assertEquals(1, NutritionSystem.categoryTier(20.0, 40.0, 15.0));
		assertEquals(1, NutritionSystem.categoryTier(39.99, 40.0, 15.0));
		assertEquals(2, NutritionSystem.categoryTier(8.0, 40.0, 15.0));
		assertEquals(2, NutritionSystem.categoryTier(0.0, 40.0, 15.0));
	}

	@Test
	void starvingAndWellFedBoundaries() {
		PlayerState ps = PlayerState.defaults();
		ps.grain = 20.0;
		ps.protein = 20.0;
		ps.produce = 20.0; // avg 20
		assertFalse(NutritionSystem.isStarving(ps, 10.0));
		assertFalse(NutritionSystem.isWellFed(ps, 75.0));

		ps.grain = 5.0;
		ps.protein = 5.0;
		ps.produce = 5.0; // avg 5
		assertTrue(NutritionSystem.isStarving(ps, 10.0));

		ps.grain = 90.0;
		ps.protein = 90.0;
		ps.produce = 90.0; // avg 90
		assertTrue(NutritionSystem.isWellFed(ps, 75.0));
	}

	@Test
	void foodCatalogHasKnownItems() {
		assertTrue(FoodCatalog.size() > 20);
		assertEquals(6.0, FoodCatalog.gainFor("minecraft:bread").grain, 1e-9);
		assertEquals(6.0, FoodCatalog.gainFor("minecraft:cooked_beef").protein, 1e-9);
		assertEquals(4.0, FoodCatalog.gainFor("minecraft:apple").produce, 1e-9);
		assertEquals(null, FoodCatalog.gainFor("minecraft:diamond"));
	}
}
