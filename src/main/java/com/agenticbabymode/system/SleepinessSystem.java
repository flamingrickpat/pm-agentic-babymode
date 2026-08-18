package com.agenticbabymode.system;

import com.agenticbabymode.state.PlayerState;

/**
 * Sleepiness bookkeeping. Pure logic, no Minecraft imports, unit-testable.
 *
 * Sleepiness builds up purely over in-game time (there is no idle recovery);
 * the only reset is right-clicking a bed.
 */
public final class SleepinessSystem {
	public static final double MAX = 100.0;
	public static final double MIN = 0.0;
	/** Vanilla day length in ticks. */
	public static final double TICKS_PER_DAY = 24000.0;

	private SleepinessSystem() {
	}

	public static double clamp(double value) {
		return Math.max(MIN, Math.min(MAX, value));
	}

	/** Add sleepiness, clamped to [0,100]. */
	public static void accrue(PlayerState state, double amount) {
		state.sleepiness = clamp(state.sleepiness + amount);
	}

	/** Reset sleepiness to zero (bed interaction). */
	public static void reset(PlayerState state) {
		state.sleepiness = 0.0;
	}

	/** Sleepiness gained per tick so that 100% is reached after fullAfterDays. */
	public static double perTick(double fullAfterDays) {
		return MAX / (fullAfterDays * TICKS_PER_DAY);
	}

	/**
	 * Movement/mining speed factor for a given sleepiness level and max penalty
	 * (percent). At 0 sleepiness the factor is 1.0; at 100 it is 1 - penalty/100.
	 */
	public static double movementFactor(double sleepiness, double maxPenaltyPercent) {
		double penalty = Math.max(0.0, Math.min(100.0, maxPenaltyPercent)) / 100.0;
		return 1.0 - penalty * (clamp(sleepiness) / 100.0);
	}

	/** Band index 0..4 used for status-effect tiers (0 = none). */
	public static int band(double sleepiness) {
		return (int) Math.floor(clamp(sleepiness) / 25.0);
	}
}
