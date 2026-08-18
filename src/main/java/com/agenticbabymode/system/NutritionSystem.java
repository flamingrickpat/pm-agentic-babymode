package com.agenticbabymode.system;

import com.agenticbabymode.state.PlayerState;

/**
 * Nutrition bookkeeping. Pure logic, no Minecraft imports, unit-testable.
 */
public final class NutritionSystem {
	public static final double MAX = 100.0;
	public static final double MIN = 0.0;
	/** Vanilla day length in ticks. */
	public static final double TICKS_PER_DAY = 24000.0;

	private NutritionSystem() {
	}

	public static double clamp(double value) {
		return Math.max(MIN, Math.min(MAX, value));
	}

	public static void add(PlayerState state, NutritionGain gain) {
		state.grain = clamp(state.grain + gain.grain);
		state.protein = clamp(state.protein + gain.protein);
		state.produce = clamp(state.produce + gain.produce);
	}

	/** Decay per day so that a category goes from 100 to 0 in daysToEmpty days. */
	public static double decayPerDay(double daysToEmpty) {
		return MAX / Math.max(0.001, daysToEmpty);
	}

	/** Apply natural decay of all categories over the given number of ticks. */
	public static void decay(PlayerState state, double decayPerDay, long ticks) {
		double amount = decayPerDay / TICKS_PER_DAY * ticks;
		state.grain = clamp(state.grain - amount);
		state.protein = clamp(state.protein - amount);
		state.produce = clamp(state.produce - amount);
	}

	public static double average(PlayerState state) {
		return (state.grain + state.protein + state.produce) / 3.0;
	}

	/** 0 = fine, 1 = below thresholdI, 2 = below thresholdII. */
	public static int categoryTier(double value, double thresholdI, double thresholdII) {
		if (value < thresholdII) return 2;
		if (value < thresholdI) return 1;
		return 0;
	}

	public static boolean isStarving(PlayerState state, double avgThreshold) {
		return average(state) < avgThreshold;
	}

	public static boolean isWellFed(PlayerState state, double avgThreshold) {
		return average(state) >= avgThreshold;
	}
}