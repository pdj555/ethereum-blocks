import java.util.Arrays;
import java.util.List;

/**
 * Outlier scores that one extreme value cannot hide behind.
 *
 * A classic z-score divides by the standard deviation, and the outlier being
 * measured inflates that deviation: one block with a huge average fee can pull
 * the deviation up so far that every other unusual block scores under the
 * threshold, and on a small sample the extreme block can mask itself. The
 * modified z-score (Iglewicz and Hoaglin, 1993) centres on the median and scales
 * by the median absolute deviation, neither of which a few extreme values move.
 *
 * score = 0.6745 * (x - median) / MAD, where 0.6745 makes the score comparable
 * to a classic z-score on normal data. The usual cut-off is 3.5. When more than
 * half the values are identical the MAD is zero, and the mean absolute deviation
 * scaled by 1.253314 stands in for it.
 */
final class RobustStats {

    static final double DEFAULT_THRESHOLD = 3.5;
    static final String METHOD = "modified z-score (median and median absolute deviation)";

    private RobustStats() {}

    static double median(double[] values) {
        if (values.length == 0) throw new IllegalArgumentException("no values");
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int mid = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2.0;
    }

    /** Scale for the scores: 1.4826 * MAD, or 1.253314 * mean absolute deviation when MAD is 0. */
    static double scale(double[] values) {
        double center = median(values);
        double[] deviations = new double[values.length];
        double total = 0.0;
        for (int i = 0; i < values.length; i++) {
            deviations[i] = Math.abs(values[i] - center);
            total += deviations[i];
        }
        double mad = median(deviations);
        if (mad > 0) return mad / 0.6745;
        return 1.253314 * total / values.length;
    }

    /** One score per value, in input order; all zero when every value is the same. */
    static double[] modifiedZ(double[] values) {
        double[] scores = new double[values.length];
        if (values.length == 0) return scores;
        double center = median(values);
        double scale = scale(values);
        if (scale == 0) return scores;
        for (int i = 0; i < values.length; i++) scores[i] = (values[i] - center) / scale;
        return scores;
    }

    static double[] toArray(List<? extends Number> values) {
        return values.stream().mapToDouble(Number::doubleValue).toArray();
    }
}
