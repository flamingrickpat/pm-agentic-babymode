package com.agenticbabymode.system;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps vanilla item registry ids to the nutrition they provide.
 * Pure data so it can be unit-tested without Minecraft.
 */
public final class FoodCatalog {
	private static final Map<String, NutritionGain> GAINS = new HashMap<>();

	private FoodCatalog() {
	}

	private static void add(String itemId, double grain, double protein, double produce) {
		GAINS.put(itemId, new NutritionGain(grain, protein, produce));
	}

	/**
	 * @param itemId e.g. "minecraft:bread"
	 * @return the nutrition gain, or null if the item is not in the catalog.
	 */
	public static NutritionGain gainFor(String itemId) {
		return GAINS.get(itemId);
	}

	public static int size() {
		return GAINS.size();
	}

	static {
		// ---- Grain ----
		add("minecraft:wheat", 2.0, 0.0, 0.0);
		add("minecraft:wheat_seeds", 0.5, 0.0, 0.0);
		add("minecraft:bread", 6.0, 0.0, 0.0);
		add("minecraft:cookie", 2.0, 0.0, 0.0);
		add("minecraft:cake", 5.0, 0.0, 1.0);
		add("minecraft:pumpkin_pie", 4.0, 0.0, 1.0);

		// ---- Protein ----
		add("minecraft:beef", 4.0, 4.0, 0.0);
		add("minecraft:cooked_beef", 1.0, 6.0, 0.0);
		add("minecraft:porkchop", 4.0, 4.0, 0.0);
		add("minecraft:cooked_porkchop", 1.0, 6.0, 0.0);
		add("minecraft:chicken", 3.0, 3.0, 0.0);
		add("minecraft:cooked_chicken", 1.0, 5.0, 0.0);
		add("minecraft:mutton", 3.0, 3.0, 0.0);
		add("minecraft:cooked_mutton", 1.0, 5.0, 0.0);
		add("minecraft:rabbit", 3.0, 3.0, 0.0);
		add("minecraft:cooked_rabbit", 1.0, 5.0, 0.0);
		add("minecraft:cod", 2.0, 3.0, 0.0);
		add("minecraft:cooked_cod", 0.5, 4.0, 0.0);
		add("minecraft:salmon", 2.0, 3.0, 0.0);
		add("minecraft:cooked_salmon", 0.5, 5.0, 0.0);
		add("minecraft:tropical_fish", 0.0, 3.0, 0.0);
		add("minecraft:pufferfish", 0.0, 1.0, 0.0);
		add("minecraft:egg", 0.0, 2.0, 0.0);
		add("minecraft:milk_bucket", 0.0, 2.0, 0.0);
		add("minecraft:rotten_flesh", 0.0, 1.0, 0.0);
		add("minecraft:spider_eye", 0.0, 0.5, 0.0);

		// ---- Produce ----
		add("minecraft:apple", 0.0, 0.0, 4.0);
		add("minecraft:golden_apple", 0.0, 0.0, 6.0);
		add("minecraft:enchanted_golden_apple", 0.0, 0.0, 8.0);
		add("minecraft:melon_slice", 0.0, 0.0, 2.0);
		add("minecraft:sweet_berries", 0.0, 0.0, 2.0);
		add("minecraft:glow_berries", 0.0, 0.0, 2.0);
		add("minecraft:carrot", 0.0, 0.0, 2.0);
		add("minecraft:golden_carrot", 0.0, 0.0, 4.0);
		add("minecraft:potato", 0.0, 0.0, 2.0);
		add("minecraft:baked_potato", 0.0, 0.0, 3.0);
		add("minecraft:beetroot", 0.0, 0.0, 1.0);
		add("minecraft:beetroot_soup", 0.0, 0.0, 3.0);
		add("minecraft:mushroom_stew", 0.0, 0.0, 3.0);
		add("minecraft:suspicious_stew", 0.0, 0.0, 3.0);
		add("minecraft:dried_kelp", 0.0, 0.0, 1.0);
		add("minecraft:chorus_fruit", 0.0, 0.0, 2.0);
		add("minecraft:honey_bottle", 0.0, 0.0, 2.0);
	}
}
