package com.agenticbabymode.system;

/**
 * Combat scaling arithmetic. Pure logic, no Minecraft imports, unit-testable.
 */
public final class CombatMath {
	private CombatMath() {
	}

	public static float scale(float original, double multiplier) {
		return (float) (original * multiplier);
	}

	/**
	 * Scales mob damage dealt to a player. The multiplier shrinks (or grows) the
	 * raw damage amount before armour/enchantment reductions are applied.
	 */
	public static float mobDamageToPlayer(float raw, double damageMultiplier) {
		return scale(raw, damageMultiplier);
	}

	/**
	 * Scales damage dealt by a player to a mob (e.g. "damageTakenMultiplier: 2.0"
	 * means mobs take twice as much).
	 */
	public static float playerDamageToMob(float raw, double damageTakenMultiplier) {
		return scale(raw, damageTakenMultiplier);
	}
}
