package com.sand_devil_calc;

public record DamageModel(double startingDamageReduction, double reductionPerDebuff) {
    public DamageModel {
        validateProbability("Starting damage reduction", startingDamageReduction);
        validateProbability("Reduction per debuff", reductionPerDebuff);
    }

    public double damageMultiplier(int numberOfDebuffsLanded) {
        if (numberOfDebuffsLanded < 0) {
            throw new IllegalArgumentException("Debuff count must not be negative.");
        }

        double remainingReduction = Math.max(
                1.0 - reductionPerDebuff * numberOfDebuffsLanded,
                0.0
        );
        return 1.0 - startingDamageReduction * remainingReduction * remainingReduction;
    }

    private static void validateProbability(String label, double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(label + " must be between 0 and 1.");
        }
    }
}
