package com.sand_devil_calc.legacy;

import java.util.Map;

public record SimulationResult(
        String setupName,
        long trials,
        double baseDamagePerCycle,
        double averageDamagePerCycle,
        double damageStandardDeviation,
        double averageDebuffs,
        Map<Integer, Double> debuffCountProbabilities
) {
    public SimulationResult {
        debuffCountProbabilities = Map.copyOf(debuffCountProbabilities);
    }

    public double averageDamageMultiplier() {
        if (baseDamagePerCycle == 0) {
            return 0;
        }
        return averageDamagePerCycle / baseDamagePerCycle;
    }

    public double damageStandardDeviationMultiplier() {
        if (baseDamagePerCycle == 0) {
            return 0;
        }
        return damageStandardDeviation / baseDamagePerCycle;
    }

    public double probabilityForDebuffCount(int debuffCount) {
        return debuffCountProbabilities.getOrDefault(debuffCount, 0.0);
    }

    public int maximumDebuffsObserved() {
        return debuffCountProbabilities.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }
}
