import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestRobustStats {

	@Test
	void testMedianOddAndEven() {
		assertEquals(2.0, RobustStats.median(new double[] {3, 1, 2}));
		assertEquals(2.5, RobustStats.median(new double[] {4, 1, 3, 2}));
	}

	@Test
	void testOneExtremeValueCannotHideAnother() {
		// A classic z-score uses the mean and standard deviation, and the block at 100
		// inflates both: the block at 5, twenty times its neighbours' cost spread away
		// from them, scores below zero. The modified z-score flags both.
		double[] costs = {1.0, 1.1, 0.9, 1.0, 1.05, 0.95, 5.0, 100.0};
		double mean = 0.0;
		for (double c : costs) mean += c / costs.length;
		double var = 0.0;
		for (double c : costs) var += (c - mean) * (c - mean) / costs.length;
		double classicAtFive = (5.0 - mean) / Math.sqrt(var);
		assertTrue(classicAtFive < 1.5, "classic score " + classicAtFive);

		double[] scores = RobustStats.modifiedZ(costs);
		assertTrue(scores[6] >= RobustStats.DEFAULT_THRESHOLD, "robust score " + scores[6]);
		assertTrue(scores[7] >= RobustStats.DEFAULT_THRESHOLD);
		for (int i = 0; i < 6; i++) assertTrue(Math.abs(scores[i]) < RobustStats.DEFAULT_THRESHOLD);
	}

	@Test
	void testScoresMatchClassicScaleOnNormalLikeData() {
		// median 0, MAD 1: 0.6745 * x, so a value at 3.5 / 0.6745 sits exactly on the cut-off.
		double[] values = {-2, -1, -1, 0, 0, 0, 1, 1, 2, 3.5 / 0.6745};
		double[] scores = RobustStats.modifiedZ(values);
		assertEquals(3.5, scores[9], 1e-9);
	}

	@Test
	void testZeroMadFallsBackToMeanAbsoluteDeviation() {
		double[] values = {7, 7, 7, 7, 7, 7, 50};
		double[] scores = RobustStats.modifiedZ(values);
		assertTrue(scores[6] > RobustStats.DEFAULT_THRESHOLD);
		assertEquals(0.0, scores[0]);
	}

	@Test
	void testIdenticalValuesScoreZero() {
		assertArrayEquals(new double[] {0, 0, 0}, RobustStats.modifiedZ(new double[] {4, 4, 4}));
	}
}
