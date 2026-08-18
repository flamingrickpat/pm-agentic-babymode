package com.agenticbabymode.system;

import com.agenticbabymode.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FatigueSystemTest {

	@Test
	void sprintingAccruesFatigue() {
		PlayerState ps = PlayerState.defaults();
		// 10 seconds of sprinting at 1.5/sec
		FatigueSystem.accrue(ps, 1.5 * 10.0);
		assertEquals(15.0, ps.fatigue, 1e-9);
	}

	@Test
	void restingRecoversFatigue() {
		PlayerState ps = PlayerState.defaults();
		ps.fatigue = 60.0;
		FatigueSystem.recover(ps, 3.0 * 5.0);
		assertEquals(45.0, ps.fatigue, 1e-9);
	}

	@Test
	void fatigueCannotExceed100() {
		PlayerState ps = PlayerState.defaults();
		FatigueSystem.accrue(ps, 1000.0);
		assertEquals(100.0, ps.fatigue, 1e-9);
	}

	@Test
	void fatigueCannotFallBelow0() {
		PlayerState ps = PlayerState.defaults();
		FatigueSystem.recover(ps, 1000.0);
		assertEquals(0.0, ps.fatigue, 1e-9);
	}

	@Test
	void movementFactorIsOneAtZeroFatigue() {
		assertEquals(1.0, FatigueSystem.movementFactor(0.0, 50.0), 1e-9);
	}

	@Test
	void movementFactorIsHalfAtMaxFatigueWith50PercentPenalty() {
		assertEquals(0.5, FatigueSystem.movementFactor(100.0, 50.0), 1e-9);
	}

	@Test
	void movementFactorIsLinearBetween() {
		assertEquals(0.75, FatigueSystem.movementFactor(50.0, 50.0), 1e-9);
	}

	@Test
	void bandingSplitsIntoQuarters() {
		assertEquals(0, FatigueSystem.band(0.0));
		assertEquals(1, FatigueSystem.band(25.0));
		assertEquals(2, FatigueSystem.band(50.0));
		assertEquals(3, FatigueSystem.band(75.0));
		assertEquals(4, FatigueSystem.band(100.0));
		assertTrue(FatigueSystem.band(120.0) <= 4);
	}
}
