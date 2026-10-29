package com.sand_devil_calc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Simulator {
    private final DamageModel damageModel;

    public Simulator(DamageModel damageModel) {
        this.damageModel = damageModel;
    }

    public SimulationResult run(
            long trials,
            List<Champion> champions,
            GearSetup setup,
            Random random
    ) {
        if (trials <= 0) {
            throw new IllegalArgumentException("Trial count must be greater than zero.");
        }
        if (champions == null || setup == null || random == null) {
            throw new IllegalArgumentException("Champions, gear setup, and random source are required.");
        }

        List<Champion> enabledChampions = champions.stream()
                .filter(Champion::enabled)
                .toList();
        validateUniqueNames(enabledChampions);

        double baseDamagePerCycle = enabledChampions.stream()
                .mapToDouble(champion -> champion.hitsPerCycle() * champion.damagePerHit())
                .sum();
        double totalDamage = 0;
        long totalDebuffs = 0;
        Map<Integer, Long> debuffCounts = new HashMap<>();

        for (long trial = 0; trial < trials; trial++) {
            int debuffCount = 0;
            int mitigationDebuffCount = 0;
            double feralDamageBonus = 0;

            for (Champion champion : enabledChampions) {
                debuffCount += champion.startingDebuffs();
                mitigationDebuffCount += champion.startingDebuffs();
                int championDebuffs = 0;
                boolean oneTimeDebuffApplied = false;
                GearSet gearSet = setup.gearFor(champion);

                for (int hit = 0; hit < champion.hitsPerCycle(); hit++) {
                    if (random.nextDouble() < champion.primaryDebuffChance()) {
                        debuffCount++;
                        mitigationDebuffCount++;
                        championDebuffs++;

                        if (champion.bonusDebuffsOnPrimaryProc() > 0
                                && champion.bonusDebuffChance() > 0
                                && random.nextDouble() < champion.bonusDebuffChance()) {
                            debuffCount += champion.bonusDebuffsOnPrimaryProc();
                            mitigationDebuffCount += champion.bonusDebuffsOnPrimaryProc();
                            championDebuffs += champion.bonusDebuffsOnPrimaryProc();
                        }
                    }

                    if (champion.oneTimeDebuffProcChancePerHit() > 0) {
                        boolean oneTimeDebuffProcs = random.nextDouble()
                                < champion.oneTimeDebuffProcChancePerHit();
                        boolean oneTimeDebuffLands = oneTimeDebuffProcs
                                && random.nextDouble() < champion.oneTimeDebuffLandingChance();
                        if (oneTimeDebuffLands && !oneTimeDebuffApplied) {
                            debuffCount++;
                            mitigationDebuffCount++;
                            championDebuffs++;
                            oneTimeDebuffApplied = true;
                        }
                    }

                    // Draw for every hit so gear setups use the same proc sequence.
                    double toxicRoll = random.nextDouble();
                    if (gearSet == GearSet.TOXIC && toxicRoll < champion.toxicProcChancePerHit()) {
                        debuffCount++;
                        if (champion.toxicDebuffsAffectDamageReduction()) {
                            mitigationDebuffCount++;
                        }
                    }
                }

                if (gearSet == GearSet.FERAL) {
                    feralDamageBonus += champion.feralDamageBonusPerDebuff() * championDebuffs;
                }
            }

            double damageMultiplier = damageModel.damageMultiplier(
                    mitigationDebuffCount,
                    debuffCount
            );
            double feralMultiplier = 1.0 + feralDamageBonus;

            totalDamage += baseDamagePerCycle * damageMultiplier * feralMultiplier;
            totalDebuffs += debuffCount;
            debuffCounts.merge(debuffCount, 1L, Long::sum);
        }

        Map<Integer, Double> probabilities = new HashMap<>();
        for (Map.Entry<Integer, Long> entry : debuffCounts.entrySet()) {
            probabilities.put(entry.getKey(), (double) entry.getValue() / trials);
        }

        return new SimulationResult(
                setup.name(),
                trials,
                baseDamagePerCycle,
                totalDamage / trials,
                (double) totalDebuffs / trials,
                probabilities
        );
    }

    private static void validateUniqueNames(List<Champion> champions) {
        Set<String> names = new HashSet<>();
        for (Champion champion : champions) {
            if (!names.add(champion.name())) {
                throw new IllegalArgumentException("Champion names must be unique: " + champion.name());
            }
        }
    }
}
