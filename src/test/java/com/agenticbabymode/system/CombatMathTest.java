package com.agenticbabymode.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatMathTest {

	@Test
	void mobDamageMultiplier0_25() {
		assertEquals(2.0f, CombatMath.mobDamageToPlayer(8.0f, 0.25), 1e-6f);
	}

	@Test
	void mobDamageMultiplier0_35() {
		assertEquals(2.8f, CombatMath.mobDamageToPlayer(8.0f, 0.35), 1e-6f);
	}

	@Test
	void damageTakenMultiplier2x() {
		assertEquals(16.0f, CombatMath.playerDamageToMob(8.0f, 2.0), 1e-6f);
	}

	@Test
	void identityWhenMultiplierIsOne() {
		assertEquals(8.0f, CombatMath.scale(8.0f, 1.0), 1e-6f);
	}
}
