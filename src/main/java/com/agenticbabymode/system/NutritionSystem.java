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

	/** Apply natural decay of all categories over the given number of ticks. */
	public static void decay(PlayerState state, double decayPerDay, long ticks) {
		double perTick = decayPerDay / TICKS_PER_DAY;
		double amount = perTick * ticks;
		state.grain = clamp(state.grain - amount);
		state.protein = clamp(state.protein - amount);
		state.produce = clamp(state.produce - amount);
	}

	public static double average(PlayerState state) {
		return (state.grain + state.protein + state.produce) / 3.0;
	}
}
