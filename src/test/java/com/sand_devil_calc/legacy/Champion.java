package com.sand_devil_calc.legacy;

public record Champion(
        String name,
        boolean enabled,
        int hitsPerCycle,
        double damagePerHit,
        int startingDebuffs,
        double primaryDebuffChance,
        int bonusDebuffsOnPrimaryProc,
        double bonusDebuffChance,
        double oneTimeDebuffProcChancePerHit,
        double oneTimeDebuffLandingChance,
        double toxicProcChancePerHit,
        double feralDamageBonusPerDebuff,
        boolean toxicDebuffsAffectDamageReduction
) {
    public Champion {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Champion name must not be blank.");
        }
        if (hitsPerCycle < 0 || startingDebuffs < 0 || bonusDebuffsOnPrimaryProc < 0) {
            throw new IllegalArgumentException("Hit and debuff counts must not be negative.");
        }
        if (!Double.isFinite(damagePerHit) || damagePerHit < 0) {
            throw new IllegalArgumentException("Damage per hit must be finite and non-negative.");
        }
        validateProbability("Primary debuff chance", primaryDebuffChance);
        validateProbability("Bonus debuff chance", bonusDebuffChance);
        validateProbability("One-time debuff proc chance", oneTimeDebuffProcChancePerHit);
        validateProbability("One-time debuff landing chance", oneTimeDebuffLandingChance);
        validateProbability("Toxic proc chance", toxicProcChancePerHit);
        validateProbability("Feral damage bonus", feralDamageBonusPerDebuff);
    }

    private static void validateProbability(String label, double probability) {
        if (!Double.isFinite(probability) || probability < 0 || probability > 1) {
            throw new IllegalArgumentException(label + " must be between 0 and 1.");
        }
    }
}
