package com.agenticbabymode.system;

import com.agenticbabymode.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NutritionSystemTest {

	@Test
	void eatBreadIncreasesOnlyGrain() {
		PlayerState ps = PlayerState.defaults();
		NutritionSystem.add(ps, new NutritionGain(6.0, 0.0, 0.0));
		assertEquals(6.0, ps.grain, 1e-9);
		assertEquals(0.0, ps.protein, 1e-9);
		assertEquals(0.0, ps.produce, 1e-9);
	}

	@Test
	void eatSteakIncreasesProtein() {
		PlayerState ps = PlayerState.defaults();
		NutritionSystem.add(ps, new NutritionGain(1.0, 6.0, 0.0));
		assertEquals(1.0, ps.grain, 1e-9);
		assertEquals(6.0, ps.protein, 1e-9);
		assertEquals(0.0, ps.produce, 1e-9);
	}

	@Test
	void advance24000TicksDecreasesNutritionByDecayPerDay() {
		PlayerState ps = PlayerState.defaults();
		ps.grain = 50.0;
		ps.protein = 50.0;
		ps.produce = 50.0;
		NutritionSystem.decay(ps, 10.0, 24000);
		assertEquals(40.0, ps.grain, 1e-6);
		assertEquals(40.0, ps.protein, 1e-6);
		assertEquals(40.0, ps.produce, 1e-6);
	}

	@Test
	void nutritionCannotExceed100() {
		PlayerState ps = PlayerState.defaults();
		NutritionSystem.add(ps, new NutritionGain(99.0, 0.0, 0.0));
		NutritionSystem.add(ps, new NutritionGain(99.0, 0.0, 0.0));
		assertEquals(100.0, ps.grain, 1e-9);
	}

	@Test
	void nutritionCannotFallBelow0() {
		PlayerState ps = PlayerState.defaults();
		NutritionSystem.decay(ps, 10.0, 24000 * 10);
		assertEquals(0.0, ps.grain, 1e-9);
		assertEquals(0.0, ps.protein, 1e-9);
		assertEquals(0.0, ps.produce, 1e-9);
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
	void foodCatalogHasKnownItems() {
		assertTrue(FoodCatalog.size() > 20);
		assertEquals(6.0, FoodCatalog.gainFor("minecraft:bread").grain, 1e-9);
		assertEquals(6.0, FoodCatalog.gainFor("minecraft:cooked_beef").protein, 1e-9);
		assertEquals(4.0, FoodCatalog.gainFor("minecraft:apple").produce, 1e-9);
		assertEquals(null, FoodCatalog.gainFor("minecraft:diamond"));
	}
}
