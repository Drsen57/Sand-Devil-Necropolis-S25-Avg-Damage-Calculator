package com.sand_devil_calc;

public final class DebuffResistance {
    private DebuffResistance() {
    }

    public static double failureProbability(double accuracy, double resistance) {
        if (!Double.isFinite(accuracy) || accuracy < 0
                || !Double.isFinite(resistance) || resistance < 0) {
            throw new IllegalArgumentException("Accuracy and resistance must be finite and non-negative.");
        }

        double resistanceDifference = (resistance - accuracy) / 100.0;
        if (resistanceDifference >= 0.30) {
            return 0.30 + 0.67 * (1.0 - Math.exp(3.0 * (0.30 - resistanceDifference)));
        }
        return 0.03 + 0.27 * Math.exp(6.0 * (resistanceDifference - 0.30));
    }
}
