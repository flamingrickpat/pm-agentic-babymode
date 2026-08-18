package com.agenticbabymode.system;

/**
 * Nutrition gained by eating a given item. Immutable value object.
 */
public final class NutritionGain {
	public final double grain;
	public final double protein;
	public final double produce;

	public NutritionGain(double grain, double protein, double produce) {
		this.grain = grain;
		this.protein = protein;
		this.produce = produce;
	}

	public boolean isEmpty() {
		return grain == 0.0 && protein == 0.0 && produce == 0.0;
	}
}
