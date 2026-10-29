package com.sand_devil_calc;

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
        Simulator simulator = new Simulator(new DamageModel(0.15));

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
        System.out.println("Original and refactored simulations match for all fixed-seed trials.");
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

        if (Double.compare(original.averageDamagePerCycle(), refactored.averageDamagePerCycle()) != 0
                || Double.compare(original.averageDebuffs(), refactored.averageDebuffs()) != 0
                || !original.debuffCountProbabilities().equals(
                        refactored.debuffCountProbabilities())) {
            throw new AssertionError("Results differ for setup: " + setupName);
        }
    }

    private static LegacyResult runOriginal(long seed, boolean mithralaUsesToxic) {
        Random random = new Random(seed);
        double totalDamage = 0;
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
            double damageReduction = Math.max(1.0 - 0.15 * baseDebuffs, 0.0);
            double damageMultiplier = 1.0 - damageReduction
                    * Math.max(1.0 - 0.15 * totalToxicDebuffs, 0.0);

            if (!mithralaUsesToxic) {
                damageMultiplier *= 1.0 + 0.05 * mithralaDebuffs;
            }

            totalDamage += 3.0 * damageMultiplier;
            totalDebuffs += totalToxicDebuffs;
            counts.merge(totalToxicDebuffs, 1L, Long::sum);
        }

        Map<Integer, Double> probabilities = new HashMap<>();
        for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
            probabilities.put(entry.getKey(), (double) entry.getValue() / TRIALS);
        }

        return new LegacyResult(
                totalDamage / TRIALS,
                (double) totalDebuffs / TRIALS,
                probabilities
        );
    }

    private record LegacyResult(
            double averageDamagePerCycle,
            double averageDebuffs,
            Map<Integer, Double> debuffCountProbabilities
    ) {
    }
}
