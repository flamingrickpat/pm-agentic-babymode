package com.agenticbabymode.system;

import com.agenticbabymode.state.PlayerState;

/**
 * Fatigue bookkeeping. Pure logic, no Minecraft imports, unit-testable.
 */
public final class FatigueSystem {
	public static final double MAX = 100.0;
	public static final double MIN = 0.0;

	private FatigueSystem() {
	}

	public static double clamp(double value) {
		return Math.max(MIN, Math.min(MAX, value));
	}

	/** Add fatigue from an activity, clamped to [0,100]. */
	public static void accrue(PlayerState state, double amount) {
		state.fatigue = clamp(state.fatigue + amount);
	}

	/** Recover fatigue, clamped to [0,100]. */
	public static void recover(PlayerState state, double amount) {
		state.fatigue = clamp(state.fatigue - amount);
	}

	/**
	 * Movement speed factor for a given fatigue level and max penalty (percent).
	 * At 0 fatigue the factor is 1.0; at 100 fatigue it is 1 - penalty/100.
	 */
	public static double movementFactor(double fatigue, double maxPenaltyPercent) {
		double penalty = Math.max(0.0, Math.min(100.0, maxPenaltyPercent)) / 100.0;
		return 1.0 - penalty * (clamp(fatigue) / 100.0);
	}

	/** Band index 0..4 used for status-effect tiers (0 = none). */
	public static int band(double fatigue) {
		return (int) Math.floor(clamp(fatigue) / 25.0);
	}
}
