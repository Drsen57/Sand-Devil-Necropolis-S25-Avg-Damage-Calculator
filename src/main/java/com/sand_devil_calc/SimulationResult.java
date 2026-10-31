package com.sand_devil_calc;

import java.util.Map;

public record SimulationResult(
        String setupName,
        long trials,
        long successfulTrials,
        long sleepFailedTrials,
        long sleepResistFailedTrials,
        long affinityFailedTrials,
        long damageSampleTrials,
        long bossKilledTrials,
        long damageFailedTrials,
        Map<Integer, Long> bossKillsByTurn,
        long survivedTurnThreeTrials,
        double worstTurnThreeMissPercent,
        int worstTurnThreeDebuffsLanded,
        int worstTurnThreeActiveDebuffs,
        double worstTurnFourMissPercent,
        int worstTurnFourDebuffsLanded,
        int worstTurnFourActiveDebuffs,
        double baseFenaxDamagePerCycle,
        double averageFenaxDamagePerCycle,
        double fenaxDamageStandardDeviation,
        double averageDebuffDamageMultiplier,
        double debuffDamageMultiplierStandardDeviation,
        double averageDebuffs,
        Map<Integer, Double> debuffCountProbabilities,
        Map<TeamPosition, FenaxDamageStatistics> fenaxDamageByPosition,
        Map<Integer, DamageBandStatistics> damageBandStatistics,
        RepresentativeDamageTrials representativeDamageTrials
) {
    public SimulationResult {
        debuffCountProbabilities = Map.copyOf(debuffCountProbabilities);
        bossKillsByTurn = Map.copyOf(bossKillsByTurn);
        fenaxDamageByPosition = Map.copyOf(fenaxDamageByPosition);
        damageBandStatistics = Map.copyOf(damageBandStatistics);
    }

    public double successRate() {
        return (double) successfulTrials / trials;
    }

    public double averageFenaxDamageMultiplier() {
        if (baseFenaxDamagePerCycle == 0) {
            return 0;
        }
        return averageFenaxDamagePerCycle / baseFenaxDamagePerCycle;
    }

    public double fenaxDamageStandardDeviationMultiplier() {
        if (baseFenaxDamagePerCycle == 0) {
            return 0;
        }
        return fenaxDamageStandardDeviation / baseFenaxDamagePerCycle;
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

    public SimulationResult withDamageBandStatistics(
            Map<Integer, DamageBandStatistics> statistics
    ) {
        return new SimulationResult(
                setupName,
                trials,
                successfulTrials,
                sleepFailedTrials,
                sleepResistFailedTrials,
                affinityFailedTrials,
                damageSampleTrials,
                bossKilledTrials,
                damageFailedTrials,
                bossKillsByTurn,
                survivedTurnThreeTrials,
                worstTurnThreeMissPercent,
                worstTurnThreeDebuffsLanded,
                worstTurnThreeActiveDebuffs,
                worstTurnFourMissPercent,
                worstTurnFourDebuffsLanded,
                worstTurnFourActiveDebuffs,
                baseFenaxDamagePerCycle,
                averageFenaxDamagePerCycle,
                fenaxDamageStandardDeviation,
                averageDebuffDamageMultiplier,
                debuffDamageMultiplierStandardDeviation,
                averageDebuffs,
                debuffCountProbabilities,
                fenaxDamageByPosition,
                statistics,
                representativeDamageTrials
        );
    }

    public SimulationResult withDamageBandStatistics(
            Map<Integer, DamageBandStatistics> statistics,
            RepresentativeDamageTrials representatives
    ) {
        return new SimulationResult(
                setupName,
                trials,
                successfulTrials,
                sleepFailedTrials,
                sleepResistFailedTrials,
                affinityFailedTrials,
                damageSampleTrials,
                bossKilledTrials,
                damageFailedTrials,
                bossKillsByTurn,
                survivedTurnThreeTrials,
                worstTurnThreeMissPercent,
                worstTurnThreeDebuffsLanded,
                worstTurnThreeActiveDebuffs,
                worstTurnFourMissPercent,
                worstTurnFourDebuffsLanded,
                worstTurnFourActiveDebuffs,
                baseFenaxDamagePerCycle,
                averageFenaxDamagePerCycle,
                fenaxDamageStandardDeviation,
                averageDebuffDamageMultiplier,
                debuffDamageMultiplierStandardDeviation,
                averageDebuffs,
                debuffCountProbabilities,
                fenaxDamageByPosition,
                statistics,
                representatives
        );
    }

    public record RepresentativeDamageTrials(
            DamageTrialExample medianTrial,
            DamageTrialExample meanTrial
    ) {
    }

    public record DamageTrialExample(
            long trialNumber,
            double totalFenaxDamage,
            double turnOneDamage,
            double turnThreeDamage,
            double turnThreeRemainingHp,
            double remainingBossHp,
            int debuffsLanded,
            int turnThreeActiveDebuffs,
            int finalActiveDebuffs,
            int bossKilledTurn,
            boolean affinityFailed
    ) {
    }

    public record DamageBandStatistics(
            long trialCount,
            long affinityFailedTrials,
            long damageFailedTrials,
            long turnOneWinDistanceSamples,
            double averageTurnOneWinDistancePercent,
            Map<Integer, Long> bossKillsByTurn,
            double worstTurnThreeMissPercent,
            int worstTurnThreeDebuffsLanded,
            int worstTurnThreeActiveDebuffs,
            double worstTurnFourMissPercent,
            int worstTurnFourDebuffsLanded,
            int worstTurnFourActiveDebuffs,
            Map<Integer, DebuffCountStatistics> byDebuffCount
    ) {
        public DamageBandStatistics {
            bossKillsByTurn = Map.copyOf(bossKillsByTurn);
            byDebuffCount = Map.copyOf(byDebuffCount);
        }
    }

    public record DebuffCountStatistics(
            long trialCount,
            long affinityFailedTrials,
            long damageFailedTrials,
            long turnOneWinDistanceSamples,
            double averageTurnOneWinDistancePercent,
            Map<Integer, Long> bossKillsByTurn,
            double worstTurnThreeMissPercent,
            int worstTurnThreeActiveDebuffs,
            double worstTurnFourMissPercent,
            int worstTurnFourActiveDebuffs
    ) {
        public DebuffCountStatistics {
            bossKillsByTurn = Map.copyOf(bossKillsByTurn);
        }
    }

    public record FenaxDamageStatistics(
            String championName,
            double baseDamagePerCycle,
            double averageDamagePerCycle,
            double standardDeviation
    ) {
        public double averageDamageMultiplier() {
            return baseDamagePerCycle == 0 ? 0 : averageDamagePerCycle / baseDamagePerCycle;
        }

        public double standardDeviationMultiplier() {
            return baseDamagePerCycle == 0 ? 0 : standardDeviation / baseDamagePerCycle;
        }
    }
}
