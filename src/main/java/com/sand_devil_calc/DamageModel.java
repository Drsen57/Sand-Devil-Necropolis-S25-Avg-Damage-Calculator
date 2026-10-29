package com.sand_devil_calc;

public record DamageModel(double reductionPerDebuff) {
    public DamageModel {
        validateProbability("Reduction per debuff", reductionPerDebuff);
    }

    public double damageMultiplier(int mitigationDebuffCount, int totalDebuffCount) {
        if (mitigationDebuffCount < 0 || totalDebuffCount < mitigationDebuffCount) {
            throw new IllegalArgumentException("Debuff count must not be negative.");
        }

        double mitigationBeforeDamage = Math.max(
                1.0 - reductionPerDebuff * mitigationDebuffCount,
                0.0
        );
        double mitigationAfterDamage = Math.max(
                1.0 - reductionPerDebuff * totalDebuffCount,
                0.0
        );
        return 1.0 - mitigationBeforeDamage * mitigationAfterDamage;
    }

    private static void validateProbability(String label, double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(label + " must be between 0 and 1.");
        }
    }
}
