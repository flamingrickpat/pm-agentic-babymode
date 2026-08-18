package com.agenticbabymode.system;

import com.agenticbabymode.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SleepinessSystemTest {

	@Test
	void reaches100PercentAfterFullAfterDays() {
		PlayerState ps = PlayerState.defaults();
		double perTick = SleepinessSystem.perTick(2.5);
		// 2.5 days = 60000 ticks
		for (int i = 0; i < 60000; i++) {
			SleepinessSystem.accrue(ps, perTick);
		}
		assertEquals(100.0, ps.sleepiness, 1e-6);
	}

	@Test
	void perTickRateIsInverselyProportionalToDays() {
		double slow = SleepinessSystem.perTick(7.0);
		double fast = SleepinessSystem.perTick(2.5);
		assertTrue(slow < fast);
	}

	@Test
	void sleepinessCannotExceed100() {
		PlayerState ps = PlayerState.defaults();
		SleepinessSystem.accrue(ps, 1000.0);
		assertEquals(100.0, ps.sleepiness, 1e-9);
	}

	@Test
	void bedResetsToZero() {
		PlayerState ps = PlayerState.defaults();
		SleepinessSystem.accrue(ps, 73.0);
		SleepinessSystem.reset(ps);
		assertEquals(0.0, ps.sleepiness, 1e-9);
	}

	@Test
	void movementFactorIsOneAtZeroSleepiness() {
		assertEquals(1.0, SleepinessSystem.movementFactor(0.0, 50.0), 1e-9);
	}

	@Test
	void movementFactorIsHalfAtMaxSleepinessWith50PercentPenalty() {
		assertEquals(0.5, SleepinessSystem.movementFactor(100.0, 50.0), 1e-9);
	}

	@Test
	void movementFactorIsLinearBetween() {
		assertEquals(0.75, SleepinessSystem.movementFactor(50.0, 50.0), 1e-9);
	}

	@Test
	void bandingSplitsIntoQuarters() {
		assertEquals(0, SleepinessSystem.band(0.0));
		assertEquals(1, SleepinessSystem.band(25.0));
		assertEquals(2, SleepinessSystem.band(50.0));
		assertEquals(3, SleepinessSystem.band(75.0));
		assertEquals(4, SleepinessSystem.band(100.0));
		assertTrue(SleepinessSystem.band(120.0) <= 4);
	}
}
