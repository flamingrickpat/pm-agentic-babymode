package com.agenticbabymode.state;

/**
 * Per-player mutable state (fatigue + nutrition) kept in world save state.
 * Plain data class on purpose so it can be unit-tested without Minecraft.
 */
public class PlayerState {
	public double fatigue;
	public double grain;
	public double protein;
	public double produce;
	/** Last applied status-effect band, used to avoid re-applying effects every tick. -1 = none applied. */
	public int fatigueBand = -1;
	/** Last applied nutrition effect band. -1 = none applied. */
	public int nutritionBand = -1;

	public static PlayerState defaults() {
		return new PlayerState();
	}
}
