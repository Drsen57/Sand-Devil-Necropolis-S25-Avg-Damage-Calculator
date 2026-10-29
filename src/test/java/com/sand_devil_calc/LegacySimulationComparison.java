package com.sand_devil_calc;

import com.sand_devil_calc.legacy.Champion;
import com.sand_devil_calc.legacy.DamageModel;
import com.sand_devil_calc.legacy.GearSet;
import com.sand_devil_calc.legacy.GearSetup;
import com.sand_devil_calc.legacy.SimulationResult;
import com.sand_devil_calc.legacy.Simulator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LegacySimulationComparison {
    private static final long TRIALS = 1_000_000;
    private static final long SEED = 42;

    public static void main(String[] args) {
        Champion hellhound = new Champion(
                "Hellhound", true, 1, 1.0, 1, 0.60, 0, 0.0,
                0.0, 0.0, 0.80, 0.05, true
        );
        Champion mithrala = new Champion(
                "Mithrala", true, 2, 1.0, 0, 0.97, 1, 0.25,
                0.30, 0.97, 0.80, 0.05, false
        );
        Simulator simulator = new Simulator(new DamageModel(0.75, 0.15));

        compareSetup(
                simulator,
                hellhound,
                mithrala,
                "Hellhound Toxic / Mithrala Toxic",
                GearSet.TOXIC
        );
        compareSetup(
                simulator,
                hellhound,
                mithrala,
                "Hellhound Toxic / Mithrala Feral",
                GearSet.FERAL
        );
        System.out.println("Historical legacy simulation matches corrected v1 for all fixed-seed trials.");
    }

    private static void compareSetup(
            Simulator simulator,
            Champion hellhound,
            Champion mithrala,
            String setupName,
            GearSet mithralaGear
    ) {
        GearSetup setup = new GearSetup(setupName, Map.of(
                "Hellhound", GearSet.TOXIC,
                "Mithrala", mithralaGear
        ));
        LegacyResult original = runOriginal(SEED, mithralaGear == GearSet.TOXIC);
        SimulationResult refactored = simulator.run(
                TRIALS,
                List.of(hellhound, mithrala),
                setup,
                new Random(SEED)
        );

        System.out.printf("%s%n", setupName);
        System.out.printf(
                "  Original average damage: %.12f; refactored: %.12f%n",
                original.averageDamagePerCycle(),
                refactored.averageDamagePerCycle()
        );
        System.out.printf(
                "  Original average debuffs: %.12f; refactored: %.12f%n",
                original.averageDebuffs(),
                refactored.averageDebuffs()
        );
        System.out.printf(
                "  Original damage SD: %.12f; refactored: %.12f%n",
                original.damageStandardDeviation(),
                refactored.damageStandardDeviation()
        );

        if (!approximatelyEqual(
                        original.averageDamagePerCycle(),
                        refactored.averageDamagePerCycle())
                || !approximatelyEqual(
                        original.damageStandardDeviation(),
                        refactored.damageStandardDeviation())
                || !approximatelyEqual(
                        original.averageDebuffs(),
                        refactored.averageDebuffs())
                || !original.debuffCountProbabilities().equals(
                        refactored.debuffCountProbabilities())) {
            throw new AssertionError(
                    "Results differ for setup: " + setupName
                            + "\nOriginal distribution: " + original.debuffCountProbabilities()
                            + "\nRefactored distribution: " + refactored.debuffCountProbabilities()
            );
        }
    }

    private static boolean approximatelyEqual(double first, double second) {
        return Math.abs(first - second) <= 1e-12;
    }

    private static LegacyResult runOriginal(long seed, boolean mithralaUsesToxic) {
        Random random = new Random(seed);
        double averageDamage = 0;
        double sumSquaredDamageDeviations = 0;
        long totalDebuffs = 0;
        Map<Integer, Long> counts = new HashMap<>();

        for (long trial = 0; trial < TRIALS; trial++) {
            int hellhoundDebuffs = 1;
            int mithralaDebuffs = 0;
            int mithralaToxicDebuffs = 0;
            boolean mithralaSmiteApplied = false;

            if (random.nextDouble() < 0.60) {
                hellhoundDebuffs++;
            }
            if (random.nextDouble() < 0.80) {
                hellhoundDebuffs++;
            }

            for (int hit = 0; hit < 2; hit++) {
                if (random.nextDouble() < 0.97) {
                    mithralaDebuffs++;
                    if (random.nextDouble() < 0.25) {
                        mithralaDebuffs++;
                    }
                }

                if (random.nextDouble() < 0.30
                        && random.nextDouble() < 0.97
                        && !mithralaSmiteApplied) {
                    mithralaSmiteApplied = true;
                    mithralaDebuffs++;
                }

                if (random.nextDouble() < 0.80) {
                    mithralaToxicDebuffs++;
                }
            }

            int baseDebuffs = hellhoundDebuffs + mithralaDebuffs;
            int totalToxicDebuffs = baseDebuffs
                    + (mithralaUsesToxic ? mithralaToxicDebuffs : 0);
            double damageReduction = 0.75 * Math.max(1.0 - 0.15 * baseDebuffs, 0.0);
            double damageMultiplier = 1.0 - damageReduction
                    * Math.max(1.0 - 0.15 * totalToxicDebuffs, 0.0);

            if (!mithralaUsesToxic) {
                damageMultiplier *= 1.0 + 0.05 * mithralaDebuffs;
            }

            double trialDamage = 3.0 * damageMultiplier;
            double deltaFromAverage = trialDamage - averageDamage;
            averageDamage += deltaFromAverage / (trial + 1);
            sumSquaredDamageDeviations += deltaFromAverage * (trialDamage - averageDamage);
            totalDebuffs += totalToxicDebuffs;
            counts.merge(totalToxicDebuffs, 1L, Long::sum);
        }

        Map<Integer, Double> probabilities = new HashMap<>();
        for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
            probabilities.put(entry.getKey(), (double) entry.getValue() / TRIALS);
        }

        return new LegacyResult(
                averageDamage,
                Math.sqrt(sumSquaredDamageDeviations / TRIALS),
                (double) totalDebuffs / TRIALS,
                probabilities
        );
    }

    private record LegacyResult(
            double averageDamagePerCycle,
            double damageStandardDeviation,
            double averageDebuffs,
            Map<Integer, Double> debuffCountProbabilities
    ) {
    }
}
