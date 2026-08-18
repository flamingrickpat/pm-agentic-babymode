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

	public static PlayerState defaults() {
		return new PlayerState();
	}
}
