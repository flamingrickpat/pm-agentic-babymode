package com.agenticbabymode.state;

/**
 * Per-player mutable state (sleepiness + nutrition) kept in world save state.
 * Plain data class on purpose so it can be unit-tested without Minecraft.
 */
public class PlayerState {
	public double sleepiness;
	public double grain;
	public double protein;
	public double produce;

	/** Fresh player: 0 sleepiness, full nutrition (grain/protein/produce = 100). */
	public static PlayerState defaults() {
		PlayerState ps = new PlayerState();
		ps.sleepiness = 0.0;
		ps.grain = 100.0;
		ps.protein = 100.0;
		ps.produce = 100.0;
		return ps;
	}

	/** Zeroed state (0 sleepiness, 0 nutrition) — useful for tests. */
	public static PlayerState empty() {
		return new PlayerState();
	}
}
